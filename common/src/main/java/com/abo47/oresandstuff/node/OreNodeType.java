package com.abo47.oresandstuff.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record OreNodeType(
        ResourceLocation id,
        ResourceLocation outputItem,
        double baseRatePerSecond,
        int scannerColor,
        float hardness,
        boolean enabledByDefault,
        List<QualityTier> qualityTiers,
        List<ResourceLocation> dimensions,
        Map<String, Integer> biomes,
        int minNodesPerChunk,
        int maxNodesPerChunk,
        double qualityMin,
        double qualityMax,
        int maxMinersPerNode,
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

    public double effectiveQualityMin(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.qualityMin() != null ? override.qualityMin() : qualityMin;
    }

    public double effectiveQualityMax(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.qualityMax() != null ? override.qualityMax() : qualityMax;
    }

    public boolean effectiveSurfaceSpawn(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.surfaceSpawn() != null ? override.surfaceSpawn() : surfaceSpawn;
    }

    public int effectivePlacementAttempts(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.placementAttempts() != null ? Math.max(1, override.placementAttempts()) : placementAttempts;
    }

    public int effectiveMinSpacing(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.minSpacingBlocks() != null ? Math.max(1, override.minSpacingBlocks()) : minSpacingBlocks;
    }

    public int effectiveMinY(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.minY() != null ? override.minY() : minY;
    }

    public int effectiveMaxY(String biomeName) {
        BiomeOverride override = overrideFor(biomeName);
        return override != null && override.maxY() != null ? override.maxY() : maxY;
    }

    /**
     * Index of the visual tier covering the given quality percentage, or the
     * last tier with a lower bound below it (first tier if none).
     */
    public int tierIndexFor(double quality) {
        int best = 0;
        for (int i = 0; i < qualityTiers.size(); i++) {
            if (quality >= qualityTiers.get(i).min()) {
                best = i;
            }
        }
        return best;
    }

    /**
     * Rolls the output items for one extraction: every drop entry is an
     * independent chance (0-100%); each item that hits is produced.
     */
    public List<ItemStack> rollDrops(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        if (random == null || drops == null || drops.isEmpty()) {
            return out;
        }
        for (OreNodeDrop drop : drops) {
            if (drop.weight() > 0 && random.nextInt(100) < Math.min(100, drop.weight())) {
                Item item = BuiltInRegistries.ITEM.get(drop.item());
                if (item != null && item != Items.AIR) {
                    out.add(new ItemStack(item));
                }
            }
        }
        if (out.isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(outputItem);
            if (item != null && item != Items.AIR) {
                out.add(new ItemStack(item));
            }
        }
        return out;
    }

    public record OreNodeDrop(ResourceLocation item, int weight) {
    }

    /**
     * Visual tier: the quality range (in percent) a node must roll within to
     * use this look. nodeBlock is the block the main node blocks mimic
     * (rendered via its model), visualBlock is the ore block used as the
     * decoration around the cluster.
     */
    public record QualityTier(double min, double max, ResourceLocation nodeBlockModel, ResourceLocation visualBlock) {
    }

    public record BiomeOverride(Integer minNodesPerChunk, Integer maxNodesPerChunk, Integer clusterRadius,
                                Integer scatterCount, Double qualityMin, Double qualityMax, Integer minY, Integer maxY,
                                Boolean surfaceSpawn, Integer placementAttempts,
                                Integer minSpacingBlocks) {
    }
}