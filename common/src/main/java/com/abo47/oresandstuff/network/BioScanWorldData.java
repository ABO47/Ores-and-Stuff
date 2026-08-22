package com.abo47.oresandstuff.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import com.abo47.oresandstuff.OresAndStuffMod;

public final class BioScanWorldData extends SavedData {
    private static final String DATA_NAME = OresAndStuffMod.MOD_ID + "_bio_scans";

    private final Map<UUID, List<String>> scansByPlayer = new HashMap<>();

    public BioScanWorldData() {
    }

    public static BioScanWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                BioScanWorldData::load,
                BioScanWorldData::new,
                DATA_NAME
        );
    }

    public static BioScanWorldData load(CompoundTag tag) {
        BioScanWorldData data = new BioScanWorldData();
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            UUID uuid = entry.getUUID("Player");
            ListTag ids = entry.getList("Entities", Tag.TAG_STRING);
            List<String> scanned = new ArrayList<>();
            for (int j = 0; j < ids.size(); j++) {
                scanned.add(ids.getString(j));
            }
            data.scansByPlayer.put(uuid, scanned);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, List<String>> entry : scansByPlayer.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Player", entry.getKey());
            ListTag ids = new ListTag();
            for (String id : entry.getValue()) {
                ids.add(StringTag.valueOf(id));
            }
            playerTag.put("Entities", ids);
            players.add(playerTag);
        }
        tag.put("Players", players);
        return tag;
    }

    public boolean addScan(UUID playerUuid, String entityId) {
        List<String> scans = scansByPlayer.computeIfAbsent(playerUuid, ignored -> new ArrayList<>());
        if (scans.contains(entityId)) {
            return false;
        }
        scans.add(entityId);
        setDirty();
        return true;
    }

    public List<String> bioScans(UUID playerUuid) {
        return List.copyOf(scansByPlayer.getOrDefault(playerUuid, List.of()));
    }
}