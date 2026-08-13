package com.abo47.oresandstuff.fabric;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.content.ModCreativeTabs;
import com.abo47.oresandstuff.content.ModItems;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;

public final class FabricContent {
    private FabricContent() {
    }

    static void register() {
        register(BuiltInRegistries.BLOCK, "ore_node", ModBlocks.ORE_NODE);
        register(BuiltInRegistries.BLOCK, "miner_mk1", ModBlocks.MINER_MK1);
        register(BuiltInRegistries.BLOCK, "infinite_battery", ModBlocks.INFINITE_BATTERY);

        register(BuiltInRegistries.ITEM, "ore_node", ModItems.ORE_NODE);
        register(BuiltInRegistries.ITEM, "miner_mk1", ModItems.MINER_MK1);
        register(BuiltInRegistries.ITEM, "infinite_battery", ModItems.INFINITE_BATTERY);
        register(BuiltInRegistries.ITEM, "scanner", ModItems.SCANNER);
        register(BuiltInRegistries.ITEM, "bio_scanner", ModItems.BIO_SCANNER);
        register(BuiltInRegistries.ITEM, "node_extractor_pickaxe", ModItems.NODE_EXTRACTOR_PICKAXE);

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"), ModCreativeTabs.MAIN);
    }

    private static <T> T register(Registry<? super T> registry, String path, T value) {
        return Registry.register(registry, id(path), value);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(OresAndStuffMod.MOD_ID, path);
    }
}
