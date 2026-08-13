package com.abo47.oresandstuff.content;

import com.abo47.oresandstuff.block.InfiniteBatteryBlock;
import com.abo47.oresandstuff.block.MinerBlock;
import com.abo47.oresandstuff.block.OreNodeBlock;
import com.abo47.oresandstuff.item.BioScannerItem;
import com.abo47.oresandstuff.item.NodeExtractorPickaxeItem;
import com.abo47.oresandstuff.item.ScannerItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;

public final class ModItems {
    public static final Item ORE_NODE = new BlockItem(ModBlocks.ORE_NODE, new Item.Properties());
    public static final Item MINER_MK1 = new BlockItem(ModBlocks.MINER_MK1, new Item.Properties());
    public static final Item INFINITE_BATTERY = new BlockItem(ModBlocks.INFINITE_BATTERY, new Item.Properties());
    public static final Item SCANNER = new ScannerItem(new Item.Properties().stacksTo(1));
    public static final Item BIO_SCANNER = new BioScannerItem(new Item.Properties().stacksTo(1));
    public static final Item NODE_EXTRACTOR_PICKAXE = new NodeExtractorPickaxeItem(Tiers.IRON, 1, -2.8F, new Item.Properties());

    private ModItems() {
    }
}
