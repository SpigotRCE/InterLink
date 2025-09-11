package io.github.spigotrce.interlink.packet;

import com.google.common.io.*;

import java.util.function.*;

public class PacketCodec<T> {
  private final Function<ByteArrayDataInput, T> reader;
  private final BiConsumer<T, ByteArrayDataOutput> writer;

  private PacketCodec(Function<ByteArrayDataInput, T> reader, BiConsumer<T, ByteArrayDataOutput> writer) {
    this.reader = reader;
    this.writer = writer;
  }

  public static <T> PacketCodec<T> of(Function<ByteArrayDataInput, T> reader,
    BiConsumer<T, ByteArrayDataOutput> writer) {
    return new PacketCodec<>(reader, writer);
  }

  public T read(ByteArrayDataInput in) {
    return reader.apply(in);
  }

  public void write(T value, ByteArrayDataOutput out) {
    writer.accept(value, out);
  }
}
