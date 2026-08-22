package com.abo47.oresandstuff.data.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.platform.Services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ConfigAssets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigAssets() {
    }

    public static Path folder(String sub) {
        return Services.platform().configDir().resolve(OresAndStuffMod.MOD_ID).resolve(sub);
    }

    public static Path createFolder(String sub) {
        Path folder = folder(sub);
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            OresAndStuffMod.LOGGER.error("Failed to create config folder {}", folder, e);
        }
        return folder;
    }

    public static void generateDefaults(String sub, Map<String, String> defaults) {
        Path folder = createFolder(sub);
        try (Stream<Path> files = Files.list(folder)) {
            if (files.findAny().isPresent()) {
                return;
            }
        } catch (IOException ignored) {
        }
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            try {
                Files.writeString(folder.resolve(entry.getKey()), entry.getValue(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                OresAndStuffMod.LOGGER.error("Failed to write default config file {} in {}", entry.getKey(), folder, e);
            }
        }
        OresAndStuffMod.LOGGER.info("Generated {} default config file(s) in {}", defaults.size(), folder);
    }

    /**
     * Writes only the default files that do not exist yet. Existing (possibly
     * player-edited) files are left untouched, unlike {@link #generateDefaults}.
     */
    public static void generateMissingDefaults(String sub, Map<String, String> defaults) {
        Path folder = createFolder(sub);
        int written = 0;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            Path file = folder.resolve(entry.getKey());
            if (Files.isRegularFile(file)) {
                continue;
            }
            try {
                Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
                written++;
            } catch (IOException e) {
                OresAndStuffMod.LOGGER.error("Failed to write default config file {} in {}", entry.getKey(), folder, e);
            }
        }
        if (written > 0) {
            OresAndStuffMod.LOGGER.info("Generated {} missing default config file(s) in {}", written, folder);
        }
    }

    public static List<Path> listJson(String sub) {
        Path folder = createFolder(sub);
        List<Path> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(out::add);
        } catch (IOException e) {
            OresAndStuffMod.LOGGER.error("Failed to list config folder {}", folder, e);
        }
        return out;
    }

    public static String pretty(com.google.gson.JsonObject object) {
        return GSON.toJson(object);
    }
}