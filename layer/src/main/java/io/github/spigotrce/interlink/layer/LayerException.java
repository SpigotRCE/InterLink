package io.github.spigotrce.interlink.layer;

public class LayerException extends Exception {
  public LayerException(final String message) {
    super(message);
  }

  public LayerException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public LayerException(final Throwable cause) {
    super(cause);
  }
}
