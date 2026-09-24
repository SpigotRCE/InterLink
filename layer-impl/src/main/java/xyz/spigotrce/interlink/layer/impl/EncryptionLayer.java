package xyz.spigotrce.interlink.layer.impl;

import xyz.spigotrce.interlink.cipher.Cipher;
import xyz.spigotrce.interlink.layer.Layer;
import xyz.spigotrce.interlink.layer.LayerContext;
import xyz.spigotrce.interlink.layer.LayerException;
import java.io.IOException;

/**
 * An encryption {@link Layer} delegating to a pluggable {@link Cipher}.
 *
 * <p>The {@link Cipher} is fully responsible for its own wire format, IV/nonce handling, and error
 * reporting. This layer only wires it into the pipeline and translates failures into {@link
 * LayerException}s. For example {@code xyz.spigotrce.interlink.cipher.impl.AesGcmCipher}
 * frames each message as {@code [12-byte IV][ciphertext][16-byte tag]} with a fresh random IV and
 * authenticates every byte, while {@code AesCfbCipher} and {@code AesCbcCipher} provide
 * confidentiality only. Use an authenticated cipher when tampering is a threat.
 *
 * <p>Encryption does <strong>not</strong> provide key exchange, forward secrecy, or key rotation.
 * The shared key must be exchanged out of band or via a handshake layer.
 */
public class EncryptionLayer implements Layer {
  private final Cipher cipher;

  /**
   * Creates a layer that encrypts outbound messages and decrypts inbound messages with the given
   * cipher.
   *
   * @param cipher the cipher used for encryption and decryption
   */
  public EncryptionLayer(final Cipher cipher) {
    this.cipher = cipher;
  }

  /**
   * Decrypts the message with the {@link Cipher} and continues the inbound chain with the
   * plaintext.
   *
   * @param data the encrypted message received from the wire
   * @param ctx the context used to continue the inbound chain
   * @throws LayerException if decryption fails because the message was corrupted or malformed
   */
  @Override
  public void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
    try {
      ctx.fireInbound(cipher.decrypt(data));
    } catch (final IOException e) {
      throw new LayerException("Decryption failed: message was corrupted or malformed", e);
    }
  }

  /**
   * Encrypts the message with the {@link Cipher} and continues the outbound chain with the
   * ciphertext.
   *
   * @param data the plaintext message from the application
   * @param ctx the context used to continue the outbound chain
   * @throws LayerException if encryption fails
   */
  @Override
  public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
    try {
      ctx.fireOutbound(cipher.encrypt(data));
    } catch (final IOException e) {
      throw new LayerException("Failed to encrypt data", e);
    }
  }
}
