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

import com.google.gson.JsonArray;
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
        ConfigAssets.generateMissingDefaults(FOLDER, defaultFiles());

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
        ResourceLocation visualPure = null;
        if (root.has("visual_block_pure")) {
            visualPure = ResourceLocation.tryParse(root.get("visual_block_pure").getAsString());
        }

        List<ResourceLocation> dimensions = new ArrayList<>();
        if (root.has("dimensions") && root.get("dimensions").isJsonArray()) {
            for (com.google.gson.JsonElement element : root.getAsJsonArray("dimensions")) {
                ResourceLocation dim = ResourceLocation.tryParse(element.getAsString());
                if (dim != null && !dimensions.contains(dim)) {
                    dimensions.add(dim);
                }
            }
        }
        if (dimensions.isEmpty()) {
            dimensions.add(Level.OVERWORLD.location());
        }

        ResourceLocation nodeBlock = blockModelPath(root.has("node_block")
                ? root.get("node_block").getAsString() : defaultNodeBlock(dimensions, false));
        ResourceLocation nodeBlockPure = blockModelPath(root.has("node_block_pure")
                ? root.get("node_block_pure").getAsString() : defaultNodeBlock(dimensions, true));
        if (nodeBlock == null) {
            nodeBlock = new ResourceLocation("minecraft", "block/stone");
        }
        if (nodeBlockPure == null) {
            nodeBlockPure = new ResourceLocation("minecraft", "block/deepslate");
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

        Map<String, OreNodeType.BiomeOverride> overrides = new LinkedHashMap<>();
        if (root.has("biome_overrides") && root.get("biome_overrides").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> e : root.getAsJsonObject("biome_overrides").entrySet()) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject o = e.getValue().getAsJsonObject();
                overrides.put(e.getKey(), new OreNodeType.BiomeOverride(
                        optionalInt(o, "min_nodes_per_chunk"),
                        optionalInt(o, "max_nodes_per_chunk"),
                        optionalInt(o, "cluster_radius"),
                        optionalInt(o, "scatter_count"),
                        optionalInt(o, "purity_weights.impure"),
                        optionalInt(o, "purity_weights.normal"),
                        optionalInt(o, "purity_weights.pure")
                ));
            }
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

        boolean surfaceSpawn = !root.has("surface_spawn") || root.get("surface_spawn").getAsBoolean();
        int minY = intValue(root, "min_y", 0);
        int maxY = intValue(root, "max_y", 63);

        List<OreNodeType.OreNodeDrop> drops = new ArrayList<>();
        if (root.has("drops") && root.get("drops").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> e : root.getAsJsonObject("drops").entrySet()) {
                ResourceLocation item = ResourceLocation.tryParse(e.getKey());
                if (item != null) {
                    drops.add(new OreNodeType.OreNodeDrop(item, Math.max(1, e.getValue().getAsInt())));
                }
            }
        }
        if (drops.isEmpty()) {
            drops.add(new OreNodeType.OreNodeDrop(output, 100));
        }

        return new OreNodeType(id, output, baseRate, color, hardness, enabled, visual, visualPure, nodeBlock, nodeBlockPure,
                dimensions,
                biomes, minNodes, maxNodes, impure, normal, pure, spacing, attempts, scanRadius, clusterRadius, scatter,
                surfaceSpawn, minY, maxY, drops, overrides);
    }

    public List<OreNodeType> typesForDimension(ResourceLocation dimension) {
        ensureLoaded();
        return nodeTypes.values().stream()
                .filter(t -> t.matchesDimension(dimension))
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

    public Purity rollPurity(RandomSource random, OreNodeType type, String biomeName) {
        int impure = type.effectiveImpureWeight(biomeName);
        int normal = type.effectiveNormalWeight(biomeName);
        int pure = type.effectivePureWeight(biomeName);
        int total = impure + normal + pure;
        if (total <= 0) {
            return Purity.NORMAL;
        }
        int v = random.nextInt(total);
        if (v < impure) {
            return Purity.IMPURE;
        }
        if (v < impure + normal) {
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
        for (DefaultNode node : DEFAULT_NODES) {
            files.put(node.fileName(), nodeJson(node));
        }
        return files;
    }

    /**
     * Host rock the ore node block mimics when the config does not specify one:
     * netherrack in the nether, end stone in the end, stone in the overworld.
     */
    private static String defaultNodeBlock(List<ResourceLocation> dimensions, boolean pure) {
        if (dimensions.contains(Level.NETHER.location())) {
            return pure ? "minecraft:basalt" : "minecraft:netherrack";
        }
        if (dimensions.contains(Level.END.location())) {
            return pure ? "minecraft:end_stone_bricks" : "minecraft:end_stone";
        }
        return pure ? "minecraft:deepslate" : "minecraft:stone";
    }

    /**
     * Maps a block id (e.g. {@code minecraft:netherrack}) to its block model
     * path ({@code minecraft:block/netherrack}). The generated ore node model
     * then parents that model, so any game block can be used as the node look.
     */
    private static ResourceLocation blockModelPath(String blockId) {
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id == null) {
            return null;
        }
        return new ResourceLocation(id.getNamespace(), "block/" + id.getPath());
    }

    private static final List<DefaultNode> DEFAULT_NODES = List.of(
            new DefaultNode("oresandstuff:coal", "minecraft:coal", 0.9, "#2E2E2E", "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "swamp:55,forest:45,plains:40,jungle:30,savanna:20,snowy:25,windswept:20,meadow:25,desert:10,ice:25,grove:30,mushroom:25,beach:10,river:15",
                    2, 3, 35, 45, 20, 200, 1, 192, 2, 8, true, 0, 63, 60.0, "coal.json"),
            new DefaultNode("oresandstuff:iron", "minecraft:raw_iron", 0.6, "#D8D8D8", "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "windswept:50,snowy_slopes:45,peak:40,meadow:30,plains:30,forest:25,desert:20,savanna:15,swamp:10,jungle:10,ice:35,grove:20,mushroom:15,beach:35,shore:30,river:20",
                    2, 3, 25, 50, 25, 220, 1, 192, 2, 8, true, 0, 63, 80.0, "iron.json"),
            new DefaultNode("oresandstuff:copper", "minecraft:raw_copper", 0.7, "#C97142", "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "desert:50,badlands:55,windswept:35,peak:30,savanna:20,plains:15,forest:15,swamp:5,grove:15,mushroom:15,beach:20,shore:25",
                    1, 2, 30, 45, 25, 240, 1, 192, 2, 8, true, 0, 63, 70.0, "copper.json"),
            new DefaultNode("oresandstuff:gold", "minecraft:raw_gold", 0.5, "#F2C94C", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "windswept:45,desert:40,badlands:45,peak:35,snowy_slopes:30,savanna:15,plains:15,jungle:10,grove:10,mushroom:10,beach:15,shore:10",
                    1, 2, 25, 50, 25, 260, 1, 192, 2, 8, true, 0, 63, 50.0, "gold.json"),
            new DefaultNode("oresandstuff:redstone", "minecraft:redstone", 0.4, "#D12222", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "badlands:45,desert:20,peak:20,windswept:15,plains:10,ocean:15,cave:35,deep_dark:45,river:10",
                    0, 1, 45, 40, 15, 300, 1, 192, 2, 6, false, -60, -10, 90.0, "redstone.json"),
            new DefaultNode("oresandstuff:diamond", "minecraft:diamond", 0.3, "#4DE1E1", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "plains:30,forest:25,taiga:25,snowy:20,windswept:20,desert:10,swamp:5,river:25,ocean:20,cave:30,deep_dark:20",
                    0, 1, 30, 45, 25, 320, 1, 192, 2, 6, false, -60, -20, 200.0, "diamond.json"),
            new DefaultNode("oresandstuff:emerald", "minecraft:emerald", 0.25, "#3ECF6E", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "peak:70,windswept:55,snowy_slopes:45,meadow:25,desert:10,jungle:10,cave:15",
                    0, 1, 30, 45, 25, 320, 1, 192, 2, 6, false, -60, -15, 180.0, "emerald.json"),
            new DefaultNode("oresandstuff:lapis", "minecraft:lapis_lazuli", 0.35, "#2E6BD8", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "desert:45,badlands:30,savanna:30,plains:20,jungle:10,ocean:25,cave:30,river:10",
                    0, 1, 40, 40, 20, 280, 1, 192, 2, 6, false, -60, -10, 100.0, "lapis.json"),
            new DefaultNode("oresandstuff:nether_quartz", "minecraft:quartz", 0.7, "#E8DFD6", "minecraft:nether_quartz_ore", "minecraft:quartz_block",
                    "minecraft:netherrack", "minecraft:nether_quartz_ore",
                    "minecraft:the_nether",
                    "nether_wastes:50,basalt_deltas:45,crimson_forest:25,warped_forest:25,soul_sand_valley:20",
                    1, 2, 30, 45, 25, 200, 1, 192, 2, 8, true, 0, 63, 80.0, "nether_quartz.json"),
            new DefaultNode("oresandstuff:nether_gold", "minecraft:gold_nugget", 0.5, "#E8B01C", "minecraft:nether_gold_ore", "minecraft:gold_block",
                    "minecraft:netherrack", "minecraft:nether_gold_ore",
                    "minecraft:the_nether",
                    "nether_wastes:55,basalt_deltas:45,soul_sand_valley:15,crimson_forest:10,warped_forest:10",
                    1, 2, 25, 50, 25, 220, 1, 192, 2, 8, true, 0, 63, 60.0, "nether_gold.json"),
            new DefaultNode("oresandstuff:end_diamond", "minecraft:diamond", 0.3, "#4DE1E1", "minecraft:diamond_ore", "minecraft:diamond_block",
                    "minecraft:end_stone", "minecraft:diamond_block",
                    "minecraft:the_end",
                    "end_highlands:30,end_midlands:25,the_end:20,small_end_islands:15,end_barrens:15",
                    0, 1, 30, 45, 25, 240, 1, 192, 2, 6, true, 0, 63, 200.0, "end_diamond.json"),
            new DefaultNode("oresandstuff:end_emerald", "minecraft:emerald", 0.25, "#3ECF6E", "minecraft:emerald_ore", "minecraft:emerald_block",
                    "minecraft:end_stone", "minecraft:emerald_block",
                    "minecraft:the_end",
                    "end_highlands:25,end_midlands:30,the_end:20,small_end_islands:15,end_barrens:15",
                    0, 1, 30, 45, 25, 280, 1, 192, 2, 6, true, 0, 63, 180.0, "end_emerald.json")
    );

    private record DefaultNode(String id, String output, double rate, String color, String visual, String visualPure,
                               String nodeBlock, String nodeBlockPure,
                               String dimensionsCsv, String biomesCsv, int minNodes, int maxNodes,
                               int impure, int normal, int pure, int spacing, int attempts,
                               int scannerRadius, int clusterRadius, int scatterCount,
                               boolean surfaceSpawn, int minY, int maxY, double hardness, String fileName) {
    }

    private static String nodeJson(DefaultNode node) {
        JsonObject root = new JsonObject();
        root.addProperty("id", node.id());
        root.addProperty("output_item", node.output());
        JsonObject drops = new JsonObject();
        drops.addProperty(node.output(), 100);
        root.add("drops", drops);
        root.addProperty("base_rate_per_second", node.rate());
        root.addProperty("scanner_color", node.color());
        root.addProperty("hardness", node.hardness());
        root.addProperty("enabled", true);
        root.addProperty("visual_block", node.visual());
        root.addProperty("visual_block_pure", node.visualPure());
        root.addProperty("node_block", node.nodeBlock());
        root.addProperty("node_block_pure", node.nodeBlockPure());
        JsonArray dimensions = new JsonArray();
        for (String part : node.dimensionsCsv().split(",")) {
            dimensions.add(part.trim());
        }
        root.add("dimensions", dimensions);
        JsonObject biomes = new JsonObject();
        for (String part : node.biomesCsv().split(",")) {
            int lastColon = part.lastIndexOf(':');
            biomes.addProperty(part.substring(0, lastColon), Integer.parseInt(part.substring(lastColon + 1)));
        }
        root.add("biomes", biomes);
        root.add("biome_overrides", new JsonObject());
        root.addProperty("min_nodes_per_chunk", node.minNodes());
        root.addProperty("max_nodes_per_chunk", node.maxNodes());
        JsonObject purity = new JsonObject();
        purity.addProperty("impure", node.impure());
        purity.addProperty("normal", node.normal());
        purity.addProperty("pure", node.pure());
        root.add("purity_weights", purity);
        root.addProperty("min_spacing_blocks", node.spacing());
        root.addProperty("placement_attempts", node.attempts());
        root.addProperty("scanner_radius", node.scannerRadius());
        root.addProperty("cluster_radius", node.clusterRadius());
        root.addProperty("scatter_count", node.scatterCount());
        root.addProperty("surface_spawn", node.surfaceSpawn());
        root.addProperty("min_y", node.minY());
        root.addProperty("max_y", node.maxY());
        return ConfigAssets.pretty(root);
    }

    private static Map<ResourceLocation, OreNodeType> fallbackDefaults() {
        Map<ResourceLocation, OreNodeType> out = new HashMap<>();
        for (DefaultNode node : DEFAULT_NODES) {
            OreNodeType type = parseNodeType(JsonParser.parseString(nodeJson(node)).getAsJsonObject());
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

    private static Integer optionalInt(JsonObject root, String path) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject(parts[i]);
        }
        return current.has(parts[parts.length - 1]) ? current.get(parts[parts.length - 1]).getAsInt() : null;
    }

    private static double doubleValue(JsonObject root, String key, double fallback) {
        return root.has(key) ? root.get(key).getAsDouble() : fallback;
    }

    private static String colorValue(JsonObject root) {
        String raw = root.has("scanner_color") ? root.get("scanner_color").getAsString() : "#FFFFFF";
        return raw.replace("#", "0x");
    }
}