package xyz.spigotrce.interlink.buf;

import com.google.common.io.ByteArrayDataInput;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;
import xyz.spigotrce.interlink.packet.Packet;
import xyz.spigotrce.interlink.packet.PacketCodec;

public class InputBuffer implements ByteArrayDataInput {
  final DataInput input;

  InputBuffer(final ByteArrayInputStream byteArrayInputStream) {
    input = new DataInputStream(byteArrayInputStream);
  }

  public static InputBuffer create(final byte[] data) {
    return new InputBuffer(new ByteArrayInputStream(data));
  }

  @Deprecated
  public void readFully(final byte[] b) {
    try {
      input.readFully(b);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Deprecated
  public void readFully(final byte[] b, final int off, final int len) {
    try {
      input.readFully(b, off, len);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public byte[] readByteArray() {
    try {
      final int len = readInt();
      final byte[] b = new byte[len];
      input.readFully(b);
      return b;
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public int skipBytes(final int n) {
    try {
      return input.skipBytes(n);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean readBoolean() {
    try {
      return input.readBoolean();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public byte readByte() {
    try {
      return input.readByte();
    } catch (final EOFException e) {
      throw new IllegalStateException(e);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public int readUnsignedByte() {
    try {
      return input.readUnsignedByte();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public short readShort() {
    try {
      return input.readShort();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public int readUnsignedShort() {
    try {
      return input.readUnsignedShort();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public char readChar() {
    try {
      return input.readChar();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public int readInt() {
    try {
      return input.readInt();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public long readLong() {
    try {
      return input.readLong();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public float readFloat() {
    try {
      return input.readFloat();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public double readDouble() {
    try {
      return input.readDouble();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public @Nullable String readLine() {
    try {
      return input.readLine();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public String readUTF() {
    try {
      return input.readUTF();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public <T extends Enum<T>> T readEnumConstant(final Class<T> enumClass) {
    return enumClass.getEnumConstants()[readInt()];
  }

  public <T> Optional<T> readOptional(final Function<InputBuffer, T> reader) {
    final boolean present = readBoolean();
    if (!present) {
      return Optional.empty();
    }
    return Optional.of(reader.apply(this));
  }

  public <T> List<T> readList(
      final IntFunction<T[]> generator, final Function<InputBuffer, T> reader) {
    return readList(generator, reader, Integer.MAX_VALUE);
  }

  public <T> List<T> readList(
      final IntFunction<T[]> generator,
      final Function<InputBuffer, T> reader,
      final int maxSize) {
    final int size = readInt();
    if (size < 0 || size > maxSize) {
      throw new IllegalStateException("List size out of bounds: " + size);
    }
    final List<T> list = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      list.add(reader.apply(this));
    }
    return list;
  }

  public UUID readUUID() {
    return new UUID(readLong(), readLong());
  }

  public <T extends Packet<?>> T readNestedPacket(final PacketCodec<T> codec) {
    return codec.read(this);
  }

  public <T> T readNullable(final Function<InputBuffer, T> reader) {
    return readBoolean() ? reader.apply(this) : null;
  }
}
