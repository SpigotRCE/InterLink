package io.github.spigotrce.interlink.connection;

import com.google.common.io.*;
import io.github.spigotrce.interlink.buf.*;
import io.github.spigotrce.interlink.packet.*;

import java.io.*;
import java.net.Socket;

public class Connection {
  private static final int GCM_TAG_LENGTH = 128; // bits
  private static final int GCM_NONCE_LENGTH = 12; // bytes

  private final Socket socket;
  private final PacketRegistry registry;
  private final DataOutputStream output;
  private final DataInputStream input;

  public Connection(Socket socket, PacketRegistry registry) throws IOException {
    this.socket = socket;
    this.registry = registry;
    this.output = new DataOutputStream(socket.getOutputStream());
    this.input = new DataInputStream(socket.getInputStream());
  }

  public void send(Packet<?> packet) throws IOException {
    OutputBuffer out = OutputBuffer.create();
    int id = registry.getId(packet);
    if (id == -1) {
      throw new IOException("Unregistered packet: " + packet.getClass());
    }
    out.writeInt(id);
    registry.encode(packet, out);
    byte[] data = out.toByteArray();
    output.writeInt(data.length);
    output.write(data);
    output.flush();
  }

  public Packet<?> read() throws IOException {
    int length = input.readInt();
    byte[] data = new byte[length];
    input.readFully(data);
    InputBuffer in = InputBuffer.create(data);
    int id = in.readInt();
    return registry.decode(id, in);
  }

  public Socket getSocket() {
    return socket;
  }

  public void close() throws IOException {
    input.close();
    output.close();
    socket.close();
  }
}
