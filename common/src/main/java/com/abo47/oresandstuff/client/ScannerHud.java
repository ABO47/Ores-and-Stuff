package com.abo47.oresandstuff.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.data.OreNodeDataManager;

final class ScannerHud {
    private ScannerHud() {
    }

    static final int TOP_Y = 18;
    static final int HEIGHT = 14;
    static final int HALF_WIDTH = 110;
    static final float PIX_PER_DEG = HALF_WIDTH / 90.0F;

    private static final int TICK_STEP = 5;
    private static final int TICK_MEDIUM = 15;
    private static final int TICK_MAJOR = 45;

    static void render(GuiGraphics g, int cx) {
        if (OasClient.minecraft == null || OasClient.minecraft.player == null) {
            return;
        }
        float yaw = OasClient.minecraft.player.getYRot();
        float northHeading = Mth.wrapDegrees(yaw + 180f);
        drawTape(g, cx, northHeading);
        drawCenterNotch(g, cx);
        drawMarkers(g, cx);
    }

    private static void drawTape(GuiGraphics g, int cx, float northHeading) {
        for (int heading = 0; heading < 360; heading += TICK_STEP) {
            float offset = Mth.wrapDegrees(heading - northHeading) * PIX_PER_DEG;
            if (offset < -HALF_WIDTH || offset > HALF_WIDTH) {
                continue;
            }
            int px = cx + Math.round(offset);
            boolean major = heading % TICK_MAJOR == 0;
            boolean medium = heading % TICK_MEDIUM == 0;
            int hh = major ? 7 : medium ? 5 : 3;
            int y0 = TOP_Y + (HEIGHT - hh) / 2;
            int col = major ? OasColors.BORDER_STRONG : OasColors.BORDER_BASE;
            int a = major ? 0xD8 : medium ? 0x99 : 0x66;
            g.fill(px, y0, px + 1, y0 + hh, OasColors.withAlpha(col, a));
            if (major) {
                String label = heading == 0 ? "N" : heading == 90 ? "E" : heading == 180 ? "S" : heading == 270 ? "W" : Integer.toString(heading);
                int lc = heading == 0 || heading == 90 || heading == 180 || heading == 270 ? OasColors.TEXT_PRIMARY : OasColors.TEXT_SECONDARY;
                g.drawCenteredString(OasClient.minecraft.font, label, px, TOP_Y - 8, lc);
            }
        }
    }

    private static void drawCenterNotch(GuiGraphics g, int cx) {
        g.fill(cx, TOP_Y - 2, cx + 1, TOP_Y + HEIGHT + 3, OasColors.ACCENT_SOFT);
    }

    private static void drawMarkers(GuiGraphics g, int cx) {
        if (OasClient.currentOreType == null) {
            return;
        }
        OreNodeDataManager.INSTANCE.getNodeType(OasClient.currentOreType).ifPresent(type -> {
            ResourceLocation outItem = type.outputItem();
            Item item = outItem == null ? null : BuiltInRegistries.ITEM.get(outItem);
            if (item == null || item == Items.AIR) {
                return;
            }
            long now = System.currentTimeMillis();
            for (ScannerFxTypes.TargetMarker marker : OasClient.markers) {
                if (!marker.visible(now)) {
                    continue;
                }
                if (!NodeClusterTracker.isNodeTouched(marker.pos)) {
                    continue;
                }
                String dist = (int) marker.distance + "m" + (marker.up ? " \u25B2" : " \u25BC");
                if (marker.side != 0) {
                    int edgeX = marker.side < 0 ? cx - HALF_WIDTH : cx + HALF_WIDTH;
                    g.drawCenteredString(OasClient.minecraft.font, marker.side < 0 ? "\u25C4" : "\u25BA", edgeX, TOP_Y + 2, OasColors.ACCENT_SOFT);
                    g.drawCenteredString(OasClient.minecraft.font, dist, edgeX, TOP_Y + HEIGHT + 8, OasColors.TEXT_PRIMARY);
                    continue;
                }
                int iconX = cx + Math.round(marker.smoothOffset) - 8;
                g.renderItem(new ItemStack(item), iconX, TOP_Y - 2);
                g.drawCenteredString(OasClient.minecraft.font, dist, iconX + 8, TOP_Y + HEIGHT + 8, OasColors.TEXT_PRIMARY);
            }
        });
    }
}