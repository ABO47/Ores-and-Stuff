package com.abo47.oresandstuff.miner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.abo47.oresandstuff.content.ModBlockEntities;
import com.abo47.oresandstuff.energy.EnergyStorage;
import com.abo47.oresandstuff.energy.InfiniteEnergyStorage;

public class InfiniteBatteryBlockEntity extends BlockEntity {
    private final EnergyStorage energy = new InfiniteEnergyStorage();

    public InfiniteBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_BATTERY, pos, state);
    }

    public EnergyStorage getEnergyStorage() {
        return energy;
    }
}
