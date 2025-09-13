package io.github.spigotrce.interlink.packet;

import io.github.spigotrce.interlink.buf.*;

public record ChatPacket(String message) implements Packet<ChatPacket> {
  public static final PacketCodec<ChatPacket> CODEC = PacketCodec.of(ChatPacket::new, ChatPacket::write);

  public ChatPacket(InputBuffer buffer) {
    this(buffer.readUTF());
  }

  public void write(OutputBuffer buffer) {
    buffer.writeUTF(message);
  }

  @Override public PacketCodec<ChatPacket> getCodec() {
    return null;
  }
}
