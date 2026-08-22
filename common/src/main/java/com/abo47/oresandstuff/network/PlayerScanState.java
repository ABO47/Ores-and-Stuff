package com.abo47.oresandstuff.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

public final class PlayerScanState {
    private static final Map<UUID, Long> MINE_COOLDOWNS = new HashMap<>();

    private PlayerScanState() {
    }

    public static synchronized boolean addBioScan(ServerPlayer player, String id) {
        return BioScanWorldData.get(player.serverLevel()).addScan(player.getUUID(), id);
    }

    public static synchronized List<String> bioScans(ServerPlayer player) {
        return BioScanWorldData.get(player.serverLevel()).bioScans(player.getUUID());
    }

    public static synchronized long mineCooldown(ServerPlayer player) {
        return MINE_COOLDOWNS.getOrDefault(player.getUUID(), 0L);
    }

    public static synchronized void mineCooldown(ServerPlayer player, long value) {
        MINE_COOLDOWNS.put(player.getUUID(), value);
    }
}