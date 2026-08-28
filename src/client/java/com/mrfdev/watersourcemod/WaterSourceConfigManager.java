package com.mrfdev.watersourcemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Loads and saves the local, human-readable client configuration safely. */
public final class WaterSourceConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "water-source-mod.json";

    private WaterSourceConfigManager() {
    }

    public static WaterSourceConfig load() {
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            return WaterSourceConfig.defaults();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            WaterSourceConfig config = GSON.fromJson(reader, WaterSourceConfig.class);
            if (config == null) {
                return WaterSourceConfig.defaults();
            }
            config.normalize();
            return config;
        } catch (Exception ignored) {
            // A broken local config must never prevent the client from starting.
            return WaterSourceConfig.defaults();
        }
    }

    public static void save(WaterSourceConfig source) {
        WaterSourceConfig config = source.copy();
        config.normalize();
        Path path = configPath();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(
                    temporary,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
            // Settings are best-effort. Keep the current in-memory settings if the disk is unavailable.
            try {
                Files.deleteIfExists(temporary);
            } catch (Exception ignoredCleanup) {
                // Nothing else is safe or useful to do here.
            }
        }
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
