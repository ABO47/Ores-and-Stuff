package com.abo47.oresandstuff.network;

import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class ScannerResultPacket implements IPacket {
    private ResourceLocation oreType = new ResourceLocation("oresandstuff", "iron");
    private List<NodeHit> hits = List.of();

    public ScannerResultPacket() {
    }

    public ScannerResultPacket(ResourceLocation oreType, List<NodeHit> hits) {
        this.oreType = oreType;
        this.hits = hits;
    }

    public ResourceLocation oreType() {
        return oreType;
    }

    public List<NodeHit> hits() {
        return hits;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(oreType);
        buf.writeVarInt(hits.size());
        for (NodeHit hit : hits) {
            buf.writeBlockPos(hit.pos());
            buf.writeVarInt(hit.distance());
        }
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        oreType = buf.readResourceLocation();
        int count = buf.readVarInt();
        List<NodeHit> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            decoded.add(new NodeHit(buf.readBlockPos(), buf.readVarInt()));
        }
        hits = decoded;
    }

    @Override
    public void execute(IHandlerContext handler) {
        NetworkClientState.setScannerResult(this);
    }

    public record NodeHit(BlockPos pos, int distance) {
    }
}
