package xyz.spigotrce.interlink.packet;

/**
 * A marker interface implemented by shared packet enums. Each enum constant exposes the packet
 * {@link Class} and its {@link PacketCodec}. The enum declaration order determines the packet ids.
 *
 * @author SpigotRCE
 */
public interface PacketType {
  /**
   * Returns the {@link Class} of the {@link Packet} represented by this enum constant.
   *
   * @return the packet {@link Class}
   */
  Class<? extends Packet<?>> packetClass();

  /**
   * Returns the {@link PacketCodec} used to encode and decode this {@link Packet}.
   *
   * @return the packet {@link PacketCodec}
   */
  PacketCodec<?> codec();
}
