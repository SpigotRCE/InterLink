package io.github.spigotrce.interlink.compression;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ZLibCompressorTest {
  private final ZLibCompressor compressor = new ZLibCompressor();

  @Test
  public void roundTrip() throws IOException {
    final byte[] data = new byte[4096];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 251);
    }
    assertArrayEquals(data, compressor.decompress(compressor.compress(data)));
  }

  @Test
  public void compressedDataShrunk() throws IOException {
    final byte[] data = new byte[8192];
    Arrays.fill(data, (byte) 0x41);
    assertTrue(compressor.compress(data).length < data.length);
  }

  @Test
  public void emptyRoundTrip() throws IOException {
    assertArrayEquals(new byte[0], compressor.decompress(compressor.compress(new byte[0])));
  }

  @Test
  public void textRoundTrip() throws IOException {
    final byte[] data =
        "interlink zlib compressor \u00e9\u4e2d\u6587".getBytes(StandardCharsets.UTF_8);
    assertArrayEquals(data, compressor.decompress(compressor.compress(data)));
  }

  @Test
  public void decompressGarbageRejected() {
    assertThrows(IOException.class, () -> compressor.decompress(new byte[] {1, 2, 3, 4, 5}));
  }

  @Test
  public void decompressTruncatedStreamRejected() throws IOException {
    final byte[] compressed = compressor.compress(new byte[4096]);
    final byte[] truncated = Arrays.copyOf(compressed, compressed.length / 2);
    assertThrows(IOException.class, () -> compressor.decompress(truncated));
  }

  @Test
  public void repeatedUseReusesStateSafely() throws IOException {
    final byte[] first = new byte[] {1, 2, 3, 4, 5};
    final byte[] second = new byte[2048];
    Arrays.fill(second, (byte) 0x7F);
    assertArrayEquals(first, compressor.decompress(compressor.compress(first)));
    assertArrayEquals(second, compressor.decompress(compressor.compress(second)));
    assertArrayEquals(first, compressor.decompress(compressor.compress(first)));
  }
}
