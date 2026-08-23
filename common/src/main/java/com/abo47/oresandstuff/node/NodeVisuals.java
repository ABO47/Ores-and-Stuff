package com.abo47.oresandstuff.node;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.abo47.oresandstuff.block.OreNodeBlock;
import com.abo47.oresandstuff.data.OreNodeDataManager;

public final class NodeVisuals {
    private static volatile Set<Block> visualBlockCache;

    private NodeVisuals() {
    }

    public static boolean isDecoration(Block block) {
        if (block instanceof OreNodeBlock) {
            return true;
        }
        return isVisualOre(block);
    }

    /** Whether the block is a tier visual (decorator) of the given node type. */
    public static boolean isVisualBlock(ResourceLocation typeId, Block block) {
        if (block == null) {
            return false;
        }
        OreNodeType type = OreNodeDataManager.INSTANCE.getNodeType(typeId).orElse(null);
        if (type == null) {
            return false;
        }
        for (OreNodeType.QualityTier tier : type.qualityTiers()) {
            if (BuiltInRegistries.BLOCK.get(tier.visualBlock()) == block) {
                return true;
            }
        }
        return false;
    }

    /** All configured tier visual blocks across every node type (cached). */
    public static Set<Block> visualBlocks() {
        Set<Block> cached = visualBlockCache;
        if (cached == null) {
            cached = buildVisualBlockCache();
            visualBlockCache = cached;
        }
        return cached;
    }

    private static synchronized Set<Block> buildVisualBlockCache() {
        if (visualBlockCache != null) {
            return visualBlockCache;
        }
        Set<Block> out = new HashSet<>();
        for (OreNodeType type : OreNodeDataManager.INSTANCE.nodeTypes()) {
            for (OreNodeType.QualityTier tier : type.qualityTiers()) {
                Block block = BuiltInRegistries.BLOCK.get(tier.visualBlock());
                if (block != null && block != Blocks.AIR) {
                    out.add(block);
                }
            }
        }
        return out;
    }

    /** Whether the block is a tier visual of any configured node type. */
    public static boolean isVisualOre(Block block) {
        return block != null && visualBlocks().contains(block);
    }

    public static boolean isVanillaOre(Block block) {
        return block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.NETHER_GOLD_ORE || block == Blocks.NETHER_QUARTZ_ORE
                || block == Blocks.ANCIENT_DEBRIS;
    }
}