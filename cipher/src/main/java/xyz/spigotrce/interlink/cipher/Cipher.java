package xyz.spigotrce.interlink.cipher;

import java.io.IOException;

/**
 * A symmetric confidentiality cipher used by the encryption layer.
 *
 * <p>Implementations are responsible for their own wire format including any IV, nonce, salt, or
 * checksum bytes they require, so {@link #encrypt(byte[])} always produces a self-contained frame
 * that {@link #decrypt(byte[])} can reverse.
 *
 * <p>Instances must be safe for concurrent use, since the pipeline runs the layer chain on the
 * calling thread and a shared layer may serve multiple connections.
 */
public interface Cipher {
  byte[] encrypt(byte[] data) throws IOException;

  byte[] decrypt(byte[] data) throws IOException;
}
