package com.abo47.oresandstuff.node;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public record OreNodeType(
        ResourceLocation id,
        ResourceLocation outputItem,
        double baseRatePerSecond,
        int scannerColor,
        float hardness,
        boolean enabledByDefault,
        ResourceLocation visualBlock,
        ResourceLocation dimension,
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
        int scatterCount
) {
    public int biomeWeight(String biomeName) {
        int weight = 0;
        for (Map.Entry<String, Integer> entry : biomes.entrySet()) {
            if (biomeName.contains(entry.getKey())) {
                weight += entry.getValue();
            }
        }
        return weight;
    }
}