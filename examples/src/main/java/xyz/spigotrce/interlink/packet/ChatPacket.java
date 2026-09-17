package xyz.spigotrce.interlink.packet;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;

public record ChatPacket(String message) implements Packet<ChatPacket> {
  public static final PacketCodec<ChatPacket> CODEC =
      PacketCodec.of(ChatPacket::new, ChatPacket::write);

  public ChatPacket(final InputBuffer buffer) {
    this(buffer.readUTF());
  }

  public void write(final OutputBuffer buffer) {
    buffer.writeUTF(message);
  }

  @Override
  public PacketCodec<ChatPacket> getCodec() {
    return CODEC;
  }
}
