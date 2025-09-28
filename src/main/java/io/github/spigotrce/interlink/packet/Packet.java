package io.github.spigotrce.interlink.packet;

/**
 * Packet interface
 *
 * @param <T> Type of the packet
 *
 * @author SpigotRCE
 */
public interface Packet<T> {
  PacketCodec<T> getCodec();
}
