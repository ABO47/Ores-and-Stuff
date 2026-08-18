package com.abo47.oresandstuff.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;

import com.abo47.oresandstuff.client.OasClient;

public final class BioScanLibraryPacket implements IPacket {
    private List<Entry> entries = List.of();

    public BioScanLibraryPacket() {
    }

    public BioScanLibraryPacket(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUtf(entry.entityId());
            buf.writeUtf(entry.title());
            buf.writeUtf(entry.category());
            buf.writeUtf(entry.summary());
        }
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Entry> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            decoded.add(new Entry(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf()));
        }
        entries = decoded;
    }

    @Override
    public void execute(IHandlerContext handler) {
        OasClient.onBioScanLibrary(this);
    }

    public List<Entry> entries() {
        return entries;
    }

    public record Entry(String entityId, String title, String category, String summary) {
    }
}
