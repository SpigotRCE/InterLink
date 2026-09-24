package xyz.spigotrce.interlink.compression;

import xyz.spigotrce.interlink.compressor.Compressor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * ZLibCompressor for compressing the data.
 *
 * @author SpigotRCE
 */
public final class ZLibCompressor implements Compressor {
  /**
   * Constructs a new ZLibCompressor with a fresh, lazily initialized deflater/inflater pair per
   * thread.
   */
  public ZLibCompressor() {}

  private static final ThreadLocal<Deflater> DEFLATER =
      ThreadLocal.withInitial(() -> new Deflater(Deflater.DEFAULT_COMPRESSION));
  private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(Inflater::new);

  /**
   * Compresses the given data using a {@link Deflater} at the default compression level.
   *
   * @param data the uncompressed bytes to compress
   * @return the zlib-compressed bytes
   * @throws IOException if compression fails
   */
  @Override
  public byte[] compress(final byte[] data) throws IOException {
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

  /**
   * Decompresses the given zlib-compressed data using an {@link Inflater}.
   *
   * @param data the zlib-compressed bytes to decompress
   * @return the uncompressed bytes
   * @throws IOException if decompression fails, the input is truncated, or no progress can be made
   */
  @Override
  public byte[] decompress(final byte[] data) throws IOException {
    final Inflater inflater = INFLATER.get();
    try {
      inflater.setInput(data);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final byte[] buffer = new byte[512];
      int noProgress = 0;
      while (!inflater.finished()) {
        final int count = inflater.inflate(buffer);
        if (count == 0) {
          if (++noProgress > 1024) {
            throw new IOException("Decompression made no progress, input may be truncated");
          }
        } else {
          noProgress = 0;
        }
        baos.write(buffer, 0, count);
      }
      return baos.toByteArray();
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException("Failed to decompress", e);
    } finally {
      inflater.reset();
    }
  }
}
