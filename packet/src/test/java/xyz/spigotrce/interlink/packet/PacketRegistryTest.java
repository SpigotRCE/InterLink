package xyz.spigotrce.interlink.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;
import xyz.spigotrce.interlink.packet.PacketRegistry.PacketEntry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class PacketRegistryTest {

  @Test
  public void idsAssignedInEnumOrder() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);

    assertEquals(0, registry.getId(new Alpha(1)));
    assertEquals(1, registry.getId(new Beta("x")));
  }

  @Test
  public void unregisteredPacketHasNegativeId() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);

    assertEquals(-1, registry.getId(new Unregistered("x")));
  }

  @Test
  public void encodeDecodeRoundTrip() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);

    final Alpha original = new Alpha(77);
    final OutputBuffer out = OutputBuffer.create();
    registry.encode(original, out);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(original, registry.decode(0, in));
  }

  @Test
  public void handlerInvokedOnHandle() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);
    final AtomicInteger received = new AtomicInteger();
    registry.registerPacket(TestPackets.ALPHA, (Alpha packet) -> received.set(packet.value()));

    registry.handle(new Alpha(555));
    assertEquals(555, received.get());
  }

  @Test
  public void oneWayPacketHandleThrows() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);

    assertThrows(IllegalArgumentException.class, () -> registry.handle(new Alpha(1)));
  }

  @Test
  public void handlerCanBeReRegistered() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);
    final AtomicInteger first = new AtomicInteger();
    final AtomicInteger second = new AtomicInteger();
    registry.registerPacket(TestPackets.ALPHA, (Alpha packet) -> first.set(packet.value()));
    registry.registerPacket(TestPackets.ALPHA, (Alpha packet) -> second.set(packet.value()));

    registry.handle(new Alpha(42));
    assertEquals(0, first.get());
    assertEquals(42, second.get());
  }

  @Test
  public void getPacketsIsUnmodifiable() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);

    assertThrows(UnsupportedOperationException.class, () -> registry.getPackets().clear());
  }

  @Test
  public void getPacketsPreservesEnumOrder() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);

    final List<PacketEntry<? extends Packet<?>>> packets = registry.getPackets();
    assertEquals(2, packets.size());
    assertTrue(packets.get(0).clazz() == Alpha.class);
    assertTrue(packets.get(1).clazz() == Beta.class);
  }

  @Test
  public void encodeUnregisteredThrows() {
    final PacketRegistry<TestPackets> registry = new PacketRegistry<>(TestPackets.class);

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> registry.encode(new Unregistered("unregistered"), OutputBuffer.create()));
  }

  private enum TestPackets implements PacketType {
    ALPHA(Alpha.class, Alpha.CODEC),
    BETA(Beta.class, Beta.CODEC);

    private final Class<? extends Packet<?>> packetClass;
    private final PacketCodec<?> codec;

    TestPackets(final Class<? extends Packet<?>> packetClass, final PacketCodec<?> codec) {
      this.packetClass = packetClass;
      this.codec = codec;
    }

    @Override
    public Class<? extends Packet<?>> packetClass() {
      return packetClass;
    }

    @Override
    public PacketCodec<?> codec() {
      return codec;
    }
  }

  private record Alpha(int value) implements Packet<Alpha> {
    private static final PacketCodec<Alpha> CODEC =
        PacketCodec.of(Alpha::new, Alpha::write);

    private Alpha(final InputBuffer in) {
      this(in.readInt());
    }

    private void write(final OutputBuffer out) {
      out.writeInt(value);
    }

    @Override
    public PacketCodec<Alpha> getCodec() {
      return CODEC;
    }
  }

  private record Beta(String text) implements Packet<Beta> {
    private static final PacketCodec<Beta> CODEC =
        PacketCodec.of(Beta::new, Beta::write);

    private Beta(final InputBuffer in) {
      this(in.readUTF());
    }

    private void write(final OutputBuffer out) {
      out.writeUTF(text);
    }

    @Override
    public PacketCodec<Beta> getCodec() {
      return CODEC;
    }
  }

  private record Unregistered(String text) implements Packet<Unregistered> {
    private static final PacketCodec<Unregistered> CODEC =
        PacketCodec.of(in -> new Unregistered(in.readUTF()), (p, out) -> out.writeUTF(p.text()));

    private Unregistered(final InputBuffer in) {
      this(in.readUTF());
    }

    @Override
    public PacketCodec<Unregistered> getCodec() {
      return CODEC;
    }
  }
}