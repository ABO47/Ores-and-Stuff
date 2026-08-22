package com.abo47.oresandstuff.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;

import com.abo47.oresandstuff.item.ScannerItem;

public final class ScannerModeTogglePacket implements IPacket {
    public ScannerModeTogglePacket() {
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
    }

    @Override
    public void execute(IHandlerContext handler) {
        if (handler.getPlayer() == null || handler.getServer() == null) return;
        handler.getServer().execute(() -> {
            var player = handler.getPlayer();
            // check both hands, prefer main hand
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof ScannerItem) {
                    ScannerItem.toggleMode(stack);
                    // ensure inventory sync
                    player.inventoryMenu.broadcastChanges();
                    break;
                }
            }
        });
    }
}
