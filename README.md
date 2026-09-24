# InterLink

A very simple but versatile Java networking API. Sits somewhere between "hand-roll your own sockets" and "learn Netty's 47 abstractions." You get a clean packet protocol, a pluggable transport layer, and a pipeline model for things like compression and encryption - without the PhD.

## Features

- **Packet-based protocol** - packets are records with a shared `PacketType` enum that assigns ids and codecs; registries attach per-packet handlers
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
public record HandshakePacket(String username) implements Packet<HandshakePacket> {
  public static final PacketCodec<HandshakePacket> CODEC =
      PacketCodec.of(HandshakePacket::new, HandshakePacket::write);

  public HandshakePacket(final InputBuffer buffer) {
    this(buffer.readUTF());
  }

  public void write(final OutputBuffer buffer) {
    buffer.writeUTF(username);
  }

  @Override
  public PacketCodec<HandshakePacket> getCodec() {
    return CODEC;
  }
}
```

### Declaring a shared packet enum

A `PacketType` enum lists the packets for a protocol phase. Its declaration order is the wire order, and each constant's ordinal becomes its packet id. Both sides of a connection must agree on it:

```java
public enum LoginPackets implements PacketType {
  HANDSHAKE(HandshakePacket.class, HandshakePacket.CODEC),
  DISCONNECT(DisconnectPacket.class, DisconnectPacket.CODEC),
  LOGIN_SUCCESS(LoginSuccessPacket.class, LoginSuccessPacket.CODEC);

  private final Class<? extends Packet<?>> packetClass;
  private final PacketCodec<?> codec;

  LoginPackets(final Class<? extends Packet<?>> packetClass, final PacketCodec<?> codec) {
    this.packetClass = packetClass;
    this.codec = codec;
  }

  @Override
  public Class<? extends Packet<?>> packetClass() {
    return packetClass;
  }

  @Override
  public PacketCodec<?> codec() {
    return codec;
  }
}
```

### Writing a packet registry

A registry takes the shared enum and automatically registers every packet in it in declaration order. Attach handlers per packet with `registerPacket(PacketType, Consumer)`. Handler methods must have distinct names (a generic enum constant can't resolve an overloaded method reference):

```java
public class ServerLoginPacketRegistry extends PacketRegistry<LoginPackets> {
  public final Connection<TcpTransport> connection;

  public ServerLoginPacketRegistry(final Connection<TcpTransport> connection) {
    super(LoginPackets.class);
    this.connection = connection;

    // register a specific handler for this packet type as the server
    // only handles this packet and rest of the packets are outgoing
    registerPacket(LoginPackets.HANDSHAKE, this::handleHandshake);
  }

  public void handleHandshake(final HandshakePacket packet) {
    if (TestServer.namedConnections.containsKey(packet.username())) {
      connection.send(new DisconnectPacket("Username already taken"));
      connection.close();
    } else {
      final int threashold = 256;
      connection.send(new LoginSuccessPacket(threashold));
      connection.getPipeline().addLast("compression", new CompressionLayer(new ZLibCompressor(), threashold));
      connection.setRegistry(new ServerPlayPacketRegistry(connection));
      TestServer.connections.add(connection);
      TestServer.namedConnections.put(packet.username(), connection);
      System.out.println(
          "User "
              + packet.username()
              + " connected from "
              + connection.getTransport().getHostname()
              + ":"
              + connection.getTransport().getPort());
    }
  }
}
```

Packets without a registered handler are treated as one-way: receiving one throws an `IllegalArgumentException`.

### Setting up a server

`Server.start()` runs the accept loop on the calling thread and blocks. Use the final constructor argument to run code once the listener has bound:

```java
public final class TestServer {
  public static final ArrayList<Connection<TcpTransport>> connections = new ArrayList<>();
  public static final HashMap<String, Connection<TcpTransport>> namedConnections = new HashMap<>();

