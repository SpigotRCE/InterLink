package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.packet.PacketRegistry;

public class ClientPacketRegistry extends PacketRegistry {
  public ClientPacketRegistry() {
    registerPacket(MessagePacket.class, MessagePacket.CODEC, this::handle);
  }

  public void handle(MessagePacket packet) {
    if (packet.type() == MessagePacket.Type.CHAT) {
      System.out.println("[SERVER] " + packet.message());
    } else {
      throw new AssertionError("Impossible!");
    }
  }
}
