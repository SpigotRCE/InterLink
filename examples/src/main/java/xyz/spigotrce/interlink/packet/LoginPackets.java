package xyz.spigotrce.interlink.packet;

public enum LoginPackets implements PacketType {
  HANDSHAKE(HandshakePacket.class, HandshakePacket.CODEC),
  DISCONNECT(DisconnectPacket.class, DisconnectPacket.CODEC),
  LOGIN_SUCCESS(LoginSuccessPacket.class, LoginSuccessPacket.CODEC);

  private final Class<? extends Packet<?>> packetClass;
  private final PacketCodec<?> codec;

  LoginPackets(final Class<? extends Packet<?>> packetClass, final PacketCodec<?> codec) {
    this.packetClass = packetClass;
    this.codec = codec;
  }

  @Override
  public Class<? extends Packet<?>> packetClass() {
    return packetClass;
  }

  @Override
  public PacketCodec<?> codec() {
    return codec;
  }
}
