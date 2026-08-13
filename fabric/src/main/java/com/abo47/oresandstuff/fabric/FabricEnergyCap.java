package com.abo47.oresandstuff.fabric;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

public class FabricEnergyCap implements EnergyStorage {
    private final com.abo47.oresandstuff.energy.EnergyStorage delegate;

    public FabricEnergyCap(com.abo47.oresandstuff.energy.EnergyStorage delegate) {
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
