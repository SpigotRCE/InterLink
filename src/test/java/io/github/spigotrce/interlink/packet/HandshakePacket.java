package io.github.spigotrce.interlink.packet;

import io.github.spigotrce.interlink.buf.*;

public record HandshakePacket(String username) implements Packet<HandshakePacket> {
  public static final PacketCodec<HandshakePacket> CODEC =
      PacketCodec.of(HandshakePacket::new, HandshakePacket::write);

  public HandshakePacket(final InputBuffer buffer) {
    this(buffer.readUTF());
  }

  public void write(final OutputBuffer buffer) {
    buffer.writeUTF(username);
  }

  @Override
  public PacketCodec<HandshakePacket> getCodec() {
    return null;
  }
}
