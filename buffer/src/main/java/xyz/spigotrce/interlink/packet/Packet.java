package xyz.spigotrce.interlink.packet;

/**
 * Packet interface.
 *
 * @param <T> Type of the packet
 * @author SpigotRCE
 */
public interface Packet<T> {
  /**
   * Getter for the packet {@link PacketCodec}.
   *
   * @return The PacketCodec.
   */
  PacketCodec<T> getCodec();
}
