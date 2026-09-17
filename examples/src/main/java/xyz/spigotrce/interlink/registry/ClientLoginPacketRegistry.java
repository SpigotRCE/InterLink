package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.compression.ZLibCompressor;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.layer.impl.CompressionLayer;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.HandshakePacket;
import xyz.spigotrce.interlink.packet.LoginSuccessPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;

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
