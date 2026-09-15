package io.github.spigotrce.interlink.client;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.connection.TcpTransport;
import io.github.spigotrce.interlink.packet.Packet;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Client {
  private final String host;
  private final int port;

  private final byte[] key;
  private final byte[] iv;

  private final Consumer<Connection<TcpTransport>> onConnect;
  private final Consumer<Connection<TcpTransport>> onDisconnect;
  private final BiConsumer<Connection<TcpTransport>, Throwable> onException;
  public volatile boolean lock;
  private Connection<TcpTransport> connection;

  public Client(
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
    lock = false;
  }

  public void connect() throws Exception {
    if (lock) {
      return;
    }

    final Socket socket = new Socket(host, port);
    connection = new Connection<TcpTransport>(new TcpTransport(socket), key, iv, onException);
    onConnect.accept(connection);

    lock = true;

    new Thread(
            () -> {
              while (lock) {
                final Packet<?> packet = connection.read();
                if (packet == null) {
                  if (!connection.isDisconnected()) {
                    disconnect();
                  }
                  break;
                }
                connection.getRegistry().handle(packet);
              }
            },
            "Client-Receiver")
        .start();
  }

  public void disconnect() {
    if (!lock) {
      return;
    }
    if (connection != null) {
      connection.close();
      onDisconnect.accept(connection);
      lock = false;
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

  public Connection<TcpTransport> getConnection() {
    return connection;
  }

  public void setConnection(final Connection<TcpTransport> connection) {
    this.connection = connection;
  }
}
