package io.github.spigotrce.interlink.server;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.packet.Packet;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.*;

public class Server {
  private final String host;
  private final int port;

  private final byte[] key;
  private final byte[] iv;

  private final Consumer<Connection> onConnect;
  private final Consumer<Connection> onDisconnect;
  private final BiConsumer<Connection, Throwable> onException;

  private final List<Connection> connections = Collections.synchronizedList(new ArrayList<>());

  public Server(String host, int port,
    byte[] key,
    byte[] iv,
    Consumer<Connection> onConnect,
    Consumer<Connection> onDisconnect,
    BiConsumer<Connection, Throwable> onException) {
    this.host = host;
    this.port = port;
    this.key = key;
    this.iv = iv;
    this.onConnect = onConnect;
    this.onDisconnect = onDisconnect;
    this.onException = onException;
  }

  public void start() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(new InetSocketAddress(host, port));

      while (true) {
        Socket clientSocket = serverSocket.accept();
        Connection connection = new Connection(clientSocket, key, iv, onException);
        connections.add(connection);
        onConnect.accept(connection);

        new Thread(() -> {
          while (!clientSocket.isClosed()) {
            Packet<?> packet = connection.read();
            connection.getRegistry().handle(packet);
          }

        }).start();
      }
    }
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public byte[] getKey() {
    return key;
  }

  public byte[] getIv() {
    return iv;
  }

  public Consumer<Connection> getOnConnect() {
    return onConnect;
  }

  public Consumer<Connection> getOnDisconnect() {
    return onDisconnect;
  }

  public BiConsumer<Connection, Throwable> getOnException() {
    return onException;
  }

  public List<Connection> getConnections() {
    return connections;
  }
}
