package com.abo47.oresandstuff.client.screen;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.network.NetworkChannels;
import com.abo47.oresandstuff.network.ScannerRequestPacket;

public class ScannerSelectScreen extends Screen {
    private final ItemStack scannerStack;

    public ScannerSelectScreen(ItemStack scannerStack) {
        super(Component.translatable("Scanner Targets"));
        this.scannerStack = scannerStack;
    }

    @Override
    protected void init() {
        List<ResourceLocation> ids = OreNodeDataManager.INSTANCE.orderedTypeIds();
        int cx = width / 2;
        int cy = height / 2;
        int radius = 70;

        for (int i = 0; i < ids.size(); i++) {
            ResourceLocation id = ids.get(i);
            double a = (Math.PI * 2.0 * i) / Math.max(1, ids.size());
            int x = cx + (int) (Math.cos(a) * radius) - 35;
            int y = cy + (int) (Math.sin(a) * radius) - 10;
            addRenderableWidget(Button.builder(Component.literal(id.getPath()), btn -> {
                scannerStack.getOrCreateTag().putString("SelectedType", id.toString());
                NetworkChannels.sendScannerRequest(id);
                Minecraft.getInstance().setScreen(null);
            }).bounds(x, y, 70, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, Component.translatable("Select Ore for Scanner").getString(), width / 2, 24, OasColors.TEXT_PRIMARY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
