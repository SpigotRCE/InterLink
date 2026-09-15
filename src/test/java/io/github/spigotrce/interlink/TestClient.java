package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.client.Client;
import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.connection.TcpTransport;
import io.github.spigotrce.interlink.layer.CompressionLayer;
import io.github.spigotrce.interlink.layer.EncryptionLayer;
import io.github.spigotrce.interlink.packet.ChatPacket;
import io.github.spigotrce.interlink.packet.HandshakePacket;
import io.github.spigotrce.interlink.registry.ClientLoginPacketRegistry;
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
    connection.getPipeline().addFirst("encryption", new EncryptionLayer(Shared.key));
    connection.getPipeline().addLast("compression", new CompressionLayer());
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
