package com.abo47.oresandstuff.node;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.abo47.oresandstuff.data.OreNodeDataManager;

public final class ExtractionRateService {
    private ExtractionRateService() {
    }

    public static ItemStack buildDrop(OreNodeBlockEntity node, int baseCount) {
        return buildDrop(node, baseCount, node.getPurity().manualMultiplier());
    }

    public static ItemStack buildDrop(OreNodeBlockEntity node, int baseCount, double multiplier) {
        Item item = null;
        var type = OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).orElse(null);
        if (type != null) {
            item = BuiltInRegistries.ITEM.get(type.rollDrop(node.getLevel() != null ? node.getLevel().random : null));
        }
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        int count = Math.max(1, (int) Math.round(baseCount * multiplier));
        return new ItemStack(item, count);
    }

    public static double minerItemsPerSecond(OreNodeBlockEntity node, double minerTierMultiplier) {
        var type = OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).orElse(null);
        if (type == null) {
            return 0;
        }
        return type.baseRatePerSecond() * node.getPurity().minerMultiplier() * minerTierMultiplier;
    }
}
