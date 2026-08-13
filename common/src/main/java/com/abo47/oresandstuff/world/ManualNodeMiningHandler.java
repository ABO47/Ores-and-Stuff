package com.abo47.oresandstuff.world;

import com.abo47.oresandstuff.item.NodeExtractorPickaxeItem;
import com.abo47.oresandstuff.network.PlayerScanState;
import com.abo47.oresandstuff.node.ExtractionRateService;
import com.abo47.oresandstuff.node.NodeVisuals;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class ManualNodeMiningHandler {
    private ManualNodeMiningHandler() {
    }

    public static boolean handle(Level level, Player player, BlockPos pos) {
        if (!(player.getMainHandItem().getItem() instanceof NodeExtractorPickaxeItem)) {
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
        int cooldown = switch (node.getPurity()) {
            case IMPURE -> 80;
            case NORMAL -> 50;
            case PURE -> 30;
        };
        String key = "oresandstuff_node_mine_cd";
        if (now < PlayerScanState.mineCooldown((ServerPlayer) player)) {
            return true;
        }
        var drop = ExtractionRateService.buildDrop(node, 1);
        if (!drop.isEmpty()) {
            server.addFreshEntity(new ItemEntity(server, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, drop));
        }
        PlayerScanState.mineCooldown((ServerPlayer) player, now + cooldown);
        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldown);
        return true;
    }
}
