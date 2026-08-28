package com.mrfdev.watersourcemod;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private String render(String key, Object value) throws Exception {
        String template;
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/assets/watersourcemod/lang/en_us.json")),
                StandardCharsets.UTF_8)) {
            template = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .get(key)
                    .getAsString();
        }

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
}
