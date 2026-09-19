package xyz.spigotrce.interlink.server;

import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.connection.Transport;
import xyz.spigotrce.interlink.packet.Packet;
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

  /** Invoked once, right after the listener has bound to the host and port. */
  private final Runnable onStart;

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
      final BiConsumer<Connection<T>, Throwable> onException,
      final Runnable onStart) {
    this.transportFactory = transportFactory;
    this.host = host;
    this.port = port;
    this.onConnect = onConnect;
    this.onDisconnect = onDisconnect;
    this.onException = onException;
    this.onStart = onStart;
  }

  public void start() throws Exception {
    lock = true;
    listener = transportFactory.get();
    listener.bind(host, port);
    onStart.run();

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

      final Thread receiver =
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
              });
      receiver.setDaemon(true);
      receiver.setName("Server-Connection-" + connections.size());
      receiver.start();
    }
  }

  public void stop() throws Exception {
    lock = false;
    if (listener != null) {
      listener.close();
    }
    final Connection<T>[] snapshot;
    synchronized (connections) {
      snapshot = connections.toArray(new Connection[0]);
    }
    for (final Connection<T> conn : snapshot) {
      try {
        conn.close();
      } catch (final Exception ignored) {
      }
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
