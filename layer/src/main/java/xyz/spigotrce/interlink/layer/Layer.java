package xyz.spigotrce.interlink.layer;

/**
 * A single processing step in a {@link ConnectionPipeline}. Every layer is invoked once per message
 * in each direction and transforms the message in place before continuing the chain through {@link
 * LayerContext}.
 */
public interface Layer {

  /**
   * Handles one message travelling from the wire toward the application. Transform the payload
   * in place, then either forward it with {@link LayerContext#fireInbound(byte[])}, swallow it
   * entirely by not calling {@code fireInbound}, or abort the message with {@link
   * LayerException} if it is malformed or malicious.
   *
   * @param data the payload received from the wire
   * @param ctx the context used to continue the inbound chain
   * @throws LayerException if the message is malformed or malicious
   */
  default void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
    ctx.fireInbound(data);
  }

  /**
   * Handles one message travelling from the application toward the wire. Transform the payload
   * in place, then either forward it with {@link LayerContext#fireOutbound(byte[])}, swallow it
   * entirely by not calling {@code fireOutbound}, or abort the message with {@link
   * LayerException}.
   *
   * @param data the payload travelling from the application
   * @param ctx the context used to continue the outbound chain
   * @throws LayerException if the message cannot be processed
   */
  default void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
    ctx.fireOutbound(data);
  }
}
