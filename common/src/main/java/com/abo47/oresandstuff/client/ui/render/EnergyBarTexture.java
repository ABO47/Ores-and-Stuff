package com.abo47.oresandstuff.client.ui.render;

import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

import com.lowdragmc.lowdraglib.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;

@Environment(EnvType.CLIENT)
public class EnergyBarTexture extends TransformTexture {
    private final Supplier<Double> ratio;
    private final Supplier<Integer> color;

    public EnergyBarTexture(Supplier<Double> ratio, Supplier<Integer> color) {
        this.ratio = ratio;
        this.color = color;
    }

    @Override
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int ix = (int) x;
        int iy = (int) y;
        DrawerHelper.drawSolidRect(graphics, ix, iy, width, height, OasColors.BG_0);
        double r = Math.max(0.0, Math.min(1.0, ratio.get()));
        int fillH = (int) Math.ceil(r * height);
        if (fillH <= 0) {
            return;
        }
        int cy = iy + height - fillH;
        int c = color.get();
        DrawerHelper.drawSolidRect(graphics, ix, cy, width, fillH, c);
        DrawerHelper.drawSolidRect(graphics, ix, cy, width, 1, OasColors.lighten(c, 35));
    }
}