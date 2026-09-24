package xyz.spigotrce.interlink.registry;

import xyz.spigotrce.interlink.TestServer;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.packet.ChatPacket;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.packet.PacketRegistry;
import xyz.spigotrce.interlink.packet.PlayPackets;
import java.util.Map.Entry;

/**
 * Server-side play-phase {@link PacketRegistry} for the chat demo.
 *
 * <p>Registers handlers for {@link PlayPackets#DISCONNECT} and {@link PlayPackets#CHAT}. Chat
 * messages are broadcast to every named connection with the sender's name prepended.
 *
 * @author SpigotRCE
 */
public class ServerPlayPacketRegistry extends PacketRegistry<PlayPackets> {
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
  public ServerPlayPacketRegistry(final Connection connection) {
    super(PlayPackets.class);
    this.connection = connection;

    registerPacket(PlayPackets.DISCONNECT, this::handleDisconnect);
    registerPacket(PlayPackets.CHAT, this::handleChat);
  }

  /**
   * Closes the connection when a {@link DisconnectPacket} is received.
   *
   * @param packet the received disconnect packet
   */
  public void handleDisconnect(final DisconnectPacket packet) {
    connection.close();
  }

  /**
   * Broadcasts a {@link ChatPacket} to every named connection, prefixing the message with the
   * sender's username, and prints it to the server console.
   *
   * @param packet the received chat packet
   */
  public void handleChat(final ChatPacket packet) {
    String username = "";
    for (final Connection namedConnection : TestServer.namedConnections.values()) {
      if (namedConnection == connection) {
        username =
            TestServer.namedConnections.entrySet().stream()
                .filter(entry -> entry.getValue() == connection)
                .map(Entry::getKey)
                .findFirst()
                .orElse("Unknown");
      }
    }

    final String message = "[" + username + "] " + packet.message();

    System.out.println(message);

    TestServer.namedConnections
        .values()
        .forEach(
            conn -> {
              conn.send(new ChatPacket(message));
            });
  }
}
