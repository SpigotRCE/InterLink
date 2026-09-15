package io.github.spigotrce.interlink.layer;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An authenticated encryption {@link Layer} using AES-GCM.
 *
 * <p>Wire format (per message): {@code [12-byte nonce][ciphertext + 16-byte GCM tag]}. A fresh,
 * cryptographically random nonce is generated for every message and transmitted alongside the
 * ciphertext, so callers never manage nonce state and can never accidentally reuse a {@code (key,
 * nonce)} pair - reuse under GCM is a catastrophic confidentiality/integrity break.
 *
 * <p>Edge cases handled:
 *
 * <ul>
 *   <li>Tamper/corruption: any bit-flip in transit fails GCM authentication and raises {@link
 *       LayerException}; corrupted plaintext never reaches the application.
 *   <li>Truncated frames (shorter than the nonce, or containing no tag bytes) are rejected
 *       explicitly rather than causing an unchecked bounds error.
 *   <li>Empty plaintext is supported - GCM authenticates an empty payload, so no special case is
 *       needed.
 * </ul>
 *
 * <p>A fresh {@link Cipher} is created per invocation, so the layer (and the pipeline holding it)
 * is safe for concurrent use by multiple threads.
 *
 * <p>This layer provides <em>only</em> message confidentiality and integrity for a pre-shared key.
 * It does <strong>not</strong> provide key exchange, forward secrecy, or key rotation - do not
 * mistake it for a TLS replacement. The shared key must be exchanged out of band or via a
 * handshake layer.
 */
public class EncryptionLayer implements Layer {
  private static final int NONCE_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public EncryptionLayer(final byte[] key) {
    this.key = new SecretKeySpec(key, "AES");
  }

  @Override
  public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
    try {
      final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      final byte[] nonce = new byte[NONCE_LENGTH];
      random.nextBytes(nonce);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

      final byte[] sealed = cipher.doFinal(data);
      final byte[] frame = new byte[NONCE_LENGTH + sealed.length];
      System.arraycopy(nonce, 0, frame, 0, NONCE_LENGTH);
      System.arraycopy(sealed, 0, frame, NONCE_LENGTH, sealed.length);
      ctx.fireOutbound(frame);
    } catch (final GeneralSecurityException e) {
      throw new LayerException("Failed to encrypt data", e);
    }
  }

  @Override
  public void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
    if (data.length < NONCE_LENGTH + 1) {
      throw new LayerException("Truncated encrypted frame: expected nonce + tag, got " + data.length);
    }
    try {
      final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      final byte[] nonce = Arrays.copyOfRange(data, 0, NONCE_LENGTH);
      final byte[] sealed = Arrays.copyOfRange(data, NONCE_LENGTH, data.length);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
      ctx.fireInbound(cipher.doFinal(sealed));
    } catch (final GeneralSecurityException e) {
      throw new LayerException("Decryption failed: message was tampered with or corrupted", e);
    }
  }
}
