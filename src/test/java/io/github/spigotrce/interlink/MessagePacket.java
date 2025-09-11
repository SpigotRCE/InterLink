package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.buf.*;
import io.github.spigotrce.interlink.packet.*;

public record MessagePacket(String message, Type type) implements Packet<MessagePacket> {
  public static final PacketCodec<MessagePacket> CODEC = PacketCodec.of(MessagePacket::new, MessagePacket::encode);

  public MessagePacket(InputBuffer in) {
    this(in.readUTF(), in.readEnumConstant(Type.class));
  }

  public void encode(OutputBuffer out) {
    out.writeUTF(message);
    out.writeEnumConstant(type);
  }

  @Override public PacketCodec<MessagePacket> getCodec() {
    return CODEC;
  }

  static enum Type {
    CHAT,
    COMMAND
  }
}
