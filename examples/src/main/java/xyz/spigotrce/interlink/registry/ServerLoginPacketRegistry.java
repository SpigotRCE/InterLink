package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.TestServer;
import xyz.spigotrce.interlink.compression.ZLibCompressor;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.connection.TcpTransport;
import xyz.spigotrce.interlink.layer.impl.CompressionLayer;
import xyz.spigotrce.interlink.packet.ChatPacket;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.HandshakePacket;
import xyz.spigotrce.interlink.packet.LoginPackets;
import xyz.spigotrce.interlink.packet.LoginSuccessPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;

/**
 * Server-side login-phase {@link PacketRegistry} for the chat demo.
 *
 * <p>Registers a handler for {@link LoginPackets#HANDSHAKE}. A successful handshake replies with a
 * {@link LoginSuccessPacket}, enables compression with a {@link CompressionLayer} backed by {@link
 * ZLibCompressor}, and moves the connection to the play phase with a {@link
 * ServerPlayPacketRegistry}.
 *
 * @author SpigotRCE
 */
public class ServerLoginPacketRegistry extends PacketRegistry<LoginPackets> {
  /**
   * The connection this registry decodes and handles packets for.
   */
  public final Connection<TcpTransport> connection;

  /**
   * Creates a login registry for the given connection and registers the handshake handler.
   *
   * @param connection the connection to handle packets for
   */
  public ServerLoginPacketRegistry(final Connection<TcpTransport> connection) {
    super(LoginPackets.class);
    this.connection = connection;

    registerPacket(LoginPackets.HANDSHAKE, this::handleHandshake);
  }

  /**
   * Handles a {@link HandshakePacket}. If the username is already taken, the connection is rejected
   * with a {@link DisconnectPacket} and closed. Otherwise the client is accepted with a compression
   * threshold of {@code 256}, compression is enabled, the connection moves to the play phase, and
   * every connected player is notified that the user joined.
   *
   * @param packet the received handshake packet
   */
  public void handleHandshake(final HandshakePacket packet) {
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
