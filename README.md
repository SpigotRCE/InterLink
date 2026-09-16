# InterLink

A very simple but versatile Java networking API. Sits somewhere between "hand-roll your own sockets" and "learn Netty's 47 abstractions." You get a clean packet protocol, a pluggable transport layer, and a pipeline model for things like compression and encryption - without the PhD.

## Features

- **Packet-based protocol** with a registry-driven codec - define packets as records, register them, send them
- **Transport abstraction** - ships with TCP and WebSocket implementations; write your own by implementing `Transport`
- **Pipeline architecture** - inbound/outbound middleware for compression, encryption, or whatever else you need
- **Pluggable ciphers** - AES-CFB, AES-CBC, AES-GCM out of the box, or bring your own `Cipher`
- **Pluggable compression** - `Compressor` interface with a ZLib implementation included
- **Server and client wrappers** - `Server<T>` and `Client<T>` handle the connection lifecycle so you don't have to

## Requirements

- Java 21+
- Gradle 8.10+ (if building from source)

Third-party runtime dependencies are pulled automatically by Gradle:
- `com.google.guava:guava:33.4.8-jre` (used by the buffer module)
- `org.java-websocket:Java-WebSocket:1.6.0` (used by the WebSocket transport)

## Usage

### Defining a packet

Packets are records that implement `Packet<T>`. Each one gets a static `CODEC` that handles serialization:

```java
public record PingPacket(long timestamp) implements Packet<PingPacket> {
  public static final PacketCodec<PingPacket> CODEC =
      PacketCodec.of(PingPacket::new, PingPacket::write);

  public PingPacket(final InputBuffer buffer) {
    this(buffer.readLong());
  }

  public void write(final OutputBuffer buffer) {
    buffer.writeLong(timestamp);
  }

  @Override
  public PacketCodec<PingPacket> getCodec() {
    return CODEC;
  }
}
```

### Setting up a server

```java
public final class MyServer {
  public static void main(final String[] args) throws Exception {
    final Server<TcpTransport> server = new Server<>(
        TcpTransport::new,
        "localhost",
        25565,
        MyServer::onConnect,
        MyServer::onDisconnect,
        (conn, err) -> err.printStackTrace());

    server.start();
  }

  static void onConnect(final Connection<TcpTransport> connection) {
    connection.setRegistry(new MyPacketRegistry(connection));
  }

  static void onDisconnect(final Connection<TcpTransport> connection) {
    System.out.println("Client disconnected");
  }
}
```

### Setting up a client

```java
public final class MyClient {
  public static void main(final String[] args) throws Exception {
    final Client<TcpTransport> client = new Client<>(
        TcpTransport::new,
        "localhost",
        25565,
        MyClient::onConnect,
        conn -> System.out.println("Disconnected"),
        (conn, err) -> err.printStackTrace());

    client.connect();
    client.getConnection().send(new PingPacket(System.currentTimeMillis()));
  }

  static void onConnect(final Connection<TcpTransport> connection) {
    connection.setRegistry(new MyClientPacketRegistry(connection));
  }
}
```

### Adding encryption and compression

Layers go in a pipeline. Add them in `onConnect` and they handle the rest:

```java
connection.getPipeline().addFirst("encryption", new EncryptionLayer(new AesCfbCipher(key)));
connection.getPipeline().addLast("compression", new CompressionLayer(new ZLibCompressor()));
```

## Project Structure

| Module | What it does |
|---|---|
| `buffer` | Binary read/write primitives on top of Google's Guava ByteBuffers |
| `packet` | `Packet`, `PacketCodec`, `PacketRegistry` - the protocol layer |
| `transport` | `Transport` interface and `Connection` class |
| `transport-impl` | `TcpTransport` and `WsTransport` |
| `layer` | `Layer` interface and `ConnectionPipeline` |
| `layer-impl` | `CompressionLayer` and `EncryptionLayer` |
| `cipher` / `cipher-impl` | `Cipher` interface with AES-CFB, AES-CBC, AES-GCM |
| `compressor` / `compressor-impl` | `Compressor` interface with ZLib |
| `server` | `Server<T>` - accept loop with per-connection receiver threads |
| `client` | `Client<T>` - connect and receive loop |
| `examples` | Working chat server/client demo |

## Contributing

Fork, branch, PR. Keep it simple - run `.\gradlew.bat build` before submitting. Tests are expected. Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

## License

Copyright (c) 2025-2026 SpigotRCE. Personal and non-commercial use only. See [LICENSE](LICENSE) for details.
