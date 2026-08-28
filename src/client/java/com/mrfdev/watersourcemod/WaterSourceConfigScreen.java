package com.mrfdev.watersourcemod;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/** Vanilla-only settings screen, also used as the optional Mod Menu screen. */
public final class WaterSourceConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 210;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int PAIR_GAP = 6;
    private static final int PAIRED_BUTTON_WIDTH = (BUTTON_WIDTH - PAIR_GAP) / 2;

    private final Screen parent;
    private CycleButton<Integer> sourceColorButton;
    private CycleButton<Integer> flowingColorButton;

    public WaterSourceConfigScreen(Screen parent) {
        super(Component.translatable("screen.watersourcemod.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        WaterSourceConfig config = WaterSourceModClient.config();
        int left = width / 2 - BUTTON_WIDTH - 8;
        int right = width / 2 + 8;
        int top = 34;

        addBoolean(left, top, PAIRED_BUTTON_WIDTH, "show_sources", config.isShowSources(),
                value -> WaterSourceModClient.updateConfig(c -> c.setShowSources(value)));
        addBoolean(left + PAIRED_BUTTON_WIDTH + PAIR_GAP, top, PAIRED_BUTTON_WIDTH,
                "show_flowing", config.isShowFlowing(),
                value -> WaterSourceModClient.updateConfig(c -> c.setShowFlowing(value)));
        addBoolean(left, top + ROW_GAP, "include_waterlogged", config.isIncludeWaterloggedSources(), value -> WaterSourceModClient.updateConfig(c -> c.setIncludeWaterloggedSources(value)));
        addBoolean(left, top + ROW_GAP * 2, "through_walls", config.isThroughWalls(), value -> WaterSourceModClient.updateConfig(c -> c.setThroughWalls(value)));
        addBoolean(left, top + ROW_GAP * 3, "show_labels", config.isShowLabels(), value -> WaterSourceModClient.updateConfig(c -> c.setShowLabels(value)));
        addBoolean(left, top + ROW_GAP * 4, "show_status_hud", config.isShowStatusHud(), value -> WaterSourceModClient.updateConfig(c -> c.setShowStatusHud(value)));

        addBoolean(right, top, "pulse", config.isPulse(), value -> WaterSourceModClient.updateConfig(c -> c.setPulse(value)));
        addMarkerStyle(right, top + ROW_GAP, config);
        addInteger(right, top + ROW_GAP * 2, "chunk_radius", config.getChunkRadius(),
                integerValues(WaterSourceConfig.MIN_RADIUS, WaterSourceConfig.MAX_RADIUS),
                value -> WaterSourceModClient.updateConfig(c -> c.setChunkRadius(value)));
        int colorY = top + ROW_GAP * 3;
        sourceColorButton = addColor(right, colorY, PAIRED_BUTTON_WIDTH, "source_color", config.getSourceColor(),
                new Integer[]{0xFFD21F, 0xFFF4F7FF, 0xFF8A00},
                value -> WaterSourceModClient.updateConfig(c -> c.setSourceColor(value)));
        flowingColorButton = addColor(right + PAIRED_BUTTON_WIDTH + PAIR_GAP, colorY, PAIRED_BUTTON_WIDTH,
                "flowing_color", config.getFlowingColor(),
                new Integer[]{0x00D9FF, 0x35FF9A, 0xB56CFF},
                value -> WaterSourceModClient.updateConfig(c -> c.setFlowingColor(value)));
        addSlider(right, top + ROW_GAP * 4, "opacity", config.getOpacityPercent(), 10, 100,
                value -> WaterSourceModClient.updateConfig(c -> c.setOpacityPercent(value)));
        addSlider(right, top + ROW_GAP * 5, "outline_thickness", config.getOutlineThickness(), 1, 6,
                value -> WaterSourceModClient.updateConfig(c -> c.setOutlineThickness(value)));
        addSlider(right, top + ROW_GAP * 6, "max_markers", config.getMaxMarkers(), 512, 16_384,
                value -> WaterSourceModClient.updateConfig(c -> c.setMaxMarkers(value)));

        int footerY = Math.max(height - 30, top + ROW_GAP * 7 + 4);
        addRenderableWidget(Button.builder(
                        Component.translatable("watersourcemod.config.reset"),
                        button -> {
                            WaterSourceModClient.resetConfig();
                            init(width, height);
                        })
                .bounds(width / 2 - 220, footerY, 140, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("watersourcemod.config.reset.tooltip")))
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> onClose())
                .bounds(width / 2 + 80, footerY, 140, BUTTON_HEIGHT)
                .build());
    }

    private void addBoolean(int x, int y, String key, boolean current, java.util.function.Consumer<Boolean> update) {
        addBoolean(x, y, BUTTON_WIDTH, key, current, update);
    }

    private void addBoolean(int x, int y, int width, String key, boolean current,
                            java.util.function.Consumer<Boolean> update) {
        CycleButton<Boolean> button = CycleButton.booleanBuilder(
                        Component.literal("ON").withStyle(ChatFormatting.GREEN),
                        Component.literal("OFF").withStyle(ChatFormatting.RED),
                        current)
                .create(x, y, width, BUTTON_HEIGHT, Component.translatable("watersourcemod.config." + key),
                        (ignored, value) -> update.accept(value));
        button.setTooltip(Tooltip.create(Component.translatable("watersourcemod.config." + key + ".tooltip")));
        addRenderableWidget(button);
    }

    private void addMarkerStyle(int x, int y, WaterSourceConfig config) {
        List<WaterSourceConfig.MarkerStyle> styles = Arrays.asList(WaterSourceConfig.MarkerStyle.values());
        CycleButton<WaterSourceConfig.MarkerStyle> button = CycleButton
                .<WaterSourceConfig.MarkerStyle>builder(style -> Component.translatable("watersourcemod.style." + style.key()), config.getMarkerStyle())
                .withValues(styles)
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("watersourcemod.config.marker_style"),
                        (ignored, value) -> WaterSourceModClient.updateConfig(c -> c.setMarkerStyle(value)));
        button.setTooltip(Tooltip.create(Component.translatable("watersourcemod.config.marker_style.tooltip")));
        addRenderableWidget(button);
    }

    private CycleButton<Integer> addColor(int x, int y, int width, String key, int current, Integer[] values,
                                          java.util.function.Consumer<Integer> update) {
        CycleButton<Integer> button = CycleButton
                .<Integer>builder(value -> Component.literal(String.format("#%06X", value & 0xFFFFFF)), current)
                .withValues(Arrays.asList(values))
                .create(x, y, width, BUTTON_HEIGHT, Component.translatable("watersourcemod.config." + key),
                        (ignored, value) -> update.accept(value));
        button.setTooltip(Tooltip.create(Component.translatable("watersourcemod.config." + key + ".tooltip")));
        addRenderableWidget(button);
        return button;
    }

    private void addInteger(int x, int y, String key, int current, List<Integer> values, java.util.function.Consumer<Integer> update) {
        CycleButton<Integer> button = CycleButton
                .<Integer>builder(value -> Component.literal(Integer.toString(value)), current)
                .withValues(values)
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("watersourcemod.config." + key),
                        (ignored, value) -> update.accept(value));
        button.setTooltip(Tooltip.create(Component.translatable("watersourcemod.config." + key + ".tooltip")));
        addRenderableWidget(button);
    }

    private void addSlider(int x, int y, String key, int current, int min, int max,
                           OptionInstance.ValueUpdateListener<Integer> update) {
        String translationKey = "watersourcemod.config." + key;
        OptionInstance<Integer> option = new OptionInstance<Integer>(
                translationKey,
                OptionInstance.cachedConstantTooltip(Component.translatable(translationKey + ".tooltip")),
                (caption, value) -> Component.translatable(translationKey + ".value", value),
                new OptionInstance.IntRange(min, max),
                current,
                update);
        AbstractWidget slider = option.createButton(minecraft.options, x, y, BUTTON_WIDTH);
        addRenderableWidget(slider);
    }

    private static List<Integer> integerValues(int min, int max) {
        return integerValues(min, max, 1);
    }

    private static List<Integer> integerValues(int min, int max, int step) {
        java.util.ArrayList<Integer> values = new java.util.ArrayList<>();
        for (int value = min; value <= max; value += step) {
            values.add(value);
        }
        return values;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractColorSwatch(graphics, sourceColorButton);
        extractColorSwatch(graphics, flowingColorButton);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("watersourcemod.config.subtitle"), width / 2, 22, 0xFFBBD7EA);
    }

    private static void extractColorSwatch(GuiGraphicsExtractor graphics, CycleButton<Integer> button) {
        if (button == null || !button.visible) {
            return;
        }

        int left = button.getX() + 3;
        int top = button.getY() + 4;
        int right = left + 4;
        int bottom = button.getBottom() - 4;
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xFF101010);
        graphics.fill(left, top, right, bottom, 0xFF000000 | (button.getValue() & 0xFFFFFF));
    }

    @Override
    public void onClose() {
        WaterSourceConfigManager.save(WaterSourceModClient.config());
        minecraft.gui.setScreen(parent);
    }
}
