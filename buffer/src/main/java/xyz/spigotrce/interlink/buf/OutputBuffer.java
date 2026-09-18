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

public class OutputBuffer implements ByteArrayDataOutput {
  final DataOutput output;
  final ByteArrayOutputStream byteArrayOutputStream;

  public OutputBuffer(final ByteArrayOutputStream byteArrayOutputStream) {
    this.byteArrayOutputStream = byteArrayOutputStream;
    output = new DataOutputStream(byteArrayOutputStream);
  }

  public static OutputBuffer create() {
    return new OutputBuffer(new ByteArrayOutputStream());
  }

  public void write(final int b) {
    try {
      output.write(b);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  @Deprecated
  public void write(final byte[] b) {
    try {
      output.write(b);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  @Deprecated
  public void write(final byte[] b, final int off, final int len) {
    try {
      output.write(b, off, len);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeByteArray(final byte[] b) {
    writeInt(b.length);
    write(b);
  }

  public void writeBoolean(final boolean v) {
    try {
      output.writeBoolean(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeByte(final int v) {
    try {
      output.writeByte(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeShort(final int v) {
    try {
      output.writeShort(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeChar(final int v) {
    try {
      output.writeChar(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeInt(final int v) {
    try {
      output.writeInt(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeLong(final long v) {
    try {
      output.writeLong(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeFloat(final float v) {
    try {
      output.writeFloat(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeDouble(final double v) {
    try {
      output.writeDouble(v);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeChars(final String s) {
    try {
      output.writeChars(s);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeUTF(final String s) {
    try {
      output.writeUTF(s);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeBytes(final String s) {
    try {
      output.writeBytes(s);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public byte[] toByteArray() {
    return byteArrayOutputStream.toByteArray();
  }

  public void writeEnumConstant(final Enum<?> instance) {
    writeInt(instance.ordinal());
  }

  public <T> void writeOptional(final Optional<T> value, final BiConsumer<OutputBuffer, T> writer) {
    if (value.isPresent()) {
      writeBoolean(true);
      writer.accept(this, value.get());
    } else {
      writeBoolean(false);
    }
  }

  public <T> void writeList(final List<T> list, final BiConsumer<OutputBuffer, T> writer) {
    writeInt(list.size());
    for (final T item : list) {
      writer.accept(this, item);
    }
  }

  public void writeUUID(final UUID uuid) {
    writeLong(uuid.getMostSignificantBits());
    writeLong(uuid.getLeastSignificantBits());
  }

  public <T> void writeNullable(final T value, final BiConsumer<OutputBuffer, T> writer) {
    if (value == null) {
      writeBoolean(false);
    } else {
      writeBoolean(true);
      writer.accept(this, value);
    }
  }

  public <T extends Packet<T>> void writeNestedPacket(final Packet<T> packet) {
    final PacketCodec<T> codec = (PacketCodec<T>) packet.getCodec();
    codec.write((T) packet, this);
  }
}
