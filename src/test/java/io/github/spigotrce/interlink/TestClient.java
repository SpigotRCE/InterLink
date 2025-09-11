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
            if (packet instanceof MessagePacket(String message, MessagePacket.Type type)) {
              if (type == MessagePacket.Type.CHAT) {
                System.out.println("[Server] " + message);
              } else {
                throw new AssertionError("Impossible!");
              }
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

        if (input.startsWith("!")) {
          connection.send(new MessagePacket(input, MessagePacket.Type.COMMAND));
        } else {
          connection.send(new MessagePacket(input, MessagePacket.Type.CHAT));
        }
      }

      connection.close();
    }
  }
}
