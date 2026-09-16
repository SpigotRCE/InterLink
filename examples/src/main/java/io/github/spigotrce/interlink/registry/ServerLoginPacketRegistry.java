package io.github.spigotrce.interlink.registry;

import io.github.spigotrce.interlink.TestServer;
import io.github.spigotrce.interlink.compression.ZLibCompressor;
import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.connection.TcpTransport;
import io.github.spigotrce.interlink.layer.impl.CompressionLayer;
import io.github.spigotrce.interlink.packet.ChatPacket;
import io.github.spigotrce.interlink.packet.DisconnectPacket;
import io.github.spigotrce.interlink.packet.HandshakePacket;
import io.github.spigotrce.interlink.packet.LoginSuccessPacket;
import io.github.spigotrce.interlink.packet.PacketRegistry;

public class ServerLoginPacketRegistry extends PacketRegistry {
  public final Connection<TcpTransport> connection;

  public ServerLoginPacketRegistry(final Connection<TcpTransport> connection) {
    this.connection = connection;

    registerPacket(HandshakePacket.class, HandshakePacket.CODEC, this::handle);
    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC);
    registerPacket(LoginSuccessPacket.class, LoginSuccessPacket.CODEC);
  }

  public void handle(final HandshakePacket packet) {
    if (TestServer.namedConnections.containsKey(packet.username())) {
      connection.send(new DisconnectPacket("Username already taken"));
      connection.close();
    } else {
      final int threashold = 256;
      connection.send(new LoginSuccessPacket(threashold));
      connection.getPipeline().addLast("compression", new CompressionLayer(new ZLibCompressor(), threashold));
      connection.setRegistry(new ServerPlayPacketRegistry(connection));
      TestServer.connections.add(connection);
      TestServer.namedConnections.put(packet.username(), connection);
      System.out.println(
          "User "
              + packet.username()
              + " connected from "
              + connection.getTransport().getHostname()
              + ":"
              + connection.getTransport().getPort());

      TestServer.namedConnections
          .values()
          .forEach(
              conn -> {
                conn.send(new ChatPacket("User " + packet.username() + " has joined the server"));
              });
    }
  }
}
