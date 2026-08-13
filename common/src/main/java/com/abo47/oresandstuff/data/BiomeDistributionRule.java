package com.abo47.oresandstuff.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record BiomeDistributionRule(String biomePattern,
                                    String dimension,
                                    int minNodesPerChunk,
                                    int maxNodesPerChunk,
                                    Map<ResourceLocation, Integer> oreWeights,
                                    int impureWeight,
                                    int normalWeight,
                                    int pureWeight) {
}
