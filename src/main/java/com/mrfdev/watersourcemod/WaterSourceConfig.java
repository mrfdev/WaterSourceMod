package com.mrfdev.watersourcemod;

/**
 * User-facing settings for the client overlay.
 *
 * <p>This class deliberately contains no Minecraft client references so its
 * migration, validation, profiles, and immutable runtime projections can be
 * tested without starting a game.</p>
 */
public final class WaterSourceConfig {
    public static final int CURRENT_VERSION = 2;
    public static final int MIN_RADIUS = 0;
    public static final int MAX_RADIUS = 8;
    public static final int MIN_MAX_MARKERS = 512;
    public static final int MAX_MAX_MARKERS = 16_384;
    public static final int MIN_VISIBLE_MARKERS = 64;
    public static final int MIN_SCAN_BUDGET = 1_000;
    public static final int MAX_SCAN_BUDGET = 100_000;
    public static final int MIN_SCAN_TIME_MILLIS = 1;
    public static final int MAX_SCAN_TIME_MILLIS = 12;
    public static final int MIN_RESCAN_INTERVAL_TICKS = 20;
    public static final int MAX_RESCAN_INTERVAL_TICKS = 1_200;
    public static final int MIN_RENDER_DISTANCE = 16;
    public static final int MAX_RENDER_DISTANCE = 512;
    public static final int MIN_HUD_OFFSET = -200;
    public static final int MAX_HUD_OFFSET = 200;
    public static final int MIN_HUD_SCALE_PERCENT = 50;
    public static final int MAX_HUD_SCALE_PERCENT = 150;

    /* A zero version identifies JSON written before explicit versioning. */
    private int configVersion;
    private boolean showSources = true;
    private boolean showFlowing = true;
    private boolean includeWaterloggedSources = true;
    private boolean throughWalls;
    private boolean showLabels;
    private boolean showStatusHud = true;
    private boolean pulse = true;
    private boolean holdToShow;

    /* Kept as a serialized migration bridge for Beta 1's single style. */
    private MarkerStyle markerStyle = MarkerStyle.BOX;
    private MarkerStyle sourceMarkerStyle = MarkerStyle.BOX;
    private MarkerStyle flowingMarkerStyle = MarkerStyle.PILLAR;
    private MarkerStyle waterloggedMarkerStyle = MarkerStyle.BEACON;
    private WaterloggedIndicator waterloggedIndicator = WaterloggedIndicator.CAP;
    private FluidLevelVisualization fluidLevelVisualization = FluidLevelVisualization.HEIGHT;
    private NearestMarkerMode nearestMarkerMode = NearestMarkerMode.OFF;
    private ScanBoundaryMode scanBoundaryMode = ScanBoundaryMode.OFF;
    private boolean narrateStatus;
    private boolean diagnosticMode;

    private int chunkRadius = 1;
    private VerticalRange verticalRange = VerticalRange.FULL_HEIGHT;
    private RescanMode rescanMode = RescanMode.AUTOMATIC;
    private int maxMarkers = 4_096;
    private int maxVisibleMarkers = 4_096;
    private int sourceColor = 0xFFD21F;
    private int flowingColor = 0x00D9FF;
    private int outlineColor = 0x101827;
    private int customSourceColor = 0xFFD21F;
    private int customFlowingColor = 0x00D9FF;
    private int customOutlineColor = 0x101827;
    private ColorPalette colorPalette = ColorPalette.DEFAULT;
    private int opacityPercent = 82;
    private int outlineThickness = 2;
    private int maxRenderDistance = 192;
    private int fadeStartPercent = 75;
    private int scanBudgetPerTick = 12_000;
    private int scanTimeBudgetMillis = 3;
    private int rescanIntervalTicks = 80;
    private HudAnchor hudAnchor = HudAnchor.TOP_CENTER;
    private int hudOffsetX;
    private int hudOffsetY;
    private int hudScalePercent = 100;
    private int hudBackgroundOpacityPercent = 69;
    private boolean compactHud;
    private ConfigProfile activeProfile = ConfigProfile.CUSTOM;

