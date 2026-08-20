package com.abo47.oresandstuff.world;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModNodeBlocks;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.node.OreNodeType;

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
        boolean surfaceSpawn = type.effectiveSurfaceSpawn(biomeName);

        int attempts = type.effectivePlacementAttempts(biomeName);
        for (int i = 0; i < attempts; i++) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            BlockPos pos;
            if (surfaceSpawn) {
                int y = surfaceY(level, x, z);
                pos = new BlockPos(x, y, z);
                if (random.nextInt(Math.max(1, 4 - maxNodes)) != 0 || !level.getBlockState(pos).isSolidRender(level, pos)) {
                    continue;
                }
                pos = pos.above();
                if (!passesSpacing(level, pos, type.effectiveMinSpacing(biomeName)) || isObstructed(level, pos, 4) || isSteep(level, pos, 4, 4)) {
                    continue;
                }
            } else {
                int yMin = Math.max(level.getMinBuildHeight() + 1, type.effectiveMinY(biomeName));
                int yMax = Math.min(logicalTop(level), Math.max(yMin, type.effectiveMaxY(biomeName)));
                int y = yMin + random.nextInt(Math.max(1, yMax - yMin + 1));
                pos = new BlockPos(x, y, z);
                if (random.nextInt(Math.max(1, 4 - maxNodes)) != 0 || !isUndergroundHost(level, pos)) {
                    continue;
                }
                if (level.getBlockState(pos).isAir()) {
                    pos = caveFloor(level, pos);
                    if (pos == null) {
                        continue;
                    }
                }
                if (!passesSpacing(level, pos, type.effectiveMinSpacing(biomeName))) {
                    continue;
                }
            }
            double quality = OreNodeDataManager.INSTANCE.rollQuality(random, type, biomeName);
            placeCluster(level, pos, type, quality, random, type.effectiveScatterCount(biomeName), type.effectiveClusterRadius(biomeName), surfaceSpawn);
            return true;
        }
        return false;
    }

    private static String biomeName(WorldGenLevel level, BlockPos origin) {
        Holder<Biome> biome = level.getBiome(origin);
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
    }

    /**
     * Surface height for a node spawn. In the nether the regular heightmaps
     * return the bedrock ceiling (y ~127), so nodes would spawn unreachable;
     * instead the largest air gap in the column is located and the node is
     * placed on the solid block at the bottom of that gap - the actual nether
     * floor.
     *
     * <p>The scan is limited to the dimension's logical height (y0-y128 in the
     * nether) because the nether's build height is 256 - the empty roof void
     * above the bedrock ceiling (y128-y255) would otherwise be picked as the
     * largest air gap.
     */
    private static int surfaceY(WorldGenLevel level, int x, int z) {
        if (!level.getLevel().dimension().location().equals(Level.NETHER.location())) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        }
        int maxY = logicalTop(level);
        int minY = level.getMinBuildHeight();
        int bestAirStart = -1;
        int bestAirLen = 0;
        int runStart = -1;
        int runLen = 0;
        for (int y = maxY; y > minY; y--) {
            if (level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                if (runStart == -1) {
                    runStart = y;
                }
                runLen++;
                if (runLen > bestAirLen) {
                    bestAirLen = runLen;
                    bestAirStart = runStart;
                }
            } else {
                runStart = -1;
                runLen = 0;
            }
        }
        if (bestAirStart > minY) {
            int floor = bestAirStart - bestAirLen;
            if (floor > minY) {
                return floor;
            }
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    /**
     * Highest block y usable for placement. Uses the dimension's logical height
     * (the nether is 256 tall on paper but only 0-128 is real terrain - above
     * the bedrock ceiling is an empty roof void) so nodes never spawn in the
     * inaccessible area past the nether roof.
     */
    private static int logicalTop(WorldGenLevel level) {
        return level.getLevel().getLogicalHeight() - 1;
    }

    private void placeCluster(WorldGenLevel level, BlockPos center, OreNodeType type, double quality, RandomSource random, int scatterCount, int radius, boolean surfaceSpawn) {
        ResourceLocation typeId = type.id();
        int tierIndex = type.tierIndexFor(quality);
        OreNodeType.QualityTier tier = type.qualityTiers().get(tierIndex);
        Block nodeBlock = ModNodeBlocks.get(typeId, tierIndex);
        Block visualBlock = BuiltInRegistries.BLOCK.get(tier.visualBlock());
        if (visualBlock == Blocks.AIR) {
            visualBlock = Blocks.STONE;
        }
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
                        level.setBlock(pos, visualBlock.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, nodeBlock.defaultBlockState(), 2);
                        if (level.getBlockEntity(pos) instanceof OreNodeBlockEntity node) {
                            node.setNodeTypeId(typeId);
                            node.setQualityPercent(quality);
                            node.setNodeId(nodeId);
                        }
                    }
                    placed.add(pos.immutable());
                }
            }
        }
        if (!placed.contains(center)) {
            level.setBlock(center, nodeBlock.defaultBlockState(), 2);
            if (level.getBlockEntity(center) instanceof OreNodeBlockEntity node) {
                node.setNodeTypeId(typeId);
                node.setQualityPercent(quality);
                node.setNodeId(nodeId);
            }
        }

        // Decorative scatter: extra visual ore blocks around the cluster.
        var scatterBlock = visualBlock.defaultBlockState();
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
                    || block == Blocks.DRIPSTONE_BLOCK || block == Blocks.NETHERRACK || block == Blocks.BASALT
                    || block == Blocks.END_STONE;
        }
        return false;
    }

    /**
     * A node can be buried in solid stone or sit inside a cave air pocket
     * (enclosed underground: a non-fluid floor within a few blocks below and a
     * ceiling within a few blocks above).
     */
    private boolean isUndergroundHost(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return caveFloor(level, pos) != null && hasCaveCeiling(level, pos);
        }
        Block block = state.getBlock();
        return block != Blocks.BEDROCK && block != Blocks.WATER && block != Blocks.LAVA
                && (block == Blocks.STONE || block == Blocks.DEEPSLATE || block == Blocks.ANDESITE
                || block == Blocks.DIORITE || block == Blocks.GRANITE || block == Blocks.TUFF
                || block == Blocks.DRIPSTONE_BLOCK || block == Blocks.NETHERRACK || block == Blocks.BASALT
                || block == Blocks.END_STONE
                || state.canBeReplaced());
    }

    /**
     * The air block directly above the cave floor, or null when there is no
     * non-fluid floor within 8 blocks below the given position.
     */
    private BlockPos caveFloor(WorldGenLevel level, BlockPos pos) {
        for (int d = 1; d <= 8; d++) {
            BlockState below = level.getBlockState(pos.below(d));
            Block block = below.getBlock();
            if (!below.isAir() && block != Blocks.LAVA && block != Blocks.WATER) {
                return pos.below(d - 1);
            }
        }
        return null;
    }

    /** True when a solid block exists within 16 blocks above, i.e. not open to the surface. */
    private boolean hasCaveCeiling(WorldGenLevel level, BlockPos pos) {
        for (int d = 1; d <= 16; d++) {
            if (!level.getBlockState(pos.above(d)).isAir()) {
                return true;
            }
        }
        return false;
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
