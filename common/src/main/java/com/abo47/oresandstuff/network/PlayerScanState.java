package com.abo47.oresandstuff.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerScanState {
    private static final Map<UUID, List<String>> BIO_SCANS = new HashMap<>();
    private static final Map<UUID, Long> MINE_COOLDOWNS = new HashMap<>();

    private PlayerScanState() {
    }

    public static synchronized boolean addBioScan(ServerPlayer player, String id) {
        List<String> scans = BIO_SCANS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        if (scans.contains(id)) {
            return false;
        }
        scans.add(id);
        return true;
    }

    public static synchronized List<String> bioScans(ServerPlayer player) {
        return List.copyOf(BIO_SCANS.getOrDefault(player.getUUID(), List.of()));
    }

    public static synchronized long mineCooldown(ServerPlayer player) {
        return MINE_COOLDOWNS.getOrDefault(player.getUUID(), 0L);
    }

    public static synchronized void mineCooldown(ServerPlayer player, long value) {
        MINE_COOLDOWNS.put(player.getUUID(), value);
    }
}
