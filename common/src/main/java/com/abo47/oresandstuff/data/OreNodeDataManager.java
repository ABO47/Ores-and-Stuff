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
import com.abo47.oresandstuff.node.NodeQuality;
import com.abo47.oresandstuff.node.OreNodeType;

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

        int minNodes = intValue(root, "min_nodes_per_chunk", 1);
        int maxNodes = intValue(root, "max_nodes_per_chunk", 2);
        double qualityMin = doubleValue(root, "quality_min", 75.0);
        double qualityMax = doubleValue(root, "quality_max", 125.0);
        if (qualityMax < qualityMin) {
            double tmp = qualityMin;
            qualityMin = qualityMax;
            qualityMax = tmp;
        }

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
                        optionalDouble(o, "quality_min"),
                        optionalDouble(o, "quality_max"),
                        optionalInt(o, "min_y"),
                        optionalInt(o, "max_y"),
                        optionalBoolean(o, "surface_spawn"),
                        optionalInt(o, "placement_attempts"),
                        optionalInt(o, "min_spacing_blocks")
                ));
            }
        }

        List<OreNodeType.QualityTier> tiers = parseQualityTiers(root, id, dimensions, qualityMin, qualityMax);
        if (tiers == null) {
            return null;
        }

        double baseRate = doubleValue(root, "base_rate_per_second", 0.2D);
        int color = Integer.decode(colorValue(root));
        float hardness = (float) doubleValue(root, "hardness", 100.0D);
        boolean enabled = !root.has("enabled") || root.get("enabled").getAsBoolean();

        int maxMiners = Math.max(1, intValue(root, "max_miners_per_node", 1));
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
                    drops.add(new OreNodeType.OreNodeDrop(item, Math.max(0, Math.min(100, e.getValue().getAsInt()))));
                }
            }
        }
        if (drops.isEmpty()) {
            drops = defaultDrops(output, tiers);
        }

        return new OreNodeType(id, output, baseRate, color, hardness, enabled, tiers, dimensions,
                biomes, minNodes, maxNodes, qualityMin, qualityMax, maxMiners, spacing, attempts, scanRadius, clusterRadius, scatter,
                surfaceSpawn, minY, maxY, drops, overrides);
    }

    /**
     * Parses the {@code quality_visuals} array: quality ranges mapped to the
     * blocks they render as, e.g.
     * [{"min":1,"max":30,"node_block":"minecraft:stone","visual_block":"minecraft:coal_ore"}, ...].
     * Every tier range must lie inside the node's quality_min/quality_max -
     * invalid ranges reject the whole json (null). Falls back to the legacy
     * single-tier fields (node_block/visual_block) when the array is missing.
     */
    private static List<OreNodeType.QualityTier> parseQualityTiers(JsonObject root, ResourceLocation id, List<ResourceLocation> dimensions,
            double qualityMin, double qualityMax) {
        List<OreNodeType.QualityTier> tiers = new ArrayList<>();
        if (root.has("quality_visuals") && root.get("quality_visuals").isJsonArray()) {
            for (com.google.gson.JsonElement element : root.getAsJsonArray("quality_visuals")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                double min = doubleValue(o, "min", NodeQuality.MIN);
                double max = doubleValue(o, "max", NodeQuality.MAX);
                if (min >= max || min < qualityMin || max > qualityMax) {
                    OresAndStuffMod.LOGGER.warn("Ore node {} rejected: quality_visuals tier [{},{}] outside quality range [{},{}]",
                            id, min, max, qualityMin, qualityMax);
                    return null;
                }
                String nodeBlockRaw = o.has("node_block") ? o.get("node_block").getAsString() : null;
                String visualRaw = o.has("visual_block") ? o.get("visual_block").getAsString() : null;
                ResourceLocation nodeModel = nodeBlockRaw != null ? blockModelPath(nodeBlockRaw) : null;
                ResourceLocation visual = visualRaw != null ? ResourceLocation.tryParse(visualRaw) : null;
                if (nodeModel == null) {
                    nodeModel = new ResourceLocation("minecraft", "block/stone");
                }
                if (visual == null) {
                    visual = new ResourceLocation("minecraft", id.getPath() + "_ore");
                }
                tiers.add(new OreNodeType.QualityTier(min, max, nodeModel, visual));
            }
        }
        if (tiers.isEmpty()) {
            ResourceLocation nodeModel = blockModelPath(root.has("node_block")
                    ? root.get("node_block").getAsString() : defaultNodeBlock(dimensions, false));
            ResourceLocation visual = ResourceLocation.tryParse(root.has("visual_block")
                    ? root.get("visual_block").getAsString() : "minecraft:" + id.getPath() + "_ore");
            tiers.add(new OreNodeType.QualityTier(qualityMin, qualityMax,
                    nodeModel != null ? nodeModel : new ResourceLocation("minecraft", "block/stone"),
                    visual != null ? visual : new ResourceLocation("minecraft", id.getPath() + "_ore")));
        }
        return tiers;
    }

    /**
     * Default drops when the json has no drops object: the output item plus
     * the node block of every quality tier (stone, deepslate, ...).
     */
    private static List<OreNodeType.OreNodeDrop> defaultDrops(ResourceLocation output, List<OreNodeType.QualityTier> tiers) {
        List<OreNodeType.OreNodeDrop> drops = new ArrayList<>();
        drops.add(new OreNodeType.OreNodeDrop(output, 100));
        for (OreNodeType.QualityTier tier : tiers) {
            ResourceLocation item = itemIdFromModel(tier.nodeBlockModel());
            if (item != null && !item.equals(output)) {
                drops.add(new OreNodeType.OreNodeDrop(item, 100));
            }
        }
        return drops;
    }

    private static ResourceLocation itemIdFromModel(ResourceLocation model) {
        String path = model.getPath();
        if (path.startsWith("block/")) {
            return new ResourceLocation(model.getNamespace(), path.substring("block/".length()));
        }
        return null;
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

    /**
     * Rolls a quality percentage for a node, uniformly distributed inside the
     * type's effective quality range for the biome (e.g. 23%..35%). The value
     * is the output multiplier: 34% quality = 0.34x yield.
     */
    public double rollQuality(RandomSource random, OreNodeType type, String biomeName) {
        double min = NodeQuality.clamp(type.effectiveQualityMin(biomeName));
        double max = NodeQuality.clamp(type.effectiveQualityMax(biomeName));
        if (max < min) {
            double tmp = min;
            min = max;
            max = tmp;
        }
        return Math.round((min + random.nextDouble() * (max - min)) * 10.0) / 10.0;
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
                    "swamp:55,forest:45,plains:40,jungle:30,savanna:20,snowy:25,windswept:20,meadow:25,desert:10,ice:25,grove:30,mushroom:25,beach:10,river:15", "swamp:60:160:0:40:2:true:1:200,mushroom:30:120:-50:10:1:false:2:160,desert:20:90:0:50",
                    2, 3, 60, 160, 1, 200, 1, 192, 2, 8, true, 0, 63, 60.0, "coal.json"),
            new DefaultNode("oresandstuff:iron", "minecraft:raw_iron", 0.6, "#D8D8D8", "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "windswept:50,snowy_slopes:45,peak:40,meadow:30,plains:30,forest:25,desert:20,savanna:15,swamp:10,jungle:10,ice:35,grove:20,mushroom:15,beach:35,shore:30,river:20", "snowy_slopes:70:170:0:80:2:true:1:180,beach:20:90:-30:20:1:false:2:120",
                    2, 3, 70, 150, 1, 220, 1, 192, 2, 8, true, 0, 63, 80.0, "iron.json"),
            new DefaultNode("oresandstuff:copper", "minecraft:raw_copper", 0.7, "#C97142", "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "desert:50,badlands:55,windswept:35,peak:30,savanna:20,plains:15,forest:15,swamp:5,grove:15,mushroom:15,beach:20,shore:25", "badlands:50:150:0:40:3:true:1:200,peak:40:160:-40:40:1:false:3:140",
                    1, 2, 60, 140, 1, 240, 1, 192, 2, 8, true, 0, 63, 70.0, "copper.json"),
            new DefaultNode("oresandstuff:gold", "minecraft:raw_gold", 0.5, "#F2C94C", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "windswept:45,desert:40,badlands:45,peak:35,snowy_slopes:30,savanna:15,plains:15,jungle:10,grove:10,mushroom:10,beach:15,shore:10", "windswept:80:160:0:70:1:true:1:200,jungle:20:90:-40:30:1:false:2:150",
                    1, 2, 80, 160, 1, 260, 1, 192, 2, 8, true, 0, 63, 50.0, "gold.json"),
            new DefaultNode("oresandstuff:redstone", "minecraft:redstone", 0.4, "#D12222", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "badlands:45,desert:20,peak:20,windswept:15,plains:10,ocean:15,cave:35,deep_dark:45,river:10", "cave:50:160:-60:-5:2:false:2:240,deep_dark:80:180:-60:-10:3:false:1:200",
                    0, 1, 50, 140, 1, 300, 1, 192, 2, 6, false, -60, -10, 90.0, "redstone.json"),
            new DefaultNode("oresandstuff:diamond", "minecraft:diamond", 0.3, "#4DE1E1", "minecraft:diamond_ore", "minecraft:diamond_ore",
                    "minecraft:stone", "minecraft:end_stone",
                    "minecraft:overworld,minecraft:the_end",
                    "plains:30,forest:25,taiga:25,snowy:20,windswept:20,desert:10,swamp:5,river:25,ocean:20,cave:30,deep_dark:20,end_highlands:30,end_midlands:25,the_end:20,small_end_islands:15,end_barrens:15",
                    "plains:20:80,forest:20:80,taiga:20:80,snowy:20:80,windswept:20:80,desert:20:80,swamp:20:80,river:20:80,ocean:20:80,cave:20:80,deep_dark:20:80,end_highlands:130:200:0:90,end_midlands:130:200:0:90,the_end:130:200:0:90,small_end_islands:130:200:0:90,end_barrens:130:200:0:90",
                    0, 1, 20, 200, 1, 320, 1, 192, 2, 6, true, 0, 63, 200.0, "diamond.json"),
            new DefaultNode("oresandstuff:emerald", "minecraft:emerald", 0.25, "#3ECF6E", "minecraft:emerald_ore", "minecraft:emerald_ore",
                    "minecraft:stone", "minecraft:end_stone",
                    "minecraft:overworld,minecraft:the_end",
                    "peak:70,windswept:55,snowy_slopes:45,meadow:25,desert:10,jungle:10,cave:15,end_highlands:30,end_midlands:30,the_end:20,small_end_islands:15,end_barrens:15",
                    "peak:20:80,windswept:20:80,snowy_slopes:20:80,meadow:20:80,desert:20:80,jungle:20:80,cave:20:80,end_highlands:130:200:0:90,end_midlands:130:200:0:90,the_end:130:200:0:90,small_end_islands:130:200:0:90,end_barrens:130:200:0:90",
                    0, 1, 20, 200, 1, 320, 1, 192, 2, 6, true, 0, 63, 180.0, "emerald.json"),
            new DefaultNode("oresandstuff:lapis", "minecraft:lapis_lazuli", 0.35, "#2E6BD8", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
                    "minecraft:stone", "minecraft:deepslate",
                    "minecraft:overworld",
                    "desert:45,badlands:30,savanna:30,plains:20,jungle:10,ocean:25,cave:30,river:10", "cave:50:150:-60:-10:2:false:1:220,desert:60:120:-60:-20:1:true:2:180",
                    0, 1, 50, 140, 1, 280, 1, 192, 2, 6, false, -60, -10, 100.0, "lapis.json"),
            new DefaultNode("oresandstuff:nether_quartz", "minecraft:quartz", 0.7, "#E8DFD6", "minecraft:nether_quartz_ore", "minecraft:quartz_block",
                    "minecraft:netherrack", "minecraft:nether_quartz_ore",
                    "minecraft:the_nether",
                    "nether_wastes:50,basalt_deltas:45,crimson_forest:25,warped_forest:25,soul_sand_valley:20", "basalt_deltas:60:160:0:60:2:true:1:180,soul_sand_valley:30:120:0:40:1:false:2:150",
                    1, 2, 60, 160, 1, 200, 1, 192, 2, 8, true, 0, 63, 80.0, "nether_quartz.json"),
            new DefaultNode("oresandstuff:nether_gold", "minecraft:gold_nugget", 0.5, "#E8B01C", "minecraft:nether_gold_ore", "minecraft:gold_block",
                    "minecraft:netherrack", "minecraft:nether_gold_ore",
                    "minecraft:the_nether",
                    "nether_wastes:55,basalt_deltas:45,soul_sand_valley:15,crimson_forest:10,warped_forest:10", "nether_wastes:60:160:0:70:2:true:1:200,crimson_forest:20:110:0:40:1:false:3:140",
                    1, 2, 60, 160, 1, 220, 1, 192, 2, 8, true, 0, 63, 60.0, "nether_gold.json")
    );

    private record DefaultNode(String id, String output, double rate, String color, String visual, String visualPure,
                               String nodeBlock, String nodeBlockPure,
                               String dimensionsCsv, String biomesCsv, String biomeOverridesCsv, int minNodes, int maxNodes,
                               double qualityMin, double qualityMax, int maxMiners, int spacing, int attempts,
                               int scannerRadius, int clusterRadius, int scatterCount,
                               boolean surfaceSpawn, int minY, int maxY, double hardness, String fileName) {
    }

    private static String nodeJson(DefaultNode node) {
        JsonObject root = new JsonObject();
        root.addProperty("id", node.id());
        root.addProperty("output_item", node.output());
        JsonObject drops = new JsonObject();
        drops.addProperty(node.output(), 100);
        drops.addProperty(node.nodeBlock(), 100);
        drops.addProperty(node.nodeBlockPure(), 100);
        root.add("drops", drops);
        root.addProperty("enabled", true);
        root.addProperty("hardness", node.hardness());

        root.addProperty("base_rate_per_second", node.rate());
        root.addProperty("scanner_color", node.color());
        root.addProperty("scanner_radius", node.scannerRadius());

        root.addProperty("quality_min", node.qualityMin());
        root.addProperty("quality_max", node.qualityMax());
        double mid = (node.qualityMin() + node.qualityMax()) / 2.0;
        JsonArray visuals = new JsonArray();
        visuals.add(tierJson(node.qualityMin(), mid, node.nodeBlock(), node.visual()));
        visuals.add(tierJson(mid, node.qualityMax(), node.nodeBlockPure(), node.visualPure()));
        root.add("quality_visuals", visuals);

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
        JsonObject overrides = new JsonObject();
        if (!node.biomeOverridesCsv().isEmpty()) {
            for (String part : node.biomeOverridesCsv().split(",")) {
                String[] seg = part.trim().split(":");
                if (seg.length >= 3) {
                    JsonObject o = new JsonObject();
                    o.addProperty("quality_min", Double.parseDouble(seg[1]));
                    o.addProperty("quality_max", Double.parseDouble(seg[2]));
                    if (seg.length >= 5) {
                        o.addProperty("min_y", Integer.parseInt(seg[3]));
                        o.addProperty("max_y", Integer.parseInt(seg[4]));
                    }
                    if (seg.length >= 6) {
                        o.addProperty("max_miners_per_node", Integer.parseInt(seg[5]));
                    }
                    if (seg.length >= 7) {
                        o.addProperty("surface_spawn", Boolean.parseBoolean(seg[6]));
                    }
                    if (seg.length >= 8) {
                        o.addProperty("placement_attempts", Integer.parseInt(seg[7]));
                    }
                    if (seg.length >= 9) {
                        o.addProperty("min_spacing_blocks", Integer.parseInt(seg[8]));
                    }
                    overrides.add(seg[0], o);
                }
            }
        }
        root.add("biome_overrides", overrides);

        root.addProperty("min_nodes_per_chunk", node.minNodes());
        root.addProperty("max_nodes_per_chunk", node.maxNodes());
        root.addProperty("max_miners_per_node", node.maxMiners());
        root.addProperty("min_spacing_blocks", node.spacing());
        root.addProperty("placement_attempts", node.attempts());
        root.addProperty("cluster_radius", node.clusterRadius());
        root.addProperty("scatter_count", node.scatterCount());
        root.addProperty("surface_spawn", node.surfaceSpawn());
        root.addProperty("min_y", node.minY());
        root.addProperty("max_y", node.maxY());

        return ConfigAssets.pretty(root);
    }

    private static JsonObject tierJson(double min, double max, String nodeBlock, String visualBlock) {
        JsonObject o = new JsonObject();
        o.addProperty("min", min);
        o.addProperty("max", max);
        o.addProperty("node_block", nodeBlock);
        o.addProperty("visual_block", visualBlock);
        return o;
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

    private static Boolean optionalBoolean(JsonObject root, String path) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject(parts[i]);
        }
        return current.has(parts[parts.length - 1]) ? current.get(parts[parts.length - 1]).getAsBoolean() : null;
    }

    private static Double optionalDouble(JsonObject root, String path) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject(parts[i]);
        }
        return current.has(parts[parts.length - 1]) ? current.get(parts[parts.length - 1]).getAsDouble() : null;
    }

    private static double doubleValue(JsonObject root, String key, double fallback) {
        return root.has(key) ? root.get(key).getAsDouble() : fallback;
    }

    private static String colorValue(JsonObject root) {
        String raw = root.has("scanner_color") ? root.get("scanner_color").getAsString() : "#FFFFFF";
        return raw.replace("#", "0x");
    }
}