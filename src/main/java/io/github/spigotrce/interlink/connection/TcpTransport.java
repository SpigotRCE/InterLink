package io.github.spigotrce.interlink.connection;

import java.io.*;
import java.net.Socket;

public class TcpTransport implements Transport {
  private final Socket socket;
  private final DataOutputStream out;
  private final DataInputStream in;

  public TcpTransport(Socket socket) throws IOException {
    this.socket = socket;
    this.out = new DataOutputStream(socket.getOutputStream());
    this.in = new DataInputStream(socket.getInputStream());
  }

  @Override
  public void send(byte[] data) throws IOException {
    out.writeInt(data.length);
    out.write(data);
    out.flush();
  }

  @Override
  public byte[] receive() throws IOException {
    int len = in.readInt();
    byte[] buf = new byte[len];
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
