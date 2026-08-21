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

    private static final List<MinerTier> DEFAULT_TIERS = List.of(
            new MinerTier("mk1", "Miner Mk1", 40, 40000, 160, 1.0, "miner", ""),
            new MinerTier("mk2", "Miner Mk2", 80, 80000, 320, 2.0, "miner", ""),
            new MinerTier("mk3", "Miner Mk3", 120, 120000, 480, 3.0, "miner", ""),
            new MinerTier("mk4", "Miner Mk4", 200, 200000, 800, 5.0, "miner", ""),
            new MinerTier("mk5", "Miner Mk5", 280, 280000, 1120, 7.0, "miner", ""),
            new MinerTier("mk6", "Miner Mk6", 400, 400000, 1600, 10.0, "miner", ""),
            new MinerTier("mk7", "Miner Mk7", 560, 560000, 2240, 14.0, "miner", ""),
            new MinerTier("mk8", "Miner Mk8", 760, 760000, 3040, 19.0, "miner", ""),
            new MinerTier("mk9", "Miner Mk9", 1000, 1000000, 4000, 25.0, "miner", ""),
            new MinerTier("mk10", "Miner Mk10", 1280, 1280000, 5120, 32.0, "miner", "")
    );

    private static MinerTier defaultTier = DEFAULT_TIERS.get(0);

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
            TIERS.addAll(DEFAULT_TIERS);
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
                    doubleValue(root, "rate_multiplier", 1.0),
                    stringValue(root, "model", "miner"),
                    stringValue(root, "texture", "")
            ));
        } catch (Exception e) {
            OresAndStuffMod.LOGGER.error("Failed to parse miner config file {}", file, e);
        }
    }

    private static void generateDefaults() {
        Path minersFolder = ConfigAssets.createFolder(FOLDER);
        for (MinerTier tier : DEFAULT_TIERS) {
            Path tierDir = minersFolder.resolve(tier.id());
            if (Files.isDirectory(tierDir)) {
                continue;
            }
            try {
                Files.createDirectories(tierDir.resolve("models"));
                Files.createDirectories(tierDir.resolve("textures"));
                Files.writeString(tierDir.resolve(tier.id() + ".json"), tierJson(tier), StandardCharsets.UTF_8);
                Files.writeString(tierDir.resolve("models").resolve("miner.json"),
                        "{\"parent\":\"oresandstuff:block/miner_mk1\"}", StandardCharsets.UTF_8);
                OresAndStuffMod.LOGGER.info("Generated default miner tier folder {}", tierDir);
            } catch (Exception e) {
                OresAndStuffMod.LOGGER.error("Failed to generate default miner tier folder {}", tierDir, e);
            }
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

    private static double doubleValue(JsonObject root, String key, double fallback) {
        try {
            return root.has(key) ? root.get(key).getAsDouble() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String defaultMk1Json() {
        return tierJson(DEFAULT_TIERS.get(0));
    }

    private static MinerTier defaultMk1() {
        return DEFAULT_TIERS.get(0);
    }

    private static String tierJson(MinerTier tier) {
        JsonObject root = new JsonObject();
        root.addProperty("id", tier.id());
        root.addProperty("display_name", tier.displayName());
        root.addProperty("fe_per_tick", tier.fePerTick());
        root.addProperty("buffer_fe", tier.bufferFe());
        root.addProperty("max_receive_fe", tier.maxReceiveFe());
        root.addProperty("rate_multiplier", tier.rateMultiplier());
        root.addProperty("model", tier.model());
        root.addProperty("texture", tier.texture());
        return ConfigAssets.pretty(root);
    }

    public static List<MinerTier> defaultTiers() {
        return DEFAULT_TIERS;
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

    public record MinerTier(String id, String displayName, int fePerTick, int bufferFe, int maxReceiveFe, double rateMultiplier, String model, String texture) {
    }
}