package com.abo47.oresandstuff.fabric;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModFeatures;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;
import com.abo47.oresandstuff.world.OreNodeFeature;

public final class FabricWorldgen {
    public static final ResourceKey<PlacedFeature> ORE_NODE_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("ore_node"));
    private static final List<ResourceKey<PlacedFeature>> VANILLA_ORES = List.of(
            placed("ore_coal_upper"), placed("ore_coal_lower"),
            placed("ore_iron_upper"), placed("ore_iron_middle"), placed("ore_iron_small"),
            placed("ore_gold"), placed("ore_gold_extra"), placed("ore_gold_lower"),
            placed("ore_redstone"), placed("ore_redstone_lower"),
            placed("ore_diamond"), placed("ore_diamond_large"), placed("ore_diamond_buried"),
            placed("ore_lapis"), placed("ore_lapis_buried"),
            placed("ore_copper"), placed("ore_copper_large"),
            placed("ore_emerald"),
            placed("ore_gold_nether"),
            placed("ore_quartz_nether"),
            placed("ore_ancient_debris_large"), placed("ore_ancient_debris_small"));

    private FabricWorldgen() {
    }

    public static void registerFeature() {
        ModFeatures.ORE_NODE = Registry.register(BuiltInRegistries.FEATURE, id("ore_node"), new OreNodeFeature());
    }

    public static void register() {
        BiomeModifications.create(id("features")).add(ModificationPhase.ADDITIONS, BiomeSelectors.all(), (selection, modification) -> modification.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_NODE_PLACED));
        if (OresAndStuffConfig.worldgen().removeVanillaOres) {
            for (ResourceKey<PlacedFeature> key : VANILLA_ORES) {
                BiomeModifications.create(id("remove_" + key.location().getPath())).add(ModificationPhase.REMOVALS, BiomeSelectors.all(), (selection, modification) -> {
                    try {
                        modification.getGenerationSettings().removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, key);
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        }
    }

    private static ResourceKey<PlacedFeature> placed(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation("minecraft", path));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(OresAndStuffMod.MOD_ID, path);
    }
}
