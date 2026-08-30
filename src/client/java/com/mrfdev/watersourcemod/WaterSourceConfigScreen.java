package com.mrfdev.watersourcemod;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Paged, vanilla-only settings screen, also used by the optional Mod Menu adapter. */
public final class WaterSourceConfigScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int COLUMN_GAP = 8;
    private static final int CONTENT_TOP = 64;
    private static final int KEY_COLOR = 0xD8D8D8;
    private static final int VALUE_COLOR = 0xFFFFFF;
    private static final int ENABLED_COLOR = 0x55FF55;
    private static final int DISABLED_COLOR = 0xFF7777;
    private static final List<Integer> SCAN_BUDGETS = List.of(
            1_000, 2_000, 4_000, 8_000, 12_000, 16_000, 24_000, 32_000, 50_000, 75_000, 100_000);
    private static final List<Integer> DISCOVERED_LIMITS = List.of(512, 1_024, 2_048, 4_096, 8_192, 16_384);
    private static final List<Integer> VISIBLE_LIMITS = List.of(64, 128, 256, 512, 1_024, 2_048, 4_096, 8_192, 16_384);

    private final Screen parent;
    private final List<ColorSwatch> colorSwatches = new ArrayList<>();
    private Page page;
    private Component notice = CommonComponents.EMPTY;
    private int columnWidth;
    private int left;
    private int right;

    public WaterSourceConfigScreen(Screen parent) {
        this(parent, Page.SCANNING);
    }

    private WaterSourceConfigScreen(Screen parent, Page page) {
        super(Component.translatable("screen.watersourcemod.settings"));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        clearWidgets();
        colorSwatches.clear();
        columnWidth = Math.min(210, Math.max(120, (width - 24 - COLUMN_GAP) / 2));
        left = width / 2 - columnWidth - COLUMN_GAP / 2;
        right = width / 2 + COLUMN_GAP / 2;

        addPageTabs();
        WaterSourceConfig config = WaterSourceModClient.config();
        switch (page) {
            case SCANNING -> addScanningPage(config);
            case MARKERS -> addMarkersPage(config);
            case FEATURES -> addFeaturesPage(config);
            case HUD -> addHudPage(config);
            case PROFILES -> addProfilesPage(config);
        }
        addFooter();
    }

    private void addPageTabs() {
        int gap = 4;
        int availableWidth = width - 20 - gap * (Page.values().length - 1);
        int tabWidth = Math.min(104, Math.max(40, availableWidth / Page.values().length));
        int totalWidth = tabWidth * Page.values().length + gap * (Page.values().length - 1);
        int x = (width - totalWidth) / 2;
        for (Page candidate : Page.values()) {
            Button button = Button.builder(
                            Component.translatable("watersourcemod.page." + candidate.key),
                            ignored -> {
                                page = candidate;
                                notice = CommonComponents.EMPTY;
                                init(width, height);
                            })
                    .bounds(x, 36, tabWidth, BUTTON_HEIGHT)
                    .build();
            button.active = candidate != page;
            addRenderableWidget(button);
            x += tabWidth + gap;
        }
    }

    private void addScanningPage(WaterSourceConfig config) {
        addBoolean(left, row(0), "show_sources", config.isShowSources(),
                value -> update(c -> c.setShowSources(value)));
        addBoolean(right, row(0), "show_flowing", config.isShowFlowing(),
                value -> update(c -> c.setShowFlowing(value)));
        addBoolean(left, row(1), "include_waterlogged", config.isIncludeWaterloggedSources(),
                value -> update(c -> c.setIncludeWaterloggedSources(value)));
        addBoolean(right, row(1), "hold_to_show", config.isHoldToShow(),
                value -> update(c -> c.setHoldToShow(value)));

        addIntegerCycle(left, row(2), "chunk_radius", config.getChunkRadius(),
                integerValues(WaterSourceConfig.MIN_RADIUS, WaterSourceConfig.MAX_RADIUS),
                WaterSourceConfigScreen::radiusValue,
                value -> update(c -> c.setChunkRadius(value)));
        addEnum(right, row(2), "vertical_range", config.getVerticalRange(),
                Arrays.asList(WaterSourceConfig.VerticalRange.values()),
                value -> enumValue("vertical_range", value.key()),
                value -> update(c -> c.setVerticalRange(value)));

        addEnum(left, row(3), "rescan_mode", config.getRescanMode(),
                Arrays.asList(WaterSourceConfig.RescanMode.values()),
                value -> enumValue("rescan_mode", value.key()),
                value -> update(c -> c.setRescanMode(value)));
        addSlider(right, row(3), "rescan_interval", config.getRescanIntervalTicks() / 20,
                1, 60,
                value -> Component.translatable("watersourcemod.config.rescan_interval.value", value),
                value -> update(c -> c.setRescanIntervalTicks(value * 20)));

        addIntegerCycle(left, row(4), "scan_budget", config.getScanBudgetPerTick(),
                withCurrent(SCAN_BUDGETS, config.getScanBudgetPerTick()),
                value -> Component.translatable("watersourcemod.config.scan_budget.value", value),
                value -> update(c -> c.setScanBudgetPerTick(value)));
        addSlider(right, row(4), "scan_time_budget", config.getScanTimeBudgetMillis(),
                WaterSourceConfig.MIN_SCAN_TIME_MILLIS,
                WaterSourceConfig.MAX_SCAN_TIME_MILLIS,
                value -> Component.translatable("watersourcemod.config.scan_time_budget.value", value),
                value -> update(c -> c.setScanTimeBudgetMillis(value)));

        addIntegerCycle(left, row(5), "max_discovered_markers", config.getMaxMarkers(),
                withCurrent(DISCOVERED_LIMITS, config.getMaxMarkers()),
                WaterSourceConfigScreen::integerValue,
                value -> {
                    update(c -> c.setMaxMarkers(value));
                    init(width, height);
                });
        addIntegerCycle(right, row(5), "max_visible_markers", config.getMaxVisibleMarkers(),
                withCurrent(
                        VISIBLE_LIMITS.stream().filter(value -> value <= config.getMaxMarkers()).toList(),
                        config.getMaxVisibleMarkers()),
                WaterSourceConfigScreen::integerValue,
                value -> update(c -> c.setMaxVisibleMarkers(value)));
    }

    private void addMarkersPage(WaterSourceConfig config) {
        addMarkerStyle(left, row(0), "source_marker_style", config.getSourceMarkerStyle(),
                value -> update(c -> c.setSourceMarkerStyle(value)));
        addMarkerStyle(right, row(0), "flowing_marker_style", config.getFlowingMarkerStyle(),
                value -> update(c -> c.setFlowingMarkerStyle(value)));
        addMarkerStyle(left, row(1), "waterlogged_marker_style", config.getWaterloggedMarkerStyle(),
                value -> update(c -> c.setWaterloggedMarkerStyle(value)));
        addEnum(right, row(1), "color_palette", config.getColorPalette(),
                Arrays.asList(WaterSourceConfig.ColorPalette.values()),
                value -> enumValue("palette", value.key()),
                value -> {
                    update(c -> c.applyColorPalette(value));
                    init(width, height);
                });

        addColor(left, row(2), "source_color", config.getSourceColor(), ColorTarget.SOURCE);
        addColor(right, row(2), "flowing_color", config.getFlowingColor(), ColorTarget.FLOWING);
        addColor(left, row(3), "outline_color", config.getOutlineColor(), ColorTarget.OUTLINE);

        int pairedWidth = (columnWidth - 4) / 2;
        addBoolean(right, row(3), pairedWidth, "through_walls", config.isThroughWalls(),
                value -> update(c -> c.setThroughWalls(value)));
        addBoolean(right + pairedWidth + 4, row(3), pairedWidth, "pulse", config.isPulse(),
                value -> update(c -> c.setPulse(value)));

        addSlider(left, row(4), "opacity", config.getOpacityPercent(), 10, 100,
                value -> Component.translatable("watersourcemod.config.percent.value", value),
                value -> update(c -> c.setOpacityPercent(value)));
        addSlider(right, row(4), "outline_thickness", config.getOutlineThickness(), 1, 6,
                WaterSourceConfigScreen::integerValue,
                value -> update(c -> c.setOutlineThickness(value)));
        addSlider(left, row(5), "max_render_distance", config.getMaxRenderDistance(),
                WaterSourceConfig.MIN_RENDER_DISTANCE,
                WaterSourceConfig.MAX_RENDER_DISTANCE,
                value -> Component.translatable("watersourcemod.config.blocks.value", value),
                value -> update(c -> c.setMaxRenderDistance(value)));
        addSlider(right, row(5), "fade_start", config.getFadeStartPercent(), 0, 100,
                value -> Component.translatable("watersourcemod.config.percent.value", value),
                value -> update(c -> c.setFadeStartPercent(value)));
    }

    private void addHudPage(WaterSourceConfig config) {
        addBoolean(left, row(0), "show_status_hud", config.isShowStatusHud(),
                value -> update(c -> c.setShowStatusHud(value)));
        addBoolean(right, row(0), "show_labels", config.isShowLabels(),
                value -> update(c -> c.setShowLabels(value)));
        addEnum(left, row(1), "hud_anchor", config.getHudAnchor(),
                Arrays.asList(WaterSourceConfig.HudAnchor.values()),
                value -> enumValue("hud_anchor", value.key()),
                value -> update(c -> c.setHudAnchor(value)));
        addBoolean(right, row(1), "compact_hud", config.isCompactHud(),
                value -> update(c -> c.setCompactHud(value)));
        addSlider(left, row(2), "hud_offset_x", config.getHudOffsetX(),
                WaterSourceConfig.MIN_HUD_OFFSET, WaterSourceConfig.MAX_HUD_OFFSET,
                WaterSourceConfigScreen::signedIntegerValue,
                value -> update(c -> c.setHudOffsetX(value)));
        addSlider(right, row(2), "hud_scale", config.getHudScalePercent(),
                WaterSourceConfig.MIN_HUD_SCALE_PERCENT, WaterSourceConfig.MAX_HUD_SCALE_PERCENT,
                value -> Component.translatable("watersourcemod.config.percent.value", value),
                value -> update(c -> c.setHudScalePercent(value)));
        addSlider(left, row(3), "hud_offset_y", config.getHudOffsetY(),
                WaterSourceConfig.MIN_HUD_OFFSET, WaterSourceConfig.MAX_HUD_OFFSET,
                WaterSourceConfigScreen::signedIntegerValue,
                value -> update(c -> c.setHudOffsetY(value)));
        addSlider(right, row(3), "hud_background_opacity", config.getHudBackgroundOpacityPercent(),
                0, 100,
                value -> Component.translatable("watersourcemod.config.percent.value", value),
                value -> update(c -> c.setHudBackgroundOpacityPercent(value)));
    }

    private void addFeaturesPage(WaterSourceConfig config) {
        addEnum(left, row(0), "waterlogged_indicator", config.getWaterloggedIndicator(),
                Arrays.asList(WaterSourceConfig.WaterloggedIndicator.values()),
                value -> enumValue("waterlogged_indicator", value.key()),
                value -> update(c -> c.setWaterloggedIndicator(value)));
        addEnum(right, row(0), "fluid_level_visualization", config.getFluidLevelVisualization(),
                Arrays.asList(WaterSourceConfig.FluidLevelVisualization.values()),
                value -> enumValue("fluid_level", value.key()),
                value -> update(c -> c.setFluidLevelVisualization(value)));

        addEnum(left, row(1), "nearest_marker_mode", config.getNearestMarkerMode(),
                Arrays.asList(WaterSourceConfig.NearestMarkerMode.values()),
                value -> enumValue("nearest_marker", value.key()),
                value -> update(c -> c.setNearestMarkerMode(value)));
        addEnum(right, row(1), "scan_boundary_mode", config.getScanBoundaryMode(),
                Arrays.asList(WaterSourceConfig.ScanBoundaryMode.values()),
                value -> enumValue("scan_boundary", value.key()),
                value -> update(c -> c.setScanBoundaryMode(value)));

        addBoolean(left, row(2), "narrate_status", config.isNarrateStatus(),
                value -> update(c -> c.setNarrateStatus(value)));
        addBoolean(right, row(2), "diagnostic_mode", config.isDiagnosticMode(),
                value -> update(c -> c.setDiagnosticMode(value)));

        addAction(left, row(3), "preset_accessible_patterns", () -> {
            update(c -> {
                c.setSourceMarkerStyle(WaterSourceConfig.MarkerStyle.HOLLOW);
                c.setFlowingMarkerStyle(WaterSourceConfig.MarkerStyle.STRIPES);
                c.setWaterloggedMarkerStyle(WaterSourceConfig.MarkerStyle.DOTS);
                c.setWaterloggedIndicator(WaterSourceConfig.WaterloggedIndicator.CROSS);
                c.setFluidLevelVisualization(WaterSourceConfig.FluidLevelVisualization.BOTH);
            });
            notice = Component.translatable("watersourcemod.notice.patterns_applied");
            init(width, height);
        });
        addAction(right, row(3), "preset_classic_patterns", () -> {
            update(c -> {
                c.setSourceMarkerStyle(WaterSourceConfig.MarkerStyle.BOX);
                c.setFlowingMarkerStyle(WaterSourceConfig.MarkerStyle.PILLAR);
                c.setWaterloggedMarkerStyle(WaterSourceConfig.MarkerStyle.BEACON);
                c.setWaterloggedIndicator(WaterSourceConfig.WaterloggedIndicator.CAP);
                c.setFluidLevelVisualization(WaterSourceConfig.FluidLevelVisualization.HEIGHT);
            });
            notice = Component.translatable("watersourcemod.notice.patterns_applied");
            init(width, height);
        });
    }

    private void addProfilesPage(WaterSourceConfig config) {
        addEnum(left, row(0), "profile", config.getActiveProfile(),
                Arrays.asList(WaterSourceConfig.ConfigProfile.values()),
                value -> enumValue("profile", value.key()),
                value -> {
                    WaterSourceModClient.applyProfile(value);
                    notice = Component.translatable("watersourcemod.notice.profile_applied");
                    init(width, height);
                });

        addAction(left, row(1), "preset_current_chunk", () -> {
            update(c -> c.setChunkRadius(0));
            notice = Component.translatable("watersourcemod.notice.area_applied");
            init(width, height);
        });
        addAction(left, row(2), "preset_nearby_chunks", () -> {
            update(c -> c.setChunkRadius(1));
            notice = Component.translatable("watersourcemod.notice.area_applied");
            init(width, height);
        });
        addAction(left, row(3), "preset_wide_chunks", () -> {
            update(c -> c.setChunkRadius(2));
            notice = Component.translatable("watersourcemod.notice.area_applied");
            init(width, height);
        });

        addAction(right, row(0), "export", () -> notice = Component.translatable(
                WaterSourceModClient.exportConfig()
                        ? "watersourcemod.notice.exported"
                        : "watersourcemod.notice.export_failed"));
        addAction(right, row(1), "import", () -> {
            boolean imported = WaterSourceModClient.importConfig();
            notice = Component.translatable(imported
                    ? "watersourcemod.notice.imported"
                    : "watersourcemod.notice.import_failed");
            if (imported) {
                init(width, height);
            }
        });
        addAction(right, row(2), "rescan_now", () -> {
            WaterSourceModClient.scanner().requestFullScan();
            notice = Component.translatable("watersourcemod.notice.rescan_requested");
        });
    }

    private void addFooter() {
        int footerY = Math.max(0, height - 28);
        int footerWidth = Math.min(140, Math.max(100, columnWidth - 20));
        addRenderableWidget(Button.builder(
                        Component.translatable("watersourcemod.config.reset"),
                        button -> {
                            WaterSourceModClient.resetConfig();
                            notice = Component.translatable("watersourcemod.notice.reset");
                            init(width, height);
                        })
                .bounds(left, footerY, footerWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("watersourcemod.config.reset.tooltip")))
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> onClose())
                .bounds(right + columnWidth - footerWidth, footerY, footerWidth, BUTTON_HEIGHT)
                .build());
    }

    private void addBoolean(int x, int y, String key, boolean current, Consumer<Boolean> update) {
        addBoolean(x, y, columnWidth, key, current, update);
    }

    private void addBoolean(int x, int y, int widgetWidth, String key, boolean current, Consumer<Boolean> update) {
        CycleButton<Boolean> button = CycleButton.booleanBuilder(
                        stateValue(true),
                        stateValue(false),
                        current)
                .create(x, y, widgetWidth, BUTTON_HEIGHT, optionName(key),
                        (ignored, value) -> update.accept(value));
        button.setTooltip(tooltip(key));
        addRenderableWidget(button);
    }

    private void addMarkerStyle(
            int x,
            int y,
            String key,
            WaterSourceConfig.MarkerStyle current,
            Consumer<WaterSourceConfig.MarkerStyle> update) {
        addEnum(x, y, key, current,
                Arrays.asList(WaterSourceConfig.MarkerStyle.values()),
                style -> Component.translatable("watersourcemod.style." + style.key()).withColor(VALUE_COLOR),
                update);
    }

    private <T> void addEnum(
            int x,
            int y,
            String key,
            T current,
            List<T> values,
            Function<T, Component> valueLabel,
            Consumer<T> update) {
        CycleButton<T> button = CycleButton.<T>builder(valueLabel, current)
                .withValues(values)
                .create(x, y, columnWidth, BUTTON_HEIGHT, optionName(key),
                        (ignored, value) -> update.accept(value));
        button.setTooltip(tooltip(key));
        addRenderableWidget(button);
    }

    private void addIntegerCycle(
            int x,
            int y,
            String key,
            int current,
            List<Integer> values,
            Function<Integer, Component> valueLabel,
            Consumer<Integer> update) {
        addEnum(x, y, key, current, values, valueLabel, update);
    }

    private void addSlider(
            int x,
            int y,
            String key,
            int current,
            int minimum,
            int maximum,
            Function<Integer, Component> valueLabel,
            OptionInstance.ValueUpdateListener<Integer> update) {
        String translationKey = "watersourcemod.config." + key;
        OptionInstance<Integer> option = new OptionInstance<>(
                translationKey,
                OptionInstance.cachedConstantTooltip(Component.translatable(translationKey + ".tooltip")),
                (caption, value) -> CommonComponents.optionNameValue(
                        caption.copy().withColor(KEY_COLOR),
                        valueLabel.apply(value).copy().withColor(VALUE_COLOR)),
                new OptionInstance.IntRange(minimum, maximum),
                current,
                update);
        AbstractWidget slider = option.createButton(minecraft.options, x, y, columnWidth);
        addRenderableWidget(slider);
    }

    private void addColor(int x, int y, String key, int color, ColorTarget target) {
        Component value = Component.literal(String.format("#%06X", color & 0xFFFFFF)).withColor(VALUE_COLOR);
        Button button = Button.builder(
                        CommonComponents.optionNameValue(optionName(key), value),
                        ignored -> minecraft.gui.setScreen(new WaterSourceColorPickerScreen(this, target)))
                .bounds(x, y, columnWidth, BUTTON_HEIGHT)
                .tooltip(tooltip(key))
                .build();
        addRenderableWidget(button);
        colorSwatches.add(new ColorSwatch(button, color));
    }

    private void addAction(int x, int y, String key, Runnable action) {
        addRenderableWidget(Button.builder(
                        Component.translatable("watersourcemod.config." + key),
                        ignored -> action.run())
                .bounds(x, y, columnWidth, BUTTON_HEIGHT)
                .tooltip(tooltip(key))
                .build());
    }

    private static Component optionName(String key) {
        return Component.translatable("watersourcemod.config." + key).withColor(KEY_COLOR);
    }

    private static Tooltip tooltip(String key) {
        return Tooltip.create(Component.translatable("watersourcemod.config." + key + ".tooltip"));
    }

    private static Component stateValue(boolean enabled) {
        return Component.translatable(enabled ? "watersourcemod.value.on" : "watersourcemod.value.off")
                .withColor(enabled ? ENABLED_COLOR : DISABLED_COLOR);
    }

    private static Component enumValue(String group, String key) {
        return Component.translatable("watersourcemod." + group + "." + key).withColor(VALUE_COLOR);
    }

    private static Component radiusValue(int radius) {
        if (radius == 0) {
            return Component.translatable("watersourcemod.radius.current").withColor(VALUE_COLOR);
        }
        int diameter = radius * 2 + 1;
        return Component.translatable("watersourcemod.radius.area", radius, diameter).withColor(VALUE_COLOR);
    }

    private static Component integerValue(int value) {
        return Component.literal(Integer.toString(value)).withColor(VALUE_COLOR);
    }

    private static Component signedIntegerValue(int value) {
        return Component.literal(value > 0 ? "+" + value : Integer.toString(value)).withColor(VALUE_COLOR);
    }

    private static List<Integer> integerValues(int min, int max) {
        List<Integer> values = new ArrayList<>();
        for (int value = min; value <= max; value++) {
            values.add(value);
        }
        return values;
    }

    private static List<Integer> withCurrent(List<Integer> values, int current) {
        if (values.contains(current)) {
            return values;
        }
        ArrayList<Integer> expanded = new ArrayList<>(values);
        expanded.add(current);
        expanded.sort(Comparator.naturalOrder());
        return List.copyOf(expanded);
    }

    private static void update(Consumer<WaterSourceConfig> updater) {
        WaterSourceModClient.updateConfig(updater);
    }

    private int row(int index) {
        return CONTENT_TOP + index * ROW_GAP;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (ColorSwatch swatch : colorSwatches) {
            extractColorSwatch(graphics, swatch);
        }
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        boolean hasNotice = !notice.equals(CommonComponents.EMPTY);
        graphics.centeredText(
                font,
                hasNotice ? notice : Component.translatable("watersourcemod.config.subtitle"),
                width / 2,
                22,
                hasNotice ? 0xFFFFD166 : 0xFFBBD7EA);
    }

    private static void extractColorSwatch(GuiGraphicsExtractor graphics, ColorSwatch swatch) {
        Button button = swatch.button();
        if (!button.visible) {
            return;
        }
        int left = button.getX() + 3;
        int top = button.getY() + 4;
        int right = left + 4;
        int bottom = button.getBottom() - 4;
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xFF101010);
        graphics.fill(left, top, right, bottom, 0xFF000000 | (swatch.color() & 0xFFFFFF));
    }

    @Override
    public void onClose() {
        WaterSourceModClient.saveConfig();
        minecraft.gui.setScreen(parent);
    }

    enum ColorTarget {
        SOURCE("source_color") {
            @Override int get(WaterSourceConfig config) { return config.getSourceColor(); }
            @Override void set(WaterSourceConfig config, int color) { config.setSourceColor(color); }
        },
        FLOWING("flowing_color") {
            @Override int get(WaterSourceConfig config) { return config.getFlowingColor(); }
            @Override void set(WaterSourceConfig config, int color) { config.setFlowingColor(color); }
        },
        OUTLINE("outline_color") {
            @Override int get(WaterSourceConfig config) { return config.getOutlineColor(); }
            @Override void set(WaterSourceConfig config, int color) { config.setOutlineColor(color); }
        };

        private final String key;
        ColorTarget(String key) { this.key = key; }
        String key() { return key; }
        abstract int get(WaterSourceConfig config);
        abstract void set(WaterSourceConfig config, int color);
    }

    private enum Page {
        SCANNING("scanning"), MARKERS("markers"), FEATURES("features"), HUD("hud"), PROFILES("profiles");
        private final String key;
        Page(String key) { this.key = key; }
    }

    private record ColorSwatch(Button button, int color) { }
}