    public static WaterSourceConfig defaults() {
        WaterSourceConfig config = new WaterSourceConfig();
        config.configVersion = CURRENT_VERSION;
        config.normalize();
        return config;
    }

    public WaterSourceConfig copy() {
        WaterSourceConfig copy = new WaterSourceConfig();
        copy.configVersion = configVersion;
        copy.showSources = showSources;
        copy.showFlowing = showFlowing;
        copy.includeWaterloggedSources = includeWaterloggedSources;
        copy.throughWalls = throughWalls;
        copy.showLabels = showLabels;
        copy.showStatusHud = showStatusHud;
        copy.pulse = pulse;
        copy.holdToShow = holdToShow;
        copy.markerStyle = markerStyle;
        copy.sourceMarkerStyle = sourceMarkerStyle;
        copy.flowingMarkerStyle = flowingMarkerStyle;
        copy.waterloggedMarkerStyle = waterloggedMarkerStyle;
        copy.waterloggedIndicator = waterloggedIndicator;
        copy.fluidLevelVisualization = fluidLevelVisualization;
        copy.nearestMarkerMode = nearestMarkerMode;
        copy.scanBoundaryMode = scanBoundaryMode;
        copy.narrateStatus = narrateStatus;
        copy.diagnosticMode = diagnosticMode;
        copy.chunkRadius = chunkRadius;
        copy.verticalRange = verticalRange;
        copy.rescanMode = rescanMode;
        copy.maxMarkers = maxMarkers;
        copy.maxVisibleMarkers = maxVisibleMarkers;
        copy.sourceColor = sourceColor;
        copy.flowingColor = flowingColor;
        copy.outlineColor = outlineColor;
        copy.customSourceColor = customSourceColor;
        copy.customFlowingColor = customFlowingColor;
        copy.customOutlineColor = customOutlineColor;
        copy.colorPalette = colorPalette;
        copy.opacityPercent = opacityPercent;
        copy.outlineThickness = outlineThickness;
        copy.maxRenderDistance = maxRenderDistance;
        copy.fadeStartPercent = fadeStartPercent;
        copy.scanBudgetPerTick = scanBudgetPerTick;
        copy.scanTimeBudgetMillis = scanTimeBudgetMillis;
        copy.rescanIntervalTicks = rescanIntervalTicks;
        copy.hudAnchor = hudAnchor;
        copy.hudOffsetX = hudOffsetX;
        copy.hudOffsetY = hudOffsetY;
        copy.hudScalePercent = hudScalePercent;
        copy.hudBackgroundOpacityPercent = hudBackgroundOpacityPercent;
        copy.compactHud = compactHud;
        copy.activeProfile = activeProfile;
        return copy;
    }

    /** Returns settings that determine which markers a completed scan contains. */
    public ScanSettings scanSettings() {
        return new ScanSettings(
                showSources,
                showFlowing,
                includeWaterloggedSources,
                chunkRadius,
                verticalRange,
                maxMarkers);
    }

    /** Creates the immutable, predecoded settings consumed by the renderer. */
    public RenderSettings renderSettings() {
        return new RenderSettings(
                showSources,
                showFlowing,
                includeWaterloggedSources,
                throughWalls,
                pulse,
                sourceMarkerStyle,
                flowingMarkerStyle,
                waterloggedMarkerStyle,
                waterloggedIndicator,
                fluidLevelVisualization,
                nearestMarkerMode,
                scanBoundaryMode,
                maxVisibleMarkers,
                maxRenderDistance,
                fadeStartPercent / 100F,
                RenderColor.fromRgb(sourceColor),
                RenderColor.fromRgb(flowingColor),
                RenderColor.fromRgb(outlineColor),
                opacityPercent / 100F,
                outlineThickness * 0.018F);
    }

