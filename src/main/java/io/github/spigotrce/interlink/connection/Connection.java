package io.github.spigotrce.interlink.connection;

import io.github.spigotrce.interlink.buf.*;
import io.github.spigotrce.interlink.compression.ZLibCompressor;
import io.github.spigotrce.interlink.packet.*;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.util.function.*;

public class Connection {
  private final Socket socket;
  private final DataOutputStream output;
  private final DataInputStream input;

  private final Cipher encryptCipher;
  private final Cipher decryptCipher;

  private final BiConsumer<Connection, Throwable> onException;

  private PacketRegistry registry;
  private int compressionThreshold = 0;

  private boolean disconnected = false;

  public Connection(Socket socket, byte[] key, byte[] iv, BiConsumer<Connection, Throwable> onException)
    throws Exception {
    this.socket = socket;
    this.output = new DataOutputStream(socket.getOutputStream());
    this.input = new DataInputStream(socket.getInputStream());
    this.onException = onException;

    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    IvParameterSpec ivSpec = new IvParameterSpec(iv);

    encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
    encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

    decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
    decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
  }

  public void send(Packet<?> packet) {
    try {
      OutputBuffer out = OutputBuffer.create();
      int id = registry.getId(packet);
      if (id == -1) {
        throw new IOException("Unregistered packet: " + packet.getClass());
      }

      out.writeInt(id);
      registry.encode(packet, out);

      byte[] data = out.toByteArray();

      boolean compressed = data.length >= compressionThreshold && compressionThreshold != 0;
      if (compressed) {
        data = ZLibCompressor.compress(data);
      }

      data = encryptCipher.doFinal(data);

      // [compressed flag][length][data]
      output.writeBoolean(compressed);
      output.writeInt(data.length);
      output.write(data);
      output.flush();
    } catch (Exception e) {
      onException.accept(this, e);
    }
  }

  public Packet<?> read() {
    try {
      // [compressed flag][length][data]
      boolean compressed = input.readBoolean();
      int length = input.readInt();

      byte[] data = new byte[length];
      input.readFully(data);

      data = decryptCipher.doFinal(data);


      if (compressed) {
        data = ZLibCompressor.decompress(data);
      }

      InputBuffer in = InputBuffer.create(data);
      int id = in.readInt();
      return registry.decode(id, in);
    } catch (EOFException | SocketException e) {
      if (!disconnected) {
        onException.accept(this, e);
      }
      return null;
    } catch (Exception e) {
      onException.accept(this, e);
      return null;
    }
  }

  public Socket getSocket() {
    return socket;
  }

  public DataOutputStream getOutput() {
    return output;
  }

  public DataInputStream getInput() {
    return input;
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

  public BiConsumer<Connection, Throwable> getOnException() {
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
      input.close();
      output.close();
      socket.close();
      disconnected = true;
    } catch (IOException e) {
      onException.accept(this, e);
    }
  }
}
