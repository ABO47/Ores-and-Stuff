package com.abo47.oresandstuff.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;

import com.abo47.oresandstuff.client.OasClient;
import com.abo47.oresandstuff.data.EntityScanEntry;

public final class BioScanInfoPacket implements IPacket {
    private String entityId = "minecraft:pig";
    private String title = "Unknown";
    private String category = "Unknown";
    private String summary = "No scan data.";
    private List<String> facts = List.of();

    public BioScanInfoPacket() {
    }

    public BioScanInfoPacket(EntityScanEntry entry) {
        entityId = entry.entityId().toString();
        title = entry.title();
        category = entry.category();
        summary = entry.summary();
        facts = entry.facts();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(entityId);
        buf.writeUtf(title);
        buf.writeUtf(category);
        buf.writeUtf(summary);
        buf.writeVarInt(facts.size());
        facts.forEach(buf::writeUtf);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        entityId = buf.readUtf();
        title = buf.readUtf();
        category = buf.readUtf();
        summary = buf.readUtf();
        int count = buf.readVarInt();
        List<String> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            decoded.add(buf.readUtf());
        }
        facts = decoded;
    }

    @Override
    public void execute(IHandlerContext handler) {
        OasClient.onBioScanInfo(this);
    }

    public String entityId() { return entityId; }
    public String title() { return title; }
    public String category() { return category; }
    public String summary() { return summary; }
    public List<String> facts() { return facts; }
}
