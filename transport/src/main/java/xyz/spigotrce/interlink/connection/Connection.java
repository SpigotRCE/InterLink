package xyz.spigotrce.interlink.connection;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;
import xyz.spigotrce.interlink.layer.ConnectionPipeline;
import xyz.spigotrce.interlink.packet.Packet;
import xyz.spigotrce.interlink.packet.PacketRegistry;
import java.io.EOFException;
import java.io.IOException;
import java.util.function.BiConsumer;

public class Connection<T extends Transport<T>> {
  public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024; // 8 mb

  private final T transport;
  private final ConnectionPipeline pipeline;
  private final BiConsumer<Connection<T>, Throwable> onException;

  private PacketRegistry registry;
  private volatile boolean disconnected = false;
  private volatile boolean handlingException = false;

  public Connection(
      final T transport, final BiConsumer<Connection<T>, Throwable> onException) {
    this.transport = transport;
    this.onException = onException;
    pipeline = new ConnectionPipeline(transport::receive, transport::send);
  }

  public void send(final Packet<?> packet) {
    if (disconnected) {
      return;
    }
    try {
      final OutputBuffer out = OutputBuffer.create();
      final int id = registry.getId(packet);
      if (id == -1) {
        throw new IOException("Unregistered packet: " + packet.getClass());
      }
      out.writeInt(id);
      registry.encode(packet, out);
      pipeline.write(out.toByteArray());
    } catch (final Exception e) {
      handleException(e);
    }
  }

  public Packet<?> read() {
    if (disconnected) {
      return null;
    }
    try {
      final byte[] data = pipeline.read();
      if (data == null) {
        return null;
      }
      if (data.length > MAX_FRAME_LENGTH) {
        throw new IOException("Message length out of bounds: " + data.length);
      }

      final InputBuffer in = InputBuffer.create(data);
      final int id = in.readInt();
      return registry.decode(id, in);
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  private void handleException(final Exception e) {
    if (handlingException || !transport.isOpen() || e instanceof EOFException) {
      disconnected = true;
      return;
    }
    handlingException = true;
    try {
      onException.accept(this, e);
    } finally {
      handlingException = false;
      disconnected = true;
    }
  }

  public T getTransport() {
    return transport;
  }

  public ConnectionPipeline getPipeline() {
    return pipeline;
  }

  public PacketRegistry getRegistry() {
    return registry;
  }

  public void setRegistry(final PacketRegistry registry) {
    this.registry = registry;
  }

  public BiConsumer<Connection<T>, Throwable> getOnException() {
    return onException;
  }

  public boolean isDisconnected() {
    return disconnected;
  }

  public void setDisconnected(final boolean disconnected) {
    this.disconnected = disconnected;
  }

  public void close() {
    try {
      transport.close();
    } catch (final IOException e) {
      onException.accept(this, e);
    }
    disconnected = true;
  }
}
