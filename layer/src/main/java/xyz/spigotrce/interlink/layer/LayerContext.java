package xyz.spigotrce.interlink.layer;

import java.util.Map;

/**
 * The handle a {@link Layer} uses to continue a message through the {@link ConnectionPipeline}.
 * Calling {@link #fireInbound(byte[])} or {@link #fireOutbound(byte[])} forwards the transformed
 * payload to the next layer; doing neither swallows the message.
 */
public interface LayerContext {

  /**
   * Forwards the given payload further up the inbound chain, toward the application.
   *
   * @param data the transformed payload to forward
   * @throws LayerException if a downstream layer fails to process the payload
   */
  void fireInbound(byte[] data) throws LayerException;

  /**
   * Forwards the given payload further down the outbound chain, toward the wire.
   *
   * @param data the transformed payload to forward
   * @throws LayerException if a downstream layer fails to process the payload
   */
  void fireOutbound(byte[] data) throws LayerException;

  /**
   * Returns the name the current layer was registered under in the pipeline.
   *
   * @return the layer name
   */
  String name();

  /**
   * Returns the pipeline-wide shared state store.
   *
   * @return the shared {@link Map} all layers in the pipeline read from and write to
   */
  Map<String, Object> sharedState();
}
