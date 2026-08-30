package com.mrfdev.watersourcemod;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearestWaterMarkerTest {
    @Test
    void findsNearestEligibleMarkerWithDistanceDirectionAndVerticalOffset() {
        WaterMarker north = new WaterMarker(0, 64, -5, true, false, 1F);
        WaterMarker fartherEast = new WaterMarker(8, 66, 0, false, false, 0.5F);

        NearestWaterMarker nearest = NearestWaterMarker.find(
                List.of(fartherEast, north),
                0.5D,
                64.5D,
                0.5D,
                WaterSourceConfig.defaults().renderSettings());

        assertTrue(nearest.isPresent());
        assertSame(north, nearest.marker());
        assertEquals(5, nearest.roundedDistance());
        assertEquals(NearestWaterMarker.Direction.NORTH, nearest.direction());
        assertEquals(0, nearest.verticalOffset());
    }

    @Test
    void ignoresHiddenCategoriesAndMarkersBeyondRenderDistance() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setShowSources(false);
        config.setMaxRenderDistance(16);
        config.normalize();
        WaterMarker hiddenSource = new WaterMarker(1, 64, 0, true, false, 1F);
        WaterMarker flowingEast = new WaterMarker(4, 65, 0, false, false, 0.5F);
        WaterMarker tooFar = new WaterMarker(40, 64, 0, false, false, 0.5F);

        NearestWaterMarker nearest = NearestWaterMarker.find(
                List.of(hiddenSource, tooFar, flowingEast),
                0.5D,
                64.25D,
                0.5D,
                config.renderSettings());

        assertSame(flowingEast, nearest.marker());
        assertEquals(NearestWaterMarker.Direction.EAST, nearest.direction());
        assertEquals(1, nearest.verticalOffset());

        config.setShowFlowing(false);
        NearestWaterMarker none = NearestWaterMarker.find(
                List.of(hiddenSource, tooFar, flowingEast),
                0.5D,
                64.25D,
                0.5D,
                config.renderSettings());
        assertFalse(none.isPresent());
        assertSame(NearestWaterMarker.none(), none);
    }

    @Test
    void reportsHereForAHorizontalMarkerAtThePlayerPosition() {
        WaterMarker marker = new WaterMarker(0, 66, 0, true, false, 1F);

        NearestWaterMarker nearest = NearestWaterMarker.find(
                List.of(marker),
                0.5D,
                64.5D,
                0.5D,
                WaterSourceConfig.defaults().renderSettings());

        assertEquals(NearestWaterMarker.Direction.HERE, nearest.direction());
        assertEquals(2, nearest.verticalOffset());
    }

    @Test
    void includesAMarkerExactlyAtTheConfiguredRenderDistance() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setMaxRenderDistance(16);
        config.normalize();
        WaterMarker boundaryMarker = new WaterMarker(16, 64, 0, true, false, 1F);

        NearestWaterMarker nearest = NearestWaterMarker.find(
                List.of(boundaryMarker),
                0.5D,
                64.5D,
                0.5D,
                config.renderSettings());

        assertSame(boundaryMarker, nearest.marker());
        assertEquals(16, nearest.roundedDistance());
    }
}
