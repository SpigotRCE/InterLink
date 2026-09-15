package io.github.spigotrce.interlink.buf;

import com.google.common.io.ByteArrayDataOutput;
import io.github.spigotrce.interlink.packet.*;
import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

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

  public void write(final byte[] b) {
    try {
      output.write(b);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void write(final byte[] b, final int off, final int len) {
    try {
      output.write(b, off, len);
    } catch (final IOException impossible) {
      throw new AssertionError(impossible);
    }
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

  public void writeNestedPacket(final Packet<?> packet) {
    @SuppressWarnings("unchecked")
    final PacketCodec<Packet<?>> codec = (PacketCodec<Packet<?>>) packet.getCodec();
    codec.write(packet, this);
  }

  public <T> void writeOptional(final T value, final BiConsumer<OutputBuffer, T> writer) {
    if (value == null) {
      writeBoolean(false);
    } else {
      writeBoolean(true);
      writer.accept(this, value);
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
}
