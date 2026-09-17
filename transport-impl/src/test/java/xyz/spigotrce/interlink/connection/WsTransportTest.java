package xyz.spigotrce.interlink.connection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class WsTransportTest {

  @Test
  public void roundTripOverWebSocket() throws Exception {
    final int port = freePort();
    final WsTransport server = new WsTransport();
    server.bind("127.0.0.1", port);
    assertTrue(server.isOpen());

    final WsTransport client = connectWithRetry(port);
    final WsTransport accepted = acceptClient(server);

    final byte[] payload = "hello websocket".getBytes();
    client.send(payload);
    assertArrayEquals(payload, accepted.receive());

    final byte[] reply = "reply".getBytes();
    accepted.send(reply);
    assertArrayEquals(reply, client.receive());

    client.close();
    server.close();
  }

  private static int freePort() {
    try (final ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static WsTransport connectWithRetry(final int port) throws Exception {
    final long deadline = System.currentTimeMillis() + 5_000;
    while (System.currentTimeMillis() < deadline) {
      final WsTransport candidate = new WsTransport();
      try {
        return candidate.connect("127.0.0.1", port);
      } catch (final IOException e) {
        Thread.sleep(50);
      }
    }
    throw new IllegalStateException("could not connect to server on port " + port);
  }

  private static WsTransport acceptClient(final WsTransport server) throws Exception {
    final CountDownLatch acceptedLatch = new CountDownLatch(1);
    final WsTransport[] accepted = new WsTransport[1];
    new Thread(
            () -> {
              try {
                accepted[0] = server.accept();
                acceptedLatch.countDown();
              } catch (final IOException e) {
                throw new RuntimeException(e);
              }
            })
        .start();
    assertTrue(acceptedLatch.await(5, TimeUnit.SECONDS));
    return accepted[0];
  }

  @Test
  public void emptyPayloadRoundTrip() throws Exception {
    final int port = freePort();
    final WsTransport server = new WsTransport();
    server.bind("127.0.0.1", port);

    final WsTransport client = connectWithRetry(port);
    final WsTransport accepted = acceptClient(server);

    client.send(new byte[0]);
    assertArrayEquals(new byte[0], accepted.receive());

    accepted.send(new byte[0]);
    assertArrayEquals(new byte[0], client.receive());

    client.close();
    server.close();
  }

  @Test
  public void largePayloadRoundTrip() throws Exception {
    final int port = freePort();
    final WsTransport server = new WsTransport();
    server.bind("127.0.0.1", port);

    final WsTransport client = connectWithRetry(port);
    final WsTransport accepted = acceptClient(server);

    final byte[] payload = new byte[512 * 1024];
    Arrays.fill(payload, (byte) 0x11);
    client.send(payload);
    assertArrayEquals(payload, accepted.receive());

    accepted.send(payload);
    assertArrayEquals(payload, client.receive());

    client.close();
    server.close();
  }

  @Test
  public void doubleBindRejected() throws IOException {
    final WsTransport server = new WsTransport();
    server.bind("127.0.0.1", freePort());
    assertThrows(IOException.class, () -> server.bind("127.0.0.1", freePort()));
    server.close();
  }

  @Test
  public void connectToClosedPortRejected() {
    final WsTransport client = new WsTransport();
    assertThrows(IOException.class, () -> client.connect("127.0.0.1", freePort()));
  }

  @Test
  public void acceptWithoutBindRejected() {
    final WsTransport ws = new WsTransport();
    assertThrows(IOException.class, ws::accept);
  }

  @Test
  public void sendWithoutConnectionRejected() {
    final WsTransport ws = new WsTransport();
    assertThrows(IOException.class, () -> ws.send(new byte[] {1}));
  }

  @Test
  public void closeStopsListening() throws Exception {
    final int port = freePort();
    final WsTransport server = new WsTransport();
    server.bind("127.0.0.1", port);
    assertTrue(server.isOpen());
    server.close();
    assertFalse(server.isOpen());
    assertThrows(IOException.class, server::accept);
  }

  @Test
  public void serverAcceptsAndExposesConnectionInfo() throws Exception {
    final int port = freePort();
    final WsTransport server = new WsTransport();
    server.bind("127.0.0.1", port);

    final WsTransport client = connectWithRetry(port);
    final WsTransport accepted = acceptClient(server);

    assertEquals("127.0.0.1", client.getHostname());
    assertEquals(port, client.getPort());
    assertTrue(client.isOpen());
    assertEquals("127.0.0.1", accepted.getHostname());

    client.close();
    server.close();
  }
}
