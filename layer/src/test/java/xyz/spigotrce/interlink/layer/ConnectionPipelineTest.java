package xyz.spigotrce.interlink.layer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ConnectionPipelineTest {

  private static final List<String> EVENTS = new ArrayList<>();

  @Test
  public void passthroughWithNoLayers() throws IOException {
    final ConnectionPipeline pipeline =
        new ConnectionPipeline(() -> new byte[] {1}, (data) -> {
          throw new IOException("no wire");
        });
    assertArrayEquals(new byte[] {1}, pipeline.read());
  }

  @Test
  public void outboundFlowsAppToWireAndInboundFlowsWireToApp() throws IOException {
    clearEvents();
    final ConnectionPipeline pipeline =
        new ConnectionPipeline(() -> new byte[] {9}, (data) -> {});
    // Application end first, then wire end: list reads [wire, app].
    pipeline.addLast("app", named("app", true));
    pipeline.addFirst("wire", named("wire", true));

    pipeline.write(new byte[] {0});
    assertEquals(List.of("app-out", "wire-out"), EVENTS);

    clearEvents();
    pipeline.read();
    assertEquals(List.of("wire-in", "app-in"), EVENTS);
  }

  private static void clearEvents() {
    EVENTS.clear();
  }

  private static Layer named(final String tag, final boolean recordInbound) {
    return new Layer() {
      @Override
      public void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
        if (recordInbound) {
          EVENTS.add(tag + "-in");
        }
        ctx.fireInbound(data);
      }

      @Override
      public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
        EVENTS.add(tag + "-out");
        ctx.fireOutbound(data);
      }
    };
  }

  @Test
  public void wireWriterReceivesProcessedFrame() throws IOException {
    final List<byte[]> wireFrames = new ArrayList<>();
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> null, wireFrames::add);
    pipeline.addFirst(
        "suffix",
        new Layer() {
          @Override
          public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
            ctx.fireOutbound(new byte[] {data[0], data[1]});
          }
        });
    pipeline.write(new byte[] {5, 6});
    assertArrayEquals(new byte[] {5, 6}, wireFrames.get(0));
  }

  @Test
  public void wireReaderExceptionPropagates() {
    final ConnectionPipeline pipeline =
        new ConnectionPipeline(
            () -> {
              throw new IOException("wire down");
            },
            (data) -> {});
    final IOException thrown = assertThrows(IOException.class, pipeline::read);
    assertTrue(thrown.getMessage().contains("wire down"));
  }

  @Test
  public void layerExceptionWrappedInIoException() {
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> null, (data) -> {});
    pipeline.addLast(
        "broken",
        new Layer() {
          @Override
          public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
            throw new LayerException("boom");
          }
        });
    final IOException thrown = assertThrows(IOException.class, () -> pipeline.write(new byte[0]));
    assertTrue(thrown.getCause() instanceof LayerException);
  }

  @Test
  public void inboundLayerCanSwallowMessage() throws IOException {
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> new byte[] {9}, (data) -> {});
    pipeline.addFirst(
        "swallow",
        new Layer() {
          @Override
          public void onInbound(final byte[] data, final LayerContext ctx) {}
        });
    assertNull(pipeline.read());
  }

  @Test
  public void inboundLayerCanQueueAdditionalFrames() throws IOException {
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> new byte[] {1}, (data) -> {});
    pipeline.addFirst(
        "multi",
        new Layer() {
          @Override
          public void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
            ctx.fireInbound(data);
            ctx.fireInbound(new byte[] {2, 3});
          }
        });
    assertArrayEquals(new byte[] {1}, pipeline.read());
    assertArrayEquals(new byte[] {2, 3}, pipeline.read());
  }

  @Test
  public void outboundLayerCanSplitIntoSeveralFrames() throws IOException {
    final List<byte[]> wireFrames = new ArrayList<>();
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> null, wireFrames::add);
    pipeline.addFirst(
        "split",
        new Layer() {
          @Override
          public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
            ctx.fireOutbound(new byte[] {data[0]});
            ctx.fireOutbound(new byte[] {data[1]});
          }
        });
    pipeline.write(new byte[] {5, 6});
    assertArrayEquals(new byte[] {5}, wireFrames.get(0));
    assertArrayEquals(new byte[] {6}, wireFrames.get(1));
  }

  @Test
  public void contextExposesLayerName() throws IOException {
    final List<String> names = new ArrayList<>();
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> new byte[] {0}, (data) -> {});
    pipeline.addFirst(
        "visible",
        new Layer() {
          @Override
          public void onInbound(final byte[] data, final LayerContext ctx) {
            names.add(ctx.name());
          }
        });
    pipeline.read();
    assertEquals("visible", names.get(0));
  }

  @Test
  public void sharedStateIsSharedAcrossLayers() throws IOException {
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> new byte[] {0}, (data) -> {});
    pipeline.addFirst(
        "stash",
        new Layer() {
          @Override
          public void onInbound(final byte[] data, final LayerContext ctx) {
            ctx.sharedState().put("key", "value");
          }
        });
    pipeline.read();
    assertEquals("value", pipeline.sharedState().get("key"));
  }

  @Test
  public void addBeforeAndAddAfterHonorRelativePosition() throws IOException {
    clearEvents();
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> new byte[] {0}, (data) -> {});
    // Outbound flows from the application end (last index) toward the wire (index 0).
    pipeline.addFirst("A", trace("A"));
    pipeline.addAfter("B", "A", trace("B"));
    pipeline.addBefore("C", "B", trace("C"));

    pipeline.write(new byte[] {0});
    assertEquals(List.of("B", "C", "A"), EVENTS);
  }

  private static Layer trace(final String name) {
    return new Layer() {
      @Override
      public void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
        ctx.fireInbound(data);
      }

      @Override
      public void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
        EVENTS.add(name);
        ctx.fireOutbound(data);
      }
    };
  }

  @Test
  public void removeDropsLayer() throws IOException {
    clearEvents();
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> new byte[] {0}, (data) -> {});
    pipeline.addFirst("keep", trace("keep"));
    pipeline.addLast("drop", trace("drop"));
    pipeline.remove("drop");
    pipeline.write(new byte[0]);
    assertEquals(List.of("keep"), EVENTS);
  }

  @Test
  public void duplicateNameRejected() {
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> null, (data) -> {});
    pipeline.addFirst("dup", new Layer() {});
    assertThrows(IllegalArgumentException.class, () -> pipeline.addLast("dup", new Layer() {}));
  }

  @Test
  public void missingTargetRejected() {
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> null, (data) -> {});
    assertThrows(
        IllegalArgumentException.class,
        () -> pipeline.addBefore("a", "nonexistent", new Layer() {}));
    assertThrows(
        IllegalArgumentException.class,
        () -> pipeline.addAfter("a", "nonexistent", new Layer() {}));
    assertThrows(IllegalArgumentException.class, () -> pipeline.remove("nonexistent"));
  }

  @Test
  public void nullArgumentsRejected() {
    final ConnectionPipeline pipeline = new ConnectionPipeline(() -> null, (data) -> {});
    assertThrows(NullPointerException.class, () -> pipeline.addFirst(null, new Layer() {}));
    assertThrows(NullPointerException.class, () -> pipeline.addFirst("n", null));
  }
}
