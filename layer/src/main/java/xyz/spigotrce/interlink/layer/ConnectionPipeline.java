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
 * <p>Thread safety: each {@link #read()} / {@link #write(byte[])} runs its whole layer chain
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

  /**
   * Creates a new pipeline wired to the given transport endpoints.
   *
   * @param reader the source of raw frames from the wire
   * @param writer the sink for raw frames to the wire
   */
  public ConnectionPipeline(final WireReader reader, final WireWriter writer) {
    this.reader = reader;
    this.writer = writer;
  }

  /**
   * Adds a layer at the wire end of the pipeline. Inbound data reaches this layer first and
   * outbound data reaches it last.
   *
   * @param name the unique name to register the layer under
   * @param layer the layer to add
   */
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

  /**
   * Adds a layer at the application end of the pipeline. Outbound data reaches this layer first
   * and inbound data reaches it last.
   *
   * @param name the unique name to register the layer under
   * @param layer the layer to add
   */
  public void addLast(final String name, final Layer layer) {
    add(name, layer, layers.size());
  }

  /**
   * Adds a layer directly before an existing layer located by name.
   *
   * @param name the unique name to register the new layer under
   * @param target the name of the existing layer to insert before
   * @param layer the layer to add
   */
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

  /**
   * Adds a layer directly after an existing layer located by name.
   *
   * @param name the unique name to register the new layer under
   * @param target the name of the existing layer to insert after
   * @param layer the layer to add
   */
  public void addAfter(final String name, final String target, final Layer layer) {
    add(name, layer, indexOf(target) + 1);
  }

  /**
   * Removes the layer registered under the given name from the pipeline.
   *
   * @param name the name of the layer to remove
   */
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
   *
   * @return the pipeline-wide shared state store
   */
  public Map<String, Object> sharedState() {
    return sharedState;
  }

  /**
   * Reads one message from the wire and runs the inbound chain over it. Returns the resulting
   * application bytes, or {@code null} if the chain produced none.
   *
   * @return the application message produced by the inbound chain, or {@code null} if none
   * @throws IOException if reading from the wire or inbound processing fails
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
   *
   * @param data the application message to run through the outbound chain
   * @throws IOException if outbound processing or writing to the wire fails
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
    /**
     * Reads one frame from the wire.
     *
     * @return the raw frame bytes read from the wire
     * @throws IOException if reading from the wire fails
     */
    byte[] read() throws IOException;
  }

  /** Writes one frame to the wire. Thrown exceptions propagate as {@link IOException}. */
  @FunctionalInterface
  public interface WireWriter {
    /**
     * Writes one frame to the wire.
     *
     * @param data the raw frame bytes to write
     * @throws IOException if writing to the wire fails
     */
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
