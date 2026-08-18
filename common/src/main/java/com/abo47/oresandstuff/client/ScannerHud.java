package com.abo47.oresandstuff.client;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ScannerHud {
    private ScannerHud() {
    }

    private static final int TOP_Y = 14;
    private static final int HEIGHT = 14;
    private static final int HALF_WIDTH = 110;
    private static final int TICK_STEP = 10;
    private static final int MAJOR_TICK = 50;

    static void render(GuiGraphics g, int cx) {
        int top = TOP_Y;
        for (int i = -HALF_WIDTH; i <= HALF_WIDTH; i += TICK_STEP) {
            int px = cx + i;
            boolean center = i == 0;
            boolean major = i % MAJOR_TICK == 0;
            int hh = center ? 8 : (major ? 5 : 3);
            int y0 = top + (HEIGHT - hh) / 2;
            int col = center ? OasColors.ACCENT_SOFT : (major ? OasColors.BORDER_STRONG : OasColors.BORDER_BASE);
            int a = center ? 0xD0 : (major ? 0x8C : 0x55);
            g.fill(px, y0, px + 1, y0 + hh, OasColors.withAlpha(col, a));
        }
        long now = System.currentTimeMillis();
        if (OasClient.currentOreType != null) {
            OreNodeDataManager.INSTANCE.getNodeType(OasClient.currentOreType).ifPresent(type -> {
                ResourceLocation outItem = type.outputItem();
                Item item = outItem == null ? null : BuiltInRegistries.ITEM.get(outItem);
                if (item == null || item == Items.AIR) return;
                drawMarkerIcons(g, now, cx, item);
            });
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
