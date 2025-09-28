package io.github.spigotrce.interlink.compression;

import java.io.*;
import java.util.zip.*;

/**
 * ZLibCompressor for compressing the data.
 *
 * @author SpigotRCE
 */
public class ZLibCompressor {
  public static byte[] compress(byte[] data) throws IOException {
    Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
    deflater.setInput(data);
    deflater.finish();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[512];
    while (!deflater.finished()) {
      int count = deflater.deflate(buffer);
      baos.write(buffer, 0, count);
    }
    deflater.end();
    return baos.toByteArray();
  }

  public static byte[] decompress(byte[] data) throws IOException {
    Inflater inflater = new Inflater();
    inflater.setInput(data);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[512];
    try {
      while (!inflater.finished()) {
        int count = inflater.inflate(buffer);
        baos.write(buffer, 0, count);
      }
    } catch (Exception e) {
      throw new IOException("Failed to decompress", e);
    } finally {
      inflater.end();
    }
    return baos.toByteArray();
  }
}
