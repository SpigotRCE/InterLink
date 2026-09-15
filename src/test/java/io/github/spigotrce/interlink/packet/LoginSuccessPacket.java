package io.github.spigotrce.interlink.packet;

import io.github.spigotrce.interlink.buf.InputBuffer;
import io.github.spigotrce.interlink.buf.OutputBuffer;

public record LoginSuccessPacket(int compressionThreshold) implements Packet<LoginSuccessPacket> {
  public static final PacketCodec<LoginSuccessPacket> CODEC =
      PacketCodec.of(LoginSuccessPacket::new, LoginSuccessPacket::write);

  public LoginSuccessPacket(final InputBuffer buffer) {
    this(buffer.readInt());
  }

  public void write(final OutputBuffer buffer) {
    buffer.writeInt(compressionThreshold);
  }

  @Override
  public PacketCodec<LoginSuccessPacket> getCodec() {
    return CODEC;
  }
}
