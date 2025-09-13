package io.github.spigotrce.interlink.registry;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.*;

import java.io.IOException;

public class ClientPlayPacketRegistry extends PacketRegistry {
  public final Connection connection;

  public ClientPlayPacketRegistry(Connection connection) {
    this.connection = connection;

    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC, this::handle);
    registerPacket(ChatPacket.class, ChatPacket.CODEC, this::handle);
  }

  public void handle(DisconnectPacket packet) {
    System.out.println("Disconnected from server: ");
    System.out.println(packet.message());
    connection.close();
  }

  public void handle(ChatPacket packet) {
    System.out.println(packet.message());
  }
}
