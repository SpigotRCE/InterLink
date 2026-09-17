package xyz.spigotrce.interlink.layer.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import xyz.spigotrce.interlink.cipher.impl.AesGcmCipher;
import xyz.spigotrce.interlink.layer.LayerContext;
import xyz.spigotrce.interlink.layer.LayerException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class EncryptionLayerTest {
  private static final byte[] KEY = "DQzxpO2A9L1n4QvR".getBytes(StandardCharsets.US_ASCII);

  @Test
  public void roundTripThroughLayer() throws Exception {
    final EncryptionLayer encrypt = new EncryptionLayer(new AesGcmCipher(KEY));
    final EncryptionLayer decrypt = new EncryptionLayer(new AesGcmCipher(KEY));
    final byte[] message = "secret message".getBytes(StandardCharsets.UTF_8);

    final List<byte[]> wire = new ArrayList<>();
    encrypt.onOutbound(message, ctxFireOutbound(wire));

    final List<byte[]> app = new ArrayList<>();
    decrypt.onInbound(wire.get(0), ctxFireInbound(app));

    assertArrayEquals(message, app.get(0));
  }

  private static LayerContext ctxFireOutbound(final List<byte[]> frames) {
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

  private static LayerContext ctxFireInbound(final List<byte[]> frames) {
    return new LayerContext() {
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
  }

  @Test
  public void malformedFrameRejected() {
    final EncryptionLayer decrypt = new EncryptionLayer(new AesGcmCipher(KEY));
    final List<byte[]> app = new ArrayList<>();
    assertThrows(
        LayerException.class,
        () -> decrypt.onInbound(new byte[] {1, 2, 3}, ctxFireInbound(app)));
  }
}
