package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.Packet;

import java.net.*;

public class TestServer {

  public static void main(String[] args) throws Exception {
    int port = 5555;

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Server listening on port " + port);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected: " + clientSocket.getInetAddress());

        Connection connection = new Connection(clientSocket, SharedPacketRegistry.registry);

        connection.send(new MessagePacket("Welcome!", MessagePacket.Type.CHAT));

        new Thread(() -> {
          try {
            while (true) {
              Packet<?> packet = connection.read();
              if (packet instanceof MessagePacket(String message, MessagePacket.Type type)) {
                if (type == MessagePacket.Type.CHAT) {
                  System.out.println("Received message: " + message);
                } else if (type == MessagePacket.Type.COMMAND) {
                  System.out.println("Received command: " + message);
                } else {
                  throw new AssertionError("Impossible!");
                }
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }).start();
      }
    }
  }
}
