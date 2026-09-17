package xyz.spigotrce.interlink.layer;

/**
 * A {@link Layer} that forwards every message unchanged in both directions. Extend this when a
 * layer only cares about one direction; the other direction is a pass-through by default.
 */
public class LayerAdapter implements Layer {}
