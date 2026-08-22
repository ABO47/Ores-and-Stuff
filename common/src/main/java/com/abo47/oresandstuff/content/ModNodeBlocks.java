package com.abo47.oresandstuff.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.abo47.oresandstuff.block.OreNodeBlock;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.node.OreNodeType;

/**
 * One ore node block per (node type, quality tier), so each tier renders with
 * its own configured block look (stone, deepslate, netherrack, ...) through
 * the regular block model system - no custom renderers.
 */
public final class ModNodeBlocks {
    private static final Map<String, OreNodeBlock> NODE_BLOCKS = new LinkedHashMap<>();

    private ModNodeBlocks() {
    }

    private static String key(ResourceLocation typeId, int tierIndex) {
        return typeId + "|" + tierIndex;
    }

    public static OreNodeBlock get(ResourceLocation typeId, int tierIndex) {
        OreNodeDataManager.INSTANCE.ensureLoaded();
        float hardness = OreNodeDataManager.INSTANCE.getNodeType(typeId)
                .map(t -> Math.max(0.5F, (float) t.hardness()))
                .orElse(-1.0F);
        return NODE_BLOCKS.computeIfAbsent(key(typeId, tierIndex), k -> new OreNodeBlock(
                BlockBehaviour.Properties.of()
                        .strength(hardness, 1.2F)
                        .sound(SoundType.STONE).noOcclusion()));
    }

    /** The generic node block plus one block per configured (type, tier). */
    public static Block[] allArray() {
        List<Block> out = new ArrayList<>();
        out.add(ModBlocks.ORE_NODE);
        OreNodeDataManager.INSTANCE.ensureLoaded();
        for (OreNodeType type : OreNodeDataManager.INSTANCE.nodeTypes()) {
            for (int t = 0; t < type.qualityTiers().size(); t++) {
                out.add(get(type.id(), t));
            }
        }
        return out.toArray(new Block[0]);
    }
}