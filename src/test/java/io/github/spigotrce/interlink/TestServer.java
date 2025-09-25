package io.github.spigotrce.interlink;

import io.github.spigotrce.interlink.connection.*;
import io.github.spigotrce.interlink.packet.DisconnectPacket;
import io.github.spigotrce.interlink.registry.ServerLoginPacketRegistry;
import io.github.spigotrce.interlink.server.Server;

import java.util.*;

public class TestServer {
  public static final ArrayList<Connection<TcpTransport>> connections = new ArrayList<>();
  public static final HashMap<String, Connection<TcpTransport>> namedConnections = new HashMap<>();

  public static void main(String[] args) throws Exception {
    Server server = new Server(Shared.host,
      Shared.port,
      Shared.key,
      Shared.iv,
      TestServer::onConnect,
      TestServer::onDisconnect,
      TestServer::onException);

    server.start();
  }

  public static void onConnect(Connection<TcpTransport> connection) {
    connection.setRegistry(new ServerLoginPacketRegistry(connection));
  }

  public static void onDisconnect(Connection<TcpTransport> connection) {
    connections.remove(connection);
  }

  public static void onException(Connection<TcpTransport> connection, Throwable throwable) {
    throwable.printStackTrace();
    if (!connection.getTransport().getSocket().isClosed()) {
      connection.send(new DisconnectPacket("Exception: " + throwable.getMessage()));
    }


    connections.remove(connection);
  }
}
