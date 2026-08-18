package com.abo47.oresandstuff.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.content.ModCreativeTabs;
import com.abo47.oresandstuff.content.ModItems;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OresAndStuffMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OresAndStuffMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OresAndStuffMod.MOD_ID);

    static {
        BLOCKS.register("ore_node", () -> ModBlocks.ORE_NODE);
        BLOCKS.register("miner_mk1", () -> ModBlocks.MINER_MK1);
        BLOCKS.register("infinite_battery", () -> ModBlocks.INFINITE_BATTERY);

        ITEMS.register("ore_node", () -> ModItems.ORE_NODE);
        ITEMS.register("miner_mk1", () -> ModItems.MINER_MK1);
        ITEMS.register("infinite_battery", () -> ModItems.INFINITE_BATTERY);
        ITEMS.register("scanner", () -> ModItems.SCANNER);
        ITEMS.register("bio_scanner", () -> ModItems.BIO_SCANNER);
        ITEMS.register("node_extractor_pickaxe", () -> ModItems.NODE_EXTRACTOR_PICKAXE);

        TABS.register("main", () -> ModCreativeTabs.MAIN);
    }

    private ForgeContent() {
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
