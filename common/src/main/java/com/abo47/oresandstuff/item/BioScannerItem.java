package com.abo47.oresandstuff.item;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.network.BioScanRequestPacket;
import com.abo47.oresandstuff.network.NetworkServices;
import com.lowdragmc.lowdraglib.networking.LDLNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class BioScannerItem extends Item {
    public BioScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            NetworkServices.sendLibrary(serverPlayer);
            return InteractionResultHolder.success(stack);
        }
        if (level.isClientSide) {
            var hit = player.pick(24.0D, 0.0F, false);
            if (hit instanceof EntityHitResult entityHit) {
                LDLNetworking.NETWORK.sendToServer(new BioScanRequestPacket(entityHit.getEntity().getId()));
            }
        }
        if (OresAndStuffConfig.bioScan().cooldownTicks > 0) {
            player.getCooldowns().addCooldown(this, OresAndStuffConfig.bioScan().cooldownTicks);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }
}
