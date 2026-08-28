package com.mrfdev.watersourcemod;

/** An immutable render candidate produced by the client-side water scan. */
public record WaterMarker(int x, int y, int z, boolean source, boolean waterlogged, float height) {
    public WaterMarker {
        height = Math.max(0.1F, Math.min(1.0F, height));
    }
}
