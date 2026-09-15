package io.github.spigotrce.interlink.layer;

import io.github.spigotrce.interlink.compression.ZLibCompressor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A zlib compression {@link Layer}.
 *
 * <p>Wire format (per message): a single flag byte followed by the payload.
 *
 * <ul>
 *   <li>{@code 0x00} + raw bytes - the message was sent uncompressed
 *   <li>{@code 0x01} + zlib stream - the message was compressed
 * </ul>
 *
 * Each message is treated as one complete, discrete unit: this layer assumes whole messages are
 * already delimited by whatever sits below the pipeline. A length-prefix framing layer, if ever
 * needed over a raw byte stream, must sit wire-ward of this one.
 *
 * <p>Edge cases handled:
 *
 * <ul>
 *   <li>Small messages below {@code threshold} are never compressed (the fixed per-stream zlib
 *       overhead would only grow them).
 *   <li>If compressing does not shrink the payload (always true for already-encrypted,
 *       incompressible data), the raw bytes are sent instead of paying the size penalty. This is
 *       also why this layer must run before encryption on the outbound side.
 *   <li>Both cases are encoded with the flag byte so the receiver knows how to interpret them.
 *   <li>Empty input passes through unchanged.
 *   <li>Truncated or corrupt compressed input raises {@link LayerException} instead of an unchecked
 *       failure.
 * </ul>
 */
public class CompressionLayer implements Layer {
  public static final String SHARED_THRESHOLD = "compression.threshold";

  private final int defaultThreshold;
  private final int maxDecompressedLength;

  public CompressionLayer() {
    this(0);
  }

  public CompressionLayer(final int defaultThreshold) {
    this(defaultThreshold, 8 * 1024 * 1024);
  }

  public CompressionLayer(final int defaultThreshold, final int maxDecompressedLength) {
    this.defaultThreshold = defaultThreshold;
    this.maxDecompressedLength = maxDecompressedLength;
  }

  @Override
  public void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
    if (data.length < 1) {
      throw new LayerException("Truncated compression frame: missing flag byte");
    }
    final byte flag = data[0];
    if (flag != 0x00 && flag != 0x01) {
      throw new LayerException("Unknown compression flag: " + flag);
    }

    final byte[] payload = new byte[data.length - 1];
    System.arraycopy(data, 1, payload, 0, payload.length);

    if (flag == 0x00) {
      ctx.fireInbound(payload);
      return;
    }

    try {
      ctx.fireInbound(decompressBounded(payload));
    } catch (final IOException e) {
      throw new LayerException("Corrupt compressed data", e);
    }
  }

  @Override
  public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
    final int threshold = threshold(ctx);
    if (data.length == 0 || data.length < threshold) {
      ctx.fireOutbound(withFlag(data, false));
      return;
    }

    final byte[] compressed;
    try {
      compressed = ZLibCompressor.compress(data);
    } catch (final IOException e) {
      throw new LayerException("Failed to compress data", e);
    }

    if (compressed.length < data.length) {
      ctx.fireOutbound(withFlag(compressed, true));
      return;
    }
    ctx.fireOutbound(withFlag(data, false));
  }

  private int threshold(final LayerContext ctx) {
    final Object stored = ctx.sharedState().get(SHARED_THRESHOLD);
    if (stored instanceof final Integer value) {
      return value;
    }
    return defaultThreshold;
  }

  private static byte[] withFlag(final byte[] data, final boolean compressed) {
    final byte[] framed = new byte[data.length + 1];
    framed[0] = compressed ? (byte) 0x01 : (byte) 0x00;
    System.arraycopy(data, 0, framed, 1, data.length);
    return framed;
  }

  private byte[] decompressBounded(final byte[] data) throws IOException {
    final Inflater inflater = new Inflater();
    try {
      inflater.setInput(data);
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final byte[] buffer = new byte[512];
      int total = 0;
      while (!inflater.finished()) {
        final int count = inflater.inflate(buffer);
        if (count == 0 && !inflater.finished()) {
          throw new IOException("Truncated or corrupt compressed data");
        }
        total += count;
        if (total > maxDecompressedLength) {
          throw new IOException("Decompressed data exceeds limit: " + maxDecompressedLength);
        }
        baos.write(buffer, 0, count);
      }
      return baos.toByteArray();
    } catch (final DataFormatException e) {
      throw new IOException("Failed to decompress", e);
    } finally {
      inflater.end();
    }
  }
}
