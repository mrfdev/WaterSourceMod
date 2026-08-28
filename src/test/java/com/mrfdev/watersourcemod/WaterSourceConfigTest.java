package com.mrfdev.watersourcemod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterSourceConfigTest {
    @Test
    void defaultsAreSafeAndMatchTheAccessibilityBrief() {
        WaterSourceConfig config = WaterSourceConfig.defaults();

        assertTrue(config.isShowSources());
        assertTrue(config.isShowFlowing());
        assertTrue(config.isIncludeWaterloggedSources());
        assertFalse(config.isThroughWalls());
        assertFalse(config.isShowLabels());
        assertEquals(1, config.getChunkRadius());
        assertEquals(WaterSourceConfig.MarkerStyle.BOX, config.getMarkerStyle());
    }

    @Test
    void normalizeClampsUntrustedConfigValues() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setChunkRadius(999);
        config.setMaxMarkers(-5);
        config.setOpacityPercent(999);
        config.setOutlineThickness(-2);
        config.setMarkerStyle(null);
        config.normalize();

        assertEquals(WaterSourceConfig.MAX_RADIUS, config.getChunkRadius());
        assertEquals(WaterSourceConfig.MIN_MAX_MARKERS, config.getMaxMarkers());
        assertEquals(100, config.getOpacityPercent());
        assertEquals(1, config.getOutlineThickness());
        assertEquals(WaterSourceConfig.MarkerStyle.BOX, config.getMarkerStyle());
    }

    @Test
    void copyIsIndependent() {
        WaterSourceConfig original = WaterSourceConfig.defaults();
        WaterSourceConfig copy = original.copy();
        copy.setThroughWalls(true);
        copy.setChunkRadius(2);

        assertNotSame(original, copy);
        assertFalse(original.isThroughWalls());
        assertEquals(1, original.getChunkRadius());
        assertTrue(copy.isThroughWalls());
        assertEquals(2, copy.getChunkRadius());
    }

    @Test
    void markerHeightIsSafeForRendering() {
        assertEquals(0.1F, new WaterMarker(0, 0, 0, false, false, -2F).height());
        assertEquals(1.0F, new WaterMarker(0, 0, 0, true, false, 2F).height());
    }
}
