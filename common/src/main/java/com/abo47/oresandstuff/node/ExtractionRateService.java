package com.abo47.oresandstuff.node;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.abo47.oresandstuff.data.OreNodeDataManager;

public final class ExtractionRateService {
    private ExtractionRateService() {
    }

    public static ItemStack buildDrop(OreNodeBlockEntity node, int baseCount) {
        Item item = null;
        var type = OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).orElse(null);
        if (type != null) {
            item = BuiltInRegistries.ITEM.get(type.outputItem());
        }
        if (item == null) {
            return ItemStack.EMPTY;
        }
        int count = Math.max(1, (int) Math.round(baseCount * node.getPurity().manualMultiplier()));
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
