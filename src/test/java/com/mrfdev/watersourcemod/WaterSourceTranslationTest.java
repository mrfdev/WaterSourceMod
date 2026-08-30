package com.mrfdev.watersourcemod;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterSourceTranslationTest {
    @Test
    void opacitySliderValueRendersAVisiblePercentSign() throws Exception {
        assertEquals("75%", render("watersourcemod.config.opacity.value", 75));
    }

    @Test
    void outlineThicknessSliderValueRendersItsNumber() throws Exception {
        assertEquals("3", render("watersourcemod.config.outline_thickness.value", 3));
    }

    @Test
    void markerLimitSliderValueRendersItsNumber() throws Exception {
        assertEquals("2048", render("watersourcemod.config.max_markers.value", 2048));
    }

    @Test
    void everyPagedSettingHasALabelAndTooltip() throws Exception {
        JsonObject translations = translations();
        String[] settingKeys = {
                "show_sources", "show_flowing", "include_waterlogged", "hold_to_show",
                "chunk_radius", "vertical_range", "rescan_mode", "rescan_interval",
                "scan_budget", "scan_time_budget", "max_discovered_markers", "max_visible_markers",
                "source_marker_style", "flowing_marker_style", "waterlogged_marker_style", "color_palette",
                "source_color", "flowing_color", "outline_color", "through_walls", "pulse",
                "opacity", "outline_thickness", "max_render_distance", "fade_start",
                "waterlogged_indicator", "fluid_level_visualization", "nearest_marker_mode",
                "scan_boundary_mode", "narrate_status", "diagnostic_mode",
                "preset_accessible_patterns", "preset_classic_patterns",
                "show_status_hud", "show_labels", "hud_anchor", "compact_hud",
                "hud_offset_x", "hud_offset_y", "hud_scale", "hud_background_opacity",
                "profile", "preset_current_chunk", "preset_nearby_chunks", "preset_wide_chunks",
                "export", "import", "rescan_now", "reset"
        };

        for (String key : settingKeys) {
            assertTrue(translations.has("watersourcemod.config." + key), "missing label for " + key);
            assertTrue(translations.has("watersourcemod.config." + key + ".tooltip"),
                    "missing tooltip for " + key);
        }
    }

    @Test
    void everyConfigEnumValueHasATranslation() throws Exception {
        JsonObject translations = translations();
        for (WaterSourceConfig.MarkerStyle value : WaterSourceConfig.MarkerStyle.values()) {
            assertTrue(translations.has("watersourcemod.style." + value.key()));
        }
        for (WaterSourceConfig.WaterloggedIndicator value : WaterSourceConfig.WaterloggedIndicator.values()) {
            assertTrue(translations.has("watersourcemod.waterlogged_indicator." + value.key()));
        }
        for (WaterSourceConfig.FluidLevelVisualization value
                : WaterSourceConfig.FluidLevelVisualization.values()) {
            assertTrue(translations.has("watersourcemod.fluid_level." + value.key()));
        }
        for (WaterSourceConfig.NearestMarkerMode value : WaterSourceConfig.NearestMarkerMode.values()) {
            assertTrue(translations.has("watersourcemod.nearest_marker." + value.key()));
        }
        for (WaterSourceConfig.ScanBoundaryMode value : WaterSourceConfig.ScanBoundaryMode.values()) {
            assertTrue(translations.has("watersourcemod.scan_boundary." + value.key()));
        }
        for (NearestWaterMarker.Direction value : NearestWaterMarker.Direction.values()) {
            assertTrue(translations.has("watersourcemod.direction." + value.key()));
        }
        for (WaterSourceConfig.VerticalRange value : WaterSourceConfig.VerticalRange.values()) {
            assertTrue(translations.has("watersourcemod.vertical_range." + value.key()));
        }
        for (WaterSourceConfig.RescanMode value : WaterSourceConfig.RescanMode.values()) {
            assertTrue(translations.has("watersourcemod.rescan_mode." + value.key()));
        }
        for (WaterSourceConfig.HudAnchor value : WaterSourceConfig.HudAnchor.values()) {
            assertTrue(translations.has("watersourcemod.hud_anchor." + value.key()));
        }
        for (WaterSourceConfig.ColorPalette value : WaterSourceConfig.ColorPalette.values()) {
            assertTrue(translations.has("watersourcemod.palette." + value.key()));
        }
        for (WaterSourceConfig.ConfigProfile value : WaterSourceConfig.ConfigProfile.values()) {
            assertTrue(translations.has("watersourcemod.profile." + value.key()));
        }
    }

    private String render(String key, Object value) throws Exception {
        String template = translations().get(key).getAsString();

        StringBuilder rendered = new StringBuilder();
        TranslatableContents contents = new TranslatableContents(
                key,
                template,
                new Object[]{value});

        contents.visit(text -> {
            rendered.append(text);
            return Optional.empty();
        });

        return rendered.toString();
    }

    private JsonObject translations() throws Exception {
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/assets/watersourcemod/lang/en_us.json")),
                StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
