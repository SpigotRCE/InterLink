package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.compression.ZLibCompressor;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.layer.impl.CompressionLayer;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.LoginPackets;
import xyz.spigotrce.interlink.packet.LoginSuccessPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;

public class ClientLoginPacketRegistry extends PacketRegistry<LoginPackets> {
  public final Connection connection;

  public ClientLoginPacketRegistry(final Connection connection) {
    super(LoginPackets.class);
    this.connection = connection;

    registerPacket(LoginPackets.DISCONNECT, this::handleDisconnect);
    registerPacket(LoginPackets.LOGIN_SUCCESS, this::handleLoginSuccess);
  }

  public void handleDisconnect(final DisconnectPacket packet) {
    System.out.println("Disconnected from server: " + packet.message());
    connection.close();
  }

  public void handleLoginSuccess(final LoginSuccessPacket packet) {
    System.out.println("Successfully logged in to server");
    connection.setRegistry(new ClientPlayPacketRegistry(connection));
    connection
        .getPipeline()
        .addLast(
            "compressor",
            new CompressionLayer(new ZLibCompressor(), packet.compressionThreshold()));
  }
}
