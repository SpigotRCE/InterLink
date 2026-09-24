package xyz.spigotrce.interlink.client;

import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.connection.Transport;
import xyz.spigotrce.interlink.packet.Packet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A client that connects to a server using the {@link Transport} implementation supplied by the
 * caller. {@link #connect()} establishes a {@link Connection} and spawns a daemon receive thread
 * named {@code Client-Receiver} that reads packets and dispatches them to the registered packet
 * handlers until {@link #disconnect()} is called.
 *
 * @param <T> the transport type
 * @author SpigotRCE
 */
public class Client<T extends Transport<T>> {
  private final Supplier<T> transportFactory;

  private final String host;
  private final int port;

  private final Consumer<Connection<T>> onConnect;
  private final Consumer<Connection<T>> onDisconnect;
  private final BiConsumer<Connection<T>, Throwable> onException;

  /**
   * Lock flag that indicates whether the client is connected. Set to {@code true} once
   * {@link #connect()} succeeds and to {@code false} by {@link #disconnect()}.
   */
  public volatile boolean lock;

  private Connection<T> connection;

  /**
   * Creates a new client with the given transport factory and connection callbacks.
   *
   * @param transportFactory supplies the {@link Transport} used for the connection
   * @param host the host to connect to
   * @param port the port to connect to
   * @param onConnect invoked once the {@link Connection} is established
   * @param onDisconnect invoked when the {@link Connection} closes
   * @param onException invoked when the {@link Connection} encounters an error
   */
  public Client(
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
    lock = false;
  }

  /**
   * Connects to the configured host and port, invokes the {@code onConnect} callback, and spawns
   * the receive thread. If the client is already connected this is a no-op.
   *
   * @throws Exception if the transport fails to connect
   */
  public void connect() throws Exception {
    if (lock) {
      return;
    }

    final T transport = transportFactory.get().connect(host, port);
    connection = new Connection<>(transport, onException);
    onConnect.accept(connection);

    lock = true;

    new Thread(
            () -> {
              try {
                while (lock) {
                  final Packet<?> packet = connection.read();
                  if (packet == null) {
                    break;
                  }
                  connection.getRegistry().handle(packet);
                }
              } catch (final Exception e) {
                onException.accept(connection, e);
              } finally {
                lock = false;
                onDisconnect.accept(connection);
              }
            },
            "Client-Receiver")
        .start();
  }

  /**
   * Disconnects the client by clearing the {@link #lock} flag and closing the active
   * {@link Connection}. If the client is not connected this is a no-op.
   */
  public void disconnect() {
    if (!lock) {
      return;
    }
    lock = false;
    if (connection != null) {
      connection.close();
    }
  }

  /**
   * Returns the {@link Supplier} that creates the {@link Transport} used for the connection.
   *
   * @return the transport factory
   */
  public Supplier<T> getTransportFactory() {
    return transportFactory;
  }

  /**
   * Returns the host the client connects to.
   *
   * @return the host the client connects to
   */
  public String getHost() {
    return host;
  }

  /**
   * Returns the port the client connects to.
   *
   * @return the port the client connects to
   */
  public int getPort() {
    return port;
  }

  /**
   * Returns the callback invoked when the client connects.
   *
   * @return the connect callback
   */
  public Consumer<Connection<T>> getOnConnect() {
    return onConnect;
  }

  /**
   * Returns the callback invoked when the client disconnects.
   *
   * @return the disconnect callback
   */
  public Consumer<Connection<T>> getOnDisconnect() {
    return onDisconnect;
  }

  /**
   * Returns the callback invoked when the {@link Connection} encounters an error.
   *
   * @return the exception callback
   */
  public BiConsumer<Connection<T>, Throwable> getOnException() {
    return onException;
  }

  /**
   * Returns the active {@link Connection}, or {@code null} if the client is not connected.
   *
   * @return the active {@link Connection}
   */
  public Connection<T> getConnection() {
    return connection;
  }

  /**
   * Replaces the active {@link Connection}.
   *
   * @param connection the {@link Connection} to set
   */
  public void setConnection(final Connection<T> connection) {
    this.connection = connection;
  }
}
