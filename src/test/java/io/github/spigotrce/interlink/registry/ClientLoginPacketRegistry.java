package io.github.spigotrce.interlink.registry;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.*;

import java.io.IOException;

public class ClientLoginPacketRegistry extends PacketRegistry {
  public final Connection connection;

  public ClientLoginPacketRegistry(Connection connection) {
    this.connection = connection;

    registerPacket(HandshakePacket.class, HandshakePacket.CODEC, this::handle);
    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC, this::handle);
    registerPacket(LoginSuccessPacket.class, LoginSuccessPacket.CODEC, this::handle);
  }

  public void handle(HandshakePacket packet) {
    throw new IllegalArgumentException("Cannot handle HandshakePacket in ClientLogin state");
  }

  public void handle(DisconnectPacket packet) {
    System.out.println("Disconnected from server: " + packet.message());
    connection.close();
  }

  public void handle(LoginSuccessPacket packet) {
    System.out.println("Successfully logged in to server");
    connection.setRegistry(new ClientPlayPacketRegistry(connection));
    connection.setCompressionThreshold(packet.compressionThreshold());
  }
}
