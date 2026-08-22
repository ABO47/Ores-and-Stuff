package com.abo47.oresandstuff.forge;

import com.abo47.oresandstuff.energy.EnergyStorage;

import net.minecraftforge.energy.IEnergyStorage;

public class ForgeEnergyCap implements IEnergyStorage {
    private final EnergyStorage delegate;

    public ForgeEnergyCap(EnergyStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return delegate.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return delegate.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return delegate.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return delegate.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return delegate.canExtract();
    }

    @Override
    public boolean canReceive() {
        return delegate.canReceive();
    }
}
