package xyz.spigotrce.interlink.packet;

public enum PlayPackets implements PacketType {
  DISCONNECT(DisconnectPacket.class, DisconnectPacket.CODEC),
  CHAT(ChatPacket.class, ChatPacket.CODEC);

  private final Class<? extends Packet<?>> packetClass;
  private final PacketCodec<?> codec;

  PlayPackets(final Class<? extends Packet<?>> packetClass, final PacketCodec<?> codec) {
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
