package xyz.spigotrce.interlink;

import xyz.spigotrce.interlink.cipher.impl.AesCfbCipher;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.connection.TcpTransport;
import xyz.spigotrce.interlink.layer.impl.EncryptionLayer;
import xyz.spigotrce.interlink.packet.DisconnectPacket;
import xyz.spigotrce.interlink.registry.ServerLoginPacketRegistry;
import xyz.spigotrce.interlink.server.Server;
import java.util.ArrayList;
import java.util.HashMap;

public final class TestServer {
  public static final ArrayList<Connection<TcpTransport>> connections = new ArrayList<>();
  public static final HashMap<String, Connection<TcpTransport>> namedConnections = new HashMap<>();

  public static void main(final String[] args) throws Exception {
    final Server<TcpTransport> server =
        new Server<TcpTransport>(
            TcpTransport::new,
            Shared.host,
            Shared.port,
            TestServer::onConnect,
            TestServer::onDisconnect,
            TestServer::onException);

    server.start();
  }

  public static void onConnect(final Connection<TcpTransport> connection) {
    connection.getPipeline().addFirst("encryption", new EncryptionLayer(new AesCfbCipher(Shared.key)));
    connection.setRegistry(new ServerLoginPacketRegistry(connection));
  }

  public static void onDisconnect(final Connection<TcpTransport> connection) {
    connections.remove(connection);
  }

  public static void onException(
      final Connection<TcpTransport> connection, final Throwable throwable) {
    throwable.printStackTrace();
    if (!connection.getTransport().getSocket().isClosed()) {
      connection.send(new DisconnectPacket("Exception: " + throwable.getMessage()));
    }

    connections.remove(connection);
  }
}
