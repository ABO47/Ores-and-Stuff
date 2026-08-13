package com.abo47.oresandstuff.item;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.client.screen.ScannerSelectScreen;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.network.NetworkChannels;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ScannerItem extends Item {
    public ScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                Minecraft.getInstance().setScreen(new ScannerSelectScreen(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        NetworkChannels.sendScannerRequest(getSelectedType(stack));
        if (OresAndStuffConfig.scanner().cooldownTicks > 0) {
            player.getCooldowns().addCooldown(this, OresAndStuffConfig.scanner().cooldownTicks);
        }
        return InteractionResultHolder.sidedSuccess(stack, true);
    }

    public static ResourceLocation getSelectedType(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ResourceLocation selected = ResourceLocation.tryParse(tag.getString("SelectedType"));
        return selected != null ? selected : OreNodeDataManager.INSTANCE.orderedTypeIds().stream().findFirst().orElse(new ResourceLocation("oresandstuff", "iron"));
    }
}
