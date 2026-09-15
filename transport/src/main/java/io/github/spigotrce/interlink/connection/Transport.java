package io.github.spigotrce.interlink.connection;

import java.io.IOException;

public interface Transport<T extends Transport<T>> {
  void send(final byte[] data) throws IOException;

  byte[] receive() throws IOException;

  void close() throws IOException;

  boolean isOpen();

  void bind(final String host, final int port) throws IOException;

  T accept() throws IOException;

  T connect(final String host, final int port) throws IOException;

  String getHostname();

  int getPort();
}
