package io.github.spigotrce.interlink.server;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.connection.TcpTransport;
import io.github.spigotrce.interlink.packet.Packet;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Server using tcp transport.
 *
 * @author SpigotRCE
 */
public class Server {
  private final String host;
  private final int port;

  private final byte[] key;
  private final byte[] iv;

  /** Consumers for events. */
  private final Consumer<Connection<TcpTransport>> onConnect;

  private final Consumer<Connection<TcpTransport>> onDisconnect;
  private final BiConsumer<Connection<TcpTransport>, Throwable> onException;

  /** List of connections. */
  private final List<Connection<TcpTransport>> connections =
      Collections.synchronizedList(new ArrayList<>());

  public volatile boolean lock;

  public Server(
      final String host,
      final int port,
      final byte[] key,
      final byte[] iv,
      final Consumer<Connection<TcpTransport>> onConnect,
      final Consumer<Connection<TcpTransport>> onDisconnect,
      final BiConsumer<Connection<TcpTransport>, Throwable> onException) {
    this.host = host;
    this.port = port;
    this.key = key;
    this.iv = iv;
    this.onConnect = onConnect;
    this.onDisconnect = onDisconnect;
    this.onException = onException;
  }

  public void start() throws Exception {
    lock = true;
    try (final ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(new InetSocketAddress(host, port));

      while (lock) {
        final Socket clientSocket = serverSocket.accept();
        final Connection<TcpTransport> connection =
            new Connection<TcpTransport>(new TcpTransport(clientSocket), key, iv, onException);
        connections.add(connection);
        onConnect.accept(connection);

        new Thread(
                () -> {
                  try {
                    while (!clientSocket.isClosed() && lock) {
                      final Packet<?> packet = connection.read();
                      if (packet == null) {
                        break;
                      }
                      connection.getRegistry().handle(packet);
                    }
                  } catch (final Exception e) {
                    onException.accept(connection, e);
                  } finally {
                    connections.remove(connection);
                    connection.close();
                    onDisconnect.accept(connection);
                  }
                })
            .start();
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
