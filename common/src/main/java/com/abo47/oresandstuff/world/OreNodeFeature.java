package com.abo47.oresandstuff.world;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.block.OreNodeBlock;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.node.NodeVisuals;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.node.OreNodeType;
import com.abo47.oresandstuff.node.Purity;

public final class OreNodeFeature extends Feature<NoneFeatureConfiguration> {
    public OreNodeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        List<OreNodeType> types = OreNodeDataManager.INSTANCE.typesForDimension(level.getLevel().dimension().location());
        if (types.isEmpty()) {
            return false;
        }

        String biomeName = biomeName(level, origin);
        List<OreNodeType> candidates = types.stream().filter(t -> t.biomeWeight(biomeName) > 0).toList();
        if (candidates.isEmpty()) {
            return false;
        }
        OreNodeType type = OreNodeDataManager.INSTANCE.rollNodeType(random, biomeName, candidates);
        int maxNodes = type.effectiveMaxNodes(biomeName);

        int attempts = Math.max(1, type.placementAttempts());
        for (int i = 0; i < attempts; i++) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            BlockPos pos;
            if (type.surfaceSpawn()) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                pos = new BlockPos(x, y, z);
                if (random.nextInt(Math.max(1, 4 - maxNodes)) != 0 || !level.getBlockState(pos).isSolidRender(level, pos)) {
                    continue;
                }
                pos = pos.above();
                if (!passesSpacing(level, pos, type.minSpacingBlocks()) || isObstructed(level, pos, 4) || isSteep(level, pos, 4, 4)) {
                    continue;
                }
            } else {
                int yMin = Math.max(level.getMinBuildHeight() + 1, type.minY());
                int yMax = Math.min(level.getMaxBuildHeight() - 1, Math.max(yMin, type.maxY()));
                int y = yMin + random.nextInt(Math.max(1, yMax - yMin + 1));
                pos = new BlockPos(x, y, z);
                if (random.nextInt(Math.max(1, 4 - maxNodes)) != 0 || !isUndergroundHost(level, pos)) {
                    continue;
                }
                if (!passesSpacing(level, pos, type.minSpacingBlocks())) {
                    continue;
                }
            }
            Purity purity = OreNodeDataManager.INSTANCE.rollPurity(random, type, biomeName);
            placeCluster(level, pos, type, purity, random, type.effectiveScatterCount(biomeName), type.effectiveClusterRadius(biomeName), type.surfaceSpawn());
            return true;
        }
        return false;
    }

    private static String biomeName(WorldGenLevel level, BlockPos origin) {
        Holder<Biome> biome = level.getBiome(origin);
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
    }

    private void placeCluster(WorldGenLevel level, BlockPos center, OreNodeType type, Purity purity, RandomSource random, int scatterCount, int radius, boolean surfaceSpawn) {
        ResourceLocation typeId = type.id();
        int quality = purity == Purity.PURE ? 2 : purity == Purity.NORMAL ? 1 : 0;
        UUID nodeId = UUID.nameUUIDFromBytes((level.getLevel().dimension().location() + ":" + center).getBytes(StandardCharsets.UTF_8));
        Set<BlockPos> placed = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) > radius + 0.35) {
                    continue;
                }
                int height = dx * dx + dz * dz < radius * radius * 0.38 && random.nextFloat() < 0.55F ? 2 : 1;
                for (int dy = 0; dy < height; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!canReplace(level, pos, surfaceSpawn) || (surfaceSpawn && dy == 0 && !level.getBlockState(pos.below()).isSolidRender(level, pos.below()))) {
                        continue;
                    }
                    if (random.nextFloat() < 0.30F) {
                        level.setBlock(pos, NodeVisuals.visualOre(typeId, purity == Purity.PURE).defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, ModBlocks.ORE_NODE.defaultBlockState().setValue(OreNodeBlock.QUALITY, quality), 2);
                        if (level.getBlockEntity(pos) instanceof OreNodeBlockEntity node) {
                            node.setNodeTypeId(typeId);
                            node.setPurity(purity);
                            node.setNodeId(nodeId);
                        }
                    }
                    placed.add(pos.immutable());
                }
            }
        }
        if (!placed.contains(center)) {
            level.setBlock(center, ModBlocks.ORE_NODE.defaultBlockState().setValue(OreNodeBlock.QUALITY, quality), 2);
            if (level.getBlockEntity(center) instanceof OreNodeBlockEntity node) {
                node.setNodeTypeId(typeId);
                node.setPurity(purity);
                node.setNodeId(nodeId);
            }
        }

        // Decorative scatter: extra visual ore blocks around the cluster.
        var scatterBlock = NodeVisuals.visualOre(typeId, purity == Purity.PURE).defaultBlockState();
        for (int s = 0; s < scatterCount; s++) {
            int sx = center.getX() + random.nextInt(radius * 2 + 1) - radius;
            int sy = center.getY() + random.nextInt(3) - 1;
            int sz = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos sp = new BlockPos(sx, sy, sz);
            if (canReplace(level, sp, surfaceSpawn) && (!surfaceSpawn || level.getBlockState(sp.below()).isSolidRender(level, sp.below()))) {
                level.setBlock(sp, scatterBlock, 2);
            }
        }
    }

    private boolean canReplace(WorldGenLevel level, BlockPos pos, boolean surfaceSpawn) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (state.canBeReplaced() || block == Blocks.TALL_GRASS || block == Blocks.GRASS || block == Blocks.FERN || block == Blocks.SNOW) {
            return true;
        }
        if (!surfaceSpawn) {
            return block == Blocks.STONE || block == Blocks.DEEPSLATE || block == Blocks.ANDESITE
                    || block == Blocks.DIORITE || block == Blocks.GRANITE || block == Blocks.TUFF
                    || block == Blocks.DRIPSTONE_BLOCK || block == Blocks.NETHERRACK || block == Blocks.BASALT;
        }
        return false;
    }

    private boolean isUndergroundHost(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        return !state.isAir() && block != Blocks.BEDROCK && block != Blocks.WATER && block != Blocks.LAVA
                && (block == Blocks.STONE || block == Blocks.DEEPSLATE || block == Blocks.ANDESITE
                || block == Blocks.DIORITE || block == Blocks.GRANITE || block == Blocks.TUFF
                || block == Blocks.DRIPSTONE_BLOCK || block == Blocks.NETHERRACK || block == Blocks.BASALT
                || state.canBeReplaced());
    }

    private boolean passesSpacing(WorldGenLevel level, BlockPos pos, int spacing) {
        int spacingChunks = Math.max(1, (spacing + 15) / 16);
        long hash = level.getSeed() ^ ((long) (pos.getX() >> 4) * 73428767L) ^ ((long) (pos.getZ() >> 4) * 912931L);
        return Math.floorMod(hash, spacingChunks) == 0;
    }

    private boolean isObstructed(WorldGenLevel level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= 6; dy++) {
                    Block block = level.getBlockState(center.offset(dx, dy, dz)).getBlock();
                    if (level.getBlockState(center.offset(dx, dy, dz)).is(BlockTags.LOGS) || level.getBlockState(center.offset(dx, dy, dz)).is(BlockTags.LEAVES) || block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.BROWN_MUSHROOM_BLOCK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSteep(WorldGenLevel level, BlockPos center, int radius, int maxDelta) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX() + dx, center.getZ() + dz);
                min = Math.min(min, y);
                max = Math.max(max, y);
                if (max - min > maxDelta) {
                    return true;
                }
            }
        }
        return false;
    }
}
