package xyz.spigotrce.interlink.layer;

public interface Layer {

  /**
   * Handles one message travelling from the wire toward the application. Transform the payload
   * in place, then either forward it with {@link LayerContext#fireInbound(byte[])}, swallow it
   * entirely by not calling {@code fireInbound}, or abort the message with {@link
   * LayerException} if it is malformed or malicious.
   */
  default void onInbound(final byte[] data, final LayerContext ctx) throws LayerException {
    ctx.fireInbound(data);
  }

  /**
   * Handles one message travelling from the application toward the wire. Transform the payload
   * in place, then either forward it with {@link LayerContext#fireOutbound(byte[])}, swallow it
   * entirely by not calling {@code fireOutbound}, or abort the message with {@link
   * LayerException}.
   */
  default void onOutbound(final byte[] data, final LayerContext ctx) throws LayerException {
    ctx.fireOutbound(data);
  }
}
