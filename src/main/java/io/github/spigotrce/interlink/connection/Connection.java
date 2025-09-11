package io.github.spigotrce.interlink.connection;

import io.github.spigotrce.interlink.buf.*;
import io.github.spigotrce.interlink.compression.ZLibCompressor;
import io.github.spigotrce.interlink.packet.*;

import java.io.*;
import java.net.Socket;

public class Connection {
  private final Socket socket;
  private final PacketRegistry registry;
  private final DataOutputStream output;
  private final DataInputStream input;
  private final int compressionThreshold;

  public Connection(Socket socket, PacketRegistry registry, int compressionThreshold) throws IOException {
    this.socket = socket;
    this.registry = registry;
    this.output = new DataOutputStream(socket.getOutputStream());
    this.input = new DataInputStream(socket.getInputStream());
    this.compressionThreshold = compressionThreshold;
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

    boolean compressed = data.length >= compressionThreshold;
    if (compressed) {
      data = ZLibCompressor.compress(data);
    }

    // [compressed flag][length][data]
    output.writeBoolean(compressed);
    output.writeInt(data.length);
    output.write(data);
    output.flush();
  }

  public Packet<?> read() throws IOException {
    // [compressed flag][length][data]
    boolean compressed = input.readBoolean();
    int length = input.readInt();

    byte[] data = new byte[length];
    input.readFully(data);

    if (compressed) {
      data = ZLibCompressor.decompress(data);
    }

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
