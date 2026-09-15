package io.github.spigotrce.interlink.connection;

import io.github.spigotrce.interlink.buf.InputBuffer;
import io.github.spigotrce.interlink.buf.OutputBuffer;
import io.github.spigotrce.interlink.compression.ZLibCompressor;
import io.github.spigotrce.interlink.packet.Packet;
import io.github.spigotrce.interlink.packet.PacketRegistry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Connection<T extends Transport<T>> {
  public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024; // 8 mb

  private final T transport;
  private final Cipher encryptCipher;
  private final Cipher decryptCipher;
  private final BiConsumer<Connection<T>, Throwable> onException;
  private final Object sendLock = new Object();

  private PacketRegistry registry;
  private int compressionThreshold = 0;
  private boolean disconnected = false;

  public Connection(
      final T transport,
      final byte[] key,
      final byte[] iv,
      final BiConsumer<Connection<T>, Throwable> onException)
      throws Exception {
    this.transport = transport;
    this.onException = onException;

    final SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    final IvParameterSpec ivSpec = new IvParameterSpec(iv);

    encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
    encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

    decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
    decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
  }

  public void send(final Packet<?> packet) {
    if (disconnected) {
      return;
    }
    try {
      final OutputBuffer out = OutputBuffer.create();
      final int id = registry.getId(packet);
      if (id == -1) {
        throw new IOException("Unregistered packet: " + packet.getClass());
      }
      out.writeInt(id);
      registry.encode(packet, out);

      byte[] data = out.toByteArray();
      final boolean compressed = data.length >= compressionThreshold && compressionThreshold > 0;
      if (compressed) {
        data = ZLibCompressor.compress(data);
      }

      final byte[] encrypted;
      synchronized (sendLock) {
        encrypted = encryptCipher.doFinal(data);
      }

      final ByteArrayOutputStream meta = new ByteArrayOutputStream();
      final DataOutputStream metaOut = new DataOutputStream(meta);
      metaOut.writeBoolean(compressed);
      metaOut.writeInt(encrypted.length);
      metaOut.write(encrypted);

      transport.send(meta.toByteArray());
    } catch (final Exception e) {
      disconnected = true;
      onException.accept(this, e);
    }
  }

  public Packet<?> read() {
    if (disconnected) {
      return null;
    }
    try {
      final byte[] frame = transport.receive();
      final DataInputStream metaIn = new DataInputStream(new ByteArrayInputStream(frame));
      final boolean compressed = metaIn.readBoolean();
      final int length = metaIn.readInt();

      if (length < 0 || length > MAX_FRAME_LENGTH) {
        throw new IOException("Frame length out of bounds: " + length);
      }

      byte[] data = new byte[length];
      metaIn.readFully(data);

      data = decryptCipher.doFinal(data);
      if (compressed) {
        data = ZLibCompressor.decompress(data);
      }

      final InputBuffer in = InputBuffer.create(data);
      final int id = in.readInt();
      return registry.decode(id, in);
    } catch (final Exception e) {
      disconnected = true;
      onException.accept(this, e);
      return null;
    }
  }

  public T getTransport() {
    return transport;
  }

  public Cipher getEncryptCipher() {
    return encryptCipher;
  }

  public Cipher getDecryptCipher() {
    return decryptCipher;
  }

  public PacketRegistry getRegistry() {
    return registry;
  }

  public void setRegistry(final PacketRegistry registry) {
    this.registry = registry;
  }

  public int getCompressionThreshold() {
    return compressionThreshold;
  }

  public void setCompressionThreshold(final int compressionThreshold) {
    this.compressionThreshold = compressionThreshold;
  }

  public BiConsumer<Connection<T>, Throwable> getOnException() {
    return onException;
  }

  public boolean isDisconnected() {
    return disconnected;
  }

  public void setDisconnected(final boolean disconnected) {
    this.disconnected = disconnected;
  }

  public void close() {
    try {
      transport.close();
    } catch (final IOException e) {
      onException.accept(this, e);
    }
    disconnected = true;
  }
}
