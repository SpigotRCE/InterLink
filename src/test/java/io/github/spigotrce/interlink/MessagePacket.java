package io.github.spigotrce.interlink;

import com.google.common.io.*;
import io.github.spigotrce.interlink.packet.*;

public record MessagePacket(String message) implements Packet<MessagePacket> {
  public static final PacketCodec<MessagePacket> CODEC = PacketCodec.of(MessagePacket::new, MessagePacket::encode);

  public MessagePacket(ByteArrayDataInput in) {
    this(in.readUTF());
  }

  public void encode(ByteArrayDataOutput out) {
    out.writeUTF(message);
  }

  @Override public PacketCodec<MessagePacket> getCodec() {
    return CODEC;
  }
}
