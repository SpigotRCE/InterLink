package io.github.spigotrce.interlink.connection;

import java.io.*;
import java.net.Socket;

public class TcpTransport implements Transport {
  private final Socket socket;
  private final DataOutputStream out;
  private final DataInputStream in;

  public TcpTransport(final Socket socket) throws IOException {
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
    in.close();
    out.close();
    socket.close();
  }

  @Override
  public boolean isOpen() {
    return !socket.isClosed();
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
}
