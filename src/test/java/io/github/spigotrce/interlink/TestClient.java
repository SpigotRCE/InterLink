package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.client.Client;
import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.*;
import io.github.spigotrce.interlink.registry.*;

import java.util.Scanner;

public class TestClient {
  public static void main(String[] args) throws Exception {
    Client testClient = new Client(Shared.host,
      Shared.port,
      Shared.key,
      Shared.iv,
      TestClient::onConnect,
      TestClient::onDisconnect,
      TestClient::onException);

    testClient.connect();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        testClient.disconnect();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, "Client-Shutdown"));

    while (true) {
      String message = input("");
      try {
        if (message.equalsIgnoreCase("/exit")) {
          testClient.disconnect();
          break;
        } else {
          testClient.getConnection().send(new ChatPacket(message));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static void onConnect(Connection connection) {
    connection.setRegistry(new ClientLoginPacketRegistry(connection));
    try {
      connection.send(new HandshakePacket(input("Enter username: ")));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void onDisconnect(Connection connection) {
    System.out.println("Disconnected from server!");
  }

  public static void onException(Throwable throwable) {
    throwable.printStackTrace();
  }

  private static String input(String message) {
    Scanner scanner = new Scanner(System.in);
    System.out.print(message);
    return scanner.nextLine();
  }
}
