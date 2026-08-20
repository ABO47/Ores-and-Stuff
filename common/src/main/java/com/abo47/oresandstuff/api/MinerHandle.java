package com.abo47.oresandstuff.api;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import com.abo47.oresandstuff.data.config.MinerTierConfig.MinerTier;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.miner.MinerStatus;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;

/**
 * Live view of a miner block entity. All values read through directly to the
 * block entity, so they stay in sync with the current tick.
 */
public final class MinerHandle {
    private final MinerBlockEntity be;

    public MinerHandle(MinerBlockEntity be) {
        this.be = be;
    }

    public MinerBlockEntity unwrap() {
        return be;
    }

    public BlockPos pos() {
        return be.getBlockPos();
    }

    public MinerTier tier() {
        return be.getTier();
    }

    public int energyStored() {
        return be.getEnergyStored();
    }

    public int maxEnergy() {
        return be.getMaxEnergyStored();
    }

    public double progress() {
        return be.getProgress();
    }

    /** Items per second the miner is currently producing (0 if no node). */
    public double ratePerSecond() {
        return be.getRatePerSecond();
    }

    public MinerStatus status() {
        return be.getStatus();
    }

    public boolean enabled() {
        return be.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        be.setEnabled(enabled);
    }

    /** The node this miner is attached to, if any. */
    public Optional<OreNodeHandle> attachedNode(Level level) {
        OreNodeBlockEntity node = be.getAttachedNode();
        if (node == null) {
            return Optional.empty();
        }
        return Optional.of(new OreNodeHandle(node.getBlockPos(), node.getNodeTypeId(), node.getQualityPercent(), node.getNodeId()));
    }

    /** The node type id currently displayed by the miner. */
    public ResourceLocation nodeTypeId() {
        return be.getNodeTypeId();
    }

    /** The node quality the miner is reading (defaults to 100 with no node). */
    public double nodeQuality() {
        return be.getNodeQuality();
    }

    /** The miner's output inventory (3 slots). */
    public IItemTransfer output() {
        return be.getOutputTransfer();
    }
}