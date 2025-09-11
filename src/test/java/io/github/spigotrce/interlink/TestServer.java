package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.Packet;

import java.net.*;

public class TestServer {

  public static void main(String[] args) throws Exception {
    int port = 5555;
    ServerPacketRegistry registry = new ServerPacketRegistry();

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Server listening on port " + port);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected: " + clientSocket.getInetAddress());

        Connection connection = new Connection(clientSocket, registry, 128, SharedKey.KEY, SharedKey.IV);

        connection.send(new MessagePacket("Welcome!", MessagePacket.Type.CHAT));

        new Thread(() -> {
          try {
            while (true) {
              Packet<?> packet = connection.read();
              registry.handle(packet);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }).start();
      }
    }
  }
}
