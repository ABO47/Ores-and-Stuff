package com.abo47.oresandstuff.client.ui.controls;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

public final class PickerTileText {
    private PickerTileText() {
    }

    public static WidgetGroup centeredLabel(int x, int y, int w, String text, int color) {
        String safeText = text == null ? "" : text;
        return new WidgetGroup(x, y, w, 10) {
            @Override
            public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                var font = Minecraft.getInstance().font;
                String fitted = fitText(safeText, Math.max(1, getSizeWidth()));
                int textWidth = font.width(fitted);
                int drawX = getPositionX() + Math.max(0, (getSizeWidth() - textWidth) / 2);
                int drawY = getPositionY() + Math.max(0, (10 - font.lineHeight) / 2);
                graphics.drawString(font, fitted, drawX, drawY, color, false);
            }
        };
    }

    private static String fitText(String text, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= width) {
            return text;
        }
        String suffix = "..";
        return font.plainSubstrByWidth(text, Math.max(1, width - font.width(suffix))) + suffix;
    }
}
