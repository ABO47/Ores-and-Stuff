package com.abo47.oresandstuff.node;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.data.OreNodeDataManager;

public final class NodeVisuals {
    private NodeVisuals() {
    }

    public static boolean isDecoration(Block block) {
        if (block == ModBlocks.ORE_NODE || block == Blocks.STONE || block == Blocks.DEEPSLATE) {
            return true;
        }
        return isVisualOre(block);
    }

    public static Block visualOre(ResourceLocation typeId, boolean deep) {
        OreNodeType type = OreNodeDataManager.INSTANCE.getNodeType(typeId).orElse(null);
        if (type == null) {
            return Blocks.AIR;
        }
        Block shallow = BuiltInRegistries.BLOCK.get(type.visualBlock());
        if (shallow == Blocks.AIR) {
            return Blocks.AIR;
        }
        if (!deep) {
            return shallow;
        }
        Block deepVariant = deepslateVariant(shallow);
        return deepVariant != Blocks.AIR ? deepVariant : shallow;
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

    private static boolean isVisualOre(Block block) {
        for (OreNodeType type : OreNodeDataManager.INSTANCE.nodeTypes()) {
            Block visual = BuiltInRegistries.BLOCK.get(type.visualBlock());
            if (visual == block) {
                return true;
            }
            Block deep = deepslateVariant(visual);
            if (deep == block) {
                return true;
            }
        }
        return false;
    }

    private static Block deepslateVariant(Block shallow) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(shallow);
        if (id == null) {
            return Blocks.AIR;
        }
        Block deep = BuiltInRegistries.BLOCK.get(new ResourceLocation(id.getNamespace(), "deepslate_" + id.getPath()));
        return deep != null ? deep : Blocks.AIR;
    }
}
