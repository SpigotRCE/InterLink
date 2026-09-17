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
  public void idsAssignedInRegistrationOrder() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(Alpha.class, Alpha.CODEC);
    registry.registerPacket(Beta.class, Beta.CODEC);

    assertEquals(0, registry.getId(new Alpha(1)));
    assertEquals(1, registry.getId(new Beta("x")));
  }

  @Test
  public void unregisteredPacketHasNegativeId() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(Alpha.class, Alpha.CODEC);

    assertEquals(-1, registry.getId(new Beta("x")));
  }

  @Test
  public void encodeDecodeRoundTrip() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(Alpha.class, Alpha.CODEC);

    final Alpha original = new Alpha(77);
    final OutputBuffer out = OutputBuffer.create();
    registry.encode(original, out);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(original, registry.decode(0, in));
  }

  @Test
  public void handlerInvokedOnHandle() {
    final PacketRegistry registry = new PacketRegistry();
    final AtomicInteger received = new AtomicInteger();
    registry.registerPacket(Alpha.class, Alpha.CODEC, packet -> received.set(packet.value()));

    registry.handle(new Alpha(555));
    assertEquals(555, received.get());
  }

  @Test
  public void oneWayPacketHandleThrows() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(Alpha.class, Alpha.CODEC);

    assertThrows(IllegalArgumentException.class, () -> registry.handle(new Alpha(1)));
  }

  @Test
  public void getPacketsIsUnmodifiable() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(Alpha.class, Alpha.CODEC);

    assertThrows(UnsupportedOperationException.class, () -> registry.getPackets().clear());
  }

  @Test
  public void getPacketsPreservesRegistrationOrder() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(Alpha.class, Alpha.CODEC);
    registry.registerPacket(Beta.class, Beta.CODEC);

    final List<PacketEntry<? extends Packet<?>>> packets = registry.getPackets();
    assertEquals(2, packets.size());
    assertTrue(packets.get(0).clazz() == Alpha.class);
    assertTrue(packets.get(1).clazz() == Beta.class);
  }

  @Test
  public void encodeUnregisteredThrows() {
    final PacketRegistry registry = new PacketRegistry();
    registry.registerPacket(Alpha.class, Alpha.CODEC);

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> registry.encode(new Beta("unregistered"), OutputBuffer.create()));
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
}
