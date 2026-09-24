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
  /**
   * Encrypts the given plaintext into a self-contained ciphertext frame.
   *
   * @param data the plaintext bytes to encrypt
   * @return the self-contained ciphertext frame
   * @throws IOException if encryption fails
   */
  byte[] encrypt(byte[] data) throws IOException;

  /**
   * Decrypts a frame produced by {@link #encrypt(byte[])}.
   *
   * @param data the encrypted frame to decrypt
   * @return the recovered plaintext
   * @throws IOException if decryption fails
   */
  byte[] decrypt(byte[] data) throws IOException;
}
