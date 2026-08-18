package com.abo47.oresandstuff.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.node.OreNodeType;
import com.abo47.oresandstuff.node.Purity;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class OreNodeDataManager extends SimpleJsonResourceReloadListener {
    public static final OreNodeDataManager INSTANCE = new OreNodeDataManager();

    private static final Logger LOGGER = OresAndStuffMod.LOGGER;
    private static final String TYPES_DIR = "ore_node_types";
    private static final String DISTRIBUTION_DIR = "ore_node_distribution";
    private static final String GENERATION_DIR = "ore_node_generation";

    private final Map<ResourceLocation, OreNodeType> nodeTypes = new HashMap<>();
    private final Map<ResourceLocation, BiomeDistributionRule> distributions = new HashMap<>();
    private NodeGenerationConfig generationConfig = NodeGenerationConfig.defaults();

    private OreNodeDataManager() {
        super(new GsonBuilder().create(), "");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        nodeTypes.clear();
        distributions.clear();
        generationConfig = NodeGenerationConfig.defaults();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            String path = id.getPath();
            JsonObject root = entry.getValue().getAsJsonObject();

            try {
                if (path.startsWith(TYPES_DIR + "/")) {
                    ResourceLocation typeId = new ResourceLocation(id.getNamespace(), path.substring((TYPES_DIR + "/").length()));
                    nodeTypes.put(typeId, parseNodeType(typeId, root));
                } else if (path.startsWith(DISTRIBUTION_DIR + "/")) {
                    ResourceLocation ruleId = new ResourceLocation(id.getNamespace(), path.substring((DISTRIBUTION_DIR + "/").length()));
                    distributions.put(ruleId, parseDistribution(root));
                } else if (path.startsWith(GENERATION_DIR + "/")) {
                    generationConfig = parseGeneration(root);
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to parse node data file {}", id, ex);
            }
        }

        if (nodeTypes.isEmpty()) {
            loadFallbackDefaults();
        }

        LOGGER.info("Loaded {} ore node types, {} biome rules", nodeTypes.size(), distributions.size());
    }

    private OreNodeType parseNodeType(ResourceLocation id, JsonObject root) {
        ResourceLocation output = new ResourceLocation(GsonHelper.getAsString(root, "output_item"));
        double baseRate = GsonHelper.getAsDouble(root, "base_rate_per_second", 0.2D);
        int color = Integer.decode(GsonHelper.getAsString(root, "scanner_color", "#FFFFFF").replace("#", "0x"));
        float hardness = GsonHelper.getAsFloat(root, "hardness", 100.0F);
        boolean enabled = GsonHelper.getAsBoolean(root, "enabled", true);
        ResourceLocation visual = new ResourceLocation(GsonHelper.getAsString(root, "visual_block", "minecraft:" + id.getPath() + "_ore"));
        return new OreNodeType(id, output, baseRate, color, hardness, enabled, visual);
    }

    private BiomeDistributionRule parseDistribution(JsonObject root) {
        String biomePattern = GsonHelper.getAsString(root, "biome_pattern", "minecraft:plains");
        String dimension = GsonHelper.getAsString(root, "dimension", "minecraft:overworld");
        int minNodes = GsonHelper.getAsInt(root, "min_nodes_per_chunk", 0);
        int maxNodes = GsonHelper.getAsInt(root, "max_nodes_per_chunk", 1);

        Map<ResourceLocation, Integer> weights = new HashMap<>();
        JsonObject weightsObj = GsonHelper.getAsJsonObject(root, "ore_weights");
        for (Map.Entry<String, JsonElement> e : weightsObj.entrySet()) {
            weights.put(new ResourceLocation(e.getKey()), e.getValue().getAsInt());
        }

        JsonObject purityObj = GsonHelper.getAsJsonObject(root, "purity_weights");
        int impure = GsonHelper.getAsInt(purityObj, "impure", 40);
        int normal = GsonHelper.getAsInt(purityObj, "normal", 45);
        int pure = GsonHelper.getAsInt(purityObj, "pure", 15);

        return new BiomeDistributionRule(biomePattern, dimension, minNodes, maxNodes, weights, impure, normal, pure);
    }

    private NodeGenerationConfig parseGeneration(JsonObject root) {
        int minSpacing = GsonHelper.getAsInt(root, "min_spacing_blocks", NodeGenerationConfig.defaults().minSpacingBlocks());
        int attempts = GsonHelper.getAsInt(root, "placement_attempts", NodeGenerationConfig.defaults().placementAttempts());
        int scanRadius = GsonHelper.getAsInt(root, "scanner_radius", NodeGenerationConfig.defaults().scannerRadius());
        return new NodeGenerationConfig(minSpacing, attempts, scanRadius);
    }

    private void loadFallbackDefaults() {
        registerDefault("iron", "minecraft:raw_iron", 0.6, "#D8D8D8", "minecraft:iron_ore");
        registerDefault("copper", "minecraft:raw_copper", 0.7, "#C97142", "minecraft:copper_ore");
        registerDefault("coal", "minecraft:coal", 0.9, "#2E2E2E", "minecraft:coal_ore");
        registerDefault("redstone", "minecraft:redstone", 0.4, "#D12222", "minecraft:redstone_ore");

        distributions.put(new ResourceLocation(OresAndStuffMod.MOD_ID, "default_plains"),
                new BiomeDistributionRule("minecraft:plains", Level.OVERWORLD.location().toString(), 1, 2,
                        Map.of(id("iron"), 30, id("copper"), 30, id("coal"), 25, id("redstone"), 15), 25, 50, 25));
        distributions.put(new ResourceLocation(OresAndStuffMod.MOD_ID, "default_mountains"),
                new BiomeDistributionRule("mountain", Level.OVERWORLD.location().toString(), 1, 3,
                        Map.of(id("iron"), 45, id("copper"), 20, id("coal"), 20, id("redstone"), 15), 15, 40, 45));
        distributions.put(new ResourceLocation(OresAndStuffMod.MOD_ID, "default_swamp"),
                new BiomeDistributionRule("swamp", Level.OVERWORLD.location().toString(), 1, 2,
                        Map.of(id("coal"), 55, id("copper"), 20, id("iron"), 15, id("redstone"), 10), 35, 45, 20));
        distributions.put(new ResourceLocation(OresAndStuffMod.MOD_ID, "default_desert"),
                new BiomeDistributionRule("desert", Level.OVERWORLD.location().toString(), 0, 1,
                        Map.of(id("copper"), 35, id("iron"), 30, id("coal"), 25, id("redstone"), 10), 45, 40, 15));
    }

    private void registerDefault(String name, String output, double baseRate, String color, String visual) {
        ResourceLocation id = id(name);
        int parsedColor = Integer.decode(color.replace("#", "0x"));
        nodeTypes.put(id, new OreNodeType(id, new ResourceLocation(output), baseRate, parsedColor, 100f, true, new ResourceLocation(visual)));
    }

    private ResourceLocation id(String path) {
        return new ResourceLocation(OresAndStuffMod.MOD_ID, path);
    }

    public List<OreNodeType> nodeTypes() {
        return new ArrayList<>(nodeTypes.values());
    }

    public Optional<OreNodeType> getNodeType(ResourceLocation id) {
        return Optional.ofNullable(nodeTypes.get(id));
    }

    public OreNodeType getAnyNodeType() {
        return enabledNodeTypes().stream().findFirst()
                .orElseThrow(() -> new JsonParseException("No enabled node types loaded"));
    }

    public List<BiomeDistributionRule> getDistributionsForDimension(ResourceLocation dimension) {
        return distributions.values().stream()
                .filter(r -> r.dimension().equals(dimension.toString()))
                .toList();
    }

    public NodeGenerationConfig generationConfig() {
        return generationConfig;
    }

    public Purity rollPurity(RandomSource random, BiomeDistributionRule rule) {
        int total = rule.impureWeight() + rule.normalWeight() + rule.pureWeight();
        if (total <= 0) {
            return Purity.NORMAL;
        }
        int v = random.nextInt(total);
        if (v < rule.impureWeight()) {
            return Purity.IMPURE;
        }
        if (v < rule.impureWeight() + rule.normalWeight()) {
            return Purity.NORMAL;
        }
        return Purity.PURE;
    }

    public OreNodeType rollNodeType(RandomSource random, BiomeDistributionRule rule) {
        Set<ResourceLocation> disabled = disabledNodeIds();
        Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> e : rule.oreWeights().entrySet()) {
            OreNodeType type = nodeTypes.get(e.getKey());
            if (type == null || !type.enabledByDefault() || disabled.contains(e.getKey())) {
                continue;
            }
            weights.put(e.getKey(), e.getValue());
        }

        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return getAnyNodeType();
        }

        int v = random.nextInt(total);
        int cursor = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : weights.entrySet()) {
            cursor += entry.getValue();
            if (v < cursor) {
                return getNodeType(entry.getKey()).orElse(getAnyNodeType());
            }
        }
        return getAnyNodeType();
    }

    public List<ResourceLocation> orderedTypeIds() {
        ArrayList<ResourceLocation> ids = new ArrayList<>(enabledNodeTypes().stream().map(OreNodeType::id).toList());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        return ids;
    }

    private List<OreNodeType> enabledNodeTypes() {
        Set<ResourceLocation> disabled = disabledNodeIds();
        return nodeTypes.values().stream()
                .filter(t -> t.enabledByDefault() && !disabled.contains(t.id()))
                .toList();
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
}
