package xyz.spigotrce.interlink;

import xyz.spigotrce.interlink.cipher.impl.AesCfbCipher;
import xyz.spigotrce.interlink.client.Client;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.connection.TcpTransport;
import xyz.spigotrce.interlink.layer.impl.EncryptionLayer;
import xyz.spigotrce.interlink.packet.ChatPacket;
import xyz.spigotrce.interlink.packet.HandshakePacket;
import xyz.spigotrce.interlink.registry.ClientLoginPacketRegistry;
import java.util.Scanner;

/**
 * A runnable chat demo client entry point for the InterLink networking library.
 *
 * <p>Connects a {@link Client} backed by {@link TcpTransport} to {@link Shared#host} and {@link
 * Shared#port}. On connect it installs an {@link EncryptionLayer} with {@link AesCfbCipher} and
 * {@link Shared#key}, registers a {@link ClientLoginPacketRegistry}, and sends a {@link
 * HandshakePacket} containing the entered username. The console loop then broadcasts each line as
 * a {@link ChatPacket}, and {@code /exit} disconnects and exits.
 *
 * @author SpigotRCE
 */
public final class TestClient {
  private TestClient() {}

  /**
   * Entry point. Connects the {@link Client}, registers a shutdown hook that disconnects on exit,
   * and runs the console chat loop.
   *
   * @param args ignored
   * @throws Exception if the client fails to connect
   */
  public static void main(final String[] args) throws Exception {
    final Client<TcpTransport> testClient =
        new Client<TcpTransport>(
            TcpTransport::new,
            Shared.host,
            Shared.port,
            TestClient::onConnect,
            TestClient::onDisconnect,
            TestClient::onException);

    testClient.connect();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    testClient.disconnect();
                  } catch (final Exception e) {
                    e.printStackTrace();
                  }
                },
                "Client-Shutdown"));

    while (true) {
      final String message = input("");
      try {
        if ("/exit".equalsIgnoreCase(message)) {
          testClient.disconnect();
          break;
        } else {
          testClient.getConnection().send(new ChatPacket(message));
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Adds an {@link EncryptionLayer} configured with {@link Shared#key}, installs the login phase
   * {@link ClientLoginPacketRegistry}, and sends a {@link HandshakePacket} with the entered
   * username.
   *
   * @param connection the established connection
   */
  public static void onConnect(final Connection<TcpTransport> connection) {
    connection.getPipeline().addFirst("encryption", new EncryptionLayer(new AesCfbCipher(Shared.key)));
    connection.setRegistry(new ClientLoginPacketRegistry(connection));
    connection.send(new HandshakePacket(input("Enter username: ")));
  }

  /**
   * Prints a message once the connection to the server has been closed.
   *
   * @param connection the closed connection
   */
  public static void onDisconnect(final Connection<TcpTransport> connection) {
    System.out.println("Disconnected from server!");
  }

  /**
   * Prints the exception that occurred on the connection.
   *
   * @param connection the connection that failed
   * @param throwable the exception that occurred
   */
  public static void onException(
      final Connection<TcpTransport> connection, final Throwable throwable) {
    throwable.printStackTrace();
  }

  private static String input(final String message) {
    final Scanner scanner = new Scanner(System.in);
    System.out.print(message);
    return scanner.nextLine();
  }
}
