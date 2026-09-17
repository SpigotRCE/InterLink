package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.TestServer;
import xyz.spigotrce.interlink.compression.ZLibCompressor;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.connection.TcpTransport;
import xyz.spigotrce.interlink.layer.impl.CompressionLayer;
import xyz.spigotrce.interlink.packet.ChatPacket;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.HandshakePacket;
import xyz.spigotrce.interlink.packet.LoginSuccessPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;

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