    /** Migrates old JSON in place, then clamps every untrusted value. */
    public void normalize() {
        if (configVersion < 2) {
            migrateFromVersion1();
        }

        configVersion = CURRENT_VERSION;
        markerStyle = markerStyle == null ? MarkerStyle.BOX : markerStyle;
        sourceMarkerStyle = sourceMarkerStyle == null ? MarkerStyle.BOX : sourceMarkerStyle;
        markerStyle = sourceMarkerStyle;
        flowingMarkerStyle = flowingMarkerStyle == null ? MarkerStyle.PILLAR : flowingMarkerStyle;
        waterloggedMarkerStyle = waterloggedMarkerStyle == null ? MarkerStyle.BEACON : waterloggedMarkerStyle;
        waterloggedIndicator = waterloggedIndicator == null ? WaterloggedIndicator.CAP : waterloggedIndicator;
        fluidLevelVisualization = fluidLevelVisualization == null
                ? FluidLevelVisualization.HEIGHT
                : fluidLevelVisualization;
        nearestMarkerMode = nearestMarkerMode == null ? NearestMarkerMode.OFF : nearestMarkerMode;
        scanBoundaryMode = scanBoundaryMode == null ? ScanBoundaryMode.OFF : scanBoundaryMode;
        verticalRange = verticalRange == null ? VerticalRange.FULL_HEIGHT : verticalRange;
        rescanMode = rescanMode == null ? RescanMode.AUTOMATIC : rescanMode;
        hudAnchor = hudAnchor == null ? HudAnchor.TOP_CENTER : hudAnchor;
        colorPalette = colorPalette == null ? ColorPalette.CUSTOM : colorPalette;
        activeProfile = activeProfile == null ? ConfigProfile.CUSTOM : activeProfile;

        chunkRadius = clamp(chunkRadius, MIN_RADIUS, MAX_RADIUS);
        maxMarkers = clamp(maxMarkers, MIN_MAX_MARKERS, MAX_MAX_MARKERS);
        maxVisibleMarkers = clamp(maxVisibleMarkers, MIN_VISIBLE_MARKERS, maxMarkers);
        sourceColor &= 0xFFFFFF;
        flowingColor &= 0xFFFFFF;
        outlineColor &= 0xFFFFFF;
        customSourceColor &= 0xFFFFFF;
        customFlowingColor &= 0xFFFFFF;
        customOutlineColor &= 0xFFFFFF;
        if (colorPalette != ColorPalette.CUSTOM
                && !colorPalette.matches(sourceColor, flowingColor, outlineColor)) {
            customSourceColor = sourceColor;
            customFlowingColor = flowingColor;
            customOutlineColor = outlineColor;
            colorPalette = ColorPalette.CUSTOM;
        }
        opacityPercent = clamp(opacityPercent, 10, 100);
        outlineThickness = clamp(outlineThickness, 1, 6);
        maxRenderDistance = clamp(maxRenderDistance, MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE);
        fadeStartPercent = clamp(fadeStartPercent, 0, 100);
        scanBudgetPerTick = clamp(scanBudgetPerTick, MIN_SCAN_BUDGET, MAX_SCAN_BUDGET);
        scanTimeBudgetMillis = clamp(scanTimeBudgetMillis, MIN_SCAN_TIME_MILLIS, MAX_SCAN_TIME_MILLIS);
        rescanIntervalTicks = clamp(
                rescanIntervalTicks,
                MIN_RESCAN_INTERVAL_TICKS,
                MAX_RESCAN_INTERVAL_TICKS);
        hudOffsetX = clamp(hudOffsetX, MIN_HUD_OFFSET, MAX_HUD_OFFSET);
        hudOffsetY = clamp(hudOffsetY, MIN_HUD_OFFSET, MAX_HUD_OFFSET);
        hudScalePercent = clamp(hudScalePercent, MIN_HUD_SCALE_PERCENT, MAX_HUD_SCALE_PERCENT);
        hudBackgroundOpacityPercent = clamp(hudBackgroundOpacityPercent, 0, 100);
    }

    private void migrateFromVersion1() {
        MarkerStyle legacyStyle = markerStyle == null ? MarkerStyle.BOX : markerStyle;
        sourceMarkerStyle = legacyStyle;
        flowingMarkerStyle = legacyStyle;
        waterloggedMarkerStyle = legacyStyle;
        maxVisibleMarkers = maxMarkers;
        customSourceColor = sourceColor;
        customFlowingColor = flowingColor;
        customOutlineColor = outlineColor;
        colorPalette = ColorPalette.CUSTOM;
        activeProfile = ConfigProfile.CUSTOM;
    }

