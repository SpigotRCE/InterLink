package io.github.spigotrce.interlink.packet;

import io.github.spigotrce.interlink.buf.*;

public record LoginSuccessPacket(int compressionThreshold) implements Packet<LoginSuccessPacket> {
  public static final PacketCodec<LoginSuccessPacket> CODEC =
    PacketCodec.of(LoginSuccessPacket::new, LoginSuccessPacket::write);

  public LoginSuccessPacket(InputBuffer buffer) {
    this(buffer.readInt());
  }

  public void write(OutputBuffer buffer) {
    buffer.writeInt(compressionThreshold);
  }

  @Override public PacketCodec<LoginSuccessPacket> getCodec() {
    return null;
  }
}
