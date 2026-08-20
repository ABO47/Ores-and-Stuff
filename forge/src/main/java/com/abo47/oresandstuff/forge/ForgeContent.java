package com.abo47.oresandstuff.forge;

import net.minecraft.core.registries.Registries;
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

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OresAndStuffMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OresAndStuffMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OresAndStuffMod.MOD_ID);

    private ForgeContent() {
    }

    static void register(IEventBus modBus) {
        MinerTierConfig.ensureLoaded();
        ModContent.registerMiners();
        OreNodeDataManager.INSTANCE.ensureLoaded();

        BLOCKS.register("ore_node", () -> ModBlocks.ORE_NODE);
        for (OreNodeType type : OreNodeDataManager.INSTANCE.nodeTypes()) {
            BLOCKS.register("ore_node_" + type.id().getPath(), () -> ModNodeBlocks.get(type.id()));
        }
        for (MinerTierConfig.MinerTier tier : MinerTierConfig.tiers()) {
            BLOCKS.register("miner_" + tier.id(), () -> ModContent.minerBlock(tier.id()));
        }
        BLOCKS.register("infinite_battery", () -> ModBlocks.INFINITE_BATTERY);

        ITEMS.register("ore_node", () -> ModItems.ORE_NODE);
        for (MinerTierConfig.MinerTier tier : MinerTierConfig.tiers()) {
            ITEMS.register("miner_" + tier.id(), () -> ModContent.minerItem(tier.id()));
        }
        ITEMS.register("infinite_battery", () -> ModItems.INFINITE_BATTERY);
        ITEMS.register("scanner", () -> ModItems.SCANNER);
        ITEMS.register("bio_scanner", () -> ModItems.BIO_SCANNER);

        TABS.register("main", () -> ModCreativeTabs.MAIN);

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}