package io.github.spigotrce.interlink.server;

import io.github.spigotrce.interlink.connection.Connection;
import io.github.spigotrce.interlink.connection.Transport;
import io.github.spigotrce.interlink.packet.Packet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Server using tcp transport.
 *
 * @author SpigotRCE
 */
public class Server<T extends Transport<T>> {
  private final Supplier<T> transportFactory;

  private final String host;
  private final int port;

  /** Consumers for events. */
  private final Consumer<Connection<T>> onConnect;

  private final Consumer<Connection<T>> onDisconnect;
  private final BiConsumer<Connection<T>, Throwable> onException;

  /** List of connections. */
  private final List<Connection<T>> connections =
      Collections.synchronizedList(new ArrayList<>());

  public volatile boolean lock;

  private T listener;

  public Server(
      final Supplier<T> transportFactory,
      final String host,
      final int port,
      final Consumer<Connection<T>> onConnect,
      final Consumer<Connection<T>> onDisconnect,
      final BiConsumer<Connection<T>, Throwable> onException) {
    this.transportFactory = transportFactory;
    this.host = host;
    this.port = port;
    this.onConnect = onConnect;
    this.onDisconnect = onDisconnect;
    this.onException = onException;
  }

  public void start() throws Exception {
    lock = true;
    listener = transportFactory.get();
    listener.bind(host, port);

    while (lock) {
      final T transport;
      try {
        transport = listener.accept();
      } catch (final Exception e) {
        if (!lock) {
          break;
        }
        throw e;
      }

      final Connection<T> connection = new Connection<>(transport, onException);
      connections.add(connection);
      onConnect.accept(connection);

      new Thread(
              () -> {
                try {
                  while (transport.isOpen() && lock) {
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

  public void stop() throws Exception {
    lock = false;
    if (listener != null) {
      listener.close();
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

  public Consumer<Connection<T>> getOnConnect() {
    return onConnect;
  }

  public Consumer<Connection<T>> getOnDisconnect() {
    return onDisconnect;
  }

  public BiConsumer<Connection<T>, Throwable> getOnException() {
    return onException;
  }

  public List<Connection<T>> getConnections() {
    return connections;
  }
}
