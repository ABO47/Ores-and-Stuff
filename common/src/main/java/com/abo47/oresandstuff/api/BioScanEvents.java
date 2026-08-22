package com.abo47.oresandstuff.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.abo47.oresandstuff.OresAndStuffMod;

/**
 * Server-side event bus for bio scan mechanics. Loader-agnostic: works from
 * Forge and Fabric mods alike.
 */
public final class BioScanEvents {
    private static final List<Consumer<BioScanEvent>> LISTENERS = new ArrayList<>();

    private BioScanEvents() {
    }

    /**
     * Registers a listener fired on the server every time a player completes a
     * bio scan. Example: {@code BioScanEvents.register(e -> { ... })}
     */
    public static void register(Consumer<BioScanEvent> listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    /**
     * @apiNote internal. Called by the mod when a scan completes.
     */
    public static void fire(BioScanEvent event) {
        for (Consumer<BioScanEvent> listener : LISTENERS) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                OresAndStuffMod.LOGGER.error("Bio scan event listener threw an exception", e);
            }
        }
    }
}
