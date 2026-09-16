package io.github.spigotrce.interlink.connection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class TcpTransportTest {

  @Test
  public void roundTripOverTcp() throws Exception {
    final int port = freePort();
    final TcpTransport server = new TcpTransport();
    server.bind("127.0.0.1", port);

    final CountDownLatch served = new CountDownLatch(1);
    final byte[] transmitted = "hello tcp".getBytes();
    new Thread(
            () -> {
              try {
                final TcpTransport accepted = server.accept();
                final byte[] data = accepted.receive();
                accepted.send(data);
                served.countDown();
              } catch (final IOException e) {
                throw new RuntimeException(e);
              }
            })
        .start();

    final TcpTransport client = new TcpTransport();
    client.connect("127.0.0.1", port);
    client.send(transmitted);
    assertArrayEquals(transmitted, client.receive());
    assertTrue(served.await(5, TimeUnit.SECONDS));

    server.close();
    client.close();
  }

  private static int freePort() {
    try (final ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void largePayloadRoundTrip() throws Exception {
    final int port = freePort();
    final TcpTransport server = new TcpTransport();
    server.bind("127.0.0.1", port);

    final byte[] payload = new byte[1024 * 1024];
    Arrays.fill(payload, (byte) 0x7F);
    final CountDownLatch served = new CountDownLatch(1);
    new Thread(
            () -> {
              try {
                final TcpTransport accepted = server.accept();
                accepted.send(payload);
                served.countDown();
              } catch (final IOException e) {
                throw new RuntimeException(e);
              }
            })
        .start();

    final TcpTransport client = new TcpTransport();
    client.connect("127.0.0.1", port);
    assertArrayEquals(payload, client.receive());
    assertTrue(served.await(10, TimeUnit.SECONDS));

    server.close();
    client.close();
  }

  @Test
  public void emptyPayloadRoundTrip() throws Exception {
    final int port = freePort();
    final TcpTransport server = new TcpTransport();
    server.bind("127.0.0.1", port);

    final byte[] payload = new byte[0];
    final CountDownLatch served = new CountDownLatch(1);
    new Thread(
            () -> {
              try {
                final TcpTransport accepted = server.accept();
                final byte[] data = accepted.receive();
                accepted.send(data);
                served.countDown();
              } catch (final IOException e) {
                throw new RuntimeException(e);
              }
            })
        .start();

    final TcpTransport client = new TcpTransport();
    client.connect("127.0.0.1", port);
    client.send(payload);
    assertArrayEquals(payload, client.receive());
    assertTrue(served.await(5, TimeUnit.SECONDS));

    server.close();
    client.close();
  }

  @Test
  public void doubleBindRejected() throws IOException {
    final TcpTransport server = new TcpTransport();
    server.bind("127.0.0.1", freePort());
    assertThrows(IOException.class, () -> server.bind("127.0.0.1", freePort()));
    server.close();
  }

  @Test
  public void connectToClosedPortRejected() {
    final int port = freePort(); // port is free, nothing listening
    final TcpTransport client = new TcpTransport();
    assertThrows(ConnectException.class, () -> client.connect("127.0.0.1", port));
  }

  @Test
  public void isOpenLifecycle() throws Exception {
    final int port = freePort();
    final TcpTransport server = new TcpTransport();
    server.bind("127.0.0.1", port);

    final TcpTransport client = new TcpTransport();
    client.connect("127.0.0.1", port);
    assertTrue(client.isOpen());
    assertEquals("127.0.0.1", client.getHostname());
    assertEquals(port, client.getPort());

    client.close();
    assertFalse(client.isOpen());
    server.close();
  }
}
