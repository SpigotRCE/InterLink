package xyz.spigotrce.interlink.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;
import xyz.spigotrce.interlink.connection.Connection;
import xyz.spigotrce.interlink.connection.TcpTransport;
import xyz.spigotrce.interlink.packet.Packet;
import xyz.spigotrce.interlink.packet.PacketCodec;
import xyz.spigotrce.interlink.packet.PacketRegistry;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class ServerTest {

  @Test
  public void serverAcceptsAndHandlesPacket() throws Exception {
    final int port = freePort();
    final AtomicInteger handledSeq = new AtomicInteger(-1);
    final CountDownLatch connected = new CountDownLatch(1);
    final CountDownLatch disconnected = new CountDownLatch(1);

    final Server<TcpTransport> server =
        new Server<>(
            TcpTransport::new,
            "127.0.0.1",
            port,
            conn -> {
              conn.setRegistry(serverRegistry(handledSeq));
              connected.countDown();
            },
            conn -> disconnected.countDown(),
            (conn, t) -> {},
            () -> {});

    final Thread serverThread =
        new Thread(
            () -> {
              try {
                server.start();
              } catch (final Exception e) {
                throw new RuntimeException(e);
              }
            });
    serverThread.setDaemon(true);
    serverThread.start();
    awaitConnectable(port);

    final TcpTransport rawClient = new TcpTransport();
    rawClient.connect("127.0.0.1", port);
    final Connection<TcpTransport> clientConn =
        new Connection<>(rawClient, (conn, t) -> {});
    clientConn.setRegistry(clientRegistry());

    clientConn.send(new PingPacket(42));
    Thread.sleep(300);
    assertEquals(42, handledSeq.get());

    clientConn.close();
    disconnected.await(5, TimeUnit.SECONDS);
    server.stop();
  }

  private static int freePort() {
    try (final ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static PacketRegistry serverRegistry(final AtomicInteger handledSeq) {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(
        PingPacket.class, PingPacket.CODEC, packet -> handledSeq.set(packet.seq()));
    return registry;
  }

  private static void awaitConnectable(final int port) throws Exception {
    final long deadline = System.currentTimeMillis() + 5_000;
    while (System.currentTimeMillis() < deadline) {
      try (final Socket probe = new Socket("127.0.0.1", port)) {
        return;
      } catch (final Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new IllegalStateException("server did not become connectable");
  }

  private static PacketRegistry clientRegistry() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(PingPacket.class, PingPacket.CODEC);
    return registry;
  }

  @Test
  public void serverReportsDisconnectOnClientClose() throws Exception {
    final int port = freePort();
    final CountDownLatch connected = new CountDownLatch(1);
    final CountDownLatch disconnected = new CountDownLatch(1);
    final AtomicInteger handledSeq = new AtomicInteger(-1);

    final Server<TcpTransport> server =
        new Server<>(
            TcpTransport::new,
            "127.0.0.1",
            port,
            conn -> {
              conn.setRegistry(serverRegistry(handledSeq));
              connected.countDown();
            },
            conn -> disconnected.countDown(),
            (conn, t) -> {},
            () -> {});

    final Thread serverThread =
        new Thread(
            () -> {
              try {
                server.start();
              } catch (final Exception e) {
                throw new RuntimeException(e);
              }
            });
    serverThread.setDaemon(true);
    serverThread.start();
    awaitConnectable(port);

    final TcpTransport rawClient = new TcpTransport();
    rawClient.connect("127.0.0.1", port);
    rawClient.close();

    assertTrue(disconnected.await(5, TimeUnit.SECONDS));
    server.stop();
  }

  private record PingPacket(int seq) implements Packet<PingPacket> {
    private static final PacketCodec<PingPacket> CODEC =
        PacketCodec.of(PingPacket::new, PingPacket::write);

    private PingPacket(final InputBuffer in) {
      this(in.readInt());
    }

    private void write(final OutputBuffer out) {
      out.writeInt(seq);
    }

    @Override
    public PacketCodec<PingPacket> getCodec() {
      return CODEC;
    }
  }
}
