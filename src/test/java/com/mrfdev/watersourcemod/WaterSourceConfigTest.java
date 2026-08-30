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
        assertFalse(config.isHoldToShow());
        assertEquals(1, config.getChunkRadius());
        assertEquals(WaterSourceConfig.CURRENT_VERSION, config.getConfigVersion());
        assertEquals(WaterSourceConfig.MarkerStyle.BOX, config.getSourceMarkerStyle());
        assertEquals(WaterSourceConfig.MarkerStyle.PILLAR, config.getFlowingMarkerStyle());
        assertEquals(WaterSourceConfig.MarkerStyle.BEACON, config.getWaterloggedMarkerStyle());
        assertEquals(WaterSourceConfig.WaterloggedIndicator.CAP, config.getWaterloggedIndicator());
        assertEquals(
                WaterSourceConfig.FluidLevelVisualization.HEIGHT,
                config.getFluidLevelVisualization());
        assertEquals(WaterSourceConfig.NearestMarkerMode.OFF, config.getNearestMarkerMode());
        assertEquals(WaterSourceConfig.ScanBoundaryMode.OFF, config.getScanBoundaryMode());
        assertFalse(config.isNarrateStatus());
        assertFalse(config.isDiagnosticMode());
        assertEquals(WaterSourceConfig.RescanMode.AUTOMATIC, config.getRescanMode());
        assertEquals(WaterSourceConfig.VerticalRange.FULL_HEIGHT, config.getVerticalRange());
    }

    @Test
    void normalizeClampsUntrustedConfigValues() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setChunkRadius(999);
        config.setMaxMarkers(-5);
        config.setMaxVisibleMarkers(99_999);
        config.setOpacityPercent(999);
        config.setOutlineThickness(-2);
        config.setMarkerStyle(null);
        config.setWaterloggedIndicator(null);
        config.setFluidLevelVisualization(null);
        config.setNearestMarkerMode(null);
        config.setScanBoundaryMode(null);
        config.setScanBudgetPerTick(Integer.MAX_VALUE);
        config.setScanTimeBudgetMillis(0);
        config.setMaxRenderDistance(0);
        config.setHudScalePercent(999);
        config.setHudOffsetX(Integer.MIN_VALUE);
        config.normalize();

        assertEquals(512, WaterSourceConfig.MIN_MAX_MARKERS);
        assertEquals(WaterSourceConfig.MAX_RADIUS, config.getChunkRadius());
        assertEquals(WaterSourceConfig.MIN_MAX_MARKERS, config.getMaxMarkers());
        assertEquals(WaterSourceConfig.MIN_MAX_MARKERS, config.getMaxVisibleMarkers());
        assertEquals(100, config.getOpacityPercent());
        assertEquals(1, config.getOutlineThickness());
        assertEquals(WaterSourceConfig.MarkerStyle.BOX, config.getMarkerStyle());
        assertEquals(WaterSourceConfig.WaterloggedIndicator.CAP, config.getWaterloggedIndicator());
        assertEquals(
                WaterSourceConfig.FluidLevelVisualization.HEIGHT,
                config.getFluidLevelVisualization());
        assertEquals(WaterSourceConfig.NearestMarkerMode.OFF, config.getNearestMarkerMode());
        assertEquals(WaterSourceConfig.ScanBoundaryMode.OFF, config.getScanBoundaryMode());
        assertEquals(WaterSourceConfig.MAX_SCAN_BUDGET, config.getScanBudgetPerTick());
        assertEquals(WaterSourceConfig.MIN_SCAN_TIME_MILLIS, config.getScanTimeBudgetMillis());
        assertEquals(WaterSourceConfig.MIN_RENDER_DISTANCE, config.getMaxRenderDistance());
        assertEquals(WaterSourceConfig.MAX_HUD_SCALE_PERCENT, config.getHudScalePercent());
        assertEquals(WaterSourceConfig.MIN_HUD_OFFSET, config.getHudOffsetX());
    }

    @Test
    void copyIsIndependent() {
        WaterSourceConfig original = WaterSourceConfig.defaults();
        WaterSourceConfig copy = original.copy();
        copy.setThroughWalls(true);
        copy.setChunkRadius(2);
        copy.setHudOffsetY(42);
        copy.setNearestMarkerMode(WaterSourceConfig.NearestMarkerMode.BOTH);
        copy.setNarrateStatus(true);

        assertNotSame(original, copy);
        assertFalse(original.isThroughWalls());
        assertEquals(1, original.getChunkRadius());
        assertEquals(0, original.getHudOffsetY());
        assertEquals(WaterSourceConfig.NearestMarkerMode.OFF, original.getNearestMarkerMode());
        assertFalse(original.isNarrateStatus());
        assertTrue(copy.isThroughWalls());
        assertEquals(2, copy.getChunkRadius());
        assertEquals(42, copy.getHudOffsetY());
        assertEquals(WaterSourceConfig.NearestMarkerMode.BOTH, copy.getNearestMarkerMode());
        assertTrue(copy.isNarrateStatus());
    }

    @Test
    void markerHeightIsSafeForRendering() {
        assertEquals(0.1F, new WaterMarker(0, 0, 0, false, false, -2F).height());
        assertEquals(1.0F, new WaterMarker(0, 0, 0, true, false, 2F).height());
    }

    @Test
    void scanSettingsIgnoreRenderOnlyChanges() {
        WaterSourceConfig original = WaterSourceConfig.defaults();
        WaterSourceConfig renderOnlyChange = original.copy();
        renderOnlyChange.setSourceColor(0x123456);
        renderOnlyChange.setOpacityPercent(42);
        renderOnlyChange.setPulse(!original.isPulse());
        renderOnlyChange.setThroughWalls(!original.isThroughWalls());
        renderOnlyChange.setMaxVisibleMarkers(512);
        renderOnlyChange.setMaxRenderDistance(32);
        renderOnlyChange.setWaterloggedIndicator(WaterSourceConfig.WaterloggedIndicator.CROSS);
        renderOnlyChange.setFluidLevelVisualization(WaterSourceConfig.FluidLevelVisualization.BOTH);
        renderOnlyChange.setNearestMarkerMode(WaterSourceConfig.NearestMarkerMode.BOTH);
        renderOnlyChange.setScanBoundaryMode(WaterSourceConfig.ScanBoundaryMode.ALL);

        assertEquals(original.scanSettings(), renderOnlyChange.scanSettings());

        WaterSourceConfig scanChange = original.copy();
        scanChange.setChunkRadius(original.getChunkRadius() + 1);
        assertFalse(original.scanSettings().equals(scanChange.scanSettings()));

        WaterSourceConfig verticalChange = original.copy();
        verticalChange.setVerticalRange(WaterSourceConfig.VerticalRange.NEARBY_16);
        assertFalse(original.scanSettings().equals(verticalChange.scanSettings()));
    }

    @Test
    void visibilityControlsFilterRetainedMarkersIndependently() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        WaterMarker source = new WaterMarker(0, 64, 0, true, false, 1.0F);
        WaterMarker flowing = new WaterMarker(1, 64, 0, false, false, 0.6F);
        WaterMarker waterloggedSource = new WaterMarker(2, 64, 0, true, true, 1.0F);

        config.setShowSources(false);
        WaterSourceConfig.RenderSettings sourcesHidden = config.renderSettings();
        assertFalse(sourcesHidden.isMarkerEnabled(source));
        assertFalse(sourcesHidden.isMarkerEnabled(waterloggedSource));
        assertTrue(sourcesHidden.isMarkerEnabled(flowing));

        config.setShowSources(true);
        config.setShowFlowing(false);
        WaterSourceConfig.RenderSettings flowingHidden = config.renderSettings();
        assertTrue(flowingHidden.isMarkerEnabled(source));
        assertFalse(flowingHidden.isMarkerEnabled(flowing));

        config.setShowFlowing(true);
        config.setIncludeWaterloggedSources(false);
        WaterSourceConfig.RenderSettings waterloggedHidden = config.renderSettings();
        assertTrue(waterloggedHidden.isMarkerEnabled(source));
        assertTrue(waterloggedHidden.isMarkerEnabled(flowing));
        assertFalse(waterloggedHidden.isMarkerEnabled(waterloggedSource));
    }

    @Test
    void eachWaterCategoryCanUseANonColorVisualPattern() {
        WaterSourceConfig.RenderSettings settings = WaterSourceConfig.defaults().renderSettings();

        assertEquals(WaterSourceConfig.MarkerStyle.BOX,
                settings.styleFor(new WaterMarker(0, 64, 0, true, false, 1F)));
        assertEquals(WaterSourceConfig.MarkerStyle.PILLAR,
                settings.styleFor(new WaterMarker(0, 64, 0, false, false, 0.5F)));
        assertEquals(WaterSourceConfig.MarkerStyle.BEACON,
                settings.styleFor(new WaterMarker(0, 64, 0, true, true, 1F)));
    }

    @Test
    void verticalRangesClampToWorldHeightAndUseAnExclusiveMaximum() {
        assertEquals(
                new WaterSourceConfig.ScanHeight(64, 65),
                WaterSourceConfig.VerticalRange.CURRENT_Y.bounds(64, -64, 320));
        assertEquals(
                new WaterSourceConfig.ScanHeight(-64, -47),
                WaterSourceConfig.VerticalRange.NEARBY_16.bounds(-64, -64, 320));
        assertEquals(
                new WaterSourceConfig.ScanHeight(-64, 320),
                WaterSourceConfig.VerticalRange.FULL_HEIGHT.bounds(200, -64, 320));
        assertFalse(WaterSourceConfig.VerticalRange.NEARBY_32.shouldRecenter(80, 64));
        assertTrue(WaterSourceConfig.VerticalRange.NEARBY_32.shouldRecenter(81, 64));
        assertTrue(WaterSourceConfig.VerticalRange.CURRENT_Y.shouldRecenter(65, 64));
        assertFalse(WaterSourceConfig.VerticalRange.FULL_HEIGHT.shouldRecenter(300, 64));
    }

    @Test
    void profilesAreBoundedAndNeverEnableThroughWalls() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setThroughWalls(true);

        config.applyProfile(WaterSourceConfig.ConfigProfile.LOW_PERFORMANCE);

        assertFalse(config.isThroughWalls());
        assertFalse(config.isPulse());
        assertTrue(config.isCompactHud());
        assertEquals(0, config.getChunkRadius());
        assertEquals(512, config.getMaxVisibleMarkers());
        assertEquals(WaterSourceConfig.ConfigProfile.LOW_PERFORMANCE, config.getActiveProfile());
        assertEquals(WaterSourceConfig.WaterloggedIndicator.OFF, config.getWaterloggedIndicator());
        assertEquals(WaterSourceConfig.NearestMarkerMode.OFF, config.getNearestMarkerMode());
        assertEquals(WaterSourceConfig.ScanBoundaryMode.OFF, config.getScanBoundaryMode());
    }

    @Test
    void accessibilityAndExplorationProfilesEnableOnlyBoundedOptionalFeatures() {
        WaterSourceConfig accessibility = WaterSourceConfig.defaults();
        accessibility.applyProfile(WaterSourceConfig.ConfigProfile.ACCESSIBILITY);

        assertEquals(WaterSourceConfig.MarkerStyle.HOLLOW, accessibility.getSourceMarkerStyle());
        assertEquals(WaterSourceConfig.MarkerStyle.STRIPES, accessibility.getFlowingMarkerStyle());
        assertEquals(WaterSourceConfig.MarkerStyle.DOTS, accessibility.getWaterloggedMarkerStyle());
        assertEquals(WaterSourceConfig.WaterloggedIndicator.CROSS,
                accessibility.getWaterloggedIndicator());
        assertEquals(WaterSourceConfig.FluidLevelVisualization.BOTH,
                accessibility.getFluidLevelVisualization());
        assertEquals(WaterSourceConfig.NearestMarkerMode.HUD, accessibility.getNearestMarkerMode());
        assertTrue(accessibility.isNarrateStatus());
        assertFalse(accessibility.isThroughWalls());

        WaterSourceConfig exploration = WaterSourceConfig.defaults();
        exploration.applyProfile(WaterSourceConfig.ConfigProfile.EXPLORATION);

        assertEquals(WaterSourceConfig.NearestMarkerMode.BOTH, exploration.getNearestMarkerMode());
        assertEquals(WaterSourceConfig.ScanBoundaryMode.CURRENT, exploration.getScanBoundaryMode());
        assertFalse(exploration.isThroughWalls());
    }

    @Test
    void fluidLevelModesExposeIndependentHeightAndGaugeCues() {
        assertFalse(WaterSourceConfig.FluidLevelVisualization.OFF.usesHeight());
        assertFalse(WaterSourceConfig.FluidLevelVisualization.OFF.usesGauge());
        assertTrue(WaterSourceConfig.FluidLevelVisualization.HEIGHT.usesHeight());
        assertFalse(WaterSourceConfig.FluidLevelVisualization.HEIGHT.usesGauge());
        assertFalse(WaterSourceConfig.FluidLevelVisualization.GAUGE.usesHeight());
        assertTrue(WaterSourceConfig.FluidLevelVisualization.GAUGE.usesGauge());
        assertTrue(WaterSourceConfig.FluidLevelVisualization.BOTH.usesHeight());
        assertTrue(WaterSourceConfig.FluidLevelVisualization.BOTH.usesGauge());
    }

    @Test
    void scanBoundaryCanBeTheOnlyRenderableGeometry() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setShowSources(false);
        config.setShowFlowing(false);

        assertFalse(config.renderSettings().hasRenderableGeometry());

        config.setScanBoundaryMode(WaterSourceConfig.ScanBoundaryMode.CURRENT);
        assertTrue(config.renderSettings().hasRenderableGeometry());
    }

    @Test
    void presetPalettesDoNotOverwriteTheSavedCustomPalette() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setSourceColor(0x123456);
        config.setFlowingColor(0xABCDEF);
        config.setOutlineColor(0x010203);

        config.applyColorPalette(WaterSourceConfig.ColorPalette.COLOR_BLIND_SAFE);
        assertEquals(0xF0E442, config.getSourceColor());
        assertEquals(WaterSourceConfig.ColorPalette.COLOR_BLIND_SAFE, config.getColorPalette());

        config.applyColorPalette(WaterSourceConfig.ColorPalette.CUSTOM);
        assertEquals(0x123456, config.getSourceColor());
        assertEquals(0xABCDEF, config.getFlowingColor());
        assertEquals(0x010203, config.getOutlineColor());
    }

    @Test
    void hudAnchorsApplyOffsetsAndRemainOnScreen() {
        assertEquals(
                new WaterSourceConfig.HudPosition(8, 8),
                WaterSourceConfig.HudAnchor.TOP_LEFT.position(320, 240, 100, 20, 0, 0));
        assertEquals(
                new WaterSourceConfig.HudPosition(220, 220),
                WaterSourceConfig.HudAnchor.BOTTOM_RIGHT.position(320, 240, 100, 20, 500, 500));
        assertEquals(
                new WaterSourceConfig.HudPosition(110, 110),
                WaterSourceConfig.HudAnchor.BOTTOM_CENTER.position(320, 240, 100, 20, 0, -102));
    }

    @Test
    void renderSettingsAreImmutableAndColorsArePredecoded() {
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setSourceColor(0x804020);
        config.setOpacityPercent(50);
        config.setOutlineThickness(3);
        config.normalize();
        WaterSourceConfig.RenderSettings settings = config.renderSettings();

        config.setSourceColor(0xFFFFFF);
        config.setOpacityPercent(100);
        config.setOutlineThickness(6);

        assertEquals(128F / 255F, settings.sourceColor().red());
        assertEquals(64F / 255F, settings.sourceColor().green());
        assertEquals(32F / 255F, settings.sourceColor().blue());
        assertEquals(0.5F, settings.baseAlpha());
        assertEquals(3F * 0.018F, settings.outlineBorder());
    }
}
