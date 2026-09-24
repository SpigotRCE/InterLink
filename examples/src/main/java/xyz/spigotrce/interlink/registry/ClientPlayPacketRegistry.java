package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.packet.ChatPacket;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;
import xyz.spigotrce.interlink.packet.PlayPackets;

/**
 * Client-side play-phase {@link PacketRegistry} for the chat demo.
 *
 * <p>Registers handlers for {@link PlayPackets#DISCONNECT} and {@link PlayPackets#CHAT}. Received
 * chat messages are printed to the console.
 *
 * @author SpigotRCE
 */
public class ClientPlayPacketRegistry extends PacketRegistry<PlayPackets> {
  /**
   * The connection this registry decodes and handles packets for.
   */
  public final Connection connection;

  /**
   * Creates a play registry for the given connection and registers the disconnect and chat
   * handlers.
   *
   * @param connection the connection to handle packets for
   */
  public ClientPlayPacketRegistry(final Connection connection) {
    super(PlayPackets.class);
    this.connection = connection;

    registerPacket(PlayPackets.DISCONNECT, this::handleDisconnect);
    registerPacket(PlayPackets.CHAT, this::handleChat);
  }

  /**
   * Prints the disconnect reason from a {@link DisconnectPacket} and closes the connection.
   *
   * @param packet the received disconnect packet
   */
  public void handleDisconnect(final DisconnectPacket packet) {
    System.out.println("Disconnected from server: ");
    System.out.println(packet.message());
    connection.close();
  }

  /**
   * Prints a received chat message to the console.
   *
   * @param packet the received chat packet
   */
  public void handleChat(final ChatPacket packet) {
    System.out.println(packet.message());
  }
}
