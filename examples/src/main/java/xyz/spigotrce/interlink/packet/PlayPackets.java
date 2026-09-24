package xyz.spigotrce.interlink.packet;

/**
 * {@link PacketType} enum for the play phase of the chat demo.
 *
 * <p>Each constant binds a play packet to its {@link Class} and {@link PacketCodec}. Declaration
 * order defines the packet ids used on the wire.
 *
 * @author SpigotRCE
 */
public enum PlayPackets implements PacketType {
  /**
   * The {@link DisconnectPacket} used to terminate the connection during play.
   */
  DISCONNECT(DisconnectPacket.class, DisconnectPacket.CODEC),

  /**
   * The {@link ChatPacket} carrying a chat message between players.
   */
  CHAT(ChatPacket.class, ChatPacket.CODEC);

  private final Class<? extends Packet<?>> packetClass;
  private final PacketCodec<?> codec;

  PlayPackets(final Class<? extends Packet<?>> packetClass, final PacketCodec<?> codec) {
    this.packetClass = packetClass;
    this.codec = codec;
  }

  /**
   * Returns the {@link Class} of the {@link Packet} represented by this enum constant.
   *
   * @return the packet {@link Class}
   */
  @Override
  public Class<? extends Packet<?>> packetClass() {
    return packetClass;
  }

  /**
   * Returns the {@link PacketCodec} used to encode and decode this {@link Packet}.
   *
   * @return the packet {@link PacketCodec}
   */
  @Override
  public PacketCodec<?> codec() {
    return codec;
  }
}
