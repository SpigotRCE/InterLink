package io.github.spigotrce.interlink.packet;

import io.github.spigotrce.interlink.buf.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * A packet registry holds all packets for the current phase of the network. A packet registry should be the same for
 * both sides of a connection but can have different handlers. A packet handler can have
 */
public class PacketRegistry {
  public final List<PacketEntry<? extends Packet<?>>> packets = new ArrayList<>();

  public <T extends Packet<?>> void registerPacket(Class<T> packetClass, PacketCodec<T> codec, Consumer<T> handler) {
    packets.add(new PacketEntry<>(packetClass, codec, handler));
  }

  public <T extends Packet<?>> void registerPacket(Class<T> packetClass, PacketCodec<T> codec) {
    packets.add(new PacketEntry<>(packetClass, codec, (packet) -> {
      throw new IllegalArgumentException("No handler registered for packet: " + packetClass.getName());
    }));
  }

  public void encode(Packet<?> packet, OutputBuffer out) {
    @SuppressWarnings("unchecked") PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(getId(packet));
    entry.codec().write(packet, out);
  }

  public int getId(Packet<?> packet) {
    for (PacketEntry<? extends Packet<?>> entry : packets) {
      if (entry.clazz().equals(packet.getClass())) {
        return packets.indexOf(entry);
      }
    }
    return -1;
  }

  public Packet<?> decode(int id, InputBuffer in) {
    @SuppressWarnings("unchecked") PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(id);
    return entry.codec().read(in);
  }

  public void handle(Packet<?> packet) {
    @SuppressWarnings("unchecked") PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(getId(packet));
    entry.handler.accept(packet);
  }

  public record PacketEntry<T extends Packet<?>>(Class<T> clazz, PacketCodec<T> codec, Consumer<T> handler) {
  }
}
