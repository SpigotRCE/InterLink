package xyz.spigotrce.interlink.cipher.impl;

import xyz.spigotrce.interlink.cipher.Cipher;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An AES cipher in CBC mode with PKCS#5 padding.
 *
 * <p>Wire format (per message): {@code [16-byte IV][ciphertext]}. A fresh, cryptographically random
 * IV is generated for every message and transmitted alongside the ciphertext, so callers never
 * manage IV state and can never accidentally reuse a {@code (key, IV)} pair.
 *
 * <p>Edge cases handled:
 *
 * <ul>
 *   <li>Truncated frames (shorter than an IV plus one ciphertext block) are rejected explicitly
 *       rather than causing an unchecked bounds error.
 *   <li>Empty plaintext is supported - PKCS#5 padding always emits at least one padding block, so
 *       no special case is needed.
 * </ul>
 *
 * <p><strong>Confidentiality only, no authentication.</strong> Encryption is not signed: CBC is
 * malleable, so an active attacker can flip bits in the ciphertext and the resulting plaintext may
 * still decrypt without error (padding corruption is caught only when the last block is disturbed).
 * Do not rely on this cipher over an untrusted network. Prefer {@link AesGcmCipher} which
 * authenticates every message.
 *
 * <p>This cipher does <strong>not</strong> provide key exchange, forward secrecy, or key rotation.
 * The shared key must be exchanged out of band or via a handshake layer.
 */
public final class AesCbcCipher implements Cipher {
  private static final int IV_LENGTH = 16;
  private static final int BLOCK_LENGTH = 16;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();
  private final ThreadLocal<javax.crypto.Cipher> encryptCipher =
      ThreadLocal.withInitial(AesCbcCipher::newCipher);
  private final ThreadLocal<javax.crypto.Cipher> decryptCipher =
      ThreadLocal.withInitial(AesCbcCipher::newCipher);

  /**
   * Constructs a new AES-CBC cipher using the given raw key bytes.
   *
   * @param key the raw AES key bytes (128, 192, or 256 bits); key validity is checked when the
   *     cipher is first used
   */
  public AesCbcCipher(final byte[] key) {
    this.key = new SecretKeySpec(key, "AES");
  }

  private static javax.crypto.Cipher newCipher() {
    try {
      return javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
    } catch (final GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Encrypts the given plaintext with a fresh random 16-byte IV, producing a self-contained frame
   * of {@code [16-byte IV][ciphertext]}.
   *
   * @param data the plaintext bytes to encrypt
   * @return the encrypted frame, prefixed with the random IV
   * @throws IOException if the underlying {@code AES/CBC/PKCS5Padding} cipher fails to initialize
   *     or to process the data
   */
  @Override
  public byte[] encrypt(final byte[] data) throws IOException {
    try {
      final javax.crypto.Cipher cipher = encryptCipher.get();
      final byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

      final byte[] sealed = cipher.doFinal(data);
      final byte[] frame = new byte[IV_LENGTH + sealed.length];
      System.arraycopy(iv, 0, frame, 0, IV_LENGTH);
      System.arraycopy(sealed, 0, frame, IV_LENGTH, sealed.length);
      return frame;
    } catch (final GeneralSecurityException e) {
      throw new IOException("Failed to encrypt data", e);
    }
  }

  /**
   * Decrypts a frame produced by {@link #encrypt(byte[])}.
   *
   * @param data the complete frame of {@code [16-byte IV][ciphertext]}
   * @return the recovered plaintext
   * @throws IOException if the frame is truncated, corrupted, or the key is invalid
   */
  @Override
  public byte[] decrypt(final byte[] data) throws IOException {
    if (data.length < IV_LENGTH + BLOCK_LENGTH) {
      throw new IOException(
          "Truncated encrypted frame: expected IV + ciphertext, got " + data.length);
    }
    try {
      final javax.crypto.Cipher cipher = decryptCipher.get();
      final byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
      final byte[] sealed = Arrays.copyOfRange(data, IV_LENGTH, data.length);
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
      return cipher.doFinal(sealed);
    } catch (final GeneralSecurityException e) {
      throw new IOException("Decryption failed: message was corrupted or malformed", e);
    }
  }
}
