package com.abo47.oresandstuff.node;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import com.abo47.oresandstuff.data.OreNodeDataManager;

public final class ExtractionRateService {
    private ExtractionRateService() {
    }

    /** Drops for one extraction, count multiplied by the node's quality. */
    public static List<ItemStack> buildDrops(OreNodeBlockEntity node, int baseCount) {
        return buildDrops(node, baseCount, NodeQuality.multiplier(node.getQualityPercent()));
    }

    /**
     * Rolls every drop entry that hits its chance and applies the multiplier
     * to the count of each rolled item.
     */
    public static List<ItemStack> buildDrops(OreNodeBlockEntity node, int baseCount, double multiplier) {
        List<ItemStack> out = new ArrayList<>();
        var type = OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).orElse(null);
        if (type == null) {
            return out;
        }
        RandomSource random = node.getLevel() != null ? node.getLevel().random : RandomSource.create();
        int count = Math.max(1, (int) Math.round(baseCount * multiplier));
        for (ItemStack stack : type.rollDrops(random)) {
            out.add(stack.copyWithCount(count));
        }
        return out;
    }

    /** API variant: same as {@link #buildDrops(OreNodeBlockEntity, int, double)} for a detached node snapshot. */
    public static List<ItemStack> buildDrops(net.minecraft.resources.ResourceLocation typeId, double qualityPercent,
                                             RandomSource random, int baseCount) {
        List<ItemStack> out = new ArrayList<>();
        var type = OreNodeDataManager.INSTANCE.getNodeType(typeId).orElse(null);
        if (type == null) {
            return out;
        }
        int count = Math.max(1, (int) Math.round(baseCount * NodeQuality.multiplier(qualityPercent)));
        for (ItemStack stack : type.rollDrops(random)) {
            out.add(stack.copyWithCount(count));
        }
        return out;
    }

    public static double minerItemsPerSecond(OreNodeBlockEntity node, double minerTierMultiplier) {
        var type = OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).orElse(null);
        if (type == null) {
            return 0;
        }
        return type.baseRatePerSecond() * NodeQuality.multiplier(node.getQualityPercent()) * minerTierMultiplier;
    }
}