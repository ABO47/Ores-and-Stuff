package com.abo47.oresandstuff.fabric;

import com.abo47.oresandstuff.energy.EnergyStorage;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public class FabricEnergyCap implements team.reborn.energy.api.EnergyStorage {
    private final EnergyStorage delegate;

    public FabricEnergyCap(EnergyStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public long getCapacity() {
        return delegate.getMaxEnergyStored();
    }

    @Override
    public long getAmount() {
        return delegate.getEnergyStored();
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        return delegate.receiveEnergy((int) Math.min(maxAmount, Integer.MAX_VALUE), false);
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return delegate.extractEnergy((int) Math.min(maxAmount, Integer.MAX_VALUE), false);
    }
}
