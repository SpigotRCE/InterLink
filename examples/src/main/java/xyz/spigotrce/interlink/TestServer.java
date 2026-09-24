package xyz.spigotrce.interlink;

import java.util.ArrayList;
import java.util.HashMap;
import xyz.spigotrce.interlink.cipher.impl.AesCfbCipher;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.connection.TcpTransport;
import xyz.spigotrce.interlink.layer.impl.EncryptionLayer;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.registry.ServerLoginPacketRegistry;
import xyz.spigotrce.interlink.server.Server;

/**
 * A runnable chat demo server entry point for the InterLink networking library.
 *
 * <p>Builds a {@link Server} backed by {@link TcpTransport} bound to {@link Shared#host} and
 * {@link Shared#port} and starts it. Each accepted connection is configured with an {@link
 * EncryptionLayer} using {@link AesCfbCipher} and {@link Shared#key}, and handed a {@link
 * ServerLoginPacketRegistry} to run the login phase.
 *
 * @author SpigotRCE
 */
public final class TestServer {
  private TestServer() {}

  /**
   * Connections that have completed the login phase and joined the server.
   */
  public static final ArrayList<Connection<TcpTransport>> connections = new ArrayList<>();

  /**
   * Maps each connected player name to its {@link Connection}.
   */
  public static final HashMap<String, Connection<TcpTransport>> namedConnections = new HashMap<>();

  /**
   * Entry point. Creates and starts the chat {@link Server}.
   *
   * @param args ignored
   * @throws Exception if the server fails to bind or start
   */
  public static void main(final String[] args) throws Exception {
    final Server<TcpTransport> server =
        new Server<TcpTransport>(
            TcpTransport::new,
            Shared.host,
            Shared.port,
            TestServer::onConnect,
            TestServer::onDisconnect,
            TestServer::onException,
            () -> System.out.println("Server is listening on tcp://" + Shared.host + ":" + Shared.port));

    server.start();
  }

  /**
   * Adds an {@link EncryptionLayer} configured with {@link Shared#key} and installs the login
   * phase {@link ServerLoginPacketRegistry} on a newly accepted connection.
   *
   * @param connection the newly accepted connection
   */
  public static void onConnect(final Connection<TcpTransport> connection) {
    connection.getPipeline().addFirst("encryption", new EncryptionLayer(new AesCfbCipher(Shared.key)));
    connection.setRegistry(new ServerLoginPacketRegistry(connection));
  }

  /**
   * Removes the connection from {@link #connections} when it disconnects.
   *
   * @param connection the disconnected connection
   */
  public static void onDisconnect(final Connection<TcpTransport> connection) {
    connections.remove(connection);
  }

  /**
   * Prints the exception and, while the socket is still open, sends a {@link DisconnectPacket} with
   * the exception message before removing the connection from {@link #connections}.
   *
   * @param connection the connection that failed
   * @param throwable the exception that occurred
   */
  public static void onException(
      final Connection<TcpTransport> connection, final Throwable throwable) {
    throwable.printStackTrace();
    if (!connection.getTransport().getSocket().isClosed()) {
      connection.send(new DisconnectPacket("Exception: " + throwable.getMessage()));
    }

    connections.remove(connection);
  }
}
