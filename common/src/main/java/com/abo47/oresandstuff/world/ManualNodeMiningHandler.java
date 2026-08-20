package com.abo47.oresandstuff.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.abo47.oresandstuff.data.config.PickaxeToolConfig;
import com.abo47.oresandstuff.network.PlayerScanState;
import com.abo47.oresandstuff.node.ExtractionRateService;
import com.abo47.oresandstuff.node.NodeVisuals;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;

public final class ManualNodeMiningHandler {
    private ManualNodeMiningHandler() {
    }

    public static boolean handle(Level level, Player player, BlockPos pos) {
        ItemStack stack = player.getMainHandItem();
        if (!PickaxeToolConfig.isExtractor(stack.getItem())) {
            return false;
        }
        if (!NodeVisuals.isDecoration(level.getBlockState(pos).getBlock()) && !(level.getBlockEntity(pos) instanceof OreNodeBlockEntity)) {
            return false;
        }
        if (!(level instanceof ServerLevel server)) {
            return true;
        }
        OreNodeBlockEntity node = level.getBlockEntity(pos) instanceof OreNodeBlockEntity exact ? exact : NodeLocatorService.findAnyNodeAround(server, pos, 5);
        if (node == null) {
            return true;
        }
        long now = server.getGameTime();
        var spec = PickaxeToolConfig.specFor(stack.getItem()).orElse(null);
        if (spec == null) {
            return true;
        }
        int cooldown = spec.cooldownFor(node.getQualityPercent());
        String key = "oresandstuff_node_mine_cd";
        if (now < PlayerScanState.mineCooldown((ServerPlayer) player)) {
            return true;
        }
        var drops = ExtractionRateService.buildDrops(node, spec.extractAmount(), spec.amountMultiplierFor(node.getQualityPercent()));
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            server.addFreshEntity(new ItemEntity(server, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, drop));
        }
        if (spec.durabilityCost() > 0) {
            stack.hurtAndBreak(spec.durabilityCost(), player, p -> {
            });
        }
        PlayerScanState.mineCooldown((ServerPlayer) player, now + cooldown);
        player.getCooldowns().addCooldown(stack.getItem(), cooldown);
        return true;
    }
}
