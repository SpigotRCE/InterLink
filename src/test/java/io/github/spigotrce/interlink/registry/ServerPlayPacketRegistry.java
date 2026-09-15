package io.github.spigotrce.interlink.registry;

import io.github.spigotrce.interlink.TestServer;
import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.*;
import java.util.Map;
import java.util.Map.Entry;

public class ServerPlayPacketRegistry extends PacketRegistry {
  public final Connection connection;

  public ServerPlayPacketRegistry(final Connection connection) {
    this.connection = connection;

    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC, this::handle);
    registerPacket(ChatPacket.class, ChatPacket.CODEC, this::handle);
  }

  public void handle(final DisconnectPacket packet) {
    connection.close();
  }

  public void handle(final ChatPacket packet) {
    String username = "";
    for (final Connection namedConnection : TestServer.namedConnections.values()) {
      if (namedConnection == connection) {
        username = TestServer.namedConnections.entrySet()
          .stream()
          .filter(entry -> entry.getValue() == connection)
          .map(Entry::getKey)
          .findFirst()
          .orElse("Unknown");
      }
    }

    final String message = "[" + username + "] " + packet.message();

    System.out.println(message);

    TestServer.namedConnections.values().forEach(conn -> {
      conn.send(new ChatPacket(message));
    });
  }
}
