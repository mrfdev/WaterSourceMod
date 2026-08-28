package com.mrfdev.watersourcemod;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaterSourceTranslationTest {
    @Test
    void opacitySliderCaptionRendersAVisiblePercentSign() throws Exception {
        String template;
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/assets/watersourcemod/lang/en_us.json")),
                StandardCharsets.UTF_8)) {
            template = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .get("watersourcemod.config.opacity.value")
                    .getAsString();
        }

        StringBuilder rendered = new StringBuilder();
        TranslatableContents contents = new TranslatableContents(
                "watersourcemod.config.opacity.value",
                template,
                new Object[]{75});

        contents.visit(text -> {
            rendered.append(text);
            return Optional.empty();
        });

        assertEquals("Opacity: 75%", rendered.toString());
    }
}
