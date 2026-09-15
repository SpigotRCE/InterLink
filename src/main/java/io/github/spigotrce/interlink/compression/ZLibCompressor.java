package io.github.spigotrce.interlink.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * ZLibCompressor for compressing the data.
 *
 * @author SpigotRCE
 */
public final class ZLibCompressor {
  private static final ThreadLocal<Deflater> DEFLATER =
      ThreadLocal.withInitial(() -> new Deflater(Deflater.DEFAULT_COMPRESSION));
  private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(Inflater::new);

  public static byte[] compress(final byte[] data) throws IOException {
    final Deflater deflater = DEFLATER.get();
    try {
      deflater.setInput(data);
      deflater.finish();

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final byte[] buffer = new byte[512];
      while (!deflater.finished()) {
        final int count = deflater.deflate(buffer);
        baos.write(buffer, 0, count);
      }
      return baos.toByteArray();
    } finally {
      deflater.reset();
    }
  }

  public static byte[] decompress(final byte[] data) throws IOException {
    final Inflater inflater = INFLATER.get();
    try {
      inflater.setInput(data);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final byte[] buffer = new byte[512];
      while (!inflater.finished()) {
        final int count = inflater.inflate(buffer);
        baos.write(buffer, 0, count);
      }
      return baos.toByteArray();
    } catch (final Exception e) {
      throw new IOException("Failed to decompress", e);
    } finally {
      inflater.reset();
    }
  }
}
