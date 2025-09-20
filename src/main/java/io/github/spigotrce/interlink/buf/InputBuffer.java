package io.github.spigotrce.interlink.buf;

import com.google.common.io.ByteArrayDataInput;
import io.github.spigotrce.interlink.packet.*;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.function.*;

public class InputBuffer implements ByteArrayDataInput {
  final DataInput input;

  InputBuffer(ByteArrayInputStream byteArrayInputStream) {
    this.input = new DataInputStream(byteArrayInputStream);
  }

  public static InputBuffer create(byte[] data) {
    return new InputBuffer(new ByteArrayInputStream(data));
  }

  public void readFully(byte[] b) {
    try {
      this.input.readFully(b);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public void readFully(byte[] b, int off, int len) {
    try {
      this.input.readFully(b, off, len);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public int skipBytes(int n) {
    try {
      return this.input.skipBytes(n);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean readBoolean() {
    try {
      return this.input.readBoolean();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public byte readByte() {
    try {
      return this.input.readByte();
    } catch (EOFException e) {
      throw new IllegalStateException(e);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public int readUnsignedByte() {
    try {
      return this.input.readUnsignedByte();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public short readShort() {
    try {
      return this.input.readShort();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public int readUnsignedShort() {
    try {
      return this.input.readUnsignedShort();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public char readChar() {
    try {
      return this.input.readChar();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public int readInt() {
    try {
      return this.input.readInt();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public long readLong() {
    try {
      return this.input.readLong();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public float readFloat() {
    try {
      return this.input.readFloat();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public double readDouble() {
    try {
      return this.input.readDouble();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public @Nullable String readLine() {
    try {
      return this.input.readLine();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public String readUTF() {
    try {
      return this.input.readUTF();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public <T extends Enum<T>> T readEnumConstant(Class<T> enumClass) {
    return enumClass.getEnumConstants()[this.readInt()];
  }

  public <T extends Packet<?>> T readData(Packet<T> packet) {
    PacketCodec<T> codec = packet.getCodec();
    return codec.read(this);
  }

  public <T> Optional<T> readOptional(Function<InputBuffer, T> reader) {
    boolean present = this.readBoolean();
    if (!present) {
      return Optional.empty();
    }
    return Optional.of(reader.apply(this));
  }

  public <T> List<T> readList(IntFunction<T[]> generator, Function<InputBuffer, T> reader) {
    int size = this.readInt();
    List<T> list = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      list.add(reader.apply(this));
    }
    return list;
  }

  public UUID readUUID() {
    return new UUID(this.readLong(), this.readLong());
  }

  public <T> T readNullable(Function<InputBuffer, T> reader) {
    return readBoolean() ? reader.apply(this) : null;
  }
}
