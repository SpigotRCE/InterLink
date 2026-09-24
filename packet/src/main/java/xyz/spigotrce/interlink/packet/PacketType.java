package xyz.spigotrce.interlink.packet;

/**
 * A marker interface implemented by shared packet enums. Each enum constant exposes the packet
 * {@link Class} and its {@link PacketCodec}. The enum declaration order determines the packet ids.
 *
 * @author SpigotRCE
 */
public interface PacketType {
  Class<? extends Packet<?>> packetClass();

  PacketCodec<?> codec();
}
