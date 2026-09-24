package xyz.spigotrce.interlink.compressor;

import java.io.IOException;

/**
 * A compression codec used by the compression layer.
 *
 * <p>Implementations are responsible for their own wire format, so {@link #compress(byte[])} always
 * produces a self-contained block that {@link #decompress(byte[])} can reverse.
 *
 * <p>Instances must be safe for concurrent use, since the pipeline runs the layer chain on the
 * calling thread and a shared compressor may serve multiple connections.
 */
public interface Compressor {
  /**
   * Compresses the given data.
   *
   * @param data the uncompressed bytes to compress
   * @return the compressed bytes
   * @throws IOException if compression fails
   */
  byte[] compress(byte[] data) throws IOException;

  /**
   * Decompresses the given data.
   *
   * @param data the compressed bytes to decompress
   * @return the uncompressed bytes
   * @throws IOException if decompression fails
   */
  byte[] decompress(byte[] data) throws IOException;
}
