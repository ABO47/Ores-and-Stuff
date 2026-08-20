package com.abo47.oresandstuff.api;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.node.ExtractionRateService;
import com.abo47.oresandstuff.node.NodeQuality;
import com.abo47.oresandstuff.node.OreNodeType;

/**
 * Immutable snapshot of a single ore node block.
 * <p>
 * Obtain one via {@link NodeApi}, e.g.
 * {@code NodeApi.findNearestNode(level, pos, 8, 8)}.
 */
public record OreNodeHandle(BlockPos pos,
                            ResourceLocation typeId,
                            double qualityPercent,
                            UUID nodeId) {

    /** The configured node type, if it is still loaded. */
    public java.util.Optional<OreNodeType> type() {
        return OreNodeDataManager.INSTANCE.getNodeType(typeId);
    }

    /** Quality multiplier: quality % / 100 (200% -> 2.0). */
    public double qualityMultiplier() {
        return NodeQuality.multiplier(qualityPercent);
    }

    /**
     * Rolls the node's drop entries once: every entry that hits its chance
     * is returned, or the default output if all miss.
     */
    public List<ItemStack> rollDrops(RandomSource random) {
        return type().map(t -> t.rollDrops(random)).orElseGet(List::of);
    }

    /**
     * The rolled drops for a mining operation of {@code baseCount} units,
     * with each item's count multiplied by the node's quality.
     */
    public List<ItemStack> dropsFor(RandomSource random, int baseCount) {
        return ExtractionRateService.buildDrops(
                typeId, qualityPercent, random, Math.max(1, baseCount));
    }

    /**
     * Items produced per second by a miner of the given tier multiplier,
     * before the energy gate: baseRatePerSecond x quality x tier.
     */
    public double itemsPerSecond(double minerTierMultiplier) {
        return type().map(t -> t.baseRatePerSecond() * qualityMultiplier() * minerTierMultiplier)
                .orElse(0.0);
    }
}