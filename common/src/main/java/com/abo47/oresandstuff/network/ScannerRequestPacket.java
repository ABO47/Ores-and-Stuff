package com.abo47.oresandstuff.network;

import com.abo47.oresandstuff.item.ScannerItem;
import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class ScannerRequestPacket implements IPacket {
    private ResourceLocation oreType;

    public ScannerRequestPacket() {
    }

    public ScannerRequestPacket(ResourceLocation oreType) {
        this.oreType = oreType;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(oreType);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        oreType = buf.readResourceLocation();
    }

    @Override
    public void execute(IHandlerContext handler) {
        if (handler.getPlayer() != null && handler.getServer() != null) {
            handler.getServer().execute(() -> NetworkServices.scan(handler.getPlayer(), oreType == null ? ScannerItem.getSelectedType(handler.getPlayer().getMainHandItem()) : oreType));
        }
    }
}
