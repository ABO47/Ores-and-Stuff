package com.abo47.oresandstuff.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.abo47.oresandstuff.block.InfiniteBatteryBlock;
import com.abo47.oresandstuff.block.OreNodeBlock;

public final class ModBlocks {
    public static final Block ORE_NODE = new OreNodeBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).sound(SoundType.STONE).noOcclusion());
    public static final Block INFINITE_BATTERY = new InfiniteBatteryBlock(BlockBehaviour.Properties.of().strength(2.0F));

    private ModBlocks() {
    }
}
