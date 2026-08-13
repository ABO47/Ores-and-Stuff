package com.abo47.oresandstuff.node;

import net.minecraft.resources.ResourceLocation;

public record OreNodeType(ResourceLocation id,
                          ResourceLocation outputItem,
                          double baseRatePerSecond,
                          int scannerColor,
                          float hardness,
                          boolean enabledByDefault,
                          ResourceLocation visualBlock) {
}
