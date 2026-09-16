package io.github.spigotrce.interlink.registry;

import io.github.spigotrce.interlink.compression.ZLibCompressor;
import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.layer.impl.CompressionLayer;
import io.github.spigotrce.interlink.packet.DisconnectPacket;
import io.github.spigotrce.interlink.packet.HandshakePacket;
import io.github.spigotrce.interlink.packet.LoginSuccessPacket;
import io.github.spigotrce.interlink.packet.PacketRegistry;

public class ClientLoginPacketRegistry extends PacketRegistry {
  public final Connection connection;

  public ClientLoginPacketRegistry(final Connection connection) {
    this.connection = connection;

    registerPacket(HandshakePacket.class, HandshakePacket.CODEC);
    registerPacket(DisconnectPacket.class, DisconnectPacket.CODEC, this::handle);
    registerPacket(LoginSuccessPacket.class, LoginSuccessPacket.CODEC, this::handle);
  }

  public void handle(final DisconnectPacket packet) {
    System.out.println("Disconnected from server: " + packet.message());
    connection.close();
  }

  public void handle(final LoginSuccessPacket packet) {
    System.out.println("Successfully logged in to server");
    connection.setRegistry(new ClientPlayPacketRegistry(connection));
    connection
        .getPipeline()
        .addLast(
            "compressor",
            new CompressionLayer(new ZLibCompressor(), packet.compressionThreshold()));
  }
}
