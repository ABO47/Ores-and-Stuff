package com.abo47.oresandstuff.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.abo47.oresandstuff.block.MinerBlock;
import com.abo47.oresandstuff.data.config.MinerTierConfig;

public final class ModContent {
    private static final Map<String, Block> MINER_BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> MINER_ITEMS = new LinkedHashMap<>();

    private ModContent() {
    }

    public static void registerMiners() {
        MinerTierConfig.ensureLoaded();
    }

    public static Block minerBlock(String tierId) {
        MinerTierConfig.ensureLoaded();
        return MINER_BLOCKS.computeIfAbsent(tierId, id -> new MinerBlock(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    }

    public static Item minerItem(String tierId) {
        MinerTierConfig.ensureLoaded();
        return MINER_ITEMS.computeIfAbsent(tierId, id -> new BlockItem(minerBlock(id), new Item.Properties()));
    }

    public static List<Block> minerBlocks() {
        registerMiners();
        for (MinerTierConfig.MinerTier tier : MinerTierConfig.tiers()) {
            minerBlock(tier.id());
        }
        return List.copyOf(MINER_BLOCKS.values());
    }

    public static List<Item> minerItems() {
        registerMiners();
        for (MinerTierConfig.MinerTier tier : MinerTierConfig.tiers()) {
            minerItem(tier.id());
        }
        return List.copyOf(MINER_ITEMS.values());
    }

    public static Block[] minerBlocksArray() {
        return minerBlocks().toArray(new Block[0]);
    }
}