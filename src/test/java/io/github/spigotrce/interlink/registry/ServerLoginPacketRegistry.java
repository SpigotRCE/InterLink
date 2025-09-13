package io.github.spigotrce.interlink.registry;

import io.github.spigotrce.interlink.TestServer;
import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.*;

public class ServerLoginPacketRegistry extends PacketRegistry {
  public final Connection connection;

  public ServerLoginPacketRegistry(Connection connection) {
    this.connection = connection;

    registerPacket(HandshakePacket.class, HandshakePacket.CODEC, this::handle);
    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC, this::handle);
    registerPacket(LoginSuccessPacket.class, LoginSuccessPacket.CODEC, this::handle);
  }

  public void handle(HandshakePacket packet) {
    if (TestServer.namedConnections.containsKey(packet.username())) {
      connection.send(new DisconnectPacket("Username already taken"));
      connection.close();
    } else {
      connection.send(new LoginSuccessPacket(256));
      connection.setRegistry(new ServerPlayPacketRegistry(connection));
      TestServer.connections.add(connection);
      TestServer.namedConnections.put(packet.username(), connection);
      System.out.println("User " +
        packet.username() +
        " connected from " +
        connection.getSocket().getInetAddress().getHostAddress() +
        ":" +
        connection.getSocket().getPort());

      TestServer.namedConnections.values().forEach(conn -> {
        conn.send(new ChatPacket("User " + packet.username() + " has joined the server"));
      });
    }
  }

  public void handle(DisconnectPacket packet) {
    throw new IllegalArgumentException("Cannot handle DisconnectPacket in ServerLogin state");
  }

  public void handle(LoginSuccessPacket packet) {
    throw new IllegalArgumentException("Cannot handle LoginSuccessPacket in ServerLogin state");
  }
}
