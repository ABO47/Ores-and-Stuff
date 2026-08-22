package com.abo47.oresandstuff.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import com.abo47.oresandstuff.item.BioScannerItem;
import com.abo47.oresandstuff.item.ScannerItem;

public final class ModItems {
    public static final Item ORE_NODE = new BlockItem(ModBlocks.ORE_NODE, new Item.Properties());
    public static final Item INFINITE_BATTERY = new BlockItem(ModBlocks.INFINITE_BATTERY, new Item.Properties());
    public static final Item SCANNER = new ScannerItem(new Item.Properties().stacksTo(1));
    public static final Item BIO_SCANNER = new BioScannerItem(new Item.Properties().stacksTo(1));

    private ModItems() {
    }
}
