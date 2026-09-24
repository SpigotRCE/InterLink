package xyz.spigotrce.interlink;

import java.nio.charset.StandardCharsets;

/**
 * Shared demo constants used by the runnable {@link TestServer} and {@link TestClient}.
 *
 * @author SpigotRCE
 */
public final class Shared {
  private Shared() {}

  /**
   * The shared AES key used to configure the {@link
   * xyz.spigotrce.interlink.layer.impl.EncryptionLayer} on both sides of the connection.
   */
  public static final byte[] key = "DQzxpO2A9L1n4QvR".getBytes(StandardCharsets.UTF_8);

  /**
   * The host the {@link xyz.spigotrce.interlink.server.Server} binds to and the {@link
   * xyz.spigotrce.interlink.client.Client} connects to.
   */
  public static final String host = "localhost";

  /**
   * The port the {@link xyz.spigotrce.interlink.server.Server} binds to and the {@link
   * xyz.spigotrce.interlink.client.Client} connects to.
   */
  public static final int port = 25565;
}
