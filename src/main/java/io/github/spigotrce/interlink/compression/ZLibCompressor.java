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
  public static byte[] compress(final byte[] data) throws IOException {
    final Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
    deflater.setInput(data);
    deflater.finish();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final byte[] buffer = new byte[512];
    while (!deflater.finished()) {
      final int count = deflater.deflate(buffer);
      baos.write(buffer, 0, count);
    }
    deflater.end();
    return baos.toByteArray();
  }

  public static byte[] decompress(final byte[] data) throws IOException {
    final Inflater inflater = new Inflater();
    inflater.setInput(data);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final byte[] buffer = new byte[512];
    try {
      while (!inflater.finished()) {
        final int count = inflater.inflate(buffer);
        baos.write(buffer, 0, count);
      }
    } catch (final Exception e) {
      throw new IOException("Failed to decompress", e);
    } finally {
      inflater.end();
    }
    return baos.toByteArray();
  }
}
