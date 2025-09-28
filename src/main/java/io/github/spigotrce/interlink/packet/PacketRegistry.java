package io.github.spigotrce.interlink.packet;

import io.github.spigotrce.interlink.buf.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * A packet registry holds all packets for the current phase of the network. A packet registry should be the same for
 * both sides of a connection but can have different handlers. A packet handler has methods for handling a packet
 * when it is received.
 *
 * @author SpigotRCE
 */
public class PacketRegistry {
  public final List<PacketEntry<? extends Packet<?>>> packets = new ArrayList<>();

  /**
   * Method to register a packet. When a packet is registered, the max packet id is incremented and a
   * {{@link @PacketEntry}} is registered.
   *
   * @param packetClass {@link Class} of the packet
   * @param codec {@link PacketCodec} of the packet
   * @param handler {@link Consumer} of the packet
   * @param <T> type of the packet
   */
  public <T extends Packet<?>> void registerPacket(Class<T> packetClass, PacketCodec<T> codec, Consumer<T> handler) {
    packets.add(new PacketEntry<>(packetClass, codec, handler));
  }

  /**
   * Method to register a packet with no handler, and hence this packet is only one way i.e. it can only be sent from
   * the side it is registered. If the packet is received, it'll throw a {@link IllegalArgumentException}.
   *
   * @param packetClass {@link Class} of the packet
   * @param codec {@link PacketCodec} of the packet
   * @param <T> type of the packet
   */
  public <T extends Packet<?>> void registerPacket(Class<T> packetClass, PacketCodec<T> codec) {
    packets.add(new PacketEntry<>(packetClass, codec, (packet) -> {
      throw new IllegalArgumentException("No handler registered for packet: " + packetClass.getName());
    }));
  }

  /**
   * Encodes the {@link Packet} into a {@link OutputBuffer} using the packet codec.
   *
   * @param packet {@link Packet} to encode
   * @param out {@link OutputBuffer} to encode to
   */
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

  /**
   * Decoded the {@link Packet} from a {@link InputBuffer} using the packet codec.
   *
   * @param id the packet id
   * @param in {@link InputBuffer} to decode from
   */
  public Packet<?> decode(int id, InputBuffer in) {
    @SuppressWarnings("unchecked") PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(id);
    return entry.codec().read(in);
  }

  /**
   * Method to handle the packet.
   *
   * @param packet {@link Packet} packet to be handled.
   */
  public void handle(Packet<?> packet) {
    @SuppressWarnings("unchecked") PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(getId(packet));
    entry.handler.accept(packet);
  }

  /**
   * A packet entry containing packet's {@link Class}, {@link PacketCodec}, and a {@link Consumer} handler.
   *
   * @param clazz Packet Class
   * @param codec Packet Codec
   * @param handler Packet Handler
   * @param <T> Packet Type
   */
  public record PacketEntry<T extends Packet<?>>(Class<T> clazz, PacketCodec<T> codec, Consumer<T> handler) {
  }
}
