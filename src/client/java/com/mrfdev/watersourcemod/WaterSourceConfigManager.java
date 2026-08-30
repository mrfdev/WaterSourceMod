package com.mrfdev.watersourcemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/** Loads and saves the local, human-readable client configuration safely. */
public final class WaterSourceConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaterSourceConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "water-source-mod.json";
    private static final String EXPORT_FILE_NAME = "water-source-mod-export.json";

    private WaterSourceConfigManager() {
    }

    public static WaterSourceConfig load() {
        return load(configPath());
    }

    static WaterSourceConfig load(Path path) {
        if (!Files.isRegularFile(path)) {
            return WaterSourceConfig.defaults();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            WaterSourceConfig config = GSON.fromJson(reader, WaterSourceConfig.class);
            if (config == null) {
                throw new JsonParseException("Configuration root is null");
            }
            config.normalize();
            return config;
        } catch (CharacterCodingException exception) {
            return recoverMalformedConfig(path, exception);
        } catch (IOException exception) {
            LOGGER.warn(
                    "Could not read Water Source config {}; using defaults ({})",
                    path.getFileName(),
                    exception.getClass().getSimpleName());
            return WaterSourceConfig.defaults();
        } catch (RuntimeException exception) {
            return recoverMalformedConfig(path, exception);
        }
    }

    public static boolean save(WaterSourceConfig source) {
        return save(source, configPath());
    }

    static boolean save(WaterSourceConfig source, Path path) {
        WaterSourceConfig config = source.copy();
        config.normalize();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent == null) {
                throw new IOException("Configuration path has no parent directory");
            }
            Files.createDirectories(parent);
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
            return true;
        } catch (Exception exception) {
            // Settings are best-effort. Keep the current in-memory settings if the disk is unavailable.
            try {
                Files.deleteIfExists(temporary);
            } catch (Exception ignoredCleanup) {
                // Nothing else is safe or useful to do here.
            }
            LOGGER.warn(
                    "Could not save Water Source config {}; in-memory settings remain active ({})",
                    path.getFileName(),
                    exception.getClass().getSimpleName());
            return false;
        }
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static Path exportPath() {
        return configPath().resolveSibling(EXPORT_FILE_NAME);
    }

    /** Writes a portable copy beside the normal local configuration file. */
    public static boolean exportConfig(WaterSourceConfig source) {
        return save(source, exportPath());
    }

    /**
     * Reads the portable copy without moving or overwriting malformed input.
     * Import remains an explicit user action and never changes the active file
     * until the imported settings are subsequently saved.
     */
    public static Optional<WaterSourceConfig> importConfig() {
        return importConfig(exportPath());
    }

    static Optional<WaterSourceConfig> importConfig(Path path) {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            WaterSourceConfig config = GSON.fromJson(reader, WaterSourceConfig.class);
            if (config == null) {
                throw new JsonParseException("Configuration root is null");
            }
            config.normalize();
            return Optional.of(config);
        } catch (Exception exception) {
            LOGGER.warn(
                    "Could not import Water Source config {}; file was left unchanged ({})",
                    path.getFileName(),
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static WaterSourceConfig recoverMalformedConfig(Path path, Exception cause) {
        try {
            Path backup = moveToMalformedBackup(path);
            LOGGER.warn(
                    "Malformed Water Source config moved to {}; using defaults ({})",
                    backup.getFileName(),
                    cause.getClass().getSimpleName());
        } catch (IOException backupFailure) {
            LOGGER.warn(
                    "Malformed Water Source config could not be backed up; using defaults ({})",
                    backupFailure.getClass().getSimpleName());
        }
        return WaterSourceConfig.defaults();
    }

    private static Path moveToMalformedBackup(Path path) throws IOException {
        Path absolutePath = path.toAbsolutePath();
        Path parent = absolutePath.getParent();
        if (parent == null) {
            throw new IOException("Configuration path has no parent directory");
        }

        Path backup = Files.createTempFile(parent, path.getFileName() + ".invalid-", ".bak");
        try {
            return Files.move(absolutePath, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Files.deleteIfExists(backup);
            throw exception;
        }
    }
}
