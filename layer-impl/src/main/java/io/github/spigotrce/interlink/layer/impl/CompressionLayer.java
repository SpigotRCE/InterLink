package io.github.spigotrce.interlink.layer.impl;

import io.github.spigotrce.interlink.compressor.Compressor;
import io.github.spigotrce.interlink.layer.Layer;
import io.github.spigotrce.interlink.layer.LayerContext;
import io.github.spigotrce.interlink.layer.LayerException;
import java.io.IOException;

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
 * <p>Each message is treated as one complete, discrete unit: this layer assumes whole messages are
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

  private final Compressor compressor;
  private final int threshold;
  private final int maxDecompressedLength;

  public CompressionLayer(final Compressor compressor) {
    this(compressor, 0);
  }

  public CompressionLayer(final Compressor compressor, final int threshold) {
    this(compressor, threshold, 8 * 1024 * 1024);
  }

  public CompressionLayer(
      final Compressor compressor, final int threshold, final int maxDecompressedLength) {
    this.compressor = compressor;
    this.threshold = threshold;
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

    final byte[] decompressed;
    try {
      decompressed = compressor.decompress(payload);
    } catch (final IOException e) {
      throw new LayerException("Corrupt compressed data", e);
    }
    if (decompressed.length > maxDecompressedLength) {
      throw new LayerException("Decompressed data exceeds limit: " + maxDecompressedLength);
    }
    ctx.fireInbound(decompressed);
  }

  @Override
  public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
    if (data.length == 0 || data.length < threshold) {
      ctx.fireOutbound(withFlag(data, false));
      return;
    }

    final byte[] compressed;
    try {
      compressed = compressor.compress(data);
    } catch (final IOException e) {
      throw new LayerException("Failed to compress data", e);
    }

    if (compressed.length < data.length) {
      ctx.fireOutbound(withFlag(compressed, true));
      return;
    }
    ctx.fireOutbound(withFlag(data, false));
  }

  private static byte[] withFlag(final byte[] data, final boolean compressed) {
    final byte[] framed = new byte[data.length + 1];
    framed[0] = compressed ? (byte) 0x01 : (byte) 0x00;
    System.arraycopy(data, 0, framed, 1, data.length);
    return framed;
  }
}
