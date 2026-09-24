package xyz.spigotrce.interlink.cipher.impl;

import xyz.spigotrce.interlink.cipher.Cipher;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An AES cipher in Galois/Counter mode - authenticated encryption (AEAD).
 *
 * <p>Wire format (per message): {@code [12-byte IV][ciphertext][16-byte tag]}. A fresh,
 * cryptographically random 96-bit IV is generated for every message and transmitted alongside the
 * ciphertext, so callers never manage IV state and can never accidentally reuse a {@code (key, IV)}
 * pair. The 128-bit GCM authentication tag is computed over the ciphertext (the IV is implicitly
 * covered by the GCM construction).
 *
 * <p>Edge cases handled:
 *
 * <ul>
 *   <li>Truncated frames (shorter than an IV plus the tag) are rejected explicitly rather than
 *       causing an unchecked bounds error.
 *   <li>Tampered ciphertext or tag raises an {@link IOException} instead of returning garbage,
 *       because GCM authenticates every byte.
 *   <li>Empty plaintext is supported - the frame carries just the IV and an empty ciphertext plus
 *       its tag.
 * </ul>
 *
 * <p>Unlike the confidentiality-only ciphers (CFB, CBC), GCM detects modification. An active
 * attacker who flips bits in the ciphertext (or the tag) is detected on decryption. This is the
 * recommended mode for any connection where tampering is a threat.
 *
 * <p>This cipher does <strong>not</strong> provide key exchange, forward secrecy, or key rotation.
 * The shared key must be exchanged out of band or via a handshake layer.
 */
public final class AesGcmCipher implements Cipher {
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;
  private static final int MIN_FRAME_LENGTH = IV_LENGTH + (TAG_LENGTH_BITS / 8);

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();
  private final ThreadLocal<javax.crypto.Cipher> encryptCipher =
      ThreadLocal.withInitial(AesGcmCipher::newCipher);
  private final ThreadLocal<javax.crypto.Cipher> decryptCipher =
      ThreadLocal.withInitial(AesGcmCipher::newCipher);

  /**
   * Constructs a new AES-GCM cipher using the given raw key bytes.
   *
   * @param key the raw AES key bytes (128, 192, or 256 bits); key validity is checked when the
   *     cipher is first used
   */
  public AesGcmCipher(final byte[] key) {
    this.key = new SecretKeySpec(key, "AES");
  }

  private static javax.crypto.Cipher newCipher() {
    try {
      return javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
    } catch (final GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Authenticated-encrypts the given plaintext with a fresh random 12-byte IV, producing a
   * self-contained frame of {@code [12-byte IV][ciphertext][16-byte tag]}.
   *
   * @param data the plaintext bytes to encrypt
   * @return the encrypted frame, prefixed with the random IV and followed by the GCM tag
   * @throws IOException if the underlying {@code AES/GCM/NoPadding} cipher fails to initialize or
   *     to process the data
   */
  @Override
  public byte[] encrypt(final byte[] data) throws IOException {
    try {
      final javax.crypto.Cipher cipher = encryptCipher.get();
      final byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

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
   * Authenticated-decrypts a frame produced by {@link #encrypt(byte[])}.
   *
   * @param data the complete frame of {@code [12-byte IV][ciphertext][16-byte tag]}
   * @return the recovered plaintext
   * @throws IOException if the frame is truncated, the authentication tag does not match, or the
   *     key is invalid
   */
  @Override
  public byte[] decrypt(final byte[] data) throws IOException {
    if (data.length < MIN_FRAME_LENGTH) {
      throw new IOException(
          "Truncated encrypted frame: expected IV + ciphertext + tag, got " + data.length);
    }
    try {
      final javax.crypto.Cipher cipher = decryptCipher.get();
      final byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
      final byte[] sealed = Arrays.copyOfRange(data, IV_LENGTH, data.length);
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return cipher.doFinal(sealed);
    } catch (final GeneralSecurityException e) {
      throw new IOException("Decryption failed: message was corrupted or tampered with", e);
    }
  }
}
