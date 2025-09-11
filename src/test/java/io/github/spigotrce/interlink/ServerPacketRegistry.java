package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.packet.PacketRegistry;

public class ServerPacketRegistry extends PacketRegistry {
  public ServerPacketRegistry() {
    registerPacket(MessagePacket.class, MessagePacket.CODEC, this::handle);
  }

  public void handle(MessagePacket packet) {
    if (packet.type() == MessagePacket.Type.CHAT) {
      System.out.println("Received message: " + packet.message());
    } else if (packet.type() == MessagePacket.Type.COMMAND) {
      System.out.println("Received command: " + packet.message());
    } else {
      throw new AssertionError("Impossible!");
    }
  }
}
