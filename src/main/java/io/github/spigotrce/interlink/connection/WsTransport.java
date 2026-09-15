package io.github.spigotrce.interlink.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

public class WsTransport implements Transport<WsTransport> {
  private final BlockingQueue<byte[]> received = new LinkedBlockingQueue<>();
  private final BlockingQueue<WsTransport> accepted = new LinkedBlockingQueue<>();

  private WebSocketClient client;
  private WebSocketServer server;
  private WebSocket socket;

  private volatile boolean listening;
  private volatile boolean connected;

  public WsTransport() {}

  private WsTransport(final WebSocket socket) {
    this.socket = socket;
    connected = true;
  }

  @Override
  public void send(final byte[] data) throws IOException {
    if (!isOpen()) {
      throw new IOException("WebSocket connection is closed");
    }
    socket.send(ByteBuffer.wrap(data));
  }

  @Override
  public byte[] receive() throws IOException {
    while (true) {
      final byte[] data;
      try {
        data = received.poll(100, TimeUnit.MILLISECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while waiting for a websocket message", e);
      }
      if (data != null) {
        return data;
      }
      if (!isOpen()) {
        throw new IOException("WebSocket connection is closed");
      }
    }
  }

  @Override
  public void close() throws IOException {
    if (socket != null) {
      if (socket.isOpen()) {
        socket.close(1000, "Connection closed");
      }
      connected = false;
    } else if (server != null) {
      listening = false;
      try {
        server.stop();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while stopping websocket server", e);
      }
    } else if (client != null) {
      client.close();
    }
  }

  @Override
  public boolean isOpen() {
    if (connected && socket != null) {
      return socket.isOpen();
    }
    return listening;
  }

  @Override
  public void bind(final String host, final int port) throws IOException {
    if (server != null) {
      throw new IOException("Transport is already bound");
    }

    try (final ServerSocket probe = new ServerSocket()) {
      probe.setReuseAddress(true);
      probe.bind(new InetSocketAddress(host, port));
    }

    server =
        new WebSocketServer(
            new InetSocketAddress(host, port), Collections.singletonList(new Draft_6455())) {
          @Override
          public void onStart() {}

          @Override
          public void onOpen(final WebSocket conn, final ClientHandshake handshake) {
            final WsTransport transport = new WsTransport(conn);
            conn.setAttachment(transport);
            accepted.offer(transport);
          }

          @Override
          public void onMessage(final WebSocket conn, final String message) {}

          @Override
          public void onMessage(final WebSocket conn, final ByteBuffer message) {
            final WsTransport transport = (WsTransport) conn.getAttachment();
            if (transport != null) {
              transport.offer(message);
            }
          }

          @Override
          public void onClose(
              final WebSocket conn, final int code, final String reason, final boolean remote) {
            final WsTransport transport = (WsTransport) conn.getAttachment();
            if (transport != null) {
              transport.connected = false;
            }
          }

          @Override
          public void onError(final WebSocket conn, final Exception ex) {}
        };
    server.start();
    listening = true;
  }

  @Override
  public WsTransport accept() throws IOException {
    while (true) {
      final WsTransport transport;
      try {
        transport = accepted.poll(100, TimeUnit.MILLISECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while accepting a websocket connection", e);
      }
      if (transport != null) {
        return transport;
      }
      if (!listening) {
        throw new IOException("WebSocket server is closed");
      }
    }
  }

  @Override
  public WsTransport connect(final String host, final int port) throws IOException {
    final WsTransport[] holder = new WsTransport[1];
    client =
        new WebSocketClient(
            URI.create("ws://" + host + ":" + port), new Draft_6455()) {
          @Override
          public void onOpen(final ServerHandshake handshake) {
            holder[0] = new WsTransport(getConnection());
            getConnection().setAttachment(holder[0]);
          }

          @Override
          public void onMessage(final String message) {}

          @Override
          public void onMessage(final ByteBuffer message) {
            if (holder[0] != null) {
              holder[0].offer(message);
            }
          }

          @Override
          public void onClose(final int code, final String reason, final boolean remote) {
            if (holder[0] != null) {
              holder[0].connected = false;
            }
          }

          @Override
          public void onError(final Exception ex) {}
        };

    try {
      client.connectBlocking();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while opening websocket connection", e);
    }
    if (!client.isOpen() || holder[0] == null) {
      throw new IOException("Failed to open websocket connection to ws://" + host + ":" + port);
    }
    return holder[0];
  }

  public void offer(final ByteBuffer message) {
    final byte[] data = new byte[message.remaining()];
    message.get(data);
    received.offer(data);
  }

  public WebSocket getSocket() {
    return socket;
  }

  public WebSocketServer getServer() {
    return server;
  }
}
