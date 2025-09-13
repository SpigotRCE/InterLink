package io.github.spigotrce.interlink.registry;

import io.github.spigotrce.interlink.TestServer;
import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.*;

import java.util.Map;

public class ServerPlayPacketRegistry extends PacketRegistry {
  public final Connection connection;

  public ServerPlayPacketRegistry(Connection connection) {
    this.connection = connection;

    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC, this::handle);
    registerPacket(ChatPacket.class, ChatPacket.CODEC, this::handle);
  }

  public void handle(DisconnectPacket packet) {
    connection.close();
  }

  public void handle(ChatPacket packet) {
    String username = "";
    for (Connection namedConnection : TestServer.namedConnections.values()) {
      if (namedConnection == connection) {
        username = TestServer.namedConnections.entrySet()
          .stream()
          .filter(entry -> entry.getValue() == connection)
          .map(Map.Entry::getKey)
          .findFirst()
          .orElse("Unknown");
      }
    }

    String message = "[" + username + "] " + packet.message();

    System.out.println(message);

    TestServer.namedConnections.values().forEach(conn -> {
      conn.send(new ChatPacket(message));
    });
  }
}
