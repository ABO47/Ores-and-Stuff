package com.abo47.oresandstuff.energy;

import net.minecraft.nbt.CompoundTag;

public class SimpleEnergyStorage implements EnergyStorage {
    protected int energy;
    protected final int capacity;
    protected final int maxReceive;
    protected final int maxExtract;

    public SimpleEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }
        int received = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (received > 0 && !simulate) {
            energy += received;
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) {
            return 0;
        }
        int extracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (extracted > 0 && !simulate) {
            energy -= extracted;
        }
        return extracted;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", energy);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        energy = tag.getInt("Energy");
    }
}
