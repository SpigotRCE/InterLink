package io.github.spigotrce.interlink.client;

import io.github.spigotrce.interlink.connection.*;
import io.github.spigotrce.interlink.packet.Packet;

import java.net.Socket;
import java.util.function.*;

public class Client {
  private final String host;
  private final int port;

  private final byte[] key;
  private final byte[] iv;

  private final Consumer<Connection<TcpTransport>> onConnect;
  private final Consumer<Connection<TcpTransport>> onDisconnect;
  private final BiConsumer<Connection<TcpTransport>, Throwable> onException;
  public boolean lock;
  private Connection<TcpTransport> connection;

  public Client(String host,
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
    this.lock = false;
  }

  public void connect() throws Exception {
    if (this.lock) {
      return;
    }

    Socket socket = new Socket(host, port);
    connection = new Connection<TcpTransport>(new TcpTransport(socket), key, iv, onException);
    onConnect.accept(connection);

    this.lock = true;

    new Thread(() -> {
      while (this.lock) {
        Packet<?> packet = connection.read();
        if (packet == null) {
          if (!connection.isDisconnected()) {
            disconnect();
          }
          break;
        }
        connection.getRegistry().handle(packet);
      }
    }, "Client-Receiver").start();
  }

  public void disconnect() {
    if (!this.lock) {
      return;
    }
    if (connection != null) {
      connection.close();
      onDisconnect.accept(connection);
      this.lock = false;
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

  public void setConnection(Connection<TcpTransport> connection) {
    this.connection = connection;
  }
}
