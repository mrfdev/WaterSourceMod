package com.mrfdev.watersourcemod;

/**
 * User-facing settings for the client overlay.
 *
 * <p>This class deliberately contains no Minecraft client references so the
 * validation and copy behavior can be tested without starting a game.</p>
 */
public final class WaterSourceConfig {
    public static final int CURRENT_VERSION = 1;
    public static final int MIN_RADIUS = 1;
    public static final int MAX_RADIUS = 8;
    public static final int MIN_MAX_MARKERS = 128;
    public static final int MAX_MAX_MARKERS = 16_384;

    private int configVersion = CURRENT_VERSION;
    private boolean showSources = true;
    private boolean showFlowing = true;
    private boolean includeWaterloggedSources = true;
    private boolean throughWalls = false;
    private boolean showLabels = false;
    private boolean showStatusHud = true;
    private boolean pulse = true;
    private MarkerStyle markerStyle = MarkerStyle.BOX;
    private int chunkRadius = 1;
    private int maxMarkers = 4_096;
    private int sourceColor = 0xFFD21F;
    private int flowingColor = 0x00D9FF;
    private int outlineColor = 0x101827;
    private int opacityPercent = 82;
    private int outlineThickness = 2;
    private int scanBudgetPerTick = 12_000;
    private int rescanIntervalTicks = 80;

    public static WaterSourceConfig defaults() {
        WaterSourceConfig config = new WaterSourceConfig();
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
        copy.markerStyle = markerStyle;
        copy.chunkRadius = chunkRadius;
        copy.maxMarkers = maxMarkers;
        copy.sourceColor = sourceColor;
        copy.flowingColor = flowingColor;
        copy.outlineColor = outlineColor;
        copy.opacityPercent = opacityPercent;
        copy.outlineThickness = outlineThickness;
        copy.scanBudgetPerTick = scanBudgetPerTick;
        copy.rescanIntervalTicks = rescanIntervalTicks;
        return copy;
    }

    public void normalize() {
        configVersion = CURRENT_VERSION;
        markerStyle = markerStyle == null ? MarkerStyle.BOX : markerStyle;
        chunkRadius = clamp(chunkRadius, MIN_RADIUS, MAX_RADIUS);
        maxMarkers = clamp(maxMarkers, MIN_MAX_MARKERS, MAX_MAX_MARKERS);
        sourceColor = sourceColor & 0xFFFFFF;
        flowingColor = flowingColor & 0xFFFFFF;
        outlineColor = outlineColor & 0xFFFFFF;
        opacityPercent = clamp(opacityPercent, 10, 100);
        outlineThickness = clamp(outlineThickness, 1, 6);
        scanBudgetPerTick = clamp(scanBudgetPerTick, 1_000, 100_000);
        rescanIntervalTicks = clamp(rescanIntervalTicks, 20, 1_200);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean isShowSources() {
        return showSources;
    }

    public void setShowSources(boolean showSources) {
        this.showSources = showSources;
    }

    public boolean isShowFlowing() {
        return showFlowing;
    }

    public void setShowFlowing(boolean showFlowing) {
        this.showFlowing = showFlowing;
    }

    public boolean isIncludeWaterloggedSources() {
        return includeWaterloggedSources;
    }

    public void setIncludeWaterloggedSources(boolean includeWaterloggedSources) {
        this.includeWaterloggedSources = includeWaterloggedSources;
    }

    public boolean isThroughWalls() {
        return throughWalls;
    }

    public void setThroughWalls(boolean throughWalls) {
        this.throughWalls = throughWalls;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    public boolean isShowStatusHud() {
        return showStatusHud;
    }

    public void setShowStatusHud(boolean showStatusHud) {
        this.showStatusHud = showStatusHud;
    }

    public boolean isPulse() {
        return pulse;
    }

    public void setPulse(boolean pulse) {
        this.pulse = pulse;
    }

    public MarkerStyle getMarkerStyle() {
        return markerStyle;
    }

    public void setMarkerStyle(MarkerStyle markerStyle) {
        this.markerStyle = markerStyle;
    }

    public int getChunkRadius() {
        return chunkRadius;
    }

    public void setChunkRadius(int chunkRadius) {
        this.chunkRadius = chunkRadius;
    }

    public int getMaxMarkers() {
        return maxMarkers;
    }

    public void setMaxMarkers(int maxMarkers) {
        this.maxMarkers = maxMarkers;
    }

    public int getSourceColor() {
        return sourceColor;
    }

    public void setSourceColor(int sourceColor) {
        this.sourceColor = sourceColor;
    }

    public int getFlowingColor() {
        return flowingColor;
    }

    public void setFlowingColor(int flowingColor) {
        this.flowingColor = flowingColor;
    }

    public int getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(int outlineColor) {
        this.outlineColor = outlineColor;
    }

    public int getOpacityPercent() {
        return opacityPercent;
    }

    public void setOpacityPercent(int opacityPercent) {
        this.opacityPercent = opacityPercent;
    }

    public int getOutlineThickness() {
        return outlineThickness;
    }

    public void setOutlineThickness(int outlineThickness) {
        this.outlineThickness = outlineThickness;
    }

    public int getScanBudgetPerTick() {
        return scanBudgetPerTick;
    }

    public int getRescanIntervalTicks() {
        return rescanIntervalTicks;
    }

    public enum MarkerStyle {
        BOX("box"),
        PILLAR("pillar"),
        BEACON("beacon");

        private final String key;

        MarkerStyle(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
