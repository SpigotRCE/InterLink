package io.github.spigotrce.interlink.connection;

import io.github.spigotrce.interlink.buf.*;
import io.github.spigotrce.interlink.compression.ZLibCompressor;
import io.github.spigotrce.interlink.packet.*;

import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.io.*;
import java.util.function.BiConsumer;

public class Connection<T extends Transport> {
  private final T transport;
  private final Cipher encryptCipher;
  private final Cipher decryptCipher;
  private final BiConsumer<Connection<T>, Throwable> onException;

  private PacketRegistry registry;
  private int compressionThreshold = 0;
  private boolean disconnected = false;

  public Connection(
    T transport,
    byte[] key, byte[] iv,
    BiConsumer<Connection<T>, Throwable> onException
  ) throws Exception {
    this.transport = transport;
    this.onException = onException;

    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    IvParameterSpec ivSpec = new IvParameterSpec(iv);

    encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
    encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

    decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
    decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
  }

  public void send(Packet<?> packet) {
    if (disconnected) return;
    try {
      OutputBuffer out = OutputBuffer.create();
      int id = registry.getId(packet);
      if (id == -1) {
        throw new IOException("Unregistered packet: " + packet.getClass());
      }
      out.writeInt(id);
      registry.encode(packet, out);

      byte[] data = out.toByteArray();
      boolean compressed = data.length >= compressionThreshold && compressionThreshold > 0;
      if (compressed) data = ZLibCompressor.compress(data);

      data = encryptCipher.doFinal(data);

      // prepend metadata
      ByteArrayOutputStream meta = new ByteArrayOutputStream();
      DataOutputStream metaOut = new DataOutputStream(meta);
      metaOut.writeBoolean(compressed);
      metaOut.writeInt(data.length);
      metaOut.write(data);

      transport.send(meta.toByteArray());
    } catch (Exception e) {
      onException.accept(this, e);
    }
  }

  public Packet<?> read() {
    if (disconnected) return null;
    try {
      byte[] frame = transport.receive();
      DataInputStream metaIn = new DataInputStream(new ByteArrayInputStream(frame));
      boolean compressed = metaIn.readBoolean();
      int length = metaIn.readInt();

      byte[] data = new byte[length];
      metaIn.readFully(data);

      data = decryptCipher.doFinal(data);
      if (compressed) data = ZLibCompressor.decompress(data);

      InputBuffer in = InputBuffer.create(data);
      int id = in.readInt();
      return registry.decode(id, in);
    } catch (Exception e) {
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

  public void setRegistry(PacketRegistry registry) {
    this.registry = registry;
  }

  public int getCompressionThreshold() {
    return compressionThreshold;
  }

  public void setCompressionThreshold(int compressionThreshold) {
    this.compressionThreshold = compressionThreshold;
  }

  public BiConsumer<Connection<T>, Throwable> getOnException() {
    return onException;
  }

  public boolean isDisconnected() {
    return disconnected;
  }

  public void setDisconnected(boolean disconnected) {
    this.disconnected = disconnected;
  }

  public void close() {
    try {
      transport.close();
    } catch (IOException e) {
      onException.accept(this, e);
    }
    disconnected = true;
  }
}
