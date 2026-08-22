package com.abo47.oresandstuff.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

import com.lowdragmc.lowdraglib.networking.LDLNetworking;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.api.BioScanEvent;
import com.abo47.oresandstuff.api.BioScanEvents;
import com.abo47.oresandstuff.data.EntityScanEntry;
import com.abo47.oresandstuff.data.config.BioLibraryConfig;
import com.abo47.oresandstuff.item.ScannerItem;
import com.abo47.oresandstuff.world.NodeLocatorService;

public final class NetworkServices {
    private static final Map<EntityType<?>, Boolean> MOB_CACHE = new HashMap<>();

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
        EntityScanEntry entry = BioLibraryConfig.entryFor(living.getType(), id);
        boolean firstScan = PlayerScanState.addBioScan(player, id.toString());
        BioScanEvents.fire(new BioScanEvent(player, living, living.getType(), id, firstScan));
        LDLNetworking.NETWORK.sendToPlayer(new BioScanInfoPacket(entry), player);
    }

    public static void sendLibrary(ServerPlayer player) {
        List<String> discovered = PlayerScanState.bioScans(player);
        List<BioScanLibraryPacket.Entry> entries = new ArrayList<>();
        var bioCfg = OresAndStuffConfig.bioScan();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null) {
                continue;
            }
            if (bioCfg.hiddenEntities.contains(id.toString())) {
                continue;
            }
            if (bioCfg.hideNonMobs && !isMobType(type, player.serverLevel())) {
                continue;
            }
            EntityScanEntry entry = BioLibraryConfig.entryFor(type, id);
            boolean unlocked = discovered.contains(id.toString());
            entries.add(new BioScanLibraryPacket.Entry(id.toString(), entry.title(), entry.category(), entry.summary(), unlocked));
        }
        LDLNetworking.NETWORK.sendToPlayer(new BioScanLibraryPacket(entries), player);
    }

    private static boolean isMobType(EntityType<?> type, Level level) {
        Boolean cached = MOB_CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        boolean mob;
        try {
            Entity entity = type.create(level);
            mob = entity instanceof LivingEntity;
        } catch (Exception ignored) {
            mob = type.getCategory() != MobCategory.MISC;
        }
        MOB_CACHE.put(type, mob);
        return mob;
    }
}
