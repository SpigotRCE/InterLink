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

public final class TestClient {
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

  public static void onConnect(final Connection<TcpTransport> connection) {
    connection.getPipeline().addFirst("encryption", new EncryptionLayer(new AesCfbCipher(Shared.key)));
    connection.setRegistry(new ClientLoginPacketRegistry(connection));
    connection.send(new HandshakePacket(input("Enter username: ")));
  }

  public static void onDisconnect(final Connection<TcpTransport> connection) {
    System.out.println("Disconnected from server!");
  }

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
