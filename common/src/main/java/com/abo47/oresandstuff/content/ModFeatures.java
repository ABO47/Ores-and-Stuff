package com.abo47.oresandstuff.content;

import java.util.List;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.abo47.oresandstuff.world.OreNodeFeature;

public final class ModFeatures {
    public static Feature<NoneFeatureConfiguration> ORE_NODE;

    /**
     * Paths of the vanilla placed features that should be removed from every
     * generation step when {@code vanillaOresEnabled} is off. Note that vanilla
     * places the nether ores in the VEGETAL_DECORATION step, not
     * UNDERGROUND_ORES.
     */
    public static final List<String> VANILLA_ORE_PLACED_PATHS = List.of(
            "ore_coal_upper", "ore_coal_lower",
            "ore_iron_upper", "ore_iron_middle", "ore_iron_small",
            "ore_gold", "ore_gold_extra", "ore_gold_lower",
            "ore_redstone", "ore_redstone_lower",
            "ore_diamond", "ore_diamond_large", "ore_diamond_buried",
            "ore_lapis", "ore_lapis_buried",
            "ore_copper", "ore_copper_large",
            "ore_emerald",
            "ore_gold_nether", "ore_gold_deltas",
            "ore_quartz_nether", "ore_quartz_deltas",
            "ore_ancient_debris_large", "ore_debris_small");

    private ModFeatures() {
    }

    public static OreNodeFeature node() {
        return (OreNodeFeature) ORE_NODE;
    }
}
