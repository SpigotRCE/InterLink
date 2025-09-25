package io.github.spigotrce.interlink.connection;

import java.io.IOException;

public interface Transport {
  void send(byte[] data) throws IOException;
  byte[] receive() throws IOException;
  void close() throws IOException;
  boolean isOpen();
}
