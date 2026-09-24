package xyz.spigotrce.interlink.packet;

/**
 * {@link PacketType} enum for the login phase of the chat demo.
 *
 * <p>Each constant binds a login packet to its {@link Class} and {@link PacketCodec}. Declaration
 * order defines the packet ids used on the wire.
 *
 * @author SpigotRCE
 */
public enum LoginPackets implements PacketType {
  /**
   * The {@link HandshakePacket} sent by the client at the start of the login phase.
   */
  HANDSHAKE(HandshakePacket.class, HandshakePacket.CODEC),

  /**
   * The {@link DisconnectPacket} used to reject a login or report a connection failure.
   */
  DISCONNECT(DisconnectPacket.class, DisconnectPacket.CODEC),

  /**
   * The {@link LoginSuccessPacket} sent by the server to accept a login.
   */
  LOGIN_SUCCESS(LoginSuccessPacket.class, LoginSuccessPacket.CODEC);

  private final Class<? extends Packet<?>> packetClass;
  private final PacketCodec<?> codec;

  LoginPackets(final Class<? extends Packet<?>> packetClass, final PacketCodec<?> codec) {
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
