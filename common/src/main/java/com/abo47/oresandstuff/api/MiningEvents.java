package com.abo47.oresandstuff.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.abo47.oresandstuff.OresAndStuffMod;

/**
 * Server-side event bus for mining mechanics. Loader-agnostic: works from
 * Forge and Fabric mods alike.
 */
public final class MiningEvents {
    private static final List<Consumer<MinerExtractEvent>> LISTENERS = new ArrayList<>();

    private MiningEvents() {
    }

    /**
     * Registers a listener fired on the server every time a miner successfully
     * produces a batch of items into its output. Example:
     * {@code MiningEvents.register(e -> { ... })}
     */
    public static void register(Consumer<MinerExtractEvent> listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    /**
     * @apiNote internal. Called by the mod when a miner extracts a batch.
     */
    public static void fire(MinerExtractEvent event) {
        for (Consumer<MinerExtractEvent> listener : LISTENERS) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                OresAndStuffMod.LOGGER.error("Mining event listener threw an exception", e);
            }
        }
    }
}