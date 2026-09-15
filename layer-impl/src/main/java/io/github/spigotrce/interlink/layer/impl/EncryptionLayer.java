package io.github.spigotrce.interlink.layer.impl;

import io.github.spigotrce.interlink.layer.Layer;
import io.github.spigotrce.interlink.layer.LayerContext;
import io.github.spigotrce.interlink.layer.LayerException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An encryption {@link Layer} using AES in CFB mode with PKCS#5 padding.
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
 * <p><strong>Confidentiality only, no authentication.</strong> Encryption is not signed: CFB is
 * malleable, so an active attacker can flip bits in the ciphertext and the resulting plaintext may
 * still decrypt without error (padding corruption is caught only when the last block is disturbed).
 * Do not rely on this layer over an untrusted network. A signing layer (e.g. an HMAC or GCM) must
 * be added before real deployment.
 *
 * <p>This layer does <strong>not</strong> provide key exchange, forward secrecy, or key rotation.
 * The shared key must be exchanged out of band or via a handshake layer.
 */
public class EncryptionLayer implements Layer {
  private static final int IV_LENGTH = 16;
  private static final int BLOCK_LENGTH = 16;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();
  private final ThreadLocal<Cipher> encryptCipher =
      ThreadLocal.withInitial(EncryptionLayer::newCipher);
  private final ThreadLocal<Cipher> decryptCipher =
      ThreadLocal.withInitial(EncryptionLayer::newCipher);

  public EncryptionLayer(final byte[] key) {
    this.key = new SecretKeySpec(key, "AES");
  }

  private static Cipher newCipher() {
    try {
      return Cipher.getInstance("AES/CFB/PKCS5Padding");
    } catch (final GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
    if (data.length < IV_LENGTH + BLOCK_LENGTH) {
      throw new LayerException(
          "Truncated encrypted frame: expected IV + ciphertext, got " + data.length);
    }
    try {
      final Cipher cipher = decryptCipher.get();
      final byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
      final byte[] sealed = Arrays.copyOfRange(data, IV_LENGTH, data.length);
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
      ctx.fireInbound(cipher.doFinal(sealed));
    } catch (final GeneralSecurityException e) {
      throw new LayerException("Decryption failed: message was corrupted or malformed", e);
    }
  }

  @Override
  public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
    try {
      final Cipher cipher = encryptCipher.get();
      final byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

      final byte[] sealed = cipher.doFinal(data);
      final byte[] frame = new byte[IV_LENGTH + sealed.length];
      System.arraycopy(iv, 0, frame, 0, IV_LENGTH);
      System.arraycopy(sealed, 0, frame, IV_LENGTH, sealed.length);
      ctx.fireOutbound(frame);
    } catch (final GeneralSecurityException e) {
      throw new LayerException("Failed to encrypt data", e);
    }
  }
}
