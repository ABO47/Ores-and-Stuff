package com.abo47.oresandstuff.forge;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModFeatures;
import com.abo47.oresandstuff.world.OreNodeFeature;

import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ForgeWorldgen {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, OresAndStuffMod.MOD_ID);
    private static final RegistryObject<Feature<NoneFeatureConfiguration>> ORE_NODE_FEATURE = FEATURES.register("ore_node", OreNodeFeature::new);
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, OresAndStuffMod.MOD_ID);

    private ForgeWorldgen() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
        BIOME_MODIFIERS.register("remove_vanilla_ores", () -> RemoveVanillaOresModifier.CODEC);
        BIOME_MODIFIERS.register(modBus);
    }

    public static void bind() {
        ModFeatures.ORE_NODE = ORE_NODE_FEATURE.get();
    }

    public static final class RemoveVanillaOresModifier implements BiomeModifier {
        public static final Codec<RemoveVanillaOresModifier> CODEC = Codec.unit(RemoveVanillaOresModifier::new);

        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase != Phase.REMOVE || !OresAndStuffConfig.worldgen().removeVanillaOres) {
                return;
            }
            builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).removeIf(feature -> feature.value().getFeatures().anyMatch(this::isVanillaOre));
        }

        private boolean isVanillaOre(ConfiguredFeature<?, ?> feature) {
            return feature.feature() == Feature.ORE && feature.config() instanceof OreConfiguration ore && ore.targetStates.stream().anyMatch(target -> isVanillaOre(target.state.getBlock()));
        }

        private boolean isVanillaOre(Block block) {
            return block == Blocks.COAL_ORE || block == Blocks.IRON_ORE || block == Blocks.COPPER_ORE || block == Blocks.GOLD_ORE || block == Blocks.REDSTONE_ORE || block == Blocks.LAPIS_ORE || block == Blocks.DIAMOND_ORE || block == Blocks.EMERALD_ORE || block == Blocks.NETHER_GOLD_ORE || block == Blocks.NETHER_QUARTZ_ORE || block == Blocks.ANCIENT_DEBRIS;
        }

        @Override
        public Codec<? extends BiomeModifier> codec() {
            return CODEC;
        }
    }
}
