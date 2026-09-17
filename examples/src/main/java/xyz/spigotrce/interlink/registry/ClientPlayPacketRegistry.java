package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.packet.ChatPacket;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;

public class ClientPlayPacketRegistry extends PacketRegistry {
  public final Connection connection;

  public ClientPlayPacketRegistry(final Connection connection) {
    this.connection = connection;

    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC, this::handle);
    registerPacket(ChatPacket.class, ChatPacket.CODEC, this::handle);
  }

  public void handle(final DisconnectPacket packet) {
    System.out.println("Disconnected from server: ");
    System.out.println(packet.message());
    connection.close();
  }

  public void handle(final ChatPacket packet) {
    System.out.println(packet.message());
  }
}
