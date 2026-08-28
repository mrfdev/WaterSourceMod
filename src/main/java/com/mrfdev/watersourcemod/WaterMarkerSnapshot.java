package com.mrfdev.watersourcemod;

import java.util.List;

/**
 * Atomic handoff between an incremental scan and the render phase.
 *
 * <p>The previous completed snapshot is intentionally retained while a new
 * scan is being assembled, so an automatic refresh cannot make markers blink
 * out for the duration of the scan.</p>
 */
final class WaterMarkerSnapshot {
    private volatile List<WaterMarker> markers = List.of();

    List<WaterMarker> current() {
        return markers;
    }

    void beginScan() {
        // Keep the last completed snapshot visible until the replacement is ready.
    }

    void publish(List<WaterMarker> next) {
        markers = List.copyOf(next);
    }

    void clear() {
        markers = List.of();
    }
}
