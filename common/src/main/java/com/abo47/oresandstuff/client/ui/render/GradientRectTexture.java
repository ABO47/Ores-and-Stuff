package com.abo47.oresandstuff.client.ui.render;

import net.minecraft.client.gui.GuiGraphics;

import com.lowdragmc.lowdraglib.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;

public class GradientRectTexture extends TransformTexture {
    private final int startColor;
    private final int endColor;
    private final boolean horizontal;

    public GradientRectTexture(int startColor, int endColor, boolean horizontal) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.horizontal = horizontal;
    }

    @Override
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        DrawerHelper.drawGradientRect(graphics, x, y, width, height, startColor, endColor, horizontal);
    }
}
