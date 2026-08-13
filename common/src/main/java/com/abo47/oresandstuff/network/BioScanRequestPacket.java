package com.abo47.oresandstuff.network;

import com.lowdragmc.lowdraglib.networking.IPacket;
import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import net.minecraft.network.FriendlyByteBuf;

public final class BioScanRequestPacket implements IPacket {
    private int targetEntityId;

    public BioScanRequestPacket() {
    }

    public BioScanRequestPacket(int targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetEntityId);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        targetEntityId = buf.readVarInt();
    }

    @Override
    public void execute(IHandlerContext handler) {
        if (handler.getPlayer() != null && handler.getServer() != null) {
            handler.getServer().execute(() -> NetworkServices.bioScan(handler.getPlayer(), targetEntityId));
        }
    }
}
