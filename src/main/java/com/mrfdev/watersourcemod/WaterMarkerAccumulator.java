package com.mrfdev.watersourcemod;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects one scan's markers without ever retaining more than the configured
 * limit. Source markers take priority over flowing markers when space runs out.
 */
final class WaterMarkerAccumulator {
    private final int maxMarkers;
    private final List<WaterMarker> sourceMarkers = new ArrayList<>();
    private final List<WaterMarker> flowingMarkers = new ArrayList<>();
    private boolean limitReached;
    private long discardedCount;

    WaterMarkerAccumulator(int maxMarkers) {
        if (maxMarkers < 1) {
            throw new IllegalArgumentException("maxMarkers must be positive");
        }
        this.maxMarkers = maxMarkers;
    }

    void add(int x, int y, int z, boolean source, boolean waterlogged, float height) {
        if (source) {
            if (sourceMarkers.size() >= maxMarkers) {
                limitReached = true;
                discardedCount++;
                return;
            }
            if (size() >= maxMarkers) {
                flowingMarkers.remove(flowingMarkers.size() - 1);
                limitReached = true;
                discardedCount++;
            }
            sourceMarkers.add(new WaterMarker(x, y, z, true, waterlogged, height));
            return;
        }

        if (size() >= maxMarkers) {
            limitReached = true;
            discardedCount++;
            return;
        }
        flowingMarkers.add(new WaterMarker(x, y, z, false, waterlogged, height));
    }

    int sourceCount() {
        return sourceMarkers.size();
    }

    int flowingCount() {
        return flowingMarkers.size();
    }

    int size() {
        return sourceMarkers.size() + flowingMarkers.size();
    }

    boolean limitReached() {
        return limitReached;
    }

    long discardedCount() {
        return discardedCount;
    }

    List<WaterMarker> markersWithSourcesFirst() {
        List<WaterMarker> combined = new ArrayList<>(size());
        combined.addAll(sourceMarkers);
        combined.addAll(flowingMarkers);
        return List.copyOf(combined);
    }
}
