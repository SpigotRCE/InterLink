package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.packet.ChatPacket;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;
import xyz.spigotrce.interlink.packet.PlayPackets;

public class ClientPlayPacketRegistry extends PacketRegistry<PlayPackets> {
  public final Connection connection;

  public ClientPlayPacketRegistry(final Connection connection) {
    super(PlayPackets.class);
    this.connection = connection;

    registerPacket(PlayPackets.DISCONNECT, this::handleDisconnect);
    registerPacket(PlayPackets.CHAT, this::handleChat);
  }

  public void handleDisconnect(final DisconnectPacket packet) {
    System.out.println("Disconnected from server: ");
    System.out.println(packet.message());
    connection.close();
  }

  public void handleChat(final ChatPacket packet) {
    System.out.println(packet.message());
  }
}
