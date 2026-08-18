package com.abo47.oresandstuff.client;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ScannerHud {
    private ScannerHud() {
    }

    private static final int TOP_Y = 14;
    private static final int HALF_WIDTH = 110;
    private static final int TICK_STEP = 10;
    private static final int MAJOR_TICK = 50;
    static void render(GuiGraphics g, int cx) {
        int left = cx - HALF_WIDTH;
        int right = cx + HALF_WIDTH;
        g.fill(left, TOP_Y, right, TOP_Y + 14, OasColors.withAlpha(OasColors.TEXT_PRIMARY, 0xAA));
        g.fill(left + 1, TOP_Y + 1, right - 1, TOP_Y + 13, OasColors.withAlpha(OasColors.BG_0, 0x77));
        for (int i = -HALF_WIDTH; i <= HALF_WIDTH; i += TICK_STEP) {
            int px = cx + i;
            int hh = (i % MAJOR_TICK == 0) ? 8 : 4;
            g.fill(px, TOP_Y + 3, px + 1, TOP_Y + 3 + hh, OasColors.withAlpha(OasColors.TEXT_PRIMARY, 0xCC));
        }
        g.drawCenteredString(OasClient.minecraft.font, Component.translatable("N").getString(), cx, TOP_Y - 10, OasColors.TEXT_SECONDARY);

        long now = System.currentTimeMillis();
        if (OasClient.currentOreType != null) {
            OreNodeDataManager.INSTANCE.getNodeType(OasClient.currentOreType).ifPresent(type -> {
                ResourceLocation outItem = type.outputItem();
                Item item = outItem == null ? null : BuiltInRegistries.ITEM.get(outItem);
                if (item == null || item == Items.AIR) return;
                drawMarkerIcons(g, now, cx, item);
            });
        }
        if (OasClient.markers.isEmpty()) {
            g.drawCenteredString(OasClient.minecraft.font, Component.translatable("No scan target").getString(), cx, TOP_Y + 18, OasColors.TEXT_MUTED);
        }

        var scannerCfg = OresAndStuffConfig.scanner();
        if (scannerCfg.experimentalVisualMode == 1 && scannerCfg.holographicStatusHud) {
            boolean ready = ScannerPostProcessFx.isShaderReady();
            g.drawCenteredString(OasClient.minecraft.font,
                    ready ? "Holographic Sweep: READY" : "Holographic Sweep: SHADER MISSING",
                    cx, TOP_Y + 30, ready ? OasColors.SUCCESS : OasColors.ERROR);
        }
    }

    private static void drawMarkerIcons(GuiGraphics g, long now, int cx, Item item) {
        for (ScannerFxTypes.TargetMarker marker : OasClient.markers) {
            if (!marker.visible(now)) continue;
            if (!NodeClusterTracker.isNodeTouched(marker.pos)) continue;
            int iconX = cx + (int) marker.smoothOffset - 8;
            g.renderItem(new ItemStack(item), iconX, TOP_Y - 2);
            String d = (int) marker.distance + "m";
            g.drawCenteredString(OasClient.minecraft.font, d, iconX + 8, TOP_Y + 18, OasColors.TEXT_PRIMARY);
        }
    }
}
