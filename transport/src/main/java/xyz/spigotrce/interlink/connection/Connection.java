package xyz.spigotrce.interlink.connection;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;
import xyz.spigotrce.interlink.layer.ConnectionPipeline;
import xyz.spigotrce.interlink.packet.Packet;
import xyz.spigotrce.interlink.packet.PacketRegistry;
import java.io.EOFException;
import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * A high-level connection wrapping a {@link Transport}.
 *
 * <p>A {@link Connection} combines a {@link Transport} with a {@link ConnectionPipeline} and a
 * {@link PacketRegistry}. Outbound {@link Packet}s are resolved to their registered id, encoded by
 * the registry, prefixed with that id and written through the pipeline; inbound frames are read
 * through the pipeline, validated against {@link #MAX_FRAME_LENGTH}, decoded by the registry and
 * returned as {@link Packet}s. Errors are reported through the {@code onException} callback, after
 * which the connection is marked as disconnected.
 *
 * @param <T> the underlying transport type
 */
public class Connection<T extends Transport<T>> {
  /**
   * The maximum length in bytes of a single received frame. Longer frames are rejected with an
   * exception.
   */
  public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024; // 8 mb

  private final T transport;
  private final ConnectionPipeline pipeline;
  private final BiConsumer<Connection<T>, Throwable> onException;

  private PacketRegistry<?> registry;
  private volatile boolean disconnected = false;
  private volatile boolean handlingException = false;

  /**
   * Creates a new {@link Connection} wrapping the given transport and error callback.
   *
   * @param transport the transport backing this connection
   * @param onException the callback invoked when an exception occurs
   */
  public Connection(
      final T transport, final BiConsumer<Connection<T>, Throwable> onException) {
    this.transport = transport;
    this.onException = onException;
    pipeline = new ConnectionPipeline(transport::receive, transport::send);
  }

  /**
   * Encodes the given packet and writes it to the transport.
   *
   * <p>If the connection is disconnected this is a no-op. If the packet has no registered id or an
   * error occurs while encoding or writing, the exception is reported through the {@code
   * onException} callback.
   *
   * @param packet the packet to send
   */
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

  /**
   * Reads and decodes the next packet from the transport.
   *
   * <p>Returns {@code null} if the connection is disconnected, if the pipeline produced no data, or
   * if an error occurs while reading or decoding.
   *
   * @return the decoded packet, or {@code null}
   */
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

  /**
   * Returns the underlying {@link Transport}.
   *
   * @return the transport
   */
  public T getTransport() {
    return transport;
  }

  /**
   * Returns the {@link ConnectionPipeline} used for frame processing.
   *
   * @return the pipeline
   */
  public ConnectionPipeline getPipeline() {
    return pipeline;
  }

  /**
   * Returns the {@link PacketRegistry} used to encode and decode packets.
   *
   * @return the registry, or {@code null} if none has been set
   */
  public PacketRegistry<?> getRegistry() {
    return registry;
  }

  /**
   * Sets the {@link PacketRegistry} used to encode and decode packets.
   *
   * @param registry the registry to use
   */
  public void setRegistry(final PacketRegistry<?> registry) {
    this.registry = registry;
  }

  /**
   * Returns the callback invoked when an exception occurs.
   *
   * @return the exception callback
   */
  public BiConsumer<Connection<T>, Throwable> getOnException() {
    return onException;
  }

  /**
   * Returns whether this connection has been disconnected.
   *
   * @return {@code true} if the connection is disconnected
   */
  public boolean isDisconnected() {
    return disconnected;
  }

  /**
   * Sets the disconnected state of this connection.
   *
   * @param disconnected the new disconnected state
   */
  public void setDisconnected(final boolean disconnected) {
    this.disconnected = disconnected;
  }

  /**
   * Closes the underlying transport and marks this connection as disconnected.
   *
   * <p>If closing the transport fails, the exception is reported to the {@code onException}
   * callback.
   */
  public void close() {
    try {
      transport.close();
    } catch (final IOException e) {
      onException.accept(this, e);
    }
    disconnected = true;
  }
}
