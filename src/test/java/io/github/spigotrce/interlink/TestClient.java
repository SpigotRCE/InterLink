package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.Packet;

import java.net.Socket;
import java.util.Scanner;

public class TestClient {

  public static void main(String[] args) throws Exception {
    String host = "localhost";
    int port = 5555;

    try (Socket socket = new Socket(host, port)) {
      Connection connection = new Connection(socket, SharedPacketRegistry.registry);

      new Thread(() -> {
        try {
          while (true) {
            Packet<?> packet = connection.read();
            if (packet instanceof MessagePacket(String message)) {
              System.out.println("[Server] " + message);
            }
          }
        } catch (Exception e) {
          System.out.println("Connection closed or error: " + e.getMessage());
        }
      }, "Client-Receiver").start();

      Scanner scanner = new Scanner(System.in);
      System.out.println("Connected to server. Type messages to send:");

      while (true) {
        String input = scanner.nextLine();
        if (input.equalsIgnoreCase("exit")) {
          break;
        }

        connection.send(new MessagePacket(input));
      }

      connection.close();
    }
  }
}
