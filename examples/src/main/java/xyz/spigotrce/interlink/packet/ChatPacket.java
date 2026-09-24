package xyz.spigotrce.interlink.packet;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;

/**
 * A chat message packet carrying a single text message.
 *
 * @param message the chat message text
 * @author SpigotRCE
 */
public record ChatPacket(String message) implements Packet<ChatPacket> {
  /**
   * The {@link PacketCodec} that reads and writes {@link ChatPacket}s from and to buffers.
   */
  public static final PacketCodec<ChatPacket> CODEC =
      PacketCodec.of(ChatPacket::new, ChatPacket::write);

  /**
   * Decodes a {@link ChatPacket} by reading the message from the given buffer.
   *
   * @param buffer the buffer to read the message from
   */
  public ChatPacket(final InputBuffer buffer) {
    this(buffer.readUTF());
  }

  /**
   * Writes the message to the given buffer.
   *
   * @param buffer the buffer to write the message to
   */
  public void write(final OutputBuffer buffer) {
    buffer.writeUTF(message);
  }

  /**
   * Returns the {@link PacketCodec} used to encode and decode this packet.
   *
   * @return {@link #CODEC}
   */
  @Override
  public PacketCodec<ChatPacket> getCodec() {
    return CODEC;
  }
}
