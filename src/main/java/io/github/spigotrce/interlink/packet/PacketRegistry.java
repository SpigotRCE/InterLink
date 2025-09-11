package io.github.spigotrce.interlink.packet;

import com.google.common.io.*;

import java.util.*;

public class PacketRegistry {
  public final List<PacketEntry<? extends Packet<?>>> packets = new ArrayList<>();

  public <T extends Packet<?>> void registerPacket(Class<T> packetClass, PacketCodec<T> codec) {
    packets.add(new PacketEntry<>(packetClass, codec));
  }

  public void encode(Packet<?> packet, ByteArrayDataOutput out) {
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

  public Packet<?> decode(int id, ByteArrayDataInput in) {
    @SuppressWarnings("unchecked") PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(id);
    return entry.codec().read(in);
  }

  public record PacketEntry<T extends Packet<?>>(Class<T> clazz, PacketCodec<T> codec) {
  }
}
