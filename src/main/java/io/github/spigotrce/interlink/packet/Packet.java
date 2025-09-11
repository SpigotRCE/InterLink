package io.github.spigotrce.interlink.packet;

public interface Packet<T> {
  PacketCodec<T> getCodec();
}
