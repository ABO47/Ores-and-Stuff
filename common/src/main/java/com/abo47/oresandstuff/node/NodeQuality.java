package com.abo47.oresandstuff.node;

/**
 * Ore node quality level as a percentage (e.g. 23%, 34%, 200%). The value
 * itself is the multiplier: a node with 34% quality produces 0.34x output,
 * 200% quality produces 2x output. The allowed range per biome is configured
 * in the orenodes JSON.
 */
public final class NodeQuality {
    public static final double MIN = 1.0;
    public static final double MAX = 1000.0;

    private NodeQuality() {
    }

    public static double clamp(double quality) {
        return Math.max(MIN, Math.min(MAX, quality));
    }

    /** Output multiplier derived from the quality percentage. */
    public static double multiplier(double quality) {
        return quality / 100.0;
    }
}