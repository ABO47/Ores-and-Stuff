package com.abo47.oresandstuff.api;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.abo47.oresandstuff.miner.MinerBlockEntity;

/**
 * Server-side helpers to query miner blocks. Each handle wraps the live
 * block entity, so energy/progress values reflect the current tick.
 */
public final class MinerApi {
    private MinerApi() {
    }

    /** The miner at the exact position, if any. */
    public static Optional<MinerHandle> minerAt(Level level, BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return Optional.empty();
        }
        if (level.getBlockEntity(pos) instanceof MinerBlockEntity miner) {
            return Optional.of(new MinerHandle(miner));
        }
        return Optional.empty();
    }

    /** Every miner inside the box {@code center +- (radiusXZ, radiusY)}. */
    public static List<MinerHandle> minersNear(Level level, BlockPos center, int radiusXZ, int radiusY) {
        java.util.ArrayList<MinerHandle> out = new java.util.ArrayList<>();
        for (int dy = -radiusY; dy <= radiusY; dy++) {
            for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
                for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                    if (level.getBlockEntity(center.offset(dx, dy, dz)) instanceof MinerBlockEntity miner) {
                        out.add(new MinerHandle(miner));
                    }
                }
            }
        }
        return out;
    }
}