  public static void main(final String[] args) throws Exception {
    final Server<TcpTransport> server =
        new Server<TcpTransport>(
            TcpTransport::new,
            Shared.host,
            Shared.port,
            TestServer::onConnect,
            TestServer::onDisconnect,
            TestServer::onException,
            () -> System.out.println("Server is listening on tcp://" + Shared.host + ":" + Shared.port));

    server.start();
  }

  public static void onConnect(final Connection<TcpTransport> connection) {
    connection.getPipeline().addFirst("encryption", new EncryptionLayer(new AesCfbCipher(Shared.key)));
    connection.setRegistry(new ServerLoginPacketRegistry(connection));
  }

  public static void onDisconnect(final Connection<TcpTransport> connection) {
    connections.remove(connection);
  }

  public static void onException(
      final Connection<TcpTransport> connection, final Throwable throwable) {
    throwable.printStackTrace();
    if (!connection.getTransport().getSocket().isClosed()) {
      connection.send(new DisconnectPacket("Exception: " + throwable.getMessage()));
    }
    connections.remove(connection);
  }
}
```

### Setting up a client

```java
public final class TestClient {
  public static void main(final String[] args) throws Exception {
    final Client<TcpTransport> testClient =
        new Client<TcpTransport>(
            TcpTransport::new,
            Shared.host,
            Shared.port,
            TestClient::onConnect,
            TestClient::onDisconnect,
            TestClient::onException);

    testClient.connect();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    testClient.disconnect();
                  } catch (final Exception e) {
                    e.printStackTrace();
                  }
                },
                "Client-Shutdown"));

    while (true) {
      final String message = input("");
      try {
        if ("/exit".equalsIgnoreCase(message)) {
          testClient.disconnect();
          break;
        } else {
          testClient.getConnection().send(new ChatPacket(message));
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static void onConnect(final Connection<TcpTransport> connection) {
    connection.getPipeline().addFirst("encryption", new EncryptionLayer(new AesCfbCipher(Shared.key)));
    connection.setRegistry(new ClientLoginPacketRegistry(connection));
    connection.send(new HandshakePacket(input("Enter username: ")));
  }

  public static void onDisconnect(final Connection<TcpTransport> connection) {
    System.out.println("Disconnected from server!");
  }

  public static void onException(
      final Connection<TcpTransport> connection, final Throwable throwable) {
    throwable.printStackTrace();
  }

  private static String input(final String message) {
    final Scanner scanner = new Scanner(System.in);
    System.out.print(message);
    return scanner.nextLine();
  }
}
```

### Adding encryption and compression

Layers go in a pipeline. Both sides add encryption on connect; the client adds compression after a successful login, using the threshold it received from the server:

```java
// onConnect - both sides
connection.getPipeline().addFirst("encryption", new EncryptionLayer(new AesCfbCipher(Shared.key)));

// after login - client side, using the LoginSuccessPacket threshold
connection
    .getPipeline()
    .addLast("compressor", new CompressionLayer(new ZLibCompressor(), packet.compressionThreshold()));
```

## Project Structure

| Module | What it does |
|---|---|
| `buffer` | Binary read/write primitives on top of Google's Guava ByteBuffers |
| `packet` | `Packet`, `PacketCodec`, `PacketType`, `PacketRegistry` - the protocol layer |
| `transport` | `Transport` interface and `Connection` class |
| `transport-impl` | `TcpTransport` and `WsTransport` |
| `layer` | `Layer` interface and `ConnectionPipeline` |
| `layer-impl` | `CompressionLayer` and `EncryptionLayer` |
| `cipher` / `cipher-impl` | `Cipher` interface with AES-CFB, AES-CBC, AES-GCM |
| `compressor` / `compressor-impl` | `Compressor` interface with ZLib |
| `server` | `Server<T>` - accept loop with per-connection receiver threads |
| `client` | `Client<T>` - connect and receive loop |
| `examples` | Working chat server/client demo with login/play phases |

## Contributing

Fork, branch, PR. Keep it simple - run `.\gradlew.bat build` before submitting. Tests are expected. Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

## License

Copyright (c) 2025-2026 SpigotRCE. Personal and non-commercial use only. See [LICENSE](LICENSE) for details.
