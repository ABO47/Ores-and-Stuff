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
 * One ore node block per configured node type, so each type renders with its
 * own texture (netherrack in the nether, end stone in the end, ...) through
 * the regular block model system - no custom renderers.
 */
public final class ModNodeBlocks {
    private static final Map<ResourceLocation, OreNodeBlock> NODE_BLOCKS = new LinkedHashMap<>();

    private ModNodeBlocks() {
    }

    public static OreNodeBlock get(ResourceLocation typeId) {
        OreNodeDataManager.INSTANCE.ensureLoaded();
        return NODE_BLOCKS.computeIfAbsent(typeId, id -> new OreNodeBlock(
                BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).sound(SoundType.STONE).noOcclusion()));
    }

    /** The generic node block plus one block per configured node type. */
    public static Block[] allArray() {
        List<Block> out = new ArrayList<>();
        out.add(ModBlocks.ORE_NODE);
        OreNodeDataManager.INSTANCE.ensureLoaded();
        for (OreNodeType type : OreNodeDataManager.INSTANCE.nodeTypes()) {
            out.add(get(type.id()));
        }
        return out.toArray(new Block[0]);
    }
}