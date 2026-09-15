package io.github.spigotrce.interlink.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpTransport implements Transport<TcpTransport> {
  private ServerSocket serverSocket;
  private Socket socket;

  private DataOutputStream out;
  private DataInputStream in;

  public TcpTransport() {}

  private TcpTransport(final Socket socket) throws IOException {
    this.socket = socket;
    out = new DataOutputStream(socket.getOutputStream());
    in = new DataInputStream(socket.getInputStream());
  }

  @Override
  public void send(final byte[] data) throws IOException {
    out.writeInt(data.length);
    out.write(data);
    out.flush();
  }

  @Override
  public byte[] receive() throws IOException {
    final int len = in.readInt();
    final byte[] buf = new byte[len];
    in.readFully(buf);
    return buf;
  }

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

  @Override
  public boolean isOpen() {
    return socket != null && !socket.isClosed();
  }

  @Override
  public void bind(final String host, final int port) throws IOException {
    if (serverSocket != null) {
      throw new IOException("Transport is already bound");
    }
    serverSocket = new ServerSocket();
    serverSocket.bind(new InetSocketAddress(host, port));
  }

  @Override
  public TcpTransport accept() throws IOException {
    return new TcpTransport(serverSocket.accept());
  }

  @Override
  public TcpTransport connect(final String host, final int port) throws IOException {
    socket = new Socket(host, port);
    out = new DataOutputStream(socket.getOutputStream());
    in = new DataInputStream(socket.getInputStream());
    return this;
  }

  public Socket getSocket() {
    return socket;
  }

  public DataOutputStream getOut() {
    return out;
  }

  public DataInputStream getIn() {
    return in;
  }

  @Override
  public String getHostname() {
    if (socket != null && socket.getRemoteSocketAddress() instanceof final InetSocketAddress addr) {
      return addr.getHostString();
    }
    return null;
  }

  @Override
  public int getPort() {
    if (socket != null && socket.getRemoteSocketAddress() instanceof final InetSocketAddress addr) {
      return addr.getPort();
    }
    return -1;
  }
}
