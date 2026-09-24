package xyz.spigotrce.interlink.buf;

import com.google.common.io.ByteArrayDataOutput;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import xyz.spigotrce.interlink.packet.Packet;
import xyz.spigotrce.interlink.packet.PacketCodec;

/**
 * A lightweight, buffered output stream used to encode wire data consumable by {@link InputBuffer}.
 *
 * <p>Writes are delegated to a {@link DataOutputStream} wrapping a {@link ByteArrayOutputStream}.
 * Composite values follow a wire format the matching {@link InputBuffer} reads back: byte arrays
 * are prefixed with a length {@code int} via {@link #writeByteArray}, enum constants are stored as
 * their ordinal via {@link #writeEnumConstant}, {@link #writeUUID} emits two {@code long}s, and
 * nullable/optional values are preceded by a presence {@code boolean}.
 */
public class OutputBuffer implements ByteArrayDataOutput {
  final DataOutput output;
  final ByteArrayOutputStream byteArrayOutputStream;

  /**
   * Creates a new {@link OutputBuffer} that writes into the given stream.
   *
   * @param byteArrayOutputStream The stream the encoded bytes are written into.
   */
  public OutputBuffer(final ByteArrayOutputStream byteArrayOutputStream) {
    this.byteArrayOutputStream = byteArrayOutputStream;
    output = new DataOutputStream(byteArrayOutputStream);
  }

  /**
   * Creates a new empty {@link OutputBuffer}.
   *
   * @return A new {@link OutputBuffer} backed by a fresh {@link ByteArrayOutputStream}.
   */
  public static OutputBuffer create() {
    return new OutputBuffer(new ByteArrayOutputStream());
  }

