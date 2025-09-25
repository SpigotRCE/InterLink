package io.github.spigotrce.interlink.registry;

import io.github.spigotrce.interlink.TestServer;
import io.github.spigotrce.interlink.connection.*;
import io.github.spigotrce.interlink.packet.*;

public class ServerLoginPacketRegistry extends PacketRegistry {
  public final Connection<TcpTransport> connection;

  public ServerLoginPacketRegistry(Connection<TcpTransport> connection) {
    this.connection = connection;

    registerPacket(HandshakePacket.class, HandshakePacket.CODEC, this::handle);
    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC);
    registerPacket(LoginSuccessPacket.class, LoginSuccessPacket.CODEC);
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
        connection.getTransport().getSocket().getInetAddress().getHostAddress() +
        ":" +
        connection.getTransport().getSocket().getPort());

      TestServer.namedConnections.values().forEach(conn -> {
        conn.send(new ChatPacket("User " + packet.username() + " has joined the server"));
      });
    }
  }
}