    /** Applies a local preset without enabling through-wall rendering. */
    public void applyProfile(ConfigProfile profile) {
        ConfigProfile selected = profile == null ? ConfigProfile.CUSTOM : profile;
        switch (selected) {
            case ACCESSIBILITY -> {
                applyProfileBaseline();
                showLabels = true;
                chunkRadius = 1;
                maxMarkers = 4_096;
                maxVisibleMarkers = 4_096;
                maxRenderDistance = 192;
                scanBudgetPerTick = 12_000;
                scanTimeBudgetMillis = 3;
                rescanIntervalTicks = 80;
                hudBackgroundOpacityPercent = 75;
                sourceMarkerStyle = MarkerStyle.HOLLOW;
                flowingMarkerStyle = MarkerStyle.STRIPES;
                waterloggedMarkerStyle = MarkerStyle.DOTS;
                waterloggedIndicator = WaterloggedIndicator.CROSS;
                fluidLevelVisualization = FluidLevelVisualization.BOTH;
                nearestMarkerMode = NearestMarkerMode.HUD;
                narrateStatus = true;
            }
            case EXPLORATION -> {
                applyProfileBaseline();
                chunkRadius = 2;
                maxMarkers = 8_192;
                maxVisibleMarkers = 4_096;
                maxRenderDistance = 256;
                fadeStartPercent = 70;
                scanBudgetPerTick = 16_000;
                scanTimeBudgetMillis = 4;
                rescanIntervalTicks = 100;
                fluidLevelVisualization = FluidLevelVisualization.BOTH;
                nearestMarkerMode = NearestMarkerMode.BOTH;
                scanBoundaryMode = ScanBoundaryMode.CURRENT;
            }
            case LOW_PERFORMANCE -> {
                applyProfileBaseline();
                pulse = false;
                chunkRadius = 0;
                verticalRange = VerticalRange.NEARBY_32;
                maxMarkers = 1_024;
                maxVisibleMarkers = 512;
                maxRenderDistance = 96;
                fadeStartPercent = 70;
                scanBudgetPerTick = 4_000;
                scanTimeBudgetMillis = 1;
                rescanIntervalTicks = 160;
                hudScalePercent = 90;
                compactHud = true;
                waterloggedIndicator = WaterloggedIndicator.OFF;
            }
            case CUSTOM -> {
                // CUSTOM names the current values and is intentionally not a reset.
            }
        }
        activeProfile = selected;
        normalize();
    }

    private void applyProfileBaseline() {
        showSources = true;
        showFlowing = true;
        includeWaterloggedSources = true;
        throughWalls = false;
        showLabels = false;
        showStatusHud = true;
        pulse = true;
        holdToShow = false;
        sourceMarkerStyle = MarkerStyle.BOX;
        flowingMarkerStyle = MarkerStyle.PILLAR;
        waterloggedMarkerStyle = MarkerStyle.BEACON;
        waterloggedIndicator = WaterloggedIndicator.CAP;
        fluidLevelVisualization = FluidLevelVisualization.HEIGHT;
        nearestMarkerMode = NearestMarkerMode.OFF;
        scanBoundaryMode = ScanBoundaryMode.OFF;
        narrateStatus = false;
        diagnosticMode = false;
        verticalRange = VerticalRange.FULL_HEIGHT;
        rescanMode = RescanMode.AUTOMATIC;
        fadeStartPercent = 75;
        opacityPercent = 82;
        outlineThickness = 2;
        hudAnchor = HudAnchor.TOP_CENTER;
        hudOffsetX = 0;
        hudOffsetY = 0;
        hudScalePercent = 100;
        hudBackgroundOpacityPercent = 69;
        compactHud = false;
    }

