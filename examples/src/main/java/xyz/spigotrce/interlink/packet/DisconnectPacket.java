package xyz.spigotrce.interlink.packet;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;

public record DisconnectPacket(String message) implements Packet<DisconnectPacket> {
  public static final PacketCodec<DisconnectPacket> CODEC =
      PacketCodec.of(DisconnectPacket::new, DisconnectPacket::write);

  public DisconnectPacket(final InputBuffer buffer) {
    this(buffer.readUTF());
  }

  public void write(final OutputBuffer buffer) {
    buffer.writeUTF(message);
  }

  @Override
  public PacketCodec<DisconnectPacket> getCodec() {
    return CODEC;
  }
}
