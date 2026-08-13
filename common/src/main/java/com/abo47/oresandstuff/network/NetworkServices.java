package com.abo47.oresandstuff.network;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.data.EntityScanDataManager;
import com.abo47.oresandstuff.data.EntityScanEntry;
import com.abo47.oresandstuff.item.ScannerItem;
import com.abo47.oresandstuff.world.NodeLocatorService;
import com.lowdragmc.lowdraglib.networking.LDLNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public final class NetworkServices {
    private NetworkServices() {
    }

    public static void scan(ServerPlayer player, ResourceLocation requestedType) {
        ResourceLocation type = requestedType == null ? ScannerItem.getSelectedType(player.getMainHandItem()) : requestedType;
        var results = NodeLocatorService.findNearestN(player.serverLevel(), player.blockPosition(), type, OresAndStuffConfig.scanner().radiusCap, OresAndStuffConfig.scanner().maxResults);
        List<ScannerResultPacket.NodeHit> hits = results.stream().map(result -> new ScannerResultPacket.NodeHit(result.pos(), (int) result.distance())).toList();
        LDLNetworking.NETWORK.sendToPlayer(new ScannerResultPacket(type, hits), player);
    }

    public static void bioScan(ServerPlayer player, int entityId) {
        var entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof LivingEntity living) || player.distanceToSqr(living) > 24.0D * 24.0D) {
            return;
        }
        ResourceLocation id = living.getType().builtInRegistryHolder().key().location();
        EntityScanEntry entry = EntityScanDataManager.INSTANCE.get(id).orElseGet(() -> new EntityScanEntry(id, living.getName().getString(), "Unknown", "No custom scan data found.", List.of("Health: " + (int) living.getMaxHealth(), "Width: " + living.getBbWidth(), "Height: " + living.getBbHeight())));
        PlayerScanState.addBioScan(player, id.toString());
        LDLNetworking.NETWORK.sendToPlayer(new BioScanInfoPacket(entry), player);
    }

    public static void sendLibrary(ServerPlayer player) {
        List<BioScanLibraryPacket.Entry> entries = new ArrayList<>();
        for (String discovered : PlayerScanState.bioScans(player)) {
            ResourceLocation id = ResourceLocation.tryParse(discovered);
            if (id == null) {
                continue;
            }
            EntityScanDataManager.INSTANCE.get(id).ifPresent(entry -> entries.add(new BioScanLibraryPacket.Entry(entry.entityId().toString(), entry.title(), entry.category(), entry.summary())));
        }
        LDLNetworking.NETWORK.sendToPlayer(new BioScanLibraryPacket(entries), player);
    }
}
