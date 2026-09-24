package xyz.spigotrce.interlink.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import xyz.spigotrce.interlink.buf.InputBuffer;
import xyz.spigotrce.interlink.buf.OutputBuffer;

/**
 * A packet registry holds all packets for the current phase of the network. A packet registry
 * should be the same for both sides of a connection but can have different handlers. A packet
 * handler has methods for handling a packet when it is received.
 *
 * <p>Packets are registered automatically from a shared {@link PacketType} enum, in its declaration
 * order. The ordinal of each constant becomes the packet id. Handlers are then attached per
 * constant via {@link #registerPacket(Enum, Consumer)}.
 *
 * @param <E> the shared packet enum type
 * @author SpigotRCE
 */
public class PacketRegistry<E extends Enum<E> & PacketType> {
  private final List<PacketEntry<? extends Packet<?>>> packets = new ArrayList<>();
  private final Map<Class<?>, Integer> idCache = new HashMap<>();

  /**
   * Creates a registry that registers every constant of the given enum in declaration order. Each
   * constant's ordinal is used as its packet id.
   *
   * @param packetEnum {@link Class} of the shared packet enum
   */
  public PacketRegistry(final Class<E> packetEnum) {
    for (final E type : packetEnum.getEnumConstants()) {
      registerPacket(
          type.packetClass(),
          type.codec(),
          (packet) -> {
            throw new IllegalArgumentException(
                "No handler registered for packet: " + type.packetClass().getName());
          });
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Packet<?>> void registerPacket(
      final Class<? extends Packet<?>> packetClass,
      final PacketCodec<?> codec,
      final Consumer<? extends Packet<?>> handler) {
    final Class<T> clazz = (Class<T>) packetClass;
    final PacketCodec<T> typedCodec = (PacketCodec<T>) codec;
    final Consumer<T> typedHandler = (Consumer<T>) handler;
    idCache.put(clazz, packets.size());
    packets.add(new PacketEntry<>(clazz, typedCodec, typedHandler));
  }

  /**
   * Attaches a handler to the given packet enum constant. If no handler is registered for a packet,
   * receiving it throws an {@link IllegalArgumentException}.
   *
   * @param packetType the packet enum constant
   * @param handler {@link Consumer} of the packet
   * @param <T> type of the packet
   */
  public <T extends Packet<?>> void registerPacket(final E packetType, final Consumer<T> handler) {
    final int id = packetType.ordinal();
    @SuppressWarnings("unchecked")
    final PacketEntry<T> entry = (PacketEntry<T>) packets.get(id);
    packets.set(id, new PacketEntry<>(entry.clazz(), entry.codec(), handler));
  }

  /**
   * Encodes the {@link Packet} into a {@link OutputBuffer} using the packet codec.
   *
   * @param packet {@link Packet} to encode
   * @param out {@link OutputBuffer} to encode to
   */
  public void encode(final Packet<?> packet, final OutputBuffer out) {
    @SuppressWarnings("unchecked")
    final PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(getId(packet));
    entry.codec().write(packet, out);
  }

  public int getId(final Packet<?> packet) {
    return idCache.getOrDefault(packet.getClass(), -1);
  }

  /** Returns an unmodifiable view of the registered packets in registration order. */
  public List<PacketEntry<? extends Packet<?>>> getPackets() {
    return Collections.unmodifiableList(packets);
  }

  /**
   * Decodes the {@link Packet} from a {@link InputBuffer} using the packet codec.
   *
   * @param id the packet id
   * @param in {@link InputBuffer} to decode from
   */
  public Packet<?> decode(final int id, final InputBuffer in) {
    @SuppressWarnings("unchecked")
    final PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(id);
    return entry.codec().read(in);
  }

  /**
   * Handles the packet by dispatching it to the handler registered for its id.
   *
   * @param packet {@link Packet} packet to be handled.
   */
  public void handle(final Packet<?> packet) {
    @SuppressWarnings("unchecked")
    final PacketEntry<Packet<?>> entry = (PacketEntry<Packet<?>>) packets.get(getId(packet));
    entry.handler.accept(packet);
  }

  /**
   * A packet entry containing packet's {@link Class}, {@link PacketCodec}, and a {@link Consumer}
   * handler.
   *
   * @param clazz Packet Class
   * @param codec Packet Codec
   * @param handler Packet Handler
   * @param <T> Packet Type
   */
  public record PacketEntry<T extends Packet<?>>(
      Class<T> clazz, PacketCodec<T> codec, Consumer<T> handler) {}
}
