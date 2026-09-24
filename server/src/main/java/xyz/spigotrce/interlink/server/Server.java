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
 * A server based on the {@link Transport} implementation supplied by the caller. Each accepted
 * connection is wrapped in a {@link Connection} and handed to the {@code onConnect} callback, which
 * is responsible for registering a {@link xyz.spigotrce.interlink.packet.PacketRegistry} and
 * configuring the connection pipeline.
 *
 * <p>{@link #start()} binds the listener, runs the {@code onStart} callback, and then blocks the
 * calling thread while it runs the accept loop. Each accepted connection is processed on its own
 * daemon thread named {@code Server-Connection-<n>}.
 *
 * @param <T> the transport type
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

  /**
   * Lock flag that keeps the accept loop running. Set to {@code true} by {@link #start()} and to
   * {@code false} by {@link #stop()}.
   */
  public volatile boolean lock;

  private T listener;

  /**
   * Creates a new server with the given transport factory and connection callbacks.
   *
   * @param transportFactory supplies fresh {@link Transport} instances
   * @param host the host to bind to
   * @param port the port to bind to
   * @param onConnect invoked for each newly accepted {@link Connection}
   * @param onDisconnect invoked when a {@link Connection} closes
   * @param onException invoked when a {@link Connection} encounters an error
   * @param onStart invoked once, right after the listener binds to the host and port
   */
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

  /**
   * Starts the server by binding the listener, running the {@code onStart} callback, and then
   * blocking the calling thread for as long as the accept loop runs. Each accepted connection is
   * added to this server and handed to the {@code onConnect} callback before its receive loop is
   * started.
   *
   * @throws Exception if the listener cannot bind or an accept fails while the server is running
   */
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

  /**
   * Stops the server by clearing the {@link #lock} flag, closing the listener, and closing every
   * active {@link Connection}.
   *
   * @throws Exception if the listener fails to close
   */
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

  /**
   * Returns the {@link Supplier} that creates the transports used for the incoming connections.
   *
   * @return the transport factory
   */
  public Supplier<T> getTransportFactory() {
    return transportFactory;
  }

  /**
   * Returns the host the server binds to.
   *
   * @return the host the server binds to
   */
  public String getHost() {
    return host;
  }

  /**
   * Returns the port the server binds to.
   *
   * @return the port the server binds to
   */
  public int getPort() {
    return port;
  }

  /**
   * Returns the callback invoked when a client connects.
   *
   * @return the connect callback
   */
  public Consumer<Connection<T>> getOnConnect() {
    return onConnect;
  }

  /**
   * Returns the callback invoked when a client disconnects.
   *
   * @return the disconnect callback
   */
  public Consumer<Connection<T>> getOnDisconnect() {
    return onDisconnect;
  }

  /**
   * Returns the callback invoked when a {@link Connection} encounters an error.
   *
   * @return the exception callback
   */
  public BiConsumer<Connection<T>, Throwable> getOnException() {
    return onException;
  }

  /**
   * Returns a synchronized list of the currently active {@link Connection}s.
   *
   * @return the active {@link Connection}s
   */
  public List<Connection<T>> getConnections() {
    return connections;
  }
}
