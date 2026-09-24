package xyz.spigotrce.interlink.layer;

/**
 * Signals that a {@link Layer} failed to process a message, typically because the message was
 * malformed, corrupt, or malicious. Aborts the current chain when thrown from {@link
 * Layer#onInbound(byte[], LayerContext)} or {@link Layer#onOutbound(byte[], LayerContext)}.
 */
public class LayerException extends Exception {

  /**
   * Creates a new exception with the given detail message.
   *
   * @param message the detail message
   */
  public LayerException(final String message) {
    super(message);
  }

  /**
   * Creates a new exception with the given detail message and underlying cause.
   *
   * @param message the detail message
   * @param cause the underlying cause of this exception
   */
  public LayerException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new exception wrapping the given underlying cause.
   *
   * @param cause the underlying cause of this exception
   */
  public LayerException(final Throwable cause) {
    super(cause);
  }
}
