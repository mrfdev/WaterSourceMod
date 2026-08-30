package com.mrfdev.watersourcemod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterSourceConfigManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void malformedConfigIsPreservedAndDefaultsAreRestored() throws Exception {
        Path configPath = temporaryDirectory.resolve("water-source-mod.json");
        String malformedJson = "{ definitely-not-valid-json";
        Files.writeString(configPath, malformedJson, StandardCharsets.UTF_8);

        WaterSourceConfig loaded = WaterSourceConfigManager.load(configPath);

        assertEquals(WaterSourceConfig.defaults().scanSettings(), loaded.scanSettings());
        assertFalse(Files.exists(configPath));
        List<Path> backups;
        try (var paths = Files.list(temporaryDirectory)) {
            backups = paths.filter(path -> path.getFileName().toString().contains(".invalid-"))
                    .toList();
        }
        assertEquals(1, backups.size());
        assertEquals(malformedJson, Files.readString(backups.getFirst(), StandardCharsets.UTF_8));
    }

    @Test
    void validConfigLoadsWithoutCreatingABackup() throws Exception {
        Path configPath = temporaryDirectory.resolve("water-source-mod.json");
        Files.writeString(configPath, "{\"chunkRadius\": 3, \"maxMarkers\": 1024}", StandardCharsets.UTF_8);

        WaterSourceConfig loaded = WaterSourceConfigManager.load(configPath);

        assertEquals(3, loaded.getChunkRadius());
        assertEquals(1024, loaded.getMaxMarkers());
        assertTrue(Files.exists(configPath));
        try (var paths = Files.list(temporaryDirectory)) {
            assertEquals(1, paths.count());
        }
    }

    @Test
    void versionOneConfigMigratesWithoutChangingItsExistingMarkerAppearance() throws Exception {
        Path configPath = temporaryDirectory.resolve("water-source-mod.json");
        Files.writeString(configPath, """
                {
                  "configVersion": 1,
                  "markerStyle": "BEACON",
                  "maxMarkers": 2048,
                  "sourceColor": 1193046,
                  "flowingColor": 11259375,
                  "outlineColor": 66051
                }
                """, StandardCharsets.UTF_8);

        WaterSourceConfig loaded = WaterSourceConfigManager.load(configPath);

        assertEquals(WaterSourceConfig.CURRENT_VERSION, loaded.getConfigVersion());
        assertEquals(WaterSourceConfig.MarkerStyle.BEACON, loaded.getSourceMarkerStyle());
        assertEquals(WaterSourceConfig.MarkerStyle.BEACON, loaded.getFlowingMarkerStyle());
        assertEquals(WaterSourceConfig.MarkerStyle.BEACON, loaded.getWaterloggedMarkerStyle());
        assertEquals(2_048, loaded.getMaxMarkers());
        assertEquals(2_048, loaded.getMaxVisibleMarkers());
        assertEquals(WaterSourceConfig.ColorPalette.CUSTOM, loaded.getColorPalette());
    }

    @Test
    void exportAndImportRoundTripNormalizedSettings() {
        Path exportPath = temporaryDirectory.resolve("water-source-mod-export.json");
        WaterSourceConfig config = WaterSourceConfig.defaults();
        config.setChunkRadius(3);
        config.setVerticalRange(WaterSourceConfig.VerticalRange.NEARBY_64);
        config.setRescanMode(WaterSourceConfig.RescanMode.MANUAL);
        config.setMaxVisibleMarkers(1_024);
        config.setHudOffsetX(17);
        config.setSourceMarkerStyle(WaterSourceConfig.MarkerStyle.HOLLOW);
        config.setWaterloggedIndicator(WaterSourceConfig.WaterloggedIndicator.CROSS);
        config.setFluidLevelVisualization(WaterSourceConfig.FluidLevelVisualization.BOTH);
        config.setNearestMarkerMode(WaterSourceConfig.NearestMarkerMode.BOTH);
        config.setScanBoundaryMode(WaterSourceConfig.ScanBoundaryMode.ALL);
        config.setNarrateStatus(true);
        config.setDiagnosticMode(true);

        assertTrue(WaterSourceConfigManager.save(config, exportPath));
        WaterSourceConfig imported = WaterSourceConfigManager.importConfig(exportPath).orElseThrow();

        assertEquals(3, imported.getChunkRadius());
        assertEquals(WaterSourceConfig.VerticalRange.NEARBY_64, imported.getVerticalRange());
        assertEquals(WaterSourceConfig.RescanMode.MANUAL, imported.getRescanMode());
        assertEquals(1_024, imported.getMaxVisibleMarkers());
        assertEquals(17, imported.getHudOffsetX());
        assertEquals(WaterSourceConfig.MarkerStyle.HOLLOW, imported.getSourceMarkerStyle());
        assertEquals(WaterSourceConfig.WaterloggedIndicator.CROSS, imported.getWaterloggedIndicator());
        assertEquals(WaterSourceConfig.FluidLevelVisualization.BOTH,
                imported.getFluidLevelVisualization());
        assertEquals(WaterSourceConfig.NearestMarkerMode.BOTH, imported.getNearestMarkerMode());
        assertEquals(WaterSourceConfig.ScanBoundaryMode.ALL, imported.getScanBoundaryMode());
        assertTrue(imported.isNarrateStatus());
        assertTrue(imported.isDiagnosticMode());
    }

    @Test
    void malformedImportIsRejectedWithoutMovingOrChangingTheFile() throws Exception {
        Path exportPath = temporaryDirectory.resolve("water-source-mod-export.json");
        String malformedJson = "{ still-not-valid-json";
        Files.writeString(exportPath, malformedJson, StandardCharsets.UTF_8);

        assertTrue(WaterSourceConfigManager.importConfig(exportPath).isEmpty());
        assertTrue(Files.exists(exportPath));
        assertEquals(malformedJson, Files.readString(exportPath, StandardCharsets.UTF_8));
    }

    @Test
    void invalidUtf8IsAlsoPreservedAsMalformedInput() throws Exception {
        Path configPath = temporaryDirectory.resolve("water-source-mod.json");
        Files.write(configPath, new byte[]{(byte) 0xC3, 0x28});

        WaterSourceConfigManager.load(configPath);

        assertFalse(Files.exists(configPath));
        try (var paths = Files.list(temporaryDirectory)) {
            assertEquals(1, paths.filter(path -> path.getFileName().toString().contains(".invalid-"))
                    .count());
        }
    }
}
