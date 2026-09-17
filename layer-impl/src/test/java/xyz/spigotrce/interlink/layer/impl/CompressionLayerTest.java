package xyz.spigotrce.interlink.layer.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import xyz.spigotrce.interlink.compression.ZLibCompressor;
import xyz.spigotrce.interlink.layer.LayerContext;
import xyz.spigotrce.interlink.layer.LayerException;
import xyz.spigotrce.interlink.layer.impl.CompressionLayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class CompressionLayerTest {
  private final ZLibCompressor compressor = new ZLibCompressor();

  @Test
  public void emptyInputPassesThroughRaw() throws LayerException {
    final List<byte[]> frames = new ArrayList<>();
    new CompressionLayer(compressor).onOutbound(new byte[0], ctx(frames));
    assertArrayEquals(new byte[] {0x00}, frames.get(0));
  }

  private static LayerContext ctx(final List<byte[]> frames) {
    return new LayerContext() {
      private final Map<String, Object> state = new HashMap<>();

      @Override
      public void fireInbound(final byte[] data) {}

      @Override
      public void fireOutbound(final byte[] data) {
        frames.add(data);
      }

      @Override
      public String name() {
        return "test";
      }

      @Override
      public Map<String, Object> sharedState() {
        return state;
      }
    };
  }

  @Test
  public void belowThresholdPassesThroughRaw() throws LayerException {
    final byte[] data = "short".getBytes();
    final List<byte[]> frames = new ArrayList<>();
    new CompressionLayer(compressor, 256).onOutbound(data, ctx(frames));
    assertEquals(0x00, frames.get(0)[0]);
    assertArrayEquals(data, Arrays.copyOfRange(frames.get(0), 1, frames.get(0).length));
  }

  @Test
  public void compressibleDataIsCompressed() throws LayerException {
    final byte[] data = new byte[512];
    Arrays.fill(data, (byte) 0x41);
    final List<byte[]> frames = new ArrayList<>();
    new CompressionLayer(compressor).onOutbound(data, ctx(frames));
    assertEquals(0x01, frames.get(0)[0]);
    assertTrue(frames.get(0).length < data.length);
  }

  @Test
  public void incompressibleDataPassesThroughRaw() throws LayerException {
    final byte[] data = new byte[512];
    new Random(1).nextBytes(data);
    final List<byte[]> frames = new ArrayList<>();
    new CompressionLayer(compressor).onOutbound(data, ctx(frames));
    assertEquals(0x00, frames.get(0)[0]);
  }

  @Test
  public void inboundRawPassesThrough() throws LayerException {
    final List<byte[]> frames = new ArrayList<>();
    final LayerContext ctx =
        new LayerContext() {
          private final Map<String, Object> state = new HashMap<>();

          @Override
          public void fireInbound(final byte[] data) {
            frames.add(data);
          }

          @Override
          public void fireOutbound(final byte[] data) {}

          @Override
          public String name() {
            return "test";
          }

          @Override
          public Map<String, Object> sharedState() {
            return state;
          }
        };
    new CompressionLayer(compressor).onInbound(new byte[] {0x00, 1, 2, 3}, ctx);
    assertArrayEquals(new byte[] {1, 2, 3}, frames.get(0));
  }

  @Test
  public void inboundCompressedRoundTrips() throws Exception {
    final byte[] data = new byte[1000];
    Arrays.fill(data, (byte) 0x42);
    final byte[] compressed = compressor.compress(data);
    final byte[] frame = new byte[compressed.length + 1];
    frame[0] = 0x01;
    System.arraycopy(compressed, 0, frame, 1, compressed.length);

    final List<byte[]> frames = new ArrayList<>();
    final LayerContext ctx =
        new LayerContext() {
          private final Map<String, Object> state = new HashMap<>();

          @Override
          public void fireInbound(final byte[] data) {
            frames.add(data);
          }

          @Override
          public void fireOutbound(final byte[] data) {}

          @Override
          public String name() {
            return "test";
          }

          @Override
          public Map<String, Object> sharedState() {
            return state;
          }
        };
    new CompressionLayer(compressor).onInbound(frame, ctx);
    assertArrayEquals(data, frames.get(0));
  }

  @Test
  public void truncatedFrameMissingFlagByteRejected() {
    final List<byte[]> frames = new ArrayList<>();
    assertThrows(
        LayerException.class, () -> new CompressionLayer(compressor).onInbound(new byte[0], ctx(frames)));
  }

  @Test
  public void unknownFlagRejected() {
    final List<byte[]> frames = new ArrayList<>();
    assertThrows(
        LayerException.class,
        () -> new CompressionLayer(compressor).onInbound(new byte[] {0x02}, ctx(frames)));
  }

  @Test
  public void corruptCompressedPayloadRejected() {
    final List<byte[]> frames = new ArrayList<>();
    assertThrows(
        LayerException.class,
        () -> new CompressionLayer(compressor).onInbound(new byte[] {0x01, 99, 98, 97}, ctx(frames)));
  }

  @Test
  public void decompressExceedsLimitRejected() throws Exception {
    final byte[] data = new byte[1024];
    Arrays.fill(data, (byte) 0x41);
    final byte[] compressed = compressor.compress(data);
    final byte[] frame = new byte[compressed.length + 1];
    frame[0] = 0x01;
    System.arraycopy(compressed, 0, frame, 1, compressed.length);
    final List<byte[]> frames = new ArrayList<>();
    assertThrows(
        LayerException.class,
        () -> new CompressionLayer(compressor, 0, 512).onInbound(frame, ctx(frames)));
  }
}
