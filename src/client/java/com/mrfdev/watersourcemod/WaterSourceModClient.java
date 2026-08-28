package com.mrfdev.watersourcemod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/** Client entrypoint for the optional water source visualization. */
public final class WaterSourceModClient implements ClientModInitializer {
    public static final String MOD_ID = "watersourcemod";

    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "controls"));
    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.watersourcemod.toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F9,
            KEY_CATEGORY));
    private static final KeyMapping RESCAN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.watersourcemod.rescan",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F9,
            KEY_CATEGORY,
            1));
    private static final KeyMapping SETTINGS_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.watersourcemod.settings",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F10,
            KEY_CATEGORY,
            2));
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "status_hud");

    private static WaterSourceConfig config;
    private static WaterSourceScanner scanner;
    private static WaterSourceRenderer renderer;

    @Override
    public void onInitializeClient() {
        config = WaterSourceConfigManager.load();
        scanner = new WaterSourceScanner();
        renderer = new WaterSourceRenderer();
        renderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(WaterSourceModClient::onClientTick);
        HudElementRegistry.addLast(HUD_ID, WaterSourceModClient::extractHud);
    }

    private static void onClientTick(Minecraft client) {
        boolean rescanPressed = RESCAN_KEY.consumeClick();
        boolean togglePressed = TOGGLE_KEY.consumeClick();
        if (client.gui.screen() == null) {
            if (rescanPressed && client.hasShiftDown()) {
                scanner.requestFullScan();
            } else if (togglePressed) {
                scanner.setEnabled(!scanner.isEnabled());
            }
            if (SETTINGS_KEY.consumeClick()) {
                openSettings(client);
            }
        } else {
            // Consume gameplay bindings while a screen is open so they cannot leak into a later tick.
            if (SETTINGS_KEY.consumeClick()) {
                // Intentionally ignored while another screen owns keyboard input.
            }
        }

        scanner.tick(client, config);
    }

    private static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !scanner.isEnabled() || !config.isShowStatusHud()) {
            return;
        }

        WaterSourceScanner.ScanStatus status = scanner.status();
        int width = 214;
        int x = (graphics.guiWidth() - width) / 2;
        int y = 8;
        int height = 28 + (status.markerLimitReached() ? 12 : 0) + (config.isShowLabels() ? 20 : 0);
        graphics.fill(x, y, x + width, y + height, 0xB0101724);
        graphics.outline(x, y, width, height, 0xE0FFFFFF);

        Component title = Component.translatable("watersourcemod.hud.title");
        graphics.text(client.font, title, x + 6, y + 5, 0xFFFFFFFF);

        Component statusLine = status.scanning()
                ? Component.translatable("watersourcemod.hud.scanning", status.progressPercent(), status.loadedChunkCount(), status.requestedChunkCount())
                : Component.translatable("watersourcemod.hud.counts", status.sourceCount(), status.flowingCount());
        graphics.text(client.font, statusLine, x + 6, y + 16, 0xFFE6F4FF);

        if (status.markerLimitReached()) {
            graphics.text(client.font, Component.translatable("watersourcemod.hud.limit"), x + 6, y + 27, 0xFFFFC857);
        }

        if (config.isShowLabels()) {
            int legendY = y + 28 + (status.markerLimitReached() ? 12 : 0);
            graphics.text(client.font, Component.translatable("watersourcemod.hud.source"), x + 6, legendY, 0xFF000000 | config.getSourceColor());
            graphics.text(client.font, Component.translatable("watersourcemod.hud.flowing"), x + 108, legendY, 0xFF000000 | config.getFlowingColor());
        }
    }

    public static WaterSourceConfig config() {
        return config;
    }

    public static WaterSourceScanner scanner() {
        return scanner;
    }

    public static void updateConfig(Consumer<WaterSourceConfig> updater) {
        WaterSourceConfig updated = config.copy();
        updater.accept(updated);
        updated.normalize();
        config = updated;
        WaterSourceConfigManager.save(updated);
        scanner.onConfigChanged();
    }

    public static void resetConfig() {
        config = WaterSourceConfig.defaults();
        WaterSourceConfigManager.save(config);
        scanner.onConfigChanged();
    }

    public static void openSettings(Minecraft client) {
        client.gui.setScreen(new WaterSourceConfigScreen(client.gui.screen()));
    }

    public static void closeRenderer() {
        if (renderer != null) {
            renderer.close();
        }
    }
}
