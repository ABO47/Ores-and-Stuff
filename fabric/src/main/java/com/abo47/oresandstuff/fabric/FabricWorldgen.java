package com.abo47.oresandstuff.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModFeatures;
import com.abo47.oresandstuff.world.OreNodeFeature;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;

public final class FabricWorldgen {
    public static final ResourceKey<PlacedFeature> ORE_NODE_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("ore_node"));

    private FabricWorldgen() {
    }

    public static void registerFeature() {
        ModFeatures.ORE_NODE = Registry.register(BuiltInRegistries.FEATURE, id("ore_node"), new OreNodeFeature());
    }

    public static void register() {
        BiomeModifications.create(id("features")).add(ModificationPhase.ADDITIONS, BiomeSelectors.all(), (selection, modification) -> modification.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_NODE_PLACED));
        for (String path : ModFeatures.VANILLA_ORE_PLACED_PATHS) {
            ResourceKey<PlacedFeature> key = placed(path);
            BiomeModifications.create(id("remove_" + path)).add(ModificationPhase.REMOVALS, BiomeSelectors.all(), (selection, modification) -> {
                if (OresAndStuffConfig.worldgen().vanillaOresEnabled) {
                    return;
                }
                for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                    try {
                        modification.getGenerationSettings().removeFeature(step, key);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            });
        }
    }

    private static ResourceKey<PlacedFeature> placed(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation("minecraft", path));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(OresAndStuffMod.MOD_ID, path);
    }
}
