package io.github.spigotrce.interlink.client;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.connection.Transport;
import io.github.spigotrce.interlink.packet.Packet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Client<T extends Transport<T>> {
  private final Supplier<T> transportFactory;

  private final String host;
  private final int port;

  private final byte[] key;
  private final byte[] iv;

  private final Consumer<Connection<T>> onConnect;
  private final Consumer<Connection<T>> onDisconnect;
  private final BiConsumer<Connection<T>, Throwable> onException;

  public volatile boolean lock;

  private Connection<T> connection;

  public Client(
      final Supplier<T> transportFactory,
      final String host,
      final int port,
      final byte[] key,
      final byte[] iv,
      final Consumer<Connection<T>> onConnect,
      final Consumer<Connection<T>> onDisconnect,
      final BiConsumer<Connection<T>, Throwable> onException) {
    this.transportFactory = transportFactory;
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

    final T transport = transportFactory.get().connect(host, port);
    connection = new Connection<>(transport, key, iv, onException);
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

  public Supplier<T> getTransportFactory() {
    return transportFactory;
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

  public Consumer<Connection<T>> getOnConnect() {
    return onConnect;
  }

  public Consumer<Connection<T>> getOnDisconnect() {
    return onDisconnect;
  }

  public BiConsumer<Connection<T>, Throwable> getOnException() {
    return onException;
  }

  public Connection<T> getConnection() {
    return connection;
  }

  public void setConnection(final Connection<T> connection) {
    this.connection = connection;
  }
}
