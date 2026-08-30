package com.mrfdev.watersourcemod;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterMarkerAccumulatorTest {
    @Test
    void neverRetainsMoreThanTheConfiguredLimit() {
        WaterMarkerAccumulator accumulator = new WaterMarkerAccumulator(2);

        accumulator.add(0, 64, 0, false, false, 0.6F);
        accumulator.add(1, 64, 0, false, false, 0.7F);
        assertFalse(accumulator.limitReached());

        accumulator.add(2, 64, 0, false, false, 0.8F);

        assertEquals(2, accumulator.size());
        assertEquals(0, accumulator.sourceCount());
        assertEquals(2, accumulator.flowingCount());
        assertTrue(accumulator.limitReached());
        assertEquals(1, accumulator.discardedCount());
    }

    @Test
    void sourceMarkersDisplaceFlowingMarkersAtTheLimit() {
        WaterMarkerAccumulator accumulator = new WaterMarkerAccumulator(3);
        accumulator.add(0, 64, 0, false, false, 0.5F);
        accumulator.add(1, 64, 0, false, false, 0.6F);
        accumulator.add(2, 64, 0, false, false, 0.7F);

        accumulator.add(3, 64, 0, true, true, 1.0F);

        List<WaterMarker> markers = accumulator.markersWithSourcesFirst();
        assertEquals(3, markers.size());
        assertEquals(1, accumulator.sourceCount());
        assertEquals(2, accumulator.flowingCount());
        assertTrue(markers.getFirst().source());
        assertTrue(accumulator.limitReached());
        assertEquals(1, accumulator.discardedCount());
    }

    @Test
    void sourceOnlyOverflowRemainsBounded() {
        WaterMarkerAccumulator accumulator = new WaterMarkerAccumulator(1);
        accumulator.add(0, 64, 0, true, false, 1.0F);
        accumulator.add(1, 64, 0, true, false, 1.0F);

        assertEquals(1, accumulator.size());
        assertEquals(1, accumulator.sourceCount());
        assertEquals(0, accumulator.flowingCount());
        assertTrue(accumulator.limitReached());
        assertEquals(1, accumulator.discardedCount());
    }
}
