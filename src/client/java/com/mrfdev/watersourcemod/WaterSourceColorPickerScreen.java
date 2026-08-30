package com.mrfdev.watersourcemod;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** RGB editor for one persisted custom palette color. */
final class WaterSourceColorPickerScreen extends Screen {
    private static final int CONTROL_WIDTH = 260;
    private static final int KEY_COLOR = 0xD8D8D8;
    private static final int VALUE_COLOR = 0xFFFFFF;

    private final Screen parent;
    private final WaterSourceConfigScreen.ColorTarget target;
    private int red;
    private int green;
    private int blue;

    WaterSourceColorPickerScreen(Screen parent, WaterSourceConfigScreen.ColorTarget target) {
        super(Component.translatable(
                "screen.watersourcemod.color_picker",
                Component.translatable("watersourcemod.config." + target.key())));
        this.parent = parent;
        this.target = target;
        int color = target.get(WaterSourceModClient.config());
        red = color >> 16 & 0xFF;
        green = color >> 8 & 0xFF;
        blue = color & 0xFF;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = width / 2 - CONTROL_WIDTH / 2;
        addChannel(x, 68, "red", red, value -> {
            red = value;
            publishColor();
        });
        addChannel(x, 94, "green", green, value -> {
            green = value;
            publishColor();
        });
        addChannel(x, 120, "blue", blue, value -> {
            blue = value;
            publishColor();
        });
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, ignored -> onClose())
                .bounds(width / 2 - 70, Math.max(0, height - 30), 140, 20)
                .build());
    }

    private void addChannel(
            int x,
            int y,
            String channel,
            int current,
            OptionInstance.ValueUpdateListener<Integer> update) {
        String key = "watersourcemod.color_picker." + channel;
        OptionInstance<Integer> option = new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> CommonComponents.optionNameValue(
                        caption.copy().withColor(KEY_COLOR),
                        Component.literal(Integer.toString(value)).withColor(VALUE_COLOR)),
                new OptionInstance.IntRange(0, 255),
                current,
                update);
        AbstractWidget slider = option.createButton(minecraft.options, x, y, CONTROL_WIDTH);
        addRenderableWidget(slider);
    }

    private void publishColor() {
        int color = red << 16 | green << 8 | blue;
        WaterSourceModClient.updateConfig(config -> target.set(config, color));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int color = red << 16 | green << 8 | blue;
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(
                font,
                Component.literal(String.format("#%06X", color)),
                width / 2,
                43,
                0xFFFFFFFF);
        int previewLeft = width / 2 - 130;
        int previewTop = 154;
        graphics.fill(previewLeft - 2, previewTop - 2, previewLeft + 262, previewTop + 34, 0xFFFFFFFF);
        graphics.fill(previewLeft, previewTop, previewLeft + 130, previewTop + 32, 0xFF000000 | color);
        graphics.fill(previewLeft + 130, previewTop, previewLeft + 260, previewTop + 32, 0xFF101724);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
