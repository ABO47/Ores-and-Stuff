package com.abo47.oresandstuff.content;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.abo47.oresandstuff.world.OreNodeFeature;

public final class ModFeatures {
    public static Feature<NoneFeatureConfiguration> ORE_NODE;

    private ModFeatures() {
    }

    public static OreNodeFeature node() {
        return (OreNodeFeature) ORE_NODE;
    }
}
