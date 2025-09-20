package io.github.spigotrce.interlink.buf;

import com.google.common.io.ByteArrayDataOutput;
import io.github.spigotrce.interlink.packet.*;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

public class OutputBuffer implements ByteArrayDataOutput {
  final DataOutput output;
  final ByteArrayOutputStream byteArrayOutputStream;

  public OutputBuffer(ByteArrayOutputStream byteArrayOutputStream) {
    this.byteArrayOutputStream = byteArrayOutputStream;
    this.output = new DataOutputStream(byteArrayOutputStream);
  }

  public static OutputBuffer create() {
    return new OutputBuffer(new ByteArrayOutputStream());
  }

  public void write(int b) {
    try {
      this.output.write(b);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void write(byte[] b) {
    try {
      this.output.write(b);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void write(byte[] b, int off, int len) {
    try {
      this.output.write(b, off, len);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeBoolean(boolean v) {
    try {
      this.output.writeBoolean(v);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeByte(int v) {
    try {
      this.output.writeByte(v);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeBytes(String s) {
    try {
      this.output.writeBytes(s);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeChar(int v) {
    try {
      this.output.writeChar(v);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeChars(String s) {
    try {
      this.output.writeChars(s);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeDouble(double v) {
    try {
      this.output.writeDouble(v);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeFloat(float v) {
    try {
      this.output.writeFloat(v);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeInt(int v) {
    try {
      this.output.writeInt(v);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeLong(long v) {
    try {
      this.output.writeLong(v);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeShort(int v) {
    try {
      this.output.writeShort(v);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeUTF(String s) {
    try {
      this.output.writeUTF(s);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void writeEnumConstant(Enum<?> instance) {
    writeInt(instance.ordinal());
  }

  public void writePacket(Packet<?> packet) {
    @SuppressWarnings("unchecked") PacketCodec<Packet<?>> codec =(PacketCodec<Packet<?>>) packet.getCodec();
    codec.write(packet, this);
  }

  public <T> void writeOptional(T value, BiConsumer<OutputBuffer, T> writer) {
    if (value == null) {
      this.writeBoolean(false);
    } else {
      this.writeBoolean(true);
      writer.accept(this, value);
    }
  }

  public <T> void writeList(List<T> list, BiConsumer<OutputBuffer, T> writer) {
    this.writeInt(list.size());
    for (T item : list) {
      writer.accept(this, item);
    }
  }

  public void writeUUID(UUID uuid) {
    this.writeLong(uuid.getMostSignificantBits());
    this.writeLong(uuid.getLeastSignificantBits());
  }

  public <T> void writeNullable(T value, BiConsumer<OutputBuffer, T> writer) {
    if (value == null) {
      this.writeBoolean(false);
    } else {
      this.writeBoolean(true);
      writer.accept(this, value);
    }
  }

  public byte[] toByteArray() {
    return this.byteArrayOutputStream.toByteArray();
  }
}
