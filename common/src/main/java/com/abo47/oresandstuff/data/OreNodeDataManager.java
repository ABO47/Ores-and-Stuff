package com.abo47.oresandstuff.data;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.data.config.ConfigAssets;
import com.abo47.oresandstuff.node.OreNodeType;
import com.abo47.oresandstuff.node.Purity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class OreNodeDataManager {
    public static final OreNodeDataManager INSTANCE = new OreNodeDataManager();

    private static final String FOLDER = "orenodes";
    private final Map<ResourceLocation, OreNodeType> nodeTypes = new HashMap<>();
    private boolean loaded;

    private OreNodeDataManager() {
    }

    public synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        ConfigAssets.generateDefaults(FOLDER, defaultFiles());

        Map<ResourceLocation, OreNodeType> parsed = new HashMap<>();
        for (Path file : ConfigAssets.listJson(FOLDER)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                OreNodeType type = parseNodeType(root);
                if (type != null) {
                    parsed.put(type.id(), type);
                }
            } catch (Exception e) {
                OresAndStuffMod.LOGGER.error("Failed to parse ore node config file {}", file, e);
            }
        }
        if (parsed.isEmpty()) {
            parsed.putAll(fallbackDefaults());
        }
        nodeTypes.clear();
        nodeTypes.putAll(parsed);
        OresAndStuffMod.LOGGER.info("Loaded {} ore node type(s)", nodeTypes.size());
    }

    private static OreNodeType parseNodeType(JsonObject root) {
        ResourceLocation id = root.has("id") ? ResourceLocation.tryParse(root.get("id").getAsString()) : null;
        if (id == null) {
            return null;
        }
        String outputRaw = root.has("output_item") ? root.get("output_item").getAsString() : "minecraft:" + id.getPath() + "_ore";
        ResourceLocation output = ResourceLocation.tryParse(outputRaw);
        if (output == null) {
            return null;
        }
        ResourceLocation visual = ResourceLocation.tryParse(
                root.has("visual_block") ? root.get("visual_block").getAsString() : "minecraft:" + id.getPath() + "_ore");
        if (visual == null) {
            visual = new ResourceLocation("minecraft", "stone");
        }
        ResourceLocation dimension = ResourceLocation.tryParse(
                root.has("dimension") ? root.get("dimension").getAsString() : Level.OVERWORLD.location().toString());
        if (dimension == null) {
            dimension = Level.OVERWORLD.location();
        }

        double baseRate = doubleValue(root, "base_rate_per_second", 0.2D);
        int color = Integer.decode(colorValue(root));
        float hardness = (float) doubleValue(root, "hardness", 100.0D);
        boolean enabled = !root.has("enabled") || root.get("enabled").getAsBoolean();

        Map<String, Integer> biomes = new LinkedHashMap<>();
        if (root.has("biomes") && root.get("biomes").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> e : root.getAsJsonObject("biomes").entrySet()) {
                biomes.put(e.getKey(), e.getValue().getAsInt());
            }
        }
        if (biomes.isEmpty()) {
            biomes.put("minecraft:plains", 30);
        }

        int minNodes = intValue(root, "min_nodes_per_chunk", 1);
        int maxNodes = intValue(root, "max_nodes_per_chunk", 2);
        int impure = intValue(root, "purity_weights.impure", 25);
        int normal = intValue(root, "purity_weights.normal", 50);
        int pure = intValue(root, "purity_weights.pure", 25);

        int spacing = intValue(root, "min_spacing_blocks", 220);
        int attempts = intValue(root, "placement_attempts", 1);
        int scanRadius = intValue(root, "scanner_radius", 192);
        int clusterRadius = Math.max(1, intValue(root, "cluster_radius", 2));
        int scatter = intValue(root, "scatter_count", 8);

        return new OreNodeType(id, output, baseRate, color, hardness, enabled, visual, dimension,
                biomes, minNodes, maxNodes, impure, normal, pure, spacing, attempts, scanRadius, clusterRadius, scatter);
    }

    public List<OreNodeType> typesForDimension(ResourceLocation dimension) {
        ensureLoaded();
        return nodeTypes.values().stream()
                .filter(t -> t.dimension().equals(dimension))
                .filter(t -> t.enabledByDefault() && !disabledNodeIds().contains(t.id()))
                .toList();
    }

    public List<OreNodeType> nodeTypes() {
        ensureLoaded();
        return new ArrayList<>(nodeTypes.values());
    }

    public Optional<OreNodeType> getNodeType(ResourceLocation id) {
        ensureLoaded();
        return Optional.ofNullable(nodeTypes.get(id));
    }

    public OreNodeType getAnyNodeType() {
        ensureLoaded();
        List<OreNodeType> enabled = nodeTypes.values().stream()
                .filter(t -> t.enabledByDefault() && !disabledNodeIds().contains(t.id()))
                .toList();
        if (!enabled.isEmpty()) {
            return enabled.get(0);
        }
        return fallbackDefaults().values().stream().findFirst().orElseThrow();
    }

    public List<ResourceLocation> orderedTypeIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (OreNodeType type : nodeTypes()) {
            if (type.enabledByDefault() && !disabledNodeIds().contains(type.id())) {
                ids.add(type.id());
            }
        }
        ids.sort((a, b) -> a.toString().compareTo(b.toString()));
        return ids;
    }

    public OreNodeType rollNodeType(RandomSource random, String biomeName, List<OreNodeType> candidates) {
        if (candidates.isEmpty()) {
            return getAnyNodeType();
        }
        int total = 0;
        Map<OreNodeType, Integer> weights = new LinkedHashMap<>();
        for (OreNodeType type : candidates) {
            int w = type.biomeWeight(biomeName);
            if (w > 0) {
                weights.put(type, w);
                total += w;
            }
        }
        if (total <= 0) {
            return getAnyNodeType();
        }
        int v = random.nextInt(total);
        int cursor = 0;
        for (Map.Entry<OreNodeType, Integer> entry : weights.entrySet()) {
            cursor += entry.getValue();
            if (v < cursor) {
                return entry.getKey();
            }
        }
        return candidates.get(0);
    }

    public Purity rollPurity(RandomSource random, OreNodeType type) {
        int total = type.impureWeight() + type.normalWeight() + type.pureWeight();
        if (total <= 0) {
            return Purity.NORMAL;
        }
        int v = random.nextInt(total);
        if (v < type.impureWeight()) {
            return Purity.IMPURE;
        }
        if (v < type.impureWeight() + type.normalWeight()) {
            return Purity.NORMAL;
        }
        return Purity.PURE;
    }

    private Set<ResourceLocation> disabledNodeIds() {
        Set<ResourceLocation> out = new HashSet<>();
        for (String s : OresAndStuffConfig.worldgen().disabledNodeTypes) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    private static Map<String, String> defaultFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("iron.json", defaultJson("oresandstuff:iron", "minecraft:raw_iron", 0.6, "#D8D8D8", "minecraft:iron_ore",
                "minecraft:plains:30,mountain:45,swamp:15,desert:30", 1, 2, 25, 50, 25));
        files.put("copper.json", defaultJson("oresandstuff:copper", "minecraft:raw_copper", 0.7, "#C97142", "minecraft:copper_ore",
                "minecraft:plains:30,mountain:20,swamp:20,desert:35", 1, 2, 25, 50, 25));
        files.put("coal.json", defaultJson("oresandstuff:coal", "minecraft:coal", 0.9, "#2E2E2E", "minecraft:coal_ore",
                "minecraft:plains:25,mountain:20,swamp:55,desert:25", 1, 2, 35, 45, 20));
        files.put("redstone.json", defaultJson("oresandstuff:redstone", "minecraft:redstone", 0.4, "#D12222", "minecraft:redstone_ore",
                "minecraft:plains:15,mountain:15,swamp:10,desert:10", 0, 1, 45, 40, 15));
        return files;
    }

    private static String defaultJson(String id, String output, double rate, String color, String visual,
                                      String biomesCsv, int minNodes, int maxNodes, int impure, int normal, int pure) {
        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("output_item", output);
        root.addProperty("base_rate_per_second", rate);
        root.addProperty("scanner_color", color);
        root.addProperty("hardness", 100.0);
        root.addProperty("enabled", true);
        root.addProperty("visual_block", visual);
        root.addProperty("dimension", Level.OVERWORLD.location().toString());
        JsonObject biomes = new JsonObject();
        for (String part : biomesCsv.split(",")) {
            int lastColon = part.lastIndexOf(':');
            biomes.addProperty(part.substring(0, lastColon), Integer.parseInt(part.substring(lastColon + 1)));
        }
        root.add("biomes", biomes);
        root.addProperty("min_nodes_per_chunk", minNodes);
        root.addProperty("max_nodes_per_chunk", maxNodes);
        JsonObject purity = new JsonObject();
        purity.addProperty("impure", impure);
        purity.addProperty("normal", normal);
        purity.addProperty("pure", pure);
        root.add("purity_weights", purity);
        root.addProperty("min_spacing_blocks", 220);
        root.addProperty("placement_attempts", 1);
        root.addProperty("scanner_radius", 192);
        root.addProperty("cluster_radius", 2);
        root.addProperty("scatter_count", 8);
        return ConfigAssets.pretty(root);
    }

    private static Map<ResourceLocation, OreNodeType> fallbackDefaults() {
        Map<ResourceLocation, OreNodeType> out = new HashMap<>();
        for (OreNodeType type : List.of(
                parseNodeType(JsonParser.parseString(defaultJson("oresandstuff:iron", "minecraft:raw_iron", 0.6, "#D8D8D8", "minecraft:iron_ore",
                        "minecraft:plains:30,mountain:45,swamp:15,desert:30", 1, 2, 25, 50, 25)).getAsJsonObject()),
                parseNodeType(JsonParser.parseString(defaultJson("oresandstuff:copper", "minecraft:raw_copper", 0.7, "#C97142", "minecraft:copper_ore",
                        "minecraft:plains:30,mountain:20,swamp:20,desert:35", 1, 2, 25, 50, 25)).getAsJsonObject()),
                parseNodeType(JsonParser.parseString(defaultJson("oresandstuff:coal", "minecraft:coal", 0.9, "#2E2E2E", "minecraft:coal_ore",
                        "minecraft:plains:25,mountain:20,swamp:55,desert:25", 1, 2, 35, 45, 20)).getAsJsonObject()),
                parseNodeType(JsonParser.parseString(defaultJson("oresandstuff:redstone", "minecraft:redstone", 0.4, "#D12222", "minecraft:redstone_ore",
                        "minecraft:plains:15,mountain:15,swamp:10,desert:10", 0, 1, 45, 40, 15)).getAsJsonObject()))) {
            if (type != null) {
                out.put(type.id(), type);
            }
        }
        return out;
    }

    private static int intValue(JsonObject root, String path, int fallback) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                return fallback;
            }
            current = current.getAsJsonObject(parts[i]);
        }
        return current.has(parts[parts.length - 1]) ? current.get(parts[parts.length - 1]).getAsInt() : fallback;
    }

    private static double doubleValue(JsonObject root, String key, double fallback) {
        return root.has(key) ? root.get(key).getAsDouble() : fallback;
    }

    private static String colorValue(JsonObject root) {
        String raw = root.has("scanner_color") ? root.get("scanner_color").getAsString() : "#FFFFFF";
        return raw.replace("#", "0x");
    }
}