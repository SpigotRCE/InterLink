package xyz.spigotrce.interlink.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;
import xyz.spigotrce.interlink.connection.TcpTransport;
import xyz.spigotrce.interlink.packet.Packet;
import xyz.spigotrce.interlink.packet.PacketCodec;
import xyz.spigotrce.interlink.packet.PacketRegistry;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class ClientTest {

  @Test
  public void clientSendsPacketThatServerReceives() throws Exception {
    final int port = freePort();
    final AtomicReference<byte[]> serverReceived = new AtomicReference<>();

    final TcpTransport rawServer = new TcpTransport();
    rawServer.bind("127.0.0.1", port);

    final CountDownLatch accepted = new CountDownLatch(1);
    final Thread acceptThread =
        new Thread(
            () -> {
              try {
                final TcpTransport acceptedTransport = rawServer.accept();
                serverReceived.set(acceptedTransport.receive());
                accepted.countDown();
              } catch (final Exception e) {
                throw new RuntimeException(e);
              }
            });
    acceptThread.start();

    final CountDownLatch connected = new CountDownLatch(1);
    final CountDownLatch disconnected = new CountDownLatch(1);

    final Client<TcpTransport> client =
        new Client<>(
            TcpTransport::new,
            "127.0.0.1",
            port,
            conn -> {
              conn.setRegistry(clientRegistry(new AtomicInteger()));
              connected.countDown();
            },
            conn -> disconnected.countDown(),
            (conn, t) -> {});

    client.connect();
    connected.await(5, TimeUnit.SECONDS);

    client.getConnection().send(new PingPacket(99));
    assertTrue(accepted.await(5, TimeUnit.SECONDS));

    final InputBuffer in = InputBuffer.create(serverReceived.get());
    in.readInt();
    assertEquals(99, PingPacket.CODEC.read(in).seq());

    client.disconnect();
    disconnected.await(5, TimeUnit.SECONDS);
    rawServer.close();
  }

  private static int freePort() {
    try (final ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static PacketRegistry clientRegistry(final AtomicInteger handledSeq) {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(
        PingPacket.class, PingPacket.CODEC, packet -> handledSeq.set(packet.seq()));
    return registry;
  }

  @Test
  public void clientDisconnectFiresDisconnectEvent() throws Exception {
    final int port = freePort();
    final AtomicInteger dummy = new AtomicInteger();

    final TcpTransport rawServer = new TcpTransport();
    rawServer.bind("127.0.0.1", port);
    final Thread acceptThread =
        new Thread(
            () -> {
              try {
                rawServer.accept().receive();
              } catch (final Exception ignored) {
              }
            });
    acceptThread.start();

    final CountDownLatch connected = new CountDownLatch(1);
    final CountDownLatch disconnected = new CountDownLatch(1);

    final Client<TcpTransport> client =
        new Client<>(
            TcpTransport::new,
            "127.0.0.1",
            port,
            conn -> {
              conn.setRegistry(clientRegistry(dummy));
              connected.countDown();
            },
            conn -> disconnected.countDown(),
            (conn, t) -> {});

    client.connect();
    connected.await(5, TimeUnit.SECONDS);
    client.disconnect();

    assertTrue(disconnected.await(5, TimeUnit.SECONDS));
    assertFalse(client.lock);
    rawServer.close();
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
