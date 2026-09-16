package io.github.spigotrce.interlink.connection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.spigotrce.interlink.buf.InputBuffer;
import io.github.spigotrce.interlink.buf.OutputBuffer;
import io.github.spigotrce.interlink.packet.Packet;
import io.github.spigotrce.interlink.packet.PacketCodec;
import io.github.spigotrce.interlink.packet.PacketRegistry;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class ConnectionTest {

  @Test
  public void sendRoutesEncodedFrameThroughPipeline() {
    final FakeTransport transport = new FakeTransport();
    final AtomicReference<Throwable> error = new AtomicReference<>();
    final Connection<FakeTransport> connection =
        new Connection<>(transport, (final Connection<FakeTransport> conn, final Throwable t) -> error.set(t));
    connection.setRegistry(registry());

    final byte[] payload = new byte[] {1, 2, 3, 4};
    connection.send(new EchoPacket(payload));

    assertEquals(1, transport.outgoing.size());
    assertEquals(0, transport.outgoing.get(0)[0]);
    assertArrayEquals(encode(registry(), new EchoPacket(payload)), transport.outgoing.get(0));
    assertNull(error.get());
  }

  private static PacketRegistry registry() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(EchoPacket.class, EchoPacket.CODEC);
    return registry;
  }

  private static byte[] encode(final PacketRegistry registry, final Packet<?> packet) {
    final OutputBuffer out = OutputBuffer.create();
    out.writeInt(registry.getId(packet));
    registry.encode(packet, out);
    return out.toByteArray();
  }

  @Test
  public void readDecodesIncomingFrame() {
    final FakeTransport transport = new FakeTransport();
    final AtomicReference<Throwable> error = new AtomicReference<>();
    final Connection<FakeTransport> connection =
        new Connection<>(transport, (final Connection<FakeTransport> conn, final Throwable t) -> error.set(t));
    connection.setRegistry(registry());

    final byte[] payload = new byte[] {9, 8, 7};
    transport.incoming.add(encode(registry(), new EchoPacket(payload)));

    final EchoPacket received = (EchoPacket) connection.read();
    assertArrayEquals(payload, received.payload());
    assertNull(error.get());
  }

  @Test
  public void unregisteredPacketTriggersOnExceptionAndDisconnect() {
    final FakeTransport transport = new FakeTransport();
    final AtomicReference<Throwable> error = new AtomicReference<>();
    final Connection<FakeTransport> connection =
        new Connection<>(transport, (final Connection<FakeTransport> conn, final Throwable t) -> error.set(t));
    connection.setRegistry(registry());

    connection.send(new Unregistered(5));

    assertTrue(error.get() instanceof IOException);
    assertTrue(connection.isDisconnected());
  }

  @Test
  public void eofSilentlyDisconnects() {
    final FakeTransport transport = new FakeTransport();
    transport.eofOnReceive = true;
    final AtomicReference<Throwable> error = new AtomicReference<>();
    final Connection<FakeTransport> connection =
        new Connection<>(transport, (final Connection<FakeTransport> conn, final Throwable t) -> error.set(t));
    connection.setRegistry(registry());

    assertNull(connection.read());
    assertNull(error.get());
    assertTrue(connection.isDisconnected());
  }

  @Test
  public void sendIsNoOpAfterDisconnect() {
    final FakeTransport transport = new FakeTransport();
    final Connection<FakeTransport> connection =
        new Connection<>(transport, (conn, t) -> {});
    connection.setRegistry(registry());
    connection.setDisconnected(true);

    connection.send(new EchoPacket(new byte[] {66}));
    assertTrue(transport.outgoing.isEmpty());
  }

  @Test
  public void readReturnsNullAfterDisconnect() {
    final FakeTransport transport = new FakeTransport();
    final Connection<FakeTransport> connection =
        new Connection<>(transport, (conn, t) -> {});
    connection.setRegistry(registry());
    connection.setDisconnected(true);

    assertNull(connection.read());
  }

  @Test
  public void oversizedFrameTriggersOnException() {
    final FakeTransport transport = new FakeTransport();
    final AtomicReference<Throwable> error = new AtomicReference<>();
    final Connection<FakeTransport> connection =
        new Connection<>(transport, (final Connection<FakeTransport> conn, final Throwable t) -> error.set(t));
    connection.setRegistry(registry());

    final byte[] huge = new byte[Connection.MAX_FRAME_LENGTH + 1];
    Arrays.fill(huge, (byte) 0x55);
    // Frame is [id][payload]; payload alone at the max boundary trips the check.
    final OutputBuffer out = OutputBuffer.create();
    out.writeInt(0);
    out.write(huge);
    transport.incoming.add(out.toByteArray());

    assertNull(connection.read());
    assertTrue(error.get() instanceof IOException);
    assertTrue(error.get().getMessage().contains("out of bounds"));
    assertTrue(connection.isDisconnected());
  }

  @Test
  public void closeTransitionsAndClosesTransport() throws Exception {
    final FakeTransport transport = new FakeTransport();
    final Connection<FakeTransport> connection =
        new Connection<>(transport, (conn, t) -> {});
    connection.close();

    assertTrue(connection.isDisconnected());
    assertEquals(false, transport.isOpen());
  }

  private record EchoPacket(byte[] payload) implements Packet<EchoPacket> {
    private static final PacketCodec<EchoPacket> CODEC =
        PacketCodec.of(EchoPacket::new, EchoPacket::write);

    private EchoPacket(final InputBuffer in) {
      this(readBytes(in));
    }

    private static byte[] readBytes(final InputBuffer in) {
      final byte[] bytes = new byte[in.readInt()];
      in.readFully(bytes);
      return bytes;
    }

    private void write(final OutputBuffer out) {
      out.writeInt(payload.length);
      out.write(payload);
    }

    @Override
    public PacketCodec<EchoPacket> getCodec() {
      return CODEC;
    }
  }

  private static final class FakeTransport implements Transport<FakeTransport> {
    private final BlockingQueue<byte[]> incoming = new LinkedBlockingQueue<>();
    private final List<byte[]> outgoing = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean open = true;
    private volatile boolean eofOnReceive;

    @Override
    public void send(final byte[] data) {
      outgoing.add(data);
    }

    @Override
    public byte[] receive() throws IOException {
      if (eofOnReceive) {
        throw new EOFException("closed");
      }
      try {
        return incoming.poll(1, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException(e);
      }
    }

    @Override
    public void close() {
      open = false;
    }

    @Override
    public boolean isOpen() {
      return open;
    }

    @Override
    public void bind(final String host, final int port) {}

    @Override
    public FakeTransport accept() {
      return this;
    }

    @Override
    public FakeTransport connect(final String host, final int port) {
      return this;
    }

    @Override
    public String getHostname() {
      return null;
    }

    @Override
    public int getPort() {
      return -1;
    }
  }

  private record Unregistered(int v) implements Packet<Unregistered> {
    private static final PacketCodec<Unregistered> CODEC =
        PacketCodec.of(in -> new Unregistered(in.readInt()), (p, out) -> out.writeInt(p.v()));

    @Override
    public PacketCodec<Unregistered> getCodec() {
      return CODEC;
    }
  }
}
