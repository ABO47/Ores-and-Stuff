package com.abo47.oresandstuff.api;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Fired on the server each time a miner converts accumulated progress into a
 * batch of items and inserts it into its output.
 * <p>
 * Listen via {@link MiningEvents#register(java.util.function.Consumer)}.
 */
public record MinerExtractEvent(ServerLevel level,
                                MinerHandle miner,
                                OreNodeHandle node,
                                int units,
                                List<ItemStack> batch) {
}