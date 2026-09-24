package xyz.spigotrce.interlink.packet;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;

/**
 * A login success packet announcing that the login phase completed.
 *
 * @param compressionThreshold the minimum packet size, in bytes, at which messages are compressed
 * @author SpigotRCE
 */
public record LoginSuccessPacket(int compressionThreshold) implements Packet<LoginSuccessPacket> {
  /**
   * The {@link PacketCodec} that reads and writes {@link LoginSuccessPacket}s from and to buffers.
   */
  public static final PacketCodec<LoginSuccessPacket> CODEC =
      PacketCodec.of(LoginSuccessPacket::new, LoginSuccessPacket::write);

  /**
   * Decodes a {@link LoginSuccessPacket} by reading the compression threshold from the given
   * buffer.
   *
   * @param buffer the buffer to read the compression threshold from
   */
  public LoginSuccessPacket(final InputBuffer buffer) {
    this(buffer.readInt());
  }

  /**
   * Writes the compression threshold to the given buffer.
   *
   * @param buffer the buffer to write the compression threshold to
   */
  public void write(final OutputBuffer buffer) {
    buffer.writeInt(compressionThreshold);
  }

  /**
   * Returns the {@link PacketCodec} used to encode and decode this packet.
   *
   * @return {@link #CODEC}
   */
  @Override
  public PacketCodec<LoginSuccessPacket> getCodec() {
    return CODEC;
  }
}
