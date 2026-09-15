package io.github.spigotrce.interlink.compressor;

import java.io.IOException;

public interface Compressor {
  byte[] compress(byte[] data) throws IOException;

  byte[] decompress(byte[] data) throws IOException;
}
