package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.compression.ZLibCompressor;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.layer.impl.CompressionLayer;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.LoginPackets;
import xyz.spigotrce.interlink.packet.LoginSuccessPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;

/**
 * Client-side login-phase {@link PacketRegistry} for the chat demo.
 *
 * <p>Registers handlers for {@link LoginPackets#DISCONNECT} and {@link LoginPackets#LOGIN_SUCCESS}.
 * A successful login enables compression with the threshold announced by the {@link
 * LoginSuccessPacket} and moves the connection to the play phase with a {@link
 * ClientPlayPacketRegistry}.
 *
 * @author SpigotRCE
 */
public class ClientLoginPacketRegistry extends PacketRegistry<LoginPackets> {
  /**
   * The connection this registry decodes and handles packets for.
   */
  public final Connection connection;

  /**
   * Creates a login registry for the given connection and registers the disconnect and login
   * success handlers.
   *
   * @param connection the connection to handle packets for
   */
  public ClientLoginPacketRegistry(final Connection connection) {
    super(LoginPackets.class);
    this.connection = connection;

    registerPacket(LoginPackets.DISCONNECT, this::handleDisconnect);
    registerPacket(LoginPackets.LOGIN_SUCCESS, this::handleLoginSuccess);
  }

  /**
   * Prints the disconnect reason from a {@link DisconnectPacket} and closes the connection.
   *
   * @param packet the received disconnect packet
   */
  public void handleDisconnect(final DisconnectPacket packet) {
    System.out.println("Disconnected from server: " + packet.message());
    connection.close();
  }

  /**
   * Prints a confirmation, adds a {@link CompressionLayer} using the compression threshold from the
   * {@link LoginSuccessPacket}, and switches the connection to the play phase.
   *
   * @param packet the received login success packet
   */
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
