package xyz.spigotrce.interlink.packet;

import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;

/**
 * A handshake packet carrying the username chosen by the connecting client.
 *
 * @param username the player name
 * @author SpigotRCE
 */
public record HandshakePacket(String username) implements Packet<HandshakePacket> {
  /**
   * The {@link PacketCodec} that reads and writes {@link HandshakePacket}s from and to buffers.
   */
  public static final PacketCodec<HandshakePacket> CODEC =
      PacketCodec.of(HandshakePacket::new, HandshakePacket::write);

  /**
   * Decodes a {@link HandshakePacket} by reading the username from the given buffer.
   *
   * @param buffer the buffer to read the username from
   */
  public HandshakePacket(final InputBuffer buffer) {
    this(buffer.readUTF());
  }

  /**
   * Writes the username to the given buffer.
   *
   * @param buffer the buffer to write the username to
   */
  public void write(final OutputBuffer buffer) {
    buffer.writeUTF(username);
  }

  /**
   * Returns the {@link PacketCodec} used to encode and decode this packet.
   *
   * @return {@link #CODEC}
   */
  @Override
  public PacketCodec<HandshakePacket> getCodec() {
    return CODEC;
  }
}
