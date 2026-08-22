package com.abo47.oresandstuff.api;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import com.abo47.oresandstuff.network.BioScanWorldData;
import com.abo47.oresandstuff.network.PlayerScanState;

/**
 * Server-side helpers to query and grant bio scan unlocks. Lets other mods
 * gate their content on scan progress, e.g. unlocking a recipe only after the
 * player has scanned a specific mob.
 */
public final class BioScanApi {
    private BioScanApi() {
    }

    public static boolean isScanned(ServerPlayer player, EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && isScanned(player, id);
    }

    public static boolean isScanned(ServerPlayer player, ResourceLocation entityId) {
        return entityId != null && PlayerScanState.bioScans(player).contains(entityId.toString());
    }

    /**
     * All entity ids the player has scanned, e.g. {@code "minecraft:pig"}.
     */
    public static List<String> scannedEntityIds(ServerPlayer player) {
        return PlayerScanState.bioScans(player);
    }

    /**
     * Unlocks a scan for the player. Does not fire {@link BioScanEvents}.
     *
     * @return true if the scan was newly unlocked
     */
    public static boolean grantScan(ServerPlayer player, EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && grantScan(player, id);
    }

    /**
     * Unlocks a scan for the player. Does not fire {@link BioScanEvents}.
     *
     * @return true if the scan was newly unlocked
     */
    public static boolean grantScan(ServerPlayer player, ResourceLocation entityId) {
        return entityId != null && PlayerScanState.addBioScan(player, entityId.toString());
    }

    /**
     * Unlocks a scan for any player by UUID, including players who are
     * currently offline. The unlock is persisted in world data and applies
     * the next time they join. Does not fire {@link BioScanEvents}.
     * <p>
     * Note: storage is per-dimension (same as regular scans), so pass the
     * level whose data set should hold the unlock.
     *
     * @return true if the scan was newly unlocked
     */
    public static boolean grantScan(ServerLevel level, UUID playerId, EntityType<?> type) {
        ResourceLocation id = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && grantScan(level, playerId, id);
    }

    /**
     * Unlocks a scan for any player by UUID, including players who are
     * currently offline. The unlock is persisted in world data and applies
     * the next time they join. Does not fire {@link BioScanEvents}.
     * <p>
     * Note: storage is per-dimension (same as regular scans), so pass the
     * level whose data set should hold the unlock.
     *
     * @return true if the scan was newly unlocked
     */
    public static boolean grantScan(ServerLevel level, UUID playerId, ResourceLocation entityId) {
        return level != null && playerId != null && entityId != null
                && BioScanWorldData.get(level).addScan(playerId, entityId.toString());
    }
}
