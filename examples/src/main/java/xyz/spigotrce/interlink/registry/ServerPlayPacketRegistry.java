package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.TestServer;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.packet.ChatPacket;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;
import xyz.spigotrce.interlink.packet.PlayPackets;
import java.util.Map.Entry;

public class ServerPlayPacketRegistry extends PacketRegistry<PlayPackets> {
  public final Connection connection;

  public ServerPlayPacketRegistry(final Connection connection) {
    super(PlayPackets.class);
    this.connection = connection;

    registerPacket(PlayPackets.DISCONNECT, this::handleDisconnect);
    registerPacket(PlayPackets.CHAT, this::handleChat);
  }

  public void handleDisconnect(final DisconnectPacket packet) {
    connection.close();
  }

  public void handleChat(final ChatPacket packet) {
    String username = "";
    for (final Connection namedConnection : TestServer.namedConnections.values()) {
      if (namedConnection == connection) {
        username =
            TestServer.namedConnections.entrySet().stream()
                .filter(entry -> entry.getValue() == connection)
                .map(Entry::getKey)
                .findFirst()
                .orElse("Unknown");
      }
    }

    final String message = "[" + username + "] " + packet.message();

    System.out.println(message);

    TestServer.namedConnections
        .values()
        .forEach(
            conn -> {
              conn.send(new ChatPacket(message));
            });
  }
}
