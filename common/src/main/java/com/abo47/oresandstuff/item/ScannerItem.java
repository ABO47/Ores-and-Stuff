package com.abo47.oresandstuff.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.client.screen.ScannerSelectScreen;
import com.abo47.oresandstuff.content.ModSounds;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.network.NetworkChannels;
import com.abo47.oresandstuff.network.NetworkServices;

public class ScannerItem extends Item {
    public enum Mode {
        RESOURCE,
        BIO;

        public Mode toggle() {
            return this == RESOURCE ? BIO : RESOURCE;
        }
    }

    private static final String TAG_MODE = "ScannerMode";

    public ScannerItem(Properties properties) {
        super(properties);
    }

    public static Mode getMode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Mode.RESOURCE;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MODE)) {
            try {
                return Mode.valueOf(tag.getString(TAG_MODE));
            } catch (Exception e) {
                return Mode.RESOURCE;
            }
        }
        // legacy bio_scanner item defaults to BIO if no NBT yet
        if (stack.getItem() instanceof BioScannerItem) return Mode.BIO;
        return Mode.RESOURCE;
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateTag().putString(TAG_MODE, mode.name());
    }

    public static Mode toggleMode(ItemStack stack) {
        Mode cur = getMode(stack);
        Mode next = cur.toggle();
        setMode(stack, next);
        return next;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.oresandstuff.scanner");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Mode mode = getMode(stack);
        Component modeName = Component.translatable(mode == Mode.BIO ? "item.oresandstuff.bio_scanner" : "item.oresandstuff.resource_scanner");
        tooltip.add(Component.translatable("item.oresandstuff.scanner.mode", modeName).withStyle(s -> s.withColor(0xFF8B98A8)));
        tooltip.add(Component.translatable("key.oresandstuff.switch_scanner_mode").withStyle(s -> s.withColor(0xFF6BA8FF)));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Mode mode = getMode(stack);

        if (mode == Mode.BIO) {
            if (player.isShiftKeyDown() && !level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkServices.sendLibrary(serverPlayer);
                return InteractionResultHolder.success(stack);
            }
            if (player.isShiftKeyDown() && level.isClientSide) {
                // client shift+use in BIO mode opens library via server request (handled above server side),
                // return sidedSuccess to avoid starting use
                return InteractionResultHolder.sidedSuccess(stack, true);
            }
            // scan progress is driven by OasClient key polling; pass silently here so vanilla's
            // held-use loop never replays the use animation during long scans
            return InteractionResultHolder.pass(stack);
        } else {
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
            var scanCfg = OresAndStuffConfig.scanner();
            if (scanCfg.scanSoundEnabled) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.SCANNER_SWEEP, SoundSource.PLAYERS, (float) scanCfg.scanSoundVolume, (float) scanCfg.scanSoundPitch, false);
            }
            if (OresAndStuffConfig.scanner().cooldownTicks > 0) {
                player.getCooldowns().addCooldown(this, OresAndStuffConfig.scanner().cooldownTicks);
            }
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
    }

    public static ResourceLocation getSelectedType(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ResourceLocation selected = ResourceLocation.tryParse(tag.getString("SelectedType"));
        return selected != null ? selected : OreNodeDataManager.INSTANCE.orderedTypeIds().stream().findFirst().orElse(new ResourceLocation("oresandstuff", "iron"));
    }
}
