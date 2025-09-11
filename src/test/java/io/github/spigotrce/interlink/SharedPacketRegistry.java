package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.packet.PacketRegistry;

public class SharedPacketRegistry {
  public static PacketRegistry registry = new PacketRegistry();

  static {
    registry.registerPacket(MessagePacket.class, MessagePacket.CODEC);
  }
}
