package com.abo47.oresandstuff.world;

import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.node.NodeVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

public final class NodeProtectionHandler {
    private NodeProtectionHandler() {
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos, Block block, Player player) {
        if (player.isCreative() || !NodeVisuals.isDecoration(block)) {
            return false;
        }
        if (block == ModBlocks.ORE_NODE) {
            return true;
        }
        return NodeLocatorService.findAnyNodeAround(level, pos, 6) != null;
    }
}
