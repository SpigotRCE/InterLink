package io.github.spigotrce.interlink.packet;

@FunctionalInterface
public interface PacketHandler<T> {
  void handle(Packet<T> packet);
}
