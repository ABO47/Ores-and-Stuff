package com.abo47.oresandstuff.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.node.NodeVisuals;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;

final class NodeClusterTracker {
    private static final Map<Long, ScannerFxTypes.FlashFx> ACTIVE_FLASHES = new HashMap<>();
    private static final Map<Long, Long> HIT_BURSTS = new HashMap<>();

    private NodeClusterTracker() {
    }

    static Map<Long, ScannerFxTypes.FlashFx> activeFlashes() {
        return ACTIVE_FLASHES;
    }

    static void resetScanState() {
        ACTIVE_FLASHES.clear();
        HIT_BURSTS.clear();
    }

    static Map<Long, Long> hitBursts() {
        return HIT_BURSTS;
    }

    static boolean isNodeTouched(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    if (ACTIVE_FLASHES.containsKey(ScannerRenderUtil.packPosKey(x + dx, y + dy, z + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static void triggerOnWaveHit(Minecraft minecraft, ScannerFxTypes.ScanPulse pulse, float radius, long now,
                                 List<ScannerFxTypes.TargetMarker> markers, ResourceLocation oreType) {
        for (ScannerFxTypes.TargetMarker marker : markers) {
            if (!marker.visible(now)) continue;
            double mdx = (marker.pos.getX() + 0.5) - pulse.originX;
            double mdz = (marker.pos.getZ() + 0.5) - pulse.originZ;
            float markerRadius = (float) Math.sqrt(mdx * mdx + mdz * mdz);
            if (Math.abs(markerRadius - radius) < 1.6f) {
                addNodeClusterFlash(minecraft, marker.pos, oreType, now + 8000L);
                long key = ScannerRenderUtil.packPosKey(marker.pos.getX(), marker.pos.getY(), marker.pos.getZ());
                if (!pulse.pingedNodeKeys.contains(key)) {
                    pulse.pingedNodeKeys.add(key);
                    var sc = OresAndStuffConfig.scanner();
                    if (sc.scanSoundEnabled && minecraft.player != null && minecraft.level != null) {
                        minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, (float) sc.scanSoundVolume, (float) sc.scanSoundPitch, false);
                    }
                }
            }
        }
    }

    private static void addNodeClusterFlash(Minecraft minecraft, BlockPos center, ResourceLocation oreTypeId, long expiresAt) {
        if (minecraft == null || minecraft.level == null) return;
        Set<BlockPos> seeds = collectNodeSeeds(minecraft, center, oreTypeId, 10);
        if (seeds.isEmpty()) seeds.add(center);

        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        Set<BlockPos> cluster = new HashSet<>();
        for (BlockPos seed : seeds) q.add(seed.immutable());
        while (!q.isEmpty() && seen.size() < 12000) {
            BlockPos p = q.poll();
            long key = ScannerRenderUtil.packPosKey(p.getX(), p.getY(), p.getZ());
            if (!seen.add(key)) continue;
            if (!isNodeVisualBlock(minecraft, p, oreTypeId, center, 22)) continue;
            cluster.add(p.immutable());
            ScannerFxTypes.FlashFx fx = ACTIVE_FLASHES.get(key);
            boolean isNew = fx == null;
            if (isNew) ACTIVE_FLASHES.put(key, new ScannerFxTypes.FlashFx(p.getX(), p.getY(), p.getZ(), expiresAt));
            else fx.expiresAt = Math.max(fx.expiresAt, expiresAt);
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        if (ox == 0 && oy == 0 && oz == 0) continue;
                        q.add(p.offset(ox, oy, oz));
                    }
                }
            }
        }
    }

    private static Set<BlockPos> collectNodeSeeds(Minecraft minecraft, BlockPos center, ResourceLocation oreTypeId, int radius) {
        Set<BlockPos> seeds = new HashSet<>();
        if (minecraft == null || minecraft.level == null) return seeds;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!(minecraft.level.getBlockEntity(p) instanceof OreNodeBlockEntity be)) continue;
                    if (oreTypeId == null || be.getNodeTypeId().equals(oreTypeId)) seeds.add(p.immutable());
                }
            }
        }
        return seeds;
    }

    private static boolean isNodeVisualBlock(Minecraft minecraft, BlockPos pos, ResourceLocation oreTypeId, BlockPos center, int maxDist) {
        if (minecraft == null || minecraft.level == null) return false;
        if (Math.abs(pos.getX() - center.getX()) > maxDist || Math.abs(pos.getY() - center.getY()) > 8 || Math.abs(pos.getZ() - center.getZ()) > maxDist) return false;
        Block block = minecraft.level.getBlockState(pos).getBlock();
        if (block == ModBlocks.ORE_NODE) return true;
        Block ore = NodeVisuals.visualOre(oreTypeId, false);
        Block deepOre = NodeVisuals.visualOre(oreTypeId, true);
        return (ore != null && block == ore) || (deepOre != null && block == deepOre);
    }
}
