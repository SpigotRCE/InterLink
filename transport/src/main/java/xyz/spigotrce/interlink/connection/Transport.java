package xyz.spigotrce.interlink.connection;

import java.io.IOException;

/**
 * Abstraction over a raw, frame-oriented network connection between two peers.
 *
 * <p>A transport can be bound to accept incoming connections via {@link #bind(String, int)} and
 * {@link #accept()} or opened to a remote endpoint via {@link #connect(String, int)}. Once
 * connected it exchanges raw byte frames with {@link #send(byte[])} and {@link #receive()} and is
 * managed through {@link #close()} and {@link #isOpen()}.
 *
 * @param <T> the transport implementation type
 */
public interface Transport<T extends Transport<T>> {
  /**
   * Sends one raw data frame to the remote peer.
   *
   * @param data the frame bytes to send
   * @throws IOException if the frame cannot be written
   */
  void send(final byte[] data) throws IOException;

  /**
   * Reads one raw data frame from the remote peer, blocking until a frame is available.
   *
   * @return the received frame bytes
   * @throws IOException if a frame cannot be read
   */
  byte[] receive() throws IOException;

  /**
   * Closes the transport and releases all underlying resources.
   *
   * @throws IOException if closing fails
   */
  void close() throws IOException;

  /**
   * Returns whether this transport is currently open (connected or listening).
   *
   * @return {@code true} if the transport is open
   */
  boolean isOpen();

  /**
   * Binds this transport so it accepts incoming connections on the given host and port.
   *
   * @param host the local host to bind to
   * @param port the local port to bind to
   * @throws IOException if binding fails
   */
  void bind(final String host, final int port) throws IOException;

  /**
   * Accepts one incoming connection, blocking until a peer connects.
   *
   * @return a transport for the accepted connection
   * @throws IOException if accepting fails
   */
  T accept() throws IOException;

  /**
   * Opens a connection to a remote endpoint.
   *
   * @param host the remote host
   * @param port the remote port
   * @return the transport for the established connection
   * @throws IOException if connecting fails
   */
  T connect(final String host, final int port) throws IOException;

  /**
   * Returns the remote hostname of the current connection.
   *
   * @return the remote hostname, or {@code null} if not connected
   */
  String getHostname();

  /**
   * Returns the remote port of the current connection.
   *
   * @return the remote port, or {@code -1} if not connected
   */
  int getPort();
}
