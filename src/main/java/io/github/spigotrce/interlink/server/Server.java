package io.github.spigotrce.interlink.server;

import io.github.spigotrce.interlink.connection.*;
import io.github.spigotrce.interlink.packet.Packet;

import java.net.*;
import java.util.*;
import java.util.function.*;

public class Server {
  private final String host;
  private final int port;

  private final byte[] key;
  private final byte[] iv;

  private final Consumer<Connection<TcpTransport>> onConnect;
  private final Consumer<Connection<TcpTransport>> onDisconnect;
  private final BiConsumer<Connection<TcpTransport>, Throwable> onException;

  private final List<Connection<TcpTransport>> connections = Collections.synchronizedList(new ArrayList<>());

  public boolean lock;

  public Server(String host,
    int port,
    byte[] key,
    byte[] iv,
    Consumer<Connection<TcpTransport>> onConnect,
    Consumer<Connection<TcpTransport>> onDisconnect,
    BiConsumer<Connection<TcpTransport>, Throwable> onException) {
    this.host = host;
    this.port = port;
    this.key = key;
    this.iv = iv;
    this.onConnect = onConnect;
    this.onDisconnect = onDisconnect;
    this.onException = onException;
  }

  public void start() throws Exception {
    this.lock = true;
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(new InetSocketAddress(host, port));

      while (this.lock) {
        Socket clientSocket = serverSocket.accept();
        Connection<TcpTransport> connection = new Connection<TcpTransport>(new TcpTransport(clientSocket), key, iv, onException);
        connections.add(connection);
        onConnect.accept(connection);

        new Thread(() -> {
          while (!clientSocket.isClosed() && this.lock) {
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

  public Consumer<Connection<TcpTransport>> getOnConnect() {
    return onConnect;
  }

  public Consumer<Connection<TcpTransport>> getOnDisconnect() {
    return onDisconnect;
  }

  public BiConsumer<Connection<TcpTransport>, Throwable> getOnException() {
    return onException;
  }

  public List<Connection<TcpTransport>> getConnections() {
    return connections;
  }
}
