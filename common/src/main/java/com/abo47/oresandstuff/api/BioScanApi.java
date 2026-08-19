package com.abo47.oresandstuff.api;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

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
}
