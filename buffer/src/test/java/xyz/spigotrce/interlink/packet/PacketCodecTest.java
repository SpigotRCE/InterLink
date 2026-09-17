package xyz.spigotrce.interlink.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;
import org.junit.jupiter.api.Test;

public class PacketCodecTest {

  private final PacketCodec<TestPacket> codec = TestPacket.CODEC;

  @Test
  public void roundTrip() {
    final OutputBuffer out = OutputBuffer.create();
    codec.write(new TestPacket(1337), out);
    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(new TestPacket(1337), codec.read(in));
  }

  @Test
  public void zeroAndNegativeValues() {
    final OutputBuffer out = OutputBuffer.create();
    codec.write(new TestPacket(0), out);
    codec.write(new TestPacket(-42), out);
    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(new TestPacket(0), codec.read(in));
    assertEquals(new TestPacket(-42), codec.read(in));
  }

  private record TestPacket(int value) implements Packet<TestPacket> {
    private static final PacketCodec<TestPacket> CODEC =
        PacketCodec.of(TestPacket::new, TestPacket::write);

    private TestPacket(final InputBuffer in) {
      this(in.readInt());
    }

    private void write(final OutputBuffer out) {
      out.writeInt(value);
    }

    @Override
    public PacketCodec<TestPacket> getCodec() {
      return CODEC;
    }
  }
}
