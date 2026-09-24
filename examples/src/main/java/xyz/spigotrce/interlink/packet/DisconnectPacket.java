package xyz.spigotrce.interlink.packet;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;

/**
 * A disconnect packet carrying the reason a connection was terminated.
 *
 * @param message the disconnect reason
 * @author SpigotRCE
 */
public record DisconnectPacket(String message) implements Packet<DisconnectPacket> {
  /**
   * The {@link PacketCodec} that reads and writes {@link DisconnectPacket}s from and to buffers.
   */
  public static final PacketCodec<DisconnectPacket> CODEC =
      PacketCodec.of(DisconnectPacket::new, DisconnectPacket::write);

  /**
   * Decodes a {@link DisconnectPacket} by reading the message from the given buffer.
   *
   * @param buffer the buffer to read the message from
   */
  public DisconnectPacket(final InputBuffer buffer) {
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
  public PacketCodec<DisconnectPacket> getCodec() {
    return CODEC;
  }
}
