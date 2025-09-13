package io.github.spigotrce.interlink.packet;

import io.github.spigotrce.interlink.buf.*;

public record DisconnectPacket(String message) implements Packet<DisconnectPacket> {
  public static final PacketCodec<DisconnectPacket> CODEC = PacketCodec.of(DisconnectPacket::new, DisconnectPacket::write);

  public DisconnectPacket(InputBuffer buffer) {
    this(buffer.readUTF());
  }

  public void write(OutputBuffer buffer) {
    buffer.writeUTF(message);
  }

  @Override public PacketCodec<DisconnectPacket> getCodec() {
    return null;
  }
}
