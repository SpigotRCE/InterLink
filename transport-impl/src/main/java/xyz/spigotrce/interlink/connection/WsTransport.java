package xyz.spigotrce.interlink.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * {@link Transport} implementation backed by a WebSocket from the Java-WebSocket library.
 *
 * <p>Supports both the server side ({@link #bind(String, int)} and {@link #accept()}) and the
 * client side ({@link #connect(String, int)}). Incoming binary frames are queued and returned by
 * {@link #receive()}, while {@link #send(byte[])} sends frames as binary {@link ByteBuffer}s.
 */
public class WsTransport implements Transport<WsTransport> {
  private final BlockingQueue<byte[]> received = new LinkedBlockingQueue<>();
  private final BlockingQueue<WsTransport> accepted = new LinkedBlockingQueue<>();

  private WebSocketClient client;
  private WebSocketServer server;
  private WebSocket socket;

  private volatile boolean listening;
  private volatile boolean connected;

  private String hostname;
  private int port;

  /**
   * Creates a new, unbound and unconnected websocket transport.
   */
  public WsTransport() {}

  private WsTransport(final WebSocket socket, final String hostname, final int port) {
    this.socket = socket;
    this.hostname = hostname;
    this.port = port;
    connected = true;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Wraps the data in a {@link ByteBuffer} and sends it as a binary websocket frame.
   *
   * @throws IOException if the websocket connection is closed
   */
  @Override
  public void send(final byte[] data) throws IOException {
    if (!isOpen()) {
      throw new IOException("WebSocket connection is closed");
    }
    socket.send(ByteBuffer.wrap(data));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Polls the received-frame queue, blocking up to one second between attempts until a frame
   * arrives or the connection closes.
   *
   * @throws IOException if the websocket connection is closed or the wait is interrupted
   */
  @Override
  public byte[] receive() throws IOException {
    while (true) {
      try {
        if (!isOpen()) {
          throw new IOException("WebSocket connection is closed");
        }
        final byte[] data = received.poll(1, TimeUnit.SECONDS);
        if (data != null) {
          return data;
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while waiting for a websocket message", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Closes the active websocket (and its client), or stops the server when listening.
   *
   * @throws IOException if the server cannot be stopped
   */
  @Override
  public void close() throws IOException {
    if (socket != null) {
      if (socket.isOpen()) {
        socket.close(1000, "Connection closed");
      }
      connected = false;
      if (client != null) {
        client.close();
      }
    } else if (server != null) {
      listening = false;
      try {
        server.stop(1000);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while stopping websocket server", e);
      }
    } else if (client != null) {
      client.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns {@code true} while a connected websocket is open, or while the server is listening.
   */
  @Override
  public boolean isOpen() {
    if (connected && socket != null) {
      return socket.isOpen();
    }
    return listening;
  }

  /**
   * {@inheritDoc}
   *
   * <p>First probes the address for availability, then starts a {@link WebSocketServer} on it.
   *
   * @throws IOException if the transport is already bound or the server cannot be started
   */
  @Override
  public void bind(final String host, final int port) throws IOException {
    if (server != null) {
      throw new IOException("Transport is already bound");
    }

    try (final ServerSocket probe = new ServerSocket()) {
      probe.setReuseAddress(true);
      probe.bind(new InetSocketAddress(host, port));
    }

    try {
      server =
          new WebSocketServer(
              new InetSocketAddress(host, port), Collections.singletonList(new Draft_6455())) {
            @Override
            public void onOpen(final WebSocket conn, final ClientHandshake handshake) {
              final WsTransport transport = new WsTransport(conn, hostname(conn), remotePort(conn));
              conn.setAttachment(transport);
              accepted.offer(transport);
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
            public void onMessage(final WebSocket conn, final String message) {}

            @Override
            public void onError(final WebSocket conn, final Exception ex) {}

            @Override
            public void onStart() {}

            @Override
            public void onMessage(final WebSocket conn, final ByteBuffer message) {
              final WsTransport transport = (WsTransport) conn.getAttachment();
              if (transport != null) {
                transport.offer(message);
              }
            }
          };
      server.start();
      listening = true;
    } catch (final Exception e) {
      if (server != null) {
        try {
          server.stop(1000);
        } catch (final InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      }
      throw new IOException("Failed to bind websocket server on " + host + ":" + port, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Polls the accepted-connection queue, blocking up to one second between attempts until a
   * client connects or the server stops listening.
   *
   * @return a new {@link WsTransport} for the accepted websocket
   * @throws IOException if the server is closed or the wait is interrupted
   */
  @Override
  public WsTransport accept() throws IOException {
    while (true) {
      try {
        if (!listening) {
          throw new IOException("WebSocket server is closed");
        }
        final WsTransport transport = accepted.poll(1, TimeUnit.SECONDS);
        if (transport != null) {
          return transport;
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while accepting a websocket connection", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Opens a client websocket to {@code ws://host:port} and waits up to ten seconds for the
   * handshake to complete.
   *
   * @return a new {@link WsTransport} for the established websocket
   * @throws IOException if the connection cannot be opened or times out
   */
  @Override
  public WsTransport connect(final String host, final int port) throws IOException {
    final WsTransport[] holder = new WsTransport[1];
    final CountDownLatch opened = new CountDownLatch(1);
    client =
        new WebSocketClient(URI.create("ws://" + host + ":" + port), new Draft_6455()) {
          @Override
          public void onOpen(final ServerHandshake handshake) {
            holder[0] = new WsTransport(getConnection(), host, port);
            getConnection().setAttachment(holder[0]);
            opened.countDown();
          }

          @Override
          public void onMessage(final String message) {}

          @Override
          public void onClose(final int code, final String reason, final boolean remote) {
            if (holder[0] != null) {
              holder[0].connected = false;
            }
          }

          @Override
          public void onError(final Exception ex) {
            opened.countDown();
          }

          @Override
          public void onMessage(final ByteBuffer message) {
            if (holder[0] != null) {
              holder[0].offer(message);
            }
          }
        };

    try {
      client.connectBlocking();
      if (!opened.await(10, TimeUnit.SECONDS)) {
        throw new IOException(
            "Timed out opening websocket connection to ws://" + host + ":" + port);
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while opening websocket connection", e);
    }

    if (!client.isOpen() || holder[0] == null) {
      throw new IOException("Failed to open websocket connection to ws://" + host + ":" + port);
    }
    holder[0].client = client;
    return holder[0];
  }

  /**
   * {@inheritDoc}
   *
   * @return the remote hostname, or {@code null} if not connected
   */
  @Override
  public String getHostname() {
    return hostname;
  }

  /**
   * {@inheritDoc}
   *
   * @return the remote port, or {@code 0} if not connected
   */
  @Override
  public int getPort() {
    return port;
  }

  private static String hostname(final WebSocket webSocket) {
    if (webSocket.getRemoteSocketAddress() instanceof final InetSocketAddress addr) {
      return addr.getHostString();
    }
    return null;
  }

  private static int remotePort(final WebSocket webSocket) {
    if (webSocket.getRemoteSocketAddress() instanceof final InetSocketAddress addr) {
      return addr.getPort();
    }
    return -1;
  }

  /**
   * Queues the given binary message for retrieval by {@link #receive()}.
   *
   * @param message the received websocket message in binary form
   */
  public void offer(final ByteBuffer message) {
    final byte[] data = new byte[message.remaining()];
    message.get(data);
    received.offer(data);
  }

  /**
   * Returns the active {@link WebSocket}, or {@code null} if none is connected.
   *
   * @return the websocket
   */
  public WebSocket getSocket() {
    return socket;
  }

  /**
   * Returns the {@link WebSocketServer} when bound to listen, or {@code null} otherwise.
   *
   * @return the server
   */
  public WebSocketServer getServer() {
    return server;
  }
}
