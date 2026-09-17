package xyz.spigotrce.interlink.cipher.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class AesGcmCipherTest {
  private static final byte[] KEY = "DQzxpO2A9L1n4QvR".getBytes(StandardCharsets.US_ASCII);

  @Test
  public void roundTrip() throws IOException {
    final AesGcmCipher cipher = new AesGcmCipher(KEY);
    assertArrayEquals(randomData(256), cipher.decrypt(cipher.encrypt(randomData(256))));
  }

  private static byte[] randomData(final int length) {
    final byte[] data = new byte[length];
    new Random(42).nextBytes(data);
    return data;
  }

  @Test
  public void emptyMessageRoundTrip() throws IOException {
    final AesGcmCipher cipher = new AesGcmCipher(KEY);
    assertArrayEquals(new byte[0], cipher.decrypt(cipher.encrypt(new byte[0])));
  }

  @Test
  public void smallAndLargeMessages() throws IOException {
    final AesGcmCipher cipher = new AesGcmCipher(KEY);
    final byte[] tiny = new byte[] {42};
    final byte[] large = randomData(4096);
    assertArrayEquals(tiny, cipher.decrypt(cipher.encrypt(tiny)));
    assertArrayEquals(large, cipher.decrypt(cipher.encrypt(large)));
  }

  @Test
  public void distinctEncryptionsProduceDifferentFrames() throws IOException {
    final AesGcmCipher cipher = new AesGcmCipher(KEY);
    final byte[] data = randomData(64);
    assertNotEquals(
        Arrays.toString(cipher.encrypt(data)),
        Arrays.toString(cipher.encrypt(data)));
  }

  @Test
  public void truncatedFramesRejected() {
    final AesGcmCipher cipher = new AesGcmCipher(KEY);
    assertThrows(IOException.class, () -> cipher.decrypt(new byte[8]));
  }

  @Test
  public void detectsTampering() throws IOException {
    final AesGcmCipher cipher = new AesGcmCipher(KEY);
    final byte[] frame = cipher.encrypt(randomData(128));
    frame[frame.length - 1] ^= 0x01;
    assertThrows(IOException.class, () -> cipher.decrypt(frame));
  }
}
