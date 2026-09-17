package xyz.spigotrce.interlink.layer;

import java.util.Map;

public interface LayerContext {

  void fireInbound(byte[] data) throws LayerException;

  void fireOutbound(byte[] data) throws LayerException;

  String name();

  Map<String, Object> sharedState();
}
