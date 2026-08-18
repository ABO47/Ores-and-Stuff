package com.abo47.oresandstuff.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.abo47.oresandstuff.OresAndStuffMod;

public final class ModCreativeTabs {
    public static final CreativeModeTab MAIN = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.oresandstuff.main"))
            .icon(() -> new ItemStack(ModItems.SCANNER))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.ORE_NODE);
                output.accept(ModItems.MINER_MK1);
                output.accept(ModItems.INFINITE_BATTERY);
                output.accept(ModItems.SCANNER);
                output.accept(ModItems.BIO_SCANNER);
                output.accept(ModItems.NODE_EXTRACTOR_PICKAXE);
            })
            .build();

    private ModCreativeTabs() {
    }
}
