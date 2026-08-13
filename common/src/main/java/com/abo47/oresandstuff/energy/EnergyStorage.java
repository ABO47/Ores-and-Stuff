package com.abo47.oresandstuff.energy;

public interface EnergyStorage {
    int getEnergyStored();

    int getMaxEnergyStored();

    int receiveEnergy(int maxReceive, boolean simulate);

    int extractEnergy(int maxExtract, boolean simulate);

    boolean canReceive();

    boolean canExtract();
}
