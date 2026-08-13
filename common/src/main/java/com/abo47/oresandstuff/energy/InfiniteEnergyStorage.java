package com.abo47.oresandstuff.energy;

public class InfiniteEnergyStorage extends SimpleEnergyStorage {
    public InfiniteEnergyStorage() {
        super(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public int getEnergyStored() {
        return capacity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return maxExtract;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return true;
    }
}
