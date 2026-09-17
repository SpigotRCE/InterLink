package xyz.spigotrce.interlink.buf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class BufferTest {

  @Test
  public void primitivesRoundTrip() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeBoolean(true);
    out.writeBoolean(false);
    out.writeByte(127);
    out.writeByte(-128);
    out.writeShort(12345);
    out.writeShort(-12345);
    out.writeChar('a');
    out.writeInt(Integer.MIN_VALUE);
    out.writeInt(Integer.MAX_VALUE);
    out.writeLong(Long.MIN_VALUE);
    out.writeLong(Long.MAX_VALUE);
    out.writeFloat(3.14f);
    out.writeDouble(2.718281828);
    out.writeUTF("hello \u00e9\u4e2d\u6587");

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertTrue(in.readBoolean());
    assertEquals(false, in.readBoolean());
    assertEquals(127, in.readByte());
    assertEquals(-128, in.readByte());
    assertEquals(12345, in.readShort());
    assertEquals(-12345, in.readShort());
    assertEquals('a', in.readChar());
    assertEquals(Integer.MIN_VALUE, in.readInt());
    assertEquals(Integer.MAX_VALUE, in.readInt());
    assertEquals(Long.MIN_VALUE, in.readLong());
    assertEquals(Long.MAX_VALUE, in.readLong());
    assertEquals(3.14f, in.readFloat());
    assertEquals(2.718281828, in.readDouble());
    assertEquals("hello \u00e9\u4e2d\u6587", in.readUTF());
  }

  @Test
  public void rawBytesRoundTrip() {
    final OutputBuffer out = OutputBuffer.create();
    final byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, -1, -2, -3};
    out.write(data);
    out.write(data, 2, 4);
    out.write(-42);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    final byte[] first = new byte[data.length];
    in.readFully(first);
    assertArrayEquals(data, first);
    final byte[] second = new byte[4];
    in.readFully(second);
    assertArrayEquals(new byte[] {2, 3, 4, 5}, second);
    assertEquals(-42, in.readByte());
  }

  @Test
  public void unsignedByteAndShort() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeByte(200);
    out.writeByte(255);
    out.writeShort(60000);
    out.writeShort(0xFFFF);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(200, in.readUnsignedByte());
    assertEquals(255, in.readUnsignedByte());
    assertEquals(60000, in.readUnsignedShort());
    assertEquals(65535, in.readUnsignedShort());
  }

  @Test
  public void enumRoundTrip() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeEnumConstant(Color.RED);
    out.writeEnumConstant(Color.BLUE);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(Color.RED, in.readEnumConstant(Color.class));
    assertEquals(Color.BLUE, in.readEnumConstant(Color.class));
  }

  @Test
  public void optionalRoundTrip() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeOptional(Optional.empty(), OutputBuffer::writeUTF);
    out.writeOptional(Optional.of("present"), OutputBuffer::writeUTF);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(Optional.empty(), in.readOptional(InputBuffer::readUTF));
    assertEquals(Optional.of("present"), in.readOptional(InputBuffer::readUTF));
  }

  @Test
  public void listRoundTrip() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeList(List.of(1, 2, 3), OutputBuffer::writeInt);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(List.of(1, 2, 3), in.readList(Integer[]::new, InputBuffer::readInt));
  }

  @Test
  public void listRejectsOutOfBoundsSize() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeInt(-1);
    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertThrows(
        IllegalStateException.class, () -> in.readList(Integer[]::new, InputBuffer::readInt));
  }

  @Test
  public void listRejectsOversizedSize() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeInt(5);
    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertThrows(
        IllegalStateException.class,
        () -> in.readList(Integer[]::new, InputBuffer::readInt, 3));
  }

  @Test
  public void uuidRoundTrip() {
    final UUID uuid = UUID.randomUUID();
    final OutputBuffer out = OutputBuffer.create();
    out.writeUUID(uuid);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(uuid, in.readUUID());
  }

  @Test
  public void nullableRoundTrip() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeNullable(null, OutputBuffer::writeUTF);
    out.writeNullable("value", OutputBuffer::writeUTF);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertNull(in.readNullable(InputBuffer::readUTF));
    assertEquals("value", in.readNullable(InputBuffer::readUTF));
  }

  @Test
  public void readingPastEndThrows() {
    final OutputBuffer out = OutputBuffer.create();
    out.writeInt(42);

    final InputBuffer in = InputBuffer.create(out.toByteArray());
    assertEquals(42, in.readInt());
    assertThrows(IllegalStateException.class, in::readInt);
  }

  private enum Color {
    RED,
    GREEN,
    BLUE
  }
}