  /**
   * Writes one byte, given as the low 8 bits of {@code b}.
   *
   * @param b The byte to write.
   */
  public void write(final int b) {
    try {
      output.write(b);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated Use {@link #writeByteArray(byte[])} instead; this method has no explicit
   *     stream-aware length bookkeeping.
   */
  @Deprecated
  public void write(final byte[] b) {
    try {
      output.write(b);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated Use {@link #writeByteArray(byte[])} instead; this method has no explicit
   *     stream-aware length bookkeeping.
   */
  @Deprecated
  public void write(final byte[] b, final int off, final int len) {
    try {
      output.write(b, off, len);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes a length-prefixed byte array: the array length as an {@code int} followed by the array
   * bytes. This is the counterpart of {@link InputBuffer#readByteArray()}.
   *
   * @param b The bytes to write.
   */
  public void writeByteArray(final byte[] b) {
    writeInt(b.length);
    write(b);
  }

  /**
   * Writes one {@code boolean} as a single byte: nonzero is {@code true}.
   *
   * @param v The {@code boolean} to write.
   */
  public void writeBoolean(final boolean v) {
    try {
      output.writeBoolean(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes one signed 8-bit {@code byte}, taking the low 8 bits of {@code v}.
   *
   * @param v The 8-bit value to write.
   */
  public void writeByte(final int v) {
    try {
      output.writeByte(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes one 16-bit {@code short}, big-endian, taking the low 16 bits of {@code v}.
   *
   * @param v The 16-bit value to write.
   */
  public void writeShort(final int v) {
    try {
      output.writeShort(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes one UTF-16 {@code char}, big-endian, taking the low 16 bits of {@code v}.
   *
   * @param v The 16-bit value to write.
   */
  public void writeChar(final int v) {
    try {
      output.writeChar(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes one signed 32-bit {@code int}, big-endian.
   *
   * @param v The {@code int} to write.
   */
  public void writeInt(final int v) {
    try {
      output.writeInt(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes one signed 64-bit {@code long}, big-endian.
   *
   * @param v The {@code long} to write.
   */
  public void writeLong(final long v) {
    try {
      output.writeLong(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes one 32-bit IEEE 754 {@code float}, big-endian.
   *
   * @param v The {@code float} to write.
   */
  public void writeFloat(final float v) {
    try {
      output.writeFloat(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes one 64-bit IEEE 754 {@code double}, big-endian.
   *
   * @param v The {@code double} to write.
   */
  public void writeDouble(final double v) {
    try {
      output.writeDouble(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes the characters of the given string as two bytes each, big-endian, without a length
   * prefix.
   *
   * @param s The string whose characters are written.
   */
  public void writeChars(final String s) {
    try {
      output.writeChars(s);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes a string in a modified UTF-8 format, prefixed with an unsigned 16-bit length.
   *
   * @param s The string to write.
   */
  public void writeUTF(final String s) {
    try {
      output.writeUTF(s);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Writes the low 8 bits of each character of the given string, without a length prefix.
   *
   * @param s The string whose bytes are written.
   */
  public void writeBytes(final String s) {
    try {
      output.writeBytes(s);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Returns a copy of the bytes written so far.
   *
   * @return A new byte array with the buffered contents.
   */
  public byte[] toByteArray() {
    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Writes an {@link Enum} constant by its {@link Enum#ordinal() ordinal}, encoded as an
   * {@code int}. This is the counterpart of {@link InputBuffer#readEnumConstant}.
   *
   * @param instance The enum constant to write.
   */
  public void writeEnumConstant(final Enum<?> instance) {
    writeInt(instance.ordinal());
  }

  /**
   * Writes an {@link Optional} value. A presence {@code boolean} is written first; if the value is
   * present, it is encoded by the given writer, mirroring {@link InputBuffer#readOptional}.
   *
   * @param <T> The value type.
   * @param value The optional value to write.
   * @param writer The function that encodes the value into this buffer when present.
   */
  public <T> void writeOptional(final Optional<T> value, final BiConsumer<OutputBuffer, T> writer) {
    if (value.isPresent()) {
      writeBoolean(true);
      writer.accept(this, value.get());
    } else {
      writeBoolean(false);
    }
  }

  /**
   * Writes a length-prefixed list: the element count as an {@code int} followed by each element
   * encoded by the given writer, mirroring {@link InputBuffer#readList}.
   *
   * @param <T> The element type.
   * @param list The elements to write.
   * @param writer The function that encodes each element into this buffer.
   */
  public <T> void writeList(final List<T> list, final BiConsumer<OutputBuffer, T> writer) {
    writeInt(list.size());
    for (final T item : list) {
      writer.accept(this, item);
    }
  }

  /**
   * Writes a {@link UUID} as two {@code long}s: the most significant bits followed by the least
   * significant bits, mirroring {@link InputBuffer#readUUID()}.
   *
   * @param uuid The {@link UUID} to write.
   */
  public void writeUUID(final UUID uuid) {
    writeLong(uuid.getMostSignificantBits());
    writeLong(uuid.getLeastSignificantBits());
  }

  /**
   * Writes a nullable value. A presence {@code boolean} is written first: {@code false} for a
   * {@code null} value, otherwise {@code true} followed by the value encoded by the given writer,
   * mirroring {@link InputBuffer#readNullable}.
   *
   * @param <T> The value type.
   * @param value The value to write, or {@code null}.
   * @param writer The function that encodes the value into this buffer when non-null.
   */
  public <T> void writeNullable(final T value, final BiConsumer<OutputBuffer, T> writer) {
    if (value == null) {
      writeBoolean(false);
    } else {
      writeBoolean(true);
      writer.accept(this, value);
    }
  }

  /**
   * Writes a nested {@link Packet} by delegating to the codec obtained from
   * {@link Packet#getCodec()}, mirroring {@link InputBuffer#readNestedPacket}.
   *
   * @param <T> The packet type.
   * @param packet The packet to encode into this buffer.
   */
  public <T extends Packet<T>> void writeNestedPacket(final Packet<T> packet) {
    final PacketCodec<T> codec = (PacketCodec<T>) packet.getCodec();
    codec.write((T) packet, this);
  }
}
