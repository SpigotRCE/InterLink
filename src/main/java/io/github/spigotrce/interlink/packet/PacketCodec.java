package io.github.spigotrce.interlink.packet;

import io.github.spigotrce.interlink.buf.*;

import java.util.function.*;

public class PacketCodec<T> {
  private final Function<InputBuffer, T> reader;
  private final BiConsumer<T, OutputBuffer> writer;

  private PacketCodec(Function<InputBuffer, T> reader, BiConsumer<T, OutputBuffer> writer) {
    this.reader = reader;
    this.writer = writer;
  }

  public static <T> PacketCodec<T> of(Function<InputBuffer, T> reader,
    BiConsumer<T, OutputBuffer> writer) {
    return new PacketCodec<>(reader, writer);
  }

  public T read(InputBuffer in) {
    return reader.apply(in);
  }

  public void write(T value, OutputBuffer out) {
    writer.accept(value, out);
  }
}
