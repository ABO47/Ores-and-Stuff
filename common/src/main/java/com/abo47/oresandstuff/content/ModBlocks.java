package com.abo47.oresandstuff.content;

import com.abo47.oresandstuff.block.InfiniteBatteryBlock;
import com.abo47.oresandstuff.block.MinerBlock;
import com.abo47.oresandstuff.block.OreNodeBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;

public final class ModBlocks {
    public static final Block ORE_NODE = new OreNodeBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).sound(SoundType.STONE).noOcclusion());
    public static final Block MINER_MK1 = new MinerBlock(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops());
    public static final Block INFINITE_BATTERY = new InfiniteBatteryBlock(BlockBehaviour.Properties.of().strength(2.0F));

    private ModBlocks() {
    }
}
