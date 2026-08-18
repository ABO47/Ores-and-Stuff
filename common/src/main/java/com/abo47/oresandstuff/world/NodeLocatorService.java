package com.abo47.oresandstuff.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import com.abo47.oresandstuff.node.OreNodeBlockEntity;

public final class NodeLocatorService {
    private NodeLocatorService() {
    }

    public record NodeSearchResult(BlockPos pos, double distance) {
    }

    public static List<NodeSearchResult> findNearestN(ServerLevel level, BlockPos from, ResourceLocation type, int radius, int maxResults) {
        Map<UUID, NodeSearchResult> unique = new HashMap<>();
        int chunkRadius = Math.max(1, radius / 16);
        ChunkPos center = new ChunkPos(from);
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                if (chunk == null) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof OreNodeBlockEntity node && node.getNodeTypeId().equals(type)) {
                        double distance = Math.sqrt(from.distSqr(be.getBlockPos()));
                        if (distance <= radius) {
                            unique.merge(node.getNodeId(), new NodeSearchResult(be.getBlockPos(), distance), (a, b) -> a.distance() <= b.distance() ? a : b);
                        }
                    }
                }
            }
        }
        List<NodeSearchResult> results = new ArrayList<>(unique.values());
        results.sort(Comparator.comparingDouble(NodeSearchResult::distance));
        return results.size() > maxResults ? new ArrayList<>(results.subList(0, maxResults)) : results;
    }

    public static NodeSearchResult findNearest(ServerLevel level, BlockPos from, ResourceLocation type, int radius) {
        List<NodeSearchResult> results = findNearestN(level, from, type, radius, 1);
        return results.isEmpty() ? null : results.get(0);
    }

    public static OreNodeBlockEntity findAnyNodeAround(ServerLevel level, BlockPos from, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (level.getBlockEntity(from.offset(dx, dy, dz)) instanceof OreNodeBlockEntity node) {
                        return node;
                    }
                }
            }
        }
        return null;
    }
}
