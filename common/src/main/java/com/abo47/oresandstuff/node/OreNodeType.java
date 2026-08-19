package com.abo47.oresandstuff.node;

import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public record OreNodeType(
        ResourceLocation id,
        ResourceLocation outputItem,
        double baseRatePerSecond,
        int scannerColor,
        float hardness,
        boolean enabledByDefault,
        ResourceLocation visualBlock,
        ResourceLocation visualBlockPure,
        List<ResourceLocation> dimensions,
        Map<String, Integer> biomes,
        int minNodesPerChunk,
        int maxNodesPerChunk,
        int impureWeight,
        int normalWeight,
        int pureWeight,
        int minSpacingBlocks,
        int placementAttempts,
        int scannerRadius,
        int clusterRadius,
        int scatterCount,
        boolean surfaceSpawn,
        int minY,
        int maxY,
        List<OreNodeDrop> drops,
        Map<String, BiomeOverride> biomeOverrides
) {
    public boolean matchesDimension(ResourceLocation dimension) {
        return dimensions.contains(dimension);
    }

    public int biomeWeight(String biomeName) {
        int weight = 0;
        for (Map.Entry<String, Integer> entry : biomes.entrySet()) {
            if (biomeName.contains(entry.getKey())) {
                weight += entry.getValue();
            }
        }
        return weight;
    }

    public BiomeOverride overrideFor(String biomeName) {
        BiomeOverride best = null;
        int bestLength = -1;
        for (Map.Entry<String, BiomeOverride> entry : biomeOverrides.entrySet()) {
            if (biomeName.contains(entry.getKey()) && entry.getKey().length() > bestLength) {
                best = entry.getValue();
                bestLength = entry.getKey().length();
            }
        }
        return best;
    }

    public int effectiveMinNodes(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.minNodesPerChunk() != null ? override.minNodesPerChunk() : minNodesPerChunk;
    }

    public int effectiveMaxNodes(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.maxNodesPerChunk() != null ? override.maxNodesPerChunk() : maxNodesPerChunk;
    }

    public int effectiveClusterRadius(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.clusterRadius() != null ? override.clusterRadius() : clusterRadius;
    }

    public int effectiveScatterCount(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.scatterCount() != null ? override.scatterCount() : scatterCount;
    }

    public int effectiveImpureWeight(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.impureWeight() != null ? override.impureWeight() : impureWeight;
    }

    public int effectiveNormalWeight(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.normalWeight() != null ? override.normalWeight() : normalWeight;
    }

    public int effectivePureWeight(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.pureWeight() != null ? override.pureWeight() : pureWeight;
    }

    public ResourceLocation visualFor(boolean pure) {
        if (!pure) {
            return visualBlock;
        }
        return visualBlockPure != null ? visualBlockPure : visualBlock;
    }

    public ResourceLocation rollDrop(RandomSource random) {
        if (drops == null || drops.isEmpty()) {
            return outputItem;
        }
        int total = 0;
        for (OreNodeDrop drop : drops) {
            total += Math.max(0, drop.weight());
        }
        if (total <= 0) {
            return outputItem;
        }
        int v = random.nextInt(total);
        int cursor = 0;
        for (OreNodeDrop drop : drops) {
            cursor += Math.max(0, drop.weight());
            if (v < cursor) {
                return drop.item();
            }
        }
        return outputItem;
    }

    public record OreNodeDrop(ResourceLocation item, int weight) {
    }

    public record BiomeOverride(Integer minNodesPerChunk, Integer maxNodesPerChunk, Integer clusterRadius,
                                Integer scatterCount, Integer impureWeight, Integer normalWeight, Integer pureWeight) {
    }
}