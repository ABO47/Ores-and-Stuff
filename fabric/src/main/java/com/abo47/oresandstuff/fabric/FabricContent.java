package com.abo47.oresandstuff.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.content.ModContent;
import com.abo47.oresandstuff.content.ModCreativeTabs;
import com.abo47.oresandstuff.content.ModItems;
import com.abo47.oresandstuff.content.ModNodeBlocks;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.data.config.MinerTierConfig;
import com.abo47.oresandstuff.node.OreNodeType;

public final class FabricContent {
    private FabricContent() {
    }

    static void register() {
        MinerTierConfig.ensureLoaded();
        ModContent.registerMiners();
        OreNodeDataManager.INSTANCE.ensureLoaded();

        register(BuiltInRegistries.BLOCK, "ore_node", ModBlocks.ORE_NODE);
        for (OreNodeType type : OreNodeDataManager.INSTANCE.nodeTypes()) {
            for (int t = 0; t < type.qualityTiers().size(); t++) {
                register(BuiltInRegistries.BLOCK, "ore_node_" + type.id().getPath() + "_t" + t, ModNodeBlocks.get(type.id(), t));
            }
        }
        for (MinerTierConfig.MinerTier tier : MinerTierConfig.tiers()) {
            register(BuiltInRegistries.BLOCK, "miner_" + tier.id(), ModContent.minerBlock(tier.id()));
        }
        register(BuiltInRegistries.BLOCK, "infinite_battery", ModBlocks.INFINITE_BATTERY);

        register(BuiltInRegistries.ITEM, "ore_node", ModItems.ORE_NODE);
        for (MinerTierConfig.MinerTier tier : MinerTierConfig.tiers()) {
            register(BuiltInRegistries.ITEM, "miner_" + tier.id(), ModContent.minerItem(tier.id()));
        }
        register(BuiltInRegistries.ITEM, "infinite_battery", ModItems.INFINITE_BATTERY);
        register(BuiltInRegistries.ITEM, "scanner", ModItems.SCANNER);
        register(BuiltInRegistries.ITEM, "bio_scanner", ModItems.BIO_SCANNER);

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"), ModCreativeTabs.MAIN);
    }

    private static <T> T register(Registry<? super T> registry, String path, T value) {
        return Registry.register(registry, id(path), value);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(OresAndStuffMod.MOD_ID, path);
    }
}
