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

/**
 * A lightweight, buffered input stream used to decode wire data written by {@link OutputBuffer}.
 *
 * <p>Reads are delegated to a {@link DataInputStream} wrapping a {@link ByteArrayInputStream}.
 * Composite values follow the format produced by {@link OutputBuffer}: byte arrays are prefixed
 * with a length {@code int}, {@link #readEnumConstant} stores the enum ordinal as an {@code int},
 * {@link #readUUID} is encoded as two {@code long}s, and nullable/optional values are preceded by a
 * presence {@code boolean}.
 *
 * <p>Nullability is modeled with the jspecify {@link Nullable} annotation. Callers that use a
 * static analysis checker can rely on this contract.
 */
public class InputBuffer implements ByteArrayDataInput {
  final DataInput input;

  InputBuffer(final ByteArrayInputStream byteArrayInputStream) {
    input = new DataInputStream(byteArrayInputStream);
  }

  /**
   * Creates a new {@link InputBuffer} over the given backing bytes.
   *
   * @param data The bytes to read from.
   * @return A new {@link InputBuffer} positioned at the start of the data.
   */
  public static InputBuffer create(final byte[] data) {
    return new InputBuffer(new ByteArrayInputStream(data));
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated Use {@link #readByteArray()} instead. This method is read out of a fixed-size
   *     buffer and has no stream-aware length bookkeeping.
   */
  @Deprecated
  public void readFully(final byte[] b) {
    try {
      input.readFully(b);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated Use {@link #readByteArray()} instead. This method is read out of a fixed-size
   *     buffer and has no stream-aware length bookkeeping.
   */
  @Deprecated
  public void readFully(final byte[] b, final int off, final int len) {
    try {
      input.readFully(b, off, len);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Skips {@code n} bytes of input, discarding them.
   *
   * @param n The number of bytes to skip.
   * @return The number of bytes actually skipped, which may be less than {@code n} at the end of
   *     the stream.
   * @throws IllegalStateException If an {@link IOException} occurs while skipping.
   */
  public int skipBytes(final int n) {
    try {
      return input.skipBytes(n);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads one {@code boolean} (one byte: nonzero is {@code true}).
   *
   * @return The read {@code boolean}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public boolean readBoolean() {
    try {
      return input.readBoolean();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads and returns one signed 8-bit {@code byte}.
   *
   * @return The read {@code byte}, sign-extended to an {@code int}.
   * @throws IllegalStateException If the stream is exhausted or an {@link IOException} occurs.
   */
  public byte readByte() {
    try {
      return input.readByte();
    } catch (final EOFException e) {
      throw new IllegalStateException(e);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Reads one unsigned 8-bit value.
   *
   * @return The read value as an {@code int} in the range {@code 0} to {@code 255}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public int readUnsignedByte() {
    try {
      return input.readUnsignedByte();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads and returns one signed 16-bit {@code short}, big-endian.
   *
   * @return The read {@code short}, sign-extended to an {@code int}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public short readShort() {
    try {
      return input.readShort();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads one unsigned 16-bit value.
   *
   * @return The read value as an {@code int} in the range {@code 0} to {@code 65535}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public int readUnsignedShort() {
    try {
      return input.readUnsignedShort();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads one 16-bit value as a {@code char}, big-endian.
   *
   * @return The read {@code char}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public char readChar() {
    try {
      return input.readChar();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads and returns one signed 32-bit {@code int}, big-endian.
   *
   * @return The read {@code int}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public int readInt() {
    try {
      return input.readInt();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads and returns one signed 64-bit {@code long}, big-endian.
   *
   * @return The read {@code long}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public long readLong() {
    try {
      return input.readLong();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads and returns one 32-bit {@code float}, big-endian.
   *
   * @return The read {@code float}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public float readFloat() {
    try {
      return input.readFloat();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads and returns one 64-bit {@code double}, big-endian.
   *
   * @return The read {@code double}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public double readDouble() {
    try {
      return input.readDouble();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads the next line of text as {@link String}. Note that the underlying {@link DataInput}
   * implementation reads bytes until a line terminator or end of stream.
   *
   * @return The read line, or {@code null} if the stream is exhausted.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public @Nullable String readLine() {
    try {
      return input.readLine();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads a string encoded in a modified UTF-8 format, prefixed with an unsigned 16-bit length.
   *
   * @return The decoded {@link String}.
   * @throws IllegalStateException If an {@link IOException} occurs while reading.
   */
  public String readUTF() {
    try {
      return input.readUTF();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads a length-prefixed byte array. The leading {@code int} is the array length; that many
   * bytes are then read fully into a newly allocated array of {@code len} bytes.
   *
   * @return A new byte array of exactly {@code len} bytes holding the payload.
   * @throws IllegalStateException If an {@link IOException} occurs while reading the underlying
   *     stream.
   */
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

  /**
   * Reads an {@link Enum} constant by its ordinal. The ordinal was written as an {@code int}, so it
   * is read with {@link #readInt()} and used to index into the enum's constants.
   *
   * @param <T> The enum type.
   * @param enumClass The {@link Class} of the enum to read.
   * @return The constant of the given enum at the stored ordinal.
   * @throws ArrayIndexOutOfBoundsException If the stored ordinal does not match any constant.
   */
  public <T extends Enum<T>> T readEnumConstant(final Class<T> enumClass) {
    return enumClass.getEnumConstants()[readInt()];
  }

  /**
   * Reads an {@link Optional} value. A presence {@code boolean} is read first; if it is {@code
   * false} the result is {@link Optional#empty()}, otherwise the value is decoded by the given
   * reader and wrapped.
   *
   * @param <T> The value type.
   * @param reader The function that decodes the value from this buffer when present.
   * @return An {@link Optional} describing the value, or {@link Optional#empty()} if absent.
   */
  public <T> Optional<T> readOptional(final Function<InputBuffer, T> reader) {
    final boolean present = readBoolean();
    if (!present) {
      return Optional.empty();
    }
    return Optional.of(reader.apply(this));
  }

  /**
   * Reads a list with no explicit size limit, delegating to {@link #readList(IntFunction, Function,
   * int)} with {@link Integer#MAX_VALUE}.
   *
   * @param <T> The element type.
   * @param generator The function that produces an array of the list size; accepted for API
   *     symmetry but not used by this implementation.
   * @param reader The function that decodes each element from this buffer.
   * @return The decoded list.
   */
  public <T> List<T> readList(
      final IntFunction<T[]> generator, final Function<InputBuffer, T> reader) {
    return readList(generator, reader, Integer.MAX_VALUE);
  }

  /**
   * Reads a length-prefixed list where each element is decoded by the given reader, enforced to be
   * at most {@code maxSize} elements.
   *
   * @param <T> The element type.
   * @param generator The function that produces an array of the list size; accepted for API
   *     symmetry but not used by this implementation.
   * @param reader The function that decodes each element from this buffer.
   * @param maxSize The maximum allowed list size.
   * @return The decoded list.
   * @throws IllegalStateException If the declared size is negative or larger than {@code maxSize}.
   */
  public <T> List<T> readList(
      final IntFunction<T[]> generator, final Function<InputBuffer, T> reader, final int maxSize) {
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

  /**
   * Reads a {@link UUID} encoded as two {@code long}s: the most significant bits followed by the
   * least significant bits.
   *
   * @return The decoded {@link UUID}.
   */
  public UUID readUUID() {
    return new UUID(readLong(), readLong());
  }

  /**
   * Reads a nested {@link Packet} by delegating to its {@link PacketCodec}.
   *
   * @param <T> The packet type.
   * @param codec The codec used to decode the nested packet from this buffer.
   * @return The decoded packet.
   */
  public <T extends Packet<?>> T readNestedPacket(final PacketCodec<T> codec) {
    return codec.read(this);
  }

  /**
   * Reads a nullable value. A presence {@code boolean} is read first; if it is {@code false} the
   * result is {@code null}, otherwise the value is decoded by the given reader.
   *
   * @param <T> The value type.
   * @param reader The function that decodes the value from this buffer when present.
   * @return The decoded value, or {@code null} if absent.
   */
  public <T> T readNullable(final Function<InputBuffer, T> reader) {
    return readBoolean() ? reader.apply(this) : null;
  }
}
