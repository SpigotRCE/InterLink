package xyz.spigotrce.interlink.packet;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A Packet codec which is used to parse reader and writer methods.
 *
 * @param <T> Type of the packet
 * @author SpigotRCE
 */
public final class PacketCodec<T> {
  private final Function<InputBuffer, T> reader;
  private final BiConsumer<T, OutputBuffer> writer;

  private PacketCodec(
      final Function<InputBuffer, T> reader, final BiConsumer<T, OutputBuffer> writer) {
    this.reader = reader;
    this.writer = writer;
  }

  /**
   * Creates a new {@link PacketCodec} from the given reader and writer functions.
   *
   * @param <T> Type of the packet.
   * @param reader The function that decodes a packet from an {@link InputBuffer}.
   * @param writer The function that encodes a packet into an {@link OutputBuffer}.
   * @return A new {@link PacketCodec} for {@code T}.
   */
  public static <T> PacketCodec<T> of(
      final Function<InputBuffer, T> reader, final BiConsumer<T, OutputBuffer> writer) {
    return new PacketCodec<>(reader, writer);
  }

  /**
   * Decodes a packet from the given input buffer.
   *
   * @param in The buffer to read the packet from.
   * @return The decoded packet.
   */
  public T read(final InputBuffer in) {
    return reader.apply(in);
  }

  /**
   * Encodes the given packet into the output buffer.
   *
   * @param value The packet to encode.
   * @param out The buffer to write the packet into.
   */
  public void write(final T value, final OutputBuffer out) {
    writer.accept(value, out);
  }
}
