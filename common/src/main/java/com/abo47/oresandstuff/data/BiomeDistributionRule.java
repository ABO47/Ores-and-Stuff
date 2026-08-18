package com.abo47.oresandstuff.data;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public record BiomeDistributionRule(String biomePattern,
                                    String dimension,
                                    int minNodesPerChunk,
                                    int maxNodesPerChunk,
                                    Map<ResourceLocation, Integer> oreWeights,
                                    int impureWeight,
                                    int normalWeight,
                                    int pureWeight) {
}
