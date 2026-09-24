package xyz.spigotrce.interlink.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * {@link Transport} implementation backed by a plain TCP socket.
 *
 * <p>Frames are length-prefixed: {@link #send(byte[])} writes the frame length as an {@code int}
 * followed by the frame bytes through a {@link DataOutputStream}, and {@link #receive()} mirrors
 * that on a {@link DataInputStream}. It supports both the server side ({@link #bind(String, int)}
 * and {@link #accept()}) and the client side ({@link #connect(String, int)}).
 */
public class TcpTransport implements Transport<TcpTransport> {
  private ServerSocket serverSocket;
  private Socket socket;

  private DataOutputStream out;
  private DataInputStream in;

  /**
   * Creates a new, unbound and unconnected TCP transport.
   */
  public TcpTransport() {}

  private TcpTransport(final Socket socket) throws IOException {
    this.socket = socket;
    out = new DataOutputStream(socket.getOutputStream());
    in = new DataInputStream(socket.getInputStream());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Writes the frame length as an {@code int} followed by the frame bytes and flushes.
   */
  @Override
  public synchronized void send(final byte[] data) throws IOException {
    out.writeInt(data.length);
    out.write(data);
    out.flush();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Reads the frame length as an {@code int} and then reads exactly that many bytes.
   */
  @Override
  public byte[] receive() throws IOException {
    final int len = in.readInt();
    final byte[] buf = new byte[len];
    in.readFully(buf);
    return buf;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Closes the input stream, output stream, socket and server socket, when present.
   */
  @Override
  public void close() throws IOException {
    if (in != null) {
      in.close();
    }
    if (out != null) {
      out.close();
    }
    if (socket != null) {
      socket.close();
    }
    if (serverSocket != null) {
      serverSocket.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns {@code true} only while a connected socket is present and not closed.
   */
  @Override
  public boolean isOpen() {
    return socket != null && !socket.isClosed();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Binds a fresh {@link ServerSocket} to the given address.
   *
   * @throws IOException if the transport is already bound or binding fails
   */
  @Override
  public void bind(final String host, final int port) throws IOException {
    if (serverSocket != null) {
      throw new IOException("Transport is already bound");
    }
    serverSocket = new ServerSocket();
    serverSocket.bind(new InetSocketAddress(host, port));
  }

  /**
   * {@inheritDoc}
   *
   * @return the accepted socket wrapped in a new {@link TcpTransport}
   * @throws IOException if accepting fails
   */
  @Override
  public TcpTransport accept() throws IOException {
    return new TcpTransport(serverSocket.accept());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Opens a {@link Socket} to the given host and port.
   *
   * @return this transport, now connected to the remote endpoint
   * @throws IOException if connecting fails
   */
  @Override
  public TcpTransport connect(final String host, final int port) throws IOException {
    socket = new Socket(host, port);
    out = new DataOutputStream(socket.getOutputStream());
    in = new DataInputStream(socket.getInputStream());
    return this;
  }

  /**
   * Returns the underlying {@link Socket}, or {@code null} if not connected.
   *
   * @return the socket
   */
  public Socket getSocket() {
    return socket;
  }

  /**
   * Returns the {@link DataOutputStream} used to write frames, or {@code null} if not connected.
   *
   * @return the output stream
   */
  public DataOutputStream getOut() {
    return out;
  }

  /**
   * Returns the {@link DataInputStream} used to read frames, or {@code null} if not connected.
   *
   * @return the input stream
   */
  public DataInputStream getIn() {
    return in;
  }

  /**
   * {@inheritDoc}
   *
   * @return the remote hostname, or {@code null} if not connected
   */
  @Override
  public String getHostname() {
    if (socket != null && socket.getRemoteSocketAddress() instanceof final InetSocketAddress addr) {
      return addr.getHostString();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @return the remote port, or {@code -1} if not connected
   */
  @Override
  public int getPort() {
    if (socket != null && socket.getRemoteSocketAddress() instanceof final InetSocketAddress addr) {
      return addr.getPort();
    }
    return -1;
  }
}
