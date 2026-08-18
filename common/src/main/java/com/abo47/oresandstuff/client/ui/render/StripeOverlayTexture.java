package com.abo47.oresandstuff.client.ui.render;

import net.minecraft.client.gui.GuiGraphics;

import com.lowdragmc.lowdraglib.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;

public class StripeOverlayTexture extends TransformTexture {
    private final int color;
    private final int stripeWidth;
    private final int stripeGap;
    private final boolean vertical;

    public StripeOverlayTexture(int color, int stripeWidth, int stripeGap, boolean vertical) {
        this.color = color;
        this.stripeWidth = Math.max(1, stripeWidth);
        this.stripeGap = Math.max(1, stripeGap);
        this.vertical = vertical;
    }

    @Override
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        final int innerLeft = (int) x;
        final int innerTop = (int) y;
        final int innerRight = (int) (x + width);
        final int innerBottom = (int) (y + height);
        if (innerRight <= innerLeft || innerBottom <= innerTop) {
            return;
        }

        int step = stripeWidth + stripeGap;
        if (vertical) {
            int startX = innerLeft + (step - Math.floorMod(innerLeft, step)) % step;
            for (int sx = startX; sx < innerRight; sx += step) {
                int drawW = Math.min(stripeWidth, innerRight - sx);
                if (drawW > 0) {
                    DrawerHelper.drawSolidRect(graphics, sx, innerTop, drawW, innerBottom - innerTop, color);
                }
            }
        } else {
            int startY = innerTop + (step - Math.floorMod(innerTop, step)) % step;
            for (int sy = startY; sy < innerBottom; sy += step) {
                int drawH = Math.min(stripeWidth, innerBottom - sy);
                if (drawH > 0) {
                    DrawerHelper.drawSolidRect(graphics, innerLeft, sy, innerRight - innerLeft, drawH, color);
                }
            }
        }
    }
}
