package com.abo47.oresandstuff.world;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.block.OreNodeBlock;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.data.BiomeDistributionRule;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.node.NodeVisuals;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.node.Purity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class OreNodeFeature extends Feature<NoneFeatureConfiguration> {
    public OreNodeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        List<BiomeDistributionRule> rules = OreNodeDataManager.INSTANCE.getDistributionsForDimension(level.getLevel().dimension().location());
        if (rules.isEmpty()) {
            return false;
        }

        int attempts = OresAndStuffConfig.worldgen().nodeAttemptsPerChunk;
        int spacing = OresAndStuffConfig.worldgen().nodeMinSpacingBlocks;
        boolean placed = false;
        for (int i = 0; i < attempts; i++) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos pos = new BlockPos(x, y, z);
            Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(pos);
            String biomeName = biome.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
            BiomeDistributionRule rule = rules.stream().filter(value -> biomeName.contains(value.biomePattern())).findFirst().orElse(rules.get(0));
            if (random.nextInt(Math.max(1, 4 - rule.maxNodesPerChunk())) != 0 || !level.getBlockState(pos).isSolidRender(level, pos)) {
                continue;
            }
            pos = pos.above();
            if (!passesSpacing(level, pos, spacing) || isObstructed(level, pos, 4) || isSteep(level, pos, 4, 4)) {
                continue;
            }
            var type = OreNodeDataManager.INSTANCE.rollNodeType(random, rule);
            Purity purity = OreNodeDataManager.INSTANCE.rollPurity(random, rule);
            placeCluster(level, pos, type.id(), purity, random);
            placed = true;
        }
        return placed;
    }

    private void placeCluster(WorldGenLevel level, BlockPos center, net.minecraft.resources.ResourceLocation typeId, Purity purity, RandomSource random) {
        int radius = Math.max(2, OresAndStuffConfig.worldgen().nodeClusterRadius + random.nextInt(2));
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
                    if (!canReplace(level, pos) || (dy == 0 && !level.getBlockState(pos.below()).isSolidRender(level, pos.below()))) {
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
    }

    private boolean canReplace(WorldGenLevel level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return level.getBlockState(pos).canBeReplaced() || block == Blocks.TALL_GRASS || block == Blocks.GRASS || block == Blocks.FERN || block == Blocks.SNOW;
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
