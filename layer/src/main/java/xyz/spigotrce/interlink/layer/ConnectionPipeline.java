package xyz.spigotrce.interlink.layer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An ordered chain of {@link Layer}s attached to a connection.
 *
 * <p>The layer list is ordered from the wire end (index {@code 0}) to the application end (last
 * index). Outbound data, entering at the application end, flows through layers in reverse index
 * order toward the transport; inbound data, entering at the wire end, flows through layers in
 * forward index order back toward the application.
 *
 * <p>Ordering contract: to compose the premade compression and encryption layers as compressed
 * <em>then</em> encrypted on the wire, encryption must sit wire-ward of compression. Add them so
 * that the list reads {@code [encryption, compression]} - for example:
 *
 * <pre>{@code
 * pipeline.addFirst("encryption", new EncryptionLayer(key));
 * pipeline.addLast("compression", new CompressionLayer());
 * }</pre>
 * <p>
 * Outbound data is then compressed before encryption and inbound data is decrypted before
 * decompression. Encrypted bytes are indistinguishable from random noise and will not compress,
 * which is exactly why compression must always run application-ward of encryption.
 *
 * <p>Thread safety: each {@link #read()} / {@link #write()} runs its whole layer chain
 * synchronously on the calling thread, so no layer ever receives data on its own thread. A pipeline
 * may therefore be shared between threads as long as every {@link Layer} it contains is itself safe
 * for concurrent use. Adding or removing layers at runtime is thread-safe thanks to a copy-on-write
 * list, but reconfiguring the chain while traffic is actively flowing is not supported.
 */
public class ConnectionPipeline {

  private final WireReader reader;
  private final WireWriter writer;
  private final List<LayerEntry> layers = new CopyOnWriteArrayList<>();
  private final Map<String, Object> sharedState = new HashMap<>();
  private final BlockingQueue<byte[]> inboundQueue = new LinkedBlockingQueue<>();
  public ConnectionPipeline(final WireReader reader, final WireWriter writer) {
    this.reader = reader;
    this.writer = writer;
  }

  public void addFirst(final String name, final Layer layer) {
    add(name, layer, 0);
  }

  private void add(final String name, final Layer layer, final int index) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (layer == null) {
      throw new NullPointerException("layer");
    }
    for (final LayerEntry entry : layers) {
      if (entry.name().equals(name)) {
        throw new IllegalArgumentException("Duplicate layer name: " + name);
      }
    }
    layers.add(index, new LayerEntry(name, layer));
  }

  public void addLast(final String name, final Layer layer) {
    add(name, layer, layers.size());
  }

  public void addBefore(final String name, final String target, final Layer layer) {
    add(name, layer, indexOf(target));
  }

  private int indexOf(final String name) {
    for (int i = 0; i < layers.size(); i++) {
      if (layers.get(i).name().equals(name)) {
        return i;
      }
    }
    throw new IllegalArgumentException("No layer named: " + name);
  }

  public void addAfter(final String name, final String target, final Layer layer) {
    add(name, layer, indexOf(target) + 1);
  }

  public void remove(final String name) {
    for (int i = 0; i < layers.size(); i++) {
      if (layers.get(i).name().equals(name)) {
        layers.remove(i);
        return;
      }
    }
    throw new IllegalArgumentException("No layer named: " + name);
  }

  /**
   * The pipeline-wide shared state store. Layers read and write values here to coordinate without
   * holding direct references to each other, e.g. a handshake layer stashing a negotiated key for a
   * later layer to consume. Not thread-safe; write during setup only.
   */
  public Map<String, Object> sharedState() {
    return sharedState;
  }

  /**
   * Reads one message from the wire and runs the inbound chain over it. Returns the resulting
   * application bytes, or {@code null} if the chain produced none.
   */
  public byte[] read() throws IOException {
    final byte[] queued = inboundQueue.poll();
    if (queued != null) {
      return queued;
    }
    final byte[] raw = reader.read();
    try {
      fireInboundChain(0, raw);
    } catch (final LayerException e) {
      throw new IOException("Inbound processing failed", e);
    }
    return inboundQueue.poll();
  }

  private void fireInboundChain(final int index, final byte[] data) throws LayerException {
    if (index >= layers.size()) {
      inboundQueue.offer(data);
      return;
    }
    final LayerEntry entry = layers.get(index);
    entry.layer().onInbound(data, new Context(index, entry.name()));
  }

  /**
   * Runs the outbound chain over one application message and sends every frame the chain produces
   * to the transport.
   */
  public void write(final byte[] data) throws IOException {
    try {
      fireOutboundChain(layers.size() - 1, data);
    } catch (final LayerException e) {
      throw new IOException("Outbound processing failed", e);
    }
  }

  private void fireOutboundChain(final int index, final byte[] data) throws LayerException {
    if (index < 0) {
      try {
        writer.write(data);
      } catch (final IOException e) {
        throw new LayerException(e);
      }
      return;
    }
    final LayerEntry entry = layers.get(index);
    entry.layer().onOutbound(data, new Context(index, entry.name()));
  }

  /** Reads one frame from the wire. Thrown exceptions propagate as {@link IOException}. */
  @FunctionalInterface
  public interface WireReader {
    byte[] read() throws IOException;
  }

  /** Writes one frame to the wire. Thrown exceptions propagate as {@link IOException}. */
  @FunctionalInterface
  public interface WireWriter {
    void write(byte[] data) throws IOException;
  }

  private record LayerEntry(String name, Layer layer) {}

  private final class Context implements LayerContext {
    private final int index;
    private final String name;

    private Context(final int index, final String name) {
      this.index = index;
      this.name = name;
    }

    @Override
    public void fireInbound(final byte[] data) throws LayerException {
      fireInboundChain(index + 1, data);
    }

    @Override
    public void fireOutbound(final byte[] data) throws LayerException {
      fireOutboundChain(index - 1, data);
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Map<String, Object> sharedState() {
      return sharedState;
    }
  }
}
