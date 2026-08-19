package com.abo47.oresandstuff.data.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.abo47.oresandstuff.OresAndStuffMod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class MinerTierConfig {
    private static final String FOLDER = "miners";
    private static final List<MinerTier> TIERS = new ArrayList<>();
    private static boolean loaded;

    private static MinerTier defaultTier = defaultMk1();

    private MinerTierConfig() {
    }

    public static MinerTier defaultTier() {
        ensureLoaded();
        return defaultTier;
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        generateDefaults();

        TIERS.clear();
        Path minersFolder = ConfigAssets.createFolder(FOLDER);
        try (Stream<Path> entries = Files.list(minersFolder)) {
            for (Path entry : (Iterable<Path>) entries.sorted()::iterator) {
                if (Files.isDirectory(entry)) {
                    loadTierDirectory(entry);
                }
            }
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.error("Failed to scan miner config folder {}", minersFolder, e);
        }
        if (TIERS.isEmpty()) {
            TIERS.add(defaultMk1());
        }
        Map<String, MinerTier> byId = new LinkedHashMap<>();
        for (MinerTier tier : TIERS) {
            byId.put(tier.id(), tier);
        }
        TIERS.clear();
        TIERS.addAll(byId.values());
        RuntimeAssetPack.regenerate();
        OresAndStuffMod.LOGGER.info("Loaded {} miner tier(s)", TIERS.size());
    }

    private static void loadTierDirectory(Path dir) {
        String fallbackId = dir.getFileName().toString();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.getFileName().toString().endsWith(".json"))::iterator) {
                loadTierFile(file, fallbackId);
            }
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.error("Failed to scan miner tier folder {}", dir, e);
        }
    }

    private static void loadTierFile(Path file, String fallbackId) {
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            String id = root.has("id") ? root.get("id").getAsString().trim() : "";
            if (id.isBlank()) {
                id = fallbackId;
            }
            TIERS.add(new MinerTier(
                    id,
                    root.has("display_name") ? root.get("display_name").getAsString() : titleCase(id),
                    intValue(root, "fe_per_tick", 40),
                    intValue(root, "buffer_fe", 40000),
                    intValue(root, "max_receive_fe", 512),
                    stringValue(root, "model", "miner"),
                    stringValue(root, "texture", "")
            ));
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.error("Failed to parse miner config file {}", file, e);
        }
    }

    private static void generateDefaults() {
        Path minersFolder = ConfigAssets.createFolder(FOLDER);
        if (Files.isDirectory(minersFolder.resolve("mk1"))) {
            return;
        }
        try {
            Path tierDir = minersFolder.resolve("mk1");
            Files.createDirectories(tierDir.resolve("models"));
            Files.createDirectories(tierDir.resolve("textures"));
            Files.writeString(tierDir.resolve("mk1.json"), defaultMk1Json(), StandardCharsets.UTF_8);
            Files.writeString(tierDir.resolve("models").resolve("miner.json"),
                    "{\"parent\":\"oresandstuff:block/miner_mk1\"}", StandardCharsets.UTF_8);
            OresAndStuffMod.LOGGER.info("Generated default miner tier folder {}", tierDir);
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.error("Failed to generate default miner tier folder", e);
        }
    }

    public static synchronized List<MinerTier> tiers() {
        ensureLoaded();
        return List.copyOf(TIERS);
    }

    public static synchronized Optional<MinerTier> get(String id) {
        ensureLoaded();
        for (MinerTier tier : TIERS) {
            if (tier.id().equals(id)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }

    public static synchronized String displayName(String id) {
        return get(id).map(MinerTier::displayName).orElseGet(() -> titleCase(id));
    }

    private static int intValue(JsonObject root, String key, int fallback) {
        try {
            return root.has(key) ? root.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String stringValue(JsonObject root, String key, String fallback) {
        return root.has(key) ? root.get(key).getAsString() : fallback;
    }

    private static String defaultMk1Json() {
        JsonObject root = new JsonObject();
        root.addProperty("id", "mk1");
        root.addProperty("display_name", "Miner Mk1");
        root.addProperty("fe_per_tick", 40);
        root.addProperty("buffer_fe", 40000);
        root.addProperty("max_receive_fe", 512);
        root.addProperty("model", "miner");
        root.addProperty("texture", "");
        return ConfigAssets.pretty(root);
    }

    private static MinerTier defaultMk1() {
        return new MinerTier("mk1", "Miner Mk1", 40, 40000, 512, "miner", "");
    }

    private static String titleCase(String raw) {
        String[] parts = raw.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.isEmpty() ? raw : out.toString();
    }

    public record MinerTier(String id, String displayName, int fePerTick, int bufferFe, int maxReceiveFe, String model, String texture) {
    }
}