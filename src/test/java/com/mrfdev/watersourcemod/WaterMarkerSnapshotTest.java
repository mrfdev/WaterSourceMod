package com.mrfdev.watersourcemod;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaterMarkerSnapshotTest {
    @Test
    void keepsLastCompletedMarkersVisibleDuringAnIncrementalRescan() {
        WaterMarkerSnapshot snapshot = new WaterMarkerSnapshot();
        List<WaterMarker> firstScan = List.of(new WaterMarker(12, 72, 24, true, false, 1.0F));
        List<WaterMarker> secondScan = List.of(new WaterMarker(13, 72, 24, false, false, 0.6F));

        snapshot.publish(firstScan);
        snapshot.beginScan();

        // This is the user-visible regression: a rescan must not blank the overlay.
        assertEquals(firstScan, snapshot.current());

        snapshot.publish(secondScan);
        assertEquals(secondScan, snapshot.current());
    }
}