    /** Applies a built-in palette or restores the user's persisted custom palette. */
    public void applyColorPalette(ColorPalette palette) {
        ColorPalette selected = palette == null ? ColorPalette.CUSTOM : palette;
        if (selected == ColorPalette.CUSTOM) {
            setPaletteColors(customSourceColor, customFlowingColor, customOutlineColor);
        } else {
            setPaletteColors(selected.sourceColor, selected.flowingColor, selected.outlineColor);
        }
        colorPalette = selected;
    }

    private void setPaletteColors(int source, int flowing, int outline) {
        sourceColor = source;
        flowingColor = flowing;
        outlineColor = outline;
    }

    private void captureVisiblePaletteForEditing() {
        if (colorPalette != ColorPalette.CUSTOM) {
            customSourceColor = sourceColor;
            customFlowingColor = flowingColor;
            customOutlineColor = outlineColor;
        }
    }

    public void markCustomProfile() {
        activeProfile = ConfigProfile.CUSTOM;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getConfigVersion() { return configVersion; }
    public boolean isShowSources() { return showSources; }
    public void setShowSources(boolean value) { showSources = value; }
    public boolean isShowFlowing() { return showFlowing; }
    public void setShowFlowing(boolean value) { showFlowing = value; }
    public boolean isIncludeWaterloggedSources() { return includeWaterloggedSources; }
    public void setIncludeWaterloggedSources(boolean value) { includeWaterloggedSources = value; }
    public boolean isThroughWalls() { return throughWalls; }
    public void setThroughWalls(boolean value) { throughWalls = value; }
    public boolean isShowLabels() { return showLabels; }
    public void setShowLabels(boolean value) { showLabels = value; }
    public boolean isShowStatusHud() { return showStatusHud; }
    public void setShowStatusHud(boolean value) { showStatusHud = value; }
    public boolean isPulse() { return pulse; }
    public void setPulse(boolean value) { pulse = value; }
    public boolean isHoldToShow() { return holdToShow; }
    public void setHoldToShow(boolean value) { holdToShow = value; }

    /** Compatibility setter that applies one shape to all marker categories. */
    public void setMarkerStyle(MarkerStyle style) {
        MarkerStyle selected = style == null ? MarkerStyle.BOX : style;
        markerStyle = selected;
        sourceMarkerStyle = selected;
        flowingMarkerStyle = selected;
        waterloggedMarkerStyle = selected;
    }

    /** Compatibility getter for Beta 1 callers. */
    public MarkerStyle getMarkerStyle() { return sourceMarkerStyle; }
    public MarkerStyle getSourceMarkerStyle() { return sourceMarkerStyle; }
    public void setSourceMarkerStyle(MarkerStyle value) { sourceMarkerStyle = value; markerStyle = value; }
    public MarkerStyle getFlowingMarkerStyle() { return flowingMarkerStyle; }
    public void setFlowingMarkerStyle(MarkerStyle value) { flowingMarkerStyle = value; }
    public MarkerStyle getWaterloggedMarkerStyle() { return waterloggedMarkerStyle; }
    public void setWaterloggedMarkerStyle(MarkerStyle value) { waterloggedMarkerStyle = value; }
    public WaterloggedIndicator getWaterloggedIndicator() { return waterloggedIndicator; }
    public void setWaterloggedIndicator(WaterloggedIndicator value) { waterloggedIndicator = value; }
    public FluidLevelVisualization getFluidLevelVisualization() { return fluidLevelVisualization; }
    public void setFluidLevelVisualization(FluidLevelVisualization value) { fluidLevelVisualization = value; }
    public NearestMarkerMode getNearestMarkerMode() { return nearestMarkerMode; }
    public void setNearestMarkerMode(NearestMarkerMode value) { nearestMarkerMode = value; }
    public ScanBoundaryMode getScanBoundaryMode() { return scanBoundaryMode; }
    public void setScanBoundaryMode(ScanBoundaryMode value) { scanBoundaryMode = value; }
    public boolean isNarrateStatus() { return narrateStatus; }
    public void setNarrateStatus(boolean value) { narrateStatus = value; }
    public boolean isDiagnosticMode() { return diagnosticMode; }
    public void setDiagnosticMode(boolean value) { diagnosticMode = value; }
    public int getChunkRadius() { return chunkRadius; }
    public void setChunkRadius(int value) { chunkRadius = value; }
    public VerticalRange getVerticalRange() { return verticalRange; }
    public void setVerticalRange(VerticalRange value) { verticalRange = value; }
    public RescanMode getRescanMode() { return rescanMode; }
    public void setRescanMode(RescanMode value) { rescanMode = value; }
    public int getMaxMarkers() { return maxMarkers; }
    public void setMaxMarkers(int value) { maxMarkers = value; }
    public int getMaxVisibleMarkers() { return maxVisibleMarkers; }
    public void setMaxVisibleMarkers(int value) { maxVisibleMarkers = value; }
    public int getSourceColor() { return sourceColor; }
    public void setSourceColor(int value) {
        captureVisiblePaletteForEditing();
        sourceColor = value & 0xFFFFFF;
        customSourceColor = sourceColor;
        colorPalette = ColorPalette.CUSTOM;
    }
    public int getFlowingColor() { return flowingColor; }
    public void setFlowingColor(int value) {
        captureVisiblePaletteForEditing();
        flowingColor = value & 0xFFFFFF;
        customFlowingColor = flowingColor;
        colorPalette = ColorPalette.CUSTOM;
    }
    public int getOutlineColor() { return outlineColor; }
    public void setOutlineColor(int value) {
        captureVisiblePaletteForEditing();
        outlineColor = value & 0xFFFFFF;
        customOutlineColor = outlineColor;
        colorPalette = ColorPalette.CUSTOM;
    }
    public ColorPalette getColorPalette() { return colorPalette; }
    public int getOpacityPercent() { return opacityPercent; }
    public void setOpacityPercent(int value) { opacityPercent = value; }
    public int getOutlineThickness() { return outlineThickness; }
    public void setOutlineThickness(int value) { outlineThickness = value; }
    public int getMaxRenderDistance() { return maxRenderDistance; }
    public void setMaxRenderDistance(int value) { maxRenderDistance = value; }
    public int getFadeStartPercent() { return fadeStartPercent; }
    public void setFadeStartPercent(int value) { fadeStartPercent = value; }
    public int getScanBudgetPerTick() { return scanBudgetPerTick; }
    public void setScanBudgetPerTick(int value) { scanBudgetPerTick = value; }
    public int getScanTimeBudgetMillis() { return scanTimeBudgetMillis; }
    public void setScanTimeBudgetMillis(int value) { scanTimeBudgetMillis = value; }
    public int getRescanIntervalTicks() { return rescanIntervalTicks; }
    public void setRescanIntervalTicks(int value) { rescanIntervalTicks = value; }
    public HudAnchor getHudAnchor() { return hudAnchor; }
    public void setHudAnchor(HudAnchor value) { hudAnchor = value; }
    public int getHudOffsetX() { return hudOffsetX; }
    public void setHudOffsetX(int value) { hudOffsetX = value; }
    public int getHudOffsetY() { return hudOffsetY; }
    public void setHudOffsetY(int value) { hudOffsetY = value; }
    public int getHudScalePercent() { return hudScalePercent; }
    public void setHudScalePercent(int value) { hudScalePercent = value; }
    public int getHudBackgroundOpacityPercent() { return hudBackgroundOpacityPercent; }
    public void setHudBackgroundOpacityPercent(int value) { hudBackgroundOpacityPercent = value; }
    public boolean isCompactHud() { return compactHud; }
    public void setCompactHud(boolean value) { compactHud = value; }
    public ConfigProfile getActiveProfile() { return activeProfile; }

    public enum MarkerStyle {
        BOX("box"),
        PILLAR("pillar"),
        BEACON("beacon"),
        HOLLOW("hollow"),
        STRIPES("stripes"),
        DOTS("dots");
        private final String key;
        MarkerStyle(String key) { this.key = key; }
        public String key() { return key; }
    }

    public enum WaterloggedIndicator {
        OFF("off"), CAP("cap"), CROSS("cross");
        private final String key;
        WaterloggedIndicator(String key) { this.key = key; }
        public String key() { return key; }
    }

    public enum FluidLevelVisualization {
        OFF("off", false, false),
        HEIGHT("height", true, false),
        GAUGE("gauge", false, true),
        BOTH("both", true, true);

        private final String key;
        private final boolean height;
        private final boolean gauge;

        FluidLevelVisualization(String key, boolean height, boolean gauge) {
            this.key = key;
            this.height = height;
            this.gauge = gauge;
        }

        public String key() { return key; }
        public boolean usesHeight() { return height; }
        public boolean usesGauge() { return gauge; }
    }

    public enum NearestMarkerMode {
        OFF("off", false, false),
        HUD("hud", true, false),
        WORLD("world", false, true),
        BOTH("both", true, true);

        private final String key;
        private final boolean hud;
        private final boolean world;

        NearestMarkerMode(String key, boolean hud, boolean world) {
            this.key = key;
            this.hud = hud;
            this.world = world;
        }

        public String key() { return key; }
        public boolean showsHud() { return hud; }
        public boolean showsWorld() { return world; }
    }

    public enum ScanBoundaryMode {
        OFF("off"), CURRENT("current"), ALL("all");
        private final String key;
        ScanBoundaryMode(String key) { this.key = key; }
        public String key() { return key; }
    }

    public enum RescanMode {
        AUTOMATIC("automatic"), BLOCK_UPDATES("block_updates"), MANUAL("manual");
        private final String key;
        RescanMode(String key) { this.key = key; }
        public String key() { return key; }
    }

    public enum VerticalRange {
        CURRENT_Y("current_y", 0),
        NEARBY_16("nearby_16", 16),
        NEARBY_32("nearby_32", 32),
        NEARBY_64("nearby_64", 64),
        FULL_HEIGHT("full_height", -1);

        private final String key;
        private final int radius;
        VerticalRange(String key, int radius) { this.key = key; this.radius = radius; }
        public String key() { return key; }

        public ScanHeight bounds(int playerY, int worldMinY, int worldMaxY) {
            if (this == FULL_HEIGHT) {
                return new ScanHeight(worldMinY, Math.max(worldMinY, worldMaxY));
            }
            int minimum = Math.max(worldMinY, playerY - radius);
            int maximum = Math.min(worldMaxY, playerY + radius + 1);
            return new ScanHeight(minimum, Math.max(minimum, maximum));
        }

        /** Keeps nearby bands stable while the player makes small Y movements. */
        public boolean shouldRecenter(int playerY, int previousCenterY) {
            if (this == FULL_HEIGHT) {
                return false;
            }
            if (this == CURRENT_Y) {
                return playerY != previousCenterY;
            }
            return Math.abs((long) playerY - previousCenterY) > Math.max(1, radius / 2);
        }
    }

    public enum HudAnchor {
        TOP_LEFT("top_left", Horizontal.LEFT, Vertical.TOP),
        TOP_CENTER("top_center", Horizontal.CENTER, Vertical.TOP),
        TOP_RIGHT("top_right", Horizontal.RIGHT, Vertical.TOP),
        BOTTOM_LEFT("bottom_left", Horizontal.LEFT, Vertical.BOTTOM),
        BOTTOM_CENTER("bottom_center", Horizontal.CENTER, Vertical.BOTTOM),
        BOTTOM_RIGHT("bottom_right", Horizontal.RIGHT, Vertical.BOTTOM);

        private final String key;
        private final Horizontal horizontal;
        private final Vertical vertical;
        HudAnchor(String key, Horizontal horizontal, Vertical vertical) {
            this.key = key;
            this.horizontal = horizontal;
            this.vertical = vertical;
        }
        public String key() { return key; }

        public HudPosition position(
                int screenWidth,
                int screenHeight,
                int panelWidth,
                int panelHeight,
                int offsetX,
                int offsetY) {
            int margin = 8;
            int x = switch (horizontal) {
                case LEFT -> margin;
                case CENTER -> (screenWidth - panelWidth) / 2;
                case RIGHT -> screenWidth - panelWidth - margin;
            };
            int y = vertical == Vertical.TOP ? margin : screenHeight - panelHeight - margin;
            x = clamp(x + offsetX, 0, Math.max(0, screenWidth - panelWidth));
            y = clamp(y + offsetY, 0, Math.max(0, screenHeight - panelHeight));
            return new HudPosition(x, y);
        }

        private enum Horizontal { LEFT, CENTER, RIGHT }
        private enum Vertical { TOP, BOTTOM }
    }

    public enum ColorPalette {
        DEFAULT("default", 0xFFD21F, 0x00D9FF, 0x101827),
        COLOR_BLIND_SAFE("color_blind_safe", 0xF0E442, 0x0072B2, 0x111111),
        HIGH_CONTRAST("high_contrast", 0xFFFFFF, 0xFF4FD8, 0x000000),
        MONOCHROME("monochrome", 0xFFFFFF, 0x8F9BA8, 0x050505),
        CUSTOM("custom", -1, -1, -1);

        private final String key;
        private final int sourceColor;
        private final int flowingColor;
        private final int outlineColor;

        ColorPalette(String key, int sourceColor, int flowingColor, int outlineColor) {
            this.key = key;
            this.sourceColor = sourceColor;
            this.flowingColor = flowingColor;
            this.outlineColor = outlineColor;
        }

        public String key() { return key; }

        private boolean matches(int source, int flowing, int outline) {
            return sourceColor == source && flowingColor == flowing && outlineColor == outline;
        }
    }

    public enum ConfigProfile {
        ACCESSIBILITY("accessibility"),
        EXPLORATION("exploration"),
        LOW_PERFORMANCE("low_performance"),
        CUSTOM("custom");
        private final String key;
        ConfigProfile(String key) { this.key = key; }
        public String key() { return key; }
    }

    public record ScanHeight(int minY, int maxY) { }
    public record HudPosition(int x, int y) { }

    public record ScanSettings(
            boolean showSources,
            boolean showFlowing,
            boolean includeWaterloggedSources,
            int chunkRadius,
            VerticalRange verticalRange,
            int maxMarkers) { }

    /** Immutable renderer input derived once whenever configuration is published. */
    public record RenderSettings(
            boolean showSources,
            boolean showFlowing,
            boolean includeWaterloggedSources,
            boolean throughWalls,
            boolean pulse,
            MarkerStyle sourceMarkerStyle,
            MarkerStyle flowingMarkerStyle,
            MarkerStyle waterloggedMarkerStyle,
            WaterloggedIndicator waterloggedIndicator,
            FluidLevelVisualization fluidLevelVisualization,
            NearestMarkerMode nearestMarkerMode,
            ScanBoundaryMode scanBoundaryMode,
            int maxVisibleMarkers,
            int maxRenderDistance,
            float fadeStartFraction,
            RenderColor sourceColor,
            RenderColor flowingColor,
            RenderColor outlineColor,
            float baseAlpha,
            float outlineBorder) {
        public boolean hasEnabledMarkers() { return showSources || showFlowing; }
        public boolean hasRenderableGeometry() {
            return hasEnabledMarkers() || scanBoundaryMode != ScanBoundaryMode.OFF;
        }
        public boolean isMarkerEnabled(WaterMarker marker) {
            return (marker.source() ? showSources : showFlowing)
                    && (!marker.waterlogged() || includeWaterloggedSources);
        }
        public RenderColor colorFor(WaterMarker marker) {
            return marker.source() ? sourceColor : flowingColor;
        }
        public MarkerStyle styleFor(WaterMarker marker) {
            if (marker.waterlogged()) {
                return waterloggedMarkerStyle;
            }
            return marker.source() ? sourceMarkerStyle : flowingMarkerStyle;
        }
    }

    /** Normalized RGB components ready for direct vertex emission. */
    public record RenderColor(float red, float green, float blue) {
        private static RenderColor fromRgb(int rgb) {
            return new RenderColor(
                    ((rgb >> 16) & 0xFF) / 255F,
                    ((rgb >> 8) & 0xFF) / 255F,
                    (rgb & 0xFF) / 255F);
        }
    }
}
