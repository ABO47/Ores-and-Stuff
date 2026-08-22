package com.abo47.oresandstuff.content;

import net.minecraft.world.level.block.entity.BlockEntityType;

import com.abo47.oresandstuff.miner.InfiniteBatteryBlockEntity;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;

public final class ModBlockEntities {
    public static BlockEntityType<OreNodeBlockEntity> ORE_NODE;
    public static BlockEntityType<MinerBlockEntity> MINER;
    public static BlockEntityType<InfiniteBatteryBlockEntity> INFINITE_BATTERY;

    private ModBlockEntities() {
    }
}