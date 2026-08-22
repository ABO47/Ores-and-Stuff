package com.abo47.oresandstuff.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.node.OreNodeType;

/**
 * Server-side helpers to find ore nodes and their attached miners. Lets other
 * mods (e.g. Multiblocked2 addons) drive machines off the node system without
 * touching internal classes.
 */
public final class NodeApi {
    private NodeApi() {
    }

    /** The node block at the exact position, if any. */
    public static Optional<OreNodeHandle> nodeAt(Level level, BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return Optional.empty();
        }
        if (level.getBlockEntity(pos) instanceof OreNodeBlockEntity node) {
            return Optional.of(handle(node));
        }
        return Optional.empty();
    }

    /**
     * Nearest node inside the box {@code center +- (radiusXZ, radiusY)}.
     * Returns the node closest to the center by squared distance.
     */
    public static Optional<OreNodeHandle> findNearestNode(Level level, BlockPos center, int radiusXZ, int radiusY) {
        OreNodeBlockEntity best = null;
        long bestDist = Long.MAX_VALUE;
        for (int dy = -radiusY; dy <= radiusY; dy++) {
            for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
                for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getBlockEntity(pos) instanceof OreNodeBlockEntity node) {
                        long dist = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = node;
                        }
                    }
                }
            }
        }
        return best == null ? Optional.empty() : Optional.of(handle(best));
    }

    /** Every node inside the box {@code center +- (radiusXZ, radiusY)}. */
    public static List<OreNodeHandle> findNodesInArea(Level level, BlockPos center, int radiusXZ, int radiusY) {
        List<OreNodeHandle> out = new ArrayList<>();
        for (int dy = -radiusY; dy <= radiusY; dy++) {
            for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
                for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getBlockEntity(pos) instanceof OreNodeBlockEntity node) {
                        out.add(handle(node));
                    }
                }
            }
        }
        return out;
    }

    /** Counts miners whose attached node is this node (mirrors the in-mod limit check). */
    public static int attachedMiners(Level level, OreNodeHandle node) {
        if (level == null || node == null) {
            return 0;
        }
        OreNodeType type = OreNodeDataManager.INSTANCE.getNodeType(node.typeId()).orElse(null);
        int scanXZ = Math.min(9, Math.max(4, 2 * (type != null ? type.clusterRadius() : 2) + 1));
        int count = 0;
        for (int dx = -scanXZ; dx <= scanXZ; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -scanXZ; dz <= scanXZ; dz++) {
                    BlockPos pos = node.pos().offset(dx, dy, dz);
                    if (level.getBlockEntity(pos) instanceof MinerBlockEntity miner) {
                        OreNodeBlockEntity other = miner.getAttachedNode();
                        if (other != null && other.getNodeId().equals(node.nodeId())) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    /** Whether the node's configured max_miners_per_node is already reached. */
    public static boolean minerLimitReached(Level level, OreNodeHandle node) {
        if (node == null) {
            return false;
        }
        int max = OreNodeDataManager.INSTANCE.getNodeType(node.typeId())
                .map(OreNodeType::maxMinersPerNode).orElse(1);
        return attachedMiners(level, node) >= max;
    }

    /** All configured node type ids, e.g. {@code "oresandstuff:iron"}. */
    public static List<ResourceLocation> nodeTypeIds() {
        return OreNodeDataManager.INSTANCE.orderedTypeIds();
    }

    /** The configured node type for an id. */
    public static Optional<OreNodeType> nodeType(ResourceLocation id) {
        return OreNodeDataManager.INSTANCE.getNodeType(id);
    }

    private static OreNodeHandle handle(OreNodeBlockEntity node) {
        return new OreNodeHandle(node.getBlockPos(), node.getNodeTypeId(), node.getQualityPercent(), node.getNodeId());
    }
}