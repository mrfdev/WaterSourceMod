package com.mrfdev.watersourcemod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
    private static final KeyMapping HOLD_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.watersourcemod.hold",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F8,
            KEY_CATEGORY,
            1));
    private static final KeyMapping RESCAN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.watersourcemod.rescan",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F9,
            KEY_CATEGORY,
            2));
    private static final KeyMapping SETTINGS_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.watersourcemod.settings",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F10,
            KEY_CATEGORY,
            3));
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "status_hud");
    private static final Component HUD_TITLE = Component.translatable("watersourcemod.hud.title");
    private static final Component HUD_LIMIT = Component.translatable("watersourcemod.hud.limit");
    private static final Component HUD_SOURCE = Component.translatable("watersourcemod.hud.source");
    private static final Component HUD_FLOWING = Component.translatable("watersourcemod.hud.flowing");

    /** User configuration and its immutable render projection are swapped atomically. */
    private static final AtomicReference<PublishedConfig> CONFIG =
            new AtomicReference<>(PublishedConfig.from(WaterSourceConfig.defaults()));
    private static WaterSourceScanner scanner;
    private static WaterSourceRenderer renderer;
    private static volatile NearestWaterMarker nearestMarker = NearestWaterMarker.none();
    private static List<WaterMarker> lastNearestSnapshot = List.of();
    private static WaterSourceConfig.RenderSettings lastNearestSettings;
    private static int nearestRefreshCountdown;
    private static long lastNarratedScanSequence;
    private static boolean narrationReady;
    private static boolean configDirty;
    private static boolean overlayToggled;

    @Override
    public void onInitializeClient() {
        CONFIG.set(PublishedConfig.from(WaterSourceConfigManager.load()));
        configDirty = false;
        overlayToggled = false;
        nearestMarker = NearestWaterMarker.none();
        lastNearestSnapshot = List.of();
        lastNearestSettings = null;
        nearestRefreshCountdown = 0;
        lastNarratedScanSequence = 0;
        narrationReady = false;
        scanner = new WaterSourceScanner();
        renderer = new WaterSourceRenderer();
        ClientLifecycleEvents.CLIENT_STOPPING.register(WaterSourceModClient::onClientStopping);
        renderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(WaterSourceModClient::onClientTick);
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(scanner::onLevelChanged);
        ClientChunkEvents.CHUNK_LOAD.register(scanner::onChunkLoaded);
        ClientChunkEvents.CHUNK_UNLOAD.register(scanner::onChunkUnloaded);
        HudElementRegistry.addLast(HUD_ID, WaterSourceModClient::extractHud);
    }

    private static void onClientTick(Minecraft client) {
        boolean rescanPressed = RESCAN_KEY.consumeClick();
        boolean togglePressed = TOGGLE_KEY.consumeClick();
        if (client.gui.screen() == null) {
            if (rescanPressed && client.hasShiftDown()) {
                scanner.requestFullScan();
            } else if (togglePressed) {
                overlayToggled = !overlayToggled;
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

        PublishedConfig published = CONFIG.get();
        WaterSourceConfig config = published.config();
        boolean heldTemporarily = client.gui.screen() == null
                && config.isHoldToShow()
                && HOLD_KEY.isDown();
        boolean overlayVisible = overlayToggled || heldTemporarily;
        if (scanner.isEnabled() != overlayVisible) {
            scanner.setEnabled(overlayVisible);
        }
        long scanSequenceBeforeTick = scanner.completedScanSequence();
        scanner.tick(client, config);
        updateNearestMarker(client, published.renderSettings());
        narrateCompletedScan(client, config, scanSequenceBeforeTick);
    }

    private static void updateNearestMarker(
            Minecraft client,
            WaterSourceConfig.RenderSettings settings) {
        if (!scanner.isEnabled()
                || client.player == null
                || settings.nearestMarkerMode() == WaterSourceConfig.NearestMarkerMode.OFF) {
            nearestMarker = NearestWaterMarker.none();
            lastNearestSnapshot = List.of();
            lastNearestSettings = settings;
            nearestRefreshCountdown = 0;
            return;
        }

        List<WaterMarker> markers = scanner.markerSnapshot();
        boolean snapshotChanged = markers != lastNearestSnapshot;
        boolean settingsChanged = settings != lastNearestSettings;
        if (!snapshotChanged && !settingsChanged && nearestRefreshCountdown-- > 0) {
            return;
        }

        nearestMarker = NearestWaterMarker.find(
                markers,
                client.player.getX(),
                client.player.getEyeY(),
                client.player.getZ(),
                settings);
        lastNearestSnapshot = markers;
        lastNearestSettings = settings;
        nearestRefreshCountdown = 3;
    }

    private static void narrateCompletedScan(
            Minecraft client,
            WaterSourceConfig config,
            long scanSequenceBeforeTick) {
        if (!scanner.isEnabled()
                || !config.isNarrateStatus()
                || !client.getNarrator().isActive()) {
            narrationReady = false;
            return;
        }

        if (!narrationReady) {
            lastNarratedScanSequence = scanSequenceBeforeTick;
            narrationReady = true;
        }
        long completedSequence = scanner.completedScanSequence();
        if (completedSequence <= lastNarratedScanSequence) {
            return;
        }

        WaterSourceScanner.ScanStatus status = scanner.status();
        Component message = status.markerLimitReached()
                ? Component.translatable(
                        "watersourcemod.narration.scan_complete_limit",
                        status.markerCount(),
                        status.skippedChunkCount(),
                        status.discardedMarkerCount())
                : Component.translatable(
                        "watersourcemod.narration.scan_complete",
                        status.markerCount(),
                        status.skippedChunkCount());
        client.getNarrator().saySystemQueued(message);
        lastNarratedScanSequence = completedSequence;
    }

    private static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        WaterSourceConfig config = CONFIG.get().config();
        if (client.level == null || !scanner.isEnabled() || !config.isShowStatusHud()) {
            return;
        }

        WaterSourceScanner.ScanStatus status = scanner.status();
        int visibleCount = renderer.visibleMarkerCount();
        int discoveredCount = scanner.markerSnapshot().size();
        Component snapshotState = Component.translatable(status.staleSnapshot()
                        ? "watersourcemod.hud.snapshot_stale"
                        : "watersourcemod.hud.snapshot_current")
                .withColor(status.staleSnapshot() ? 0xFFC857 : 0x78E6A3);
        Component summaryLine = Component.translatable(
                "watersourcemod.hud.summary",
                visibleCount,
                discoveredCount,
                status.skippedChunkCount(),
                snapshotState);
        Component countsLine = Component.translatable(
                "watersourcemod.hud.counts",
                status.sourceCount(),
                status.flowingCount());
        Component progressLine = status.scanning()
                ? Component.translatable(
                        "watersourcemod.hud.scanning",
                        status.progressPercent(),
                        status.loadedChunkCount(),
                        status.requestedChunkCount())
                : null;
        NearestWaterMarker nearest = nearestMarker;
        Component nearestLine = config.getNearestMarkerMode().showsHud() && nearest.isPresent()
                ? nearestHudLine(nearest)
                : null;
        Component diagnosticScanLine = config.isDiagnosticMode()
                ? Component.translatable(
                        "watersourcemod.hud.diagnostic_scan",
                        status.scanDurationMillis(),
                        status.inspectedBlocks(),
                        status.processedBlocks(),
                        status.totalBlocks(),
                        status.discardedMarkerCount())
                : null;
        Component diagnosticRenderLine = config.isDiagnosticMode()
                ? Component.translatable(
                        "watersourcemod.hud.diagnostic_render",
                        visibleCount,
                        renderer.renderedVertexCount())
                : null;

        boolean compact = config.isCompactHud();
        int lineCount = compact ? 1 : 3;
        if (progressLine != null) {
            lineCount++;
        }
        if (nearestLine != null) {
            lineCount++;
        }
        if (status.markerLimitReached()) {
            lineCount++;
        }
        if (!compact && config.isShowLabels()) {
            lineCount++;
        }
        if (config.isDiagnosticMode()) {
            lineCount += 2;
        }

        int panelWidth = compact ? 236 : 252;
        panelWidth = Math.max(panelWidth, client.font.width(summaryLine) + 12);
        if (!compact) {
            panelWidth = Math.max(panelWidth, client.font.width(HUD_TITLE) + 12);
            panelWidth = Math.max(panelWidth, client.font.width(countsLine) + 12);
        }
        if (progressLine != null) {
            panelWidth = Math.max(panelWidth, client.font.width(progressLine) + 12);
        }
        if (nearestLine != null) {
            panelWidth = Math.max(panelWidth, client.font.width(nearestLine) + 12);
        }
        if (diagnosticScanLine != null) {
            panelWidth = Math.max(panelWidth, client.font.width(diagnosticScanLine) + 12);
            panelWidth = Math.max(panelWidth, client.font.width(diagnosticRenderLine) + 12);
        }
        int panelHeight = 10 + lineCount * 11;
        float scale = config.getHudScalePercent() / 100F;
        int maximumPanelWidth = Math.max(120, (int) (graphics.guiWidth() / scale) - 16);
        panelWidth = Math.min(panelWidth, maximumPanelWidth);
        int scaledWidth = Math.max(1, (int) Math.ceil(panelWidth * scale));
        int scaledHeight = Math.max(1, (int) Math.ceil(panelHeight * scale));
        WaterSourceConfig.HudPosition position = config.getHudAnchor().position(
                graphics.guiWidth(),
                graphics.guiHeight(),
                scaledWidth,
                scaledHeight,
                config.getHudOffsetX(),
                config.getHudOffsetY());
        int backgroundAlpha = Math.round(config.getHudBackgroundOpacityPercent() * 2.55F);

        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(position.x(), position.y());
            graphics.pose().scale(scale, scale);
            graphics.fill(0, 0, panelWidth, panelHeight, (backgroundAlpha << 24) | 0x101724);
            graphics.outline(0, 0, panelWidth, panelHeight, 0xE0FFFFFF);

            int y = 5;
            if (!compact) {
                graphics.text(client.font, HUD_TITLE, 6, y, 0xFFFFFFFF);
                y += 11;
            }
            graphics.text(client.font, summaryLine, 6, y, 0xFFE6F4FF);
            y += 11;

            if (!compact) {
                graphics.text(client.font, countsLine, 6, y, 0xFFE6F4FF);
                y += 11;
            }
            if (progressLine != null) {
                graphics.text(client.font, progressLine, 6, y, 0xFFB6DFFF);
                y += 11;
            }
            if (nearestLine != null) {
                int nearestColor = nearest.marker().source()
                        ? 0xFF000000 | config.getSourceColor()
                        : 0xFF000000 | config.getFlowingColor();
                graphics.text(client.font, nearestLine, 6, y, nearestColor);
                y += 11;
            }

            if (status.markerLimitReached()) {
                graphics.text(client.font, HUD_LIMIT, 6, y, 0xFFFFC857);
                y += 11;
            }

            if (!compact && config.isShowLabels()) {
                graphics.text(client.font, HUD_SOURCE, 6, y, 0xFF000000 | config.getSourceColor());
                graphics.text(
                        client.font,
                        HUD_FLOWING,
                        Math.max(108, panelWidth / 2),
                        y,
                        0xFF000000 | config.getFlowingColor());
                y += 11;
            }
            if (diagnosticScanLine != null) {
                graphics.text(client.font, diagnosticScanLine, 6, y, 0xFFAFC4D8);
                y += 11;
                graphics.text(client.font, diagnosticRenderLine, 6, y, 0xFFAFC4D8);
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static Component nearestHudLine(NearestWaterMarker nearest) {
        Component direction = Component.translatable(
                "watersourcemod.direction." + nearest.direction().key());
        int verticalOffset = nearest.verticalOffset();
        Component vertical = verticalOffset > 0
                ? Component.translatable("watersourcemod.vertical.up", verticalOffset)
                : verticalOffset < 0
                        ? Component.translatable("watersourcemod.vertical.down", Math.abs(verticalOffset))
                        : Component.translatable("watersourcemod.vertical.level");
        Component type = nearest.marker().source() ? HUD_SOURCE : HUD_FLOWING;
        return Component.translatable(
                "watersourcemod.hud.nearest",
                type,
                nearest.roundedDistance(),
                direction,
                vertical);
    }

    public static WaterSourceConfig config() {
        return CONFIG.get().config().copy();
    }

    public static WaterSourceConfig.RenderSettings renderSettings() {
        return CONFIG.get().renderSettings();
    }

    static NearestWaterMarker nearestMarker() {
        return nearestMarker;
    }

    public static WaterSourceScanner scanner() {
        return scanner;
    }

    public static void updateConfig(Consumer<WaterSourceConfig> updater) {
        WaterSourceConfig updated = CONFIG.get().config().copy();
        updated.markCustomProfile();
        updater.accept(updated);
        publishConfig(updated);
    }

    public static void applyProfile(WaterSourceConfig.ConfigProfile profile) {
        WaterSourceConfig updated = CONFIG.get().config().copy();
        updated.applyProfile(profile);
        publishConfig(updated);
    }

    public static boolean exportConfig() {
        return WaterSourceConfigManager.exportConfig(CONFIG.get().config());
    }

    public static boolean importConfig() {
        return WaterSourceConfigManager.importConfig()
                .map(imported -> {
                    publishConfig(imported);
                    return true;
                })
                .orElse(false);
    }

    public static void onClientBlockUpdated(ClientLevel level, BlockPos position) {
        WaterSourceScanner activeScanner = scanner;
        if (activeScanner != null) {
            activeScanner.onBlockUpdated(level, position);
        }
    }

    public static void resetConfig() {
        publishConfig(WaterSourceConfig.defaults());
    }

    public static void saveConfig() {
        if (configDirty && WaterSourceConfigManager.save(CONFIG.get().config())) {
            configDirty = false;
        }
    }

    public static void openSettings(Minecraft client) {
        client.gui.setScreen(new WaterSourceConfigScreen(client.gui.screen()));
    }

    private static void onClientStopping(Minecraft client) {
        saveConfig();
        if (renderer != null) {
            renderer.close();
        }
    }

    private static void publishConfig(WaterSourceConfig updated) {
        updated.normalize();
        CONFIG.set(PublishedConfig.from(updated));
        configDirty = true;
    }

    private record PublishedConfig(
            WaterSourceConfig config,
            WaterSourceConfig.RenderSettings renderSettings) {
        private static PublishedConfig from(WaterSourceConfig config) {
            WaterSourceConfig stableConfig = config.copy();
            stableConfig.normalize();
            return new PublishedConfig(stableConfig, stableConfig.renderSettings());
        }
    }
}
