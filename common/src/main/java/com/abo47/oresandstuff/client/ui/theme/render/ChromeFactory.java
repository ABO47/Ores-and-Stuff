package com.abo47.oresandstuff.client.ui.theme.render;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiGraphics;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;

import com.abo47.oresandstuff.client.ui.controls.IconAtlas;
import com.abo47.oresandstuff.client.theme.tokens.OasColors;

public final class ChromeFactory {
    private ChromeFactory() {
    }

    public static ButtonWidget closeIconButton(int x, int y, int w, int h, Consumer<ClickData> callback) {
        return iconButton(x, y, w, h, "close", () -> OasColors.ERROR, callback);
    }

    public static ButtonWidget iconButton(int x, int y, int w, int h, String icon, IntSupplier activeColor, Consumer<ClickData> callback) {
        IGuiTexture glyph = IconAtlas.iconTexture(icon);
        IGuiTexture iconTexture = (graphics, mouseX, mouseY, x0, y0, width, height) -> {
            int ac = activeColor.getAsInt();
            SurfaceFactory.fill(OasColors.elevatedSurface()).draw(graphics, mouseX, mouseY, x0, y0, width, height);
            drawBorder(graphics, (int) x0, (int) y0, width, height, OasColors.BORDER_BASE);
            drawGlyph(graphics, mouseX, mouseY, x0, y0, width, height, glyph, ac);
        };

        ButtonWidget btn = new ButtonWidget(x, y, w, h, iconTexture, callback);
        btn.setClientSideWidget();
        btn.setHoverTexture(GlowShaderHelper.hoverGlow());
        btn.setClickedTexture((graphics, mouseX, mouseY, x0, y0, width, height) -> {
            int ac = activeColor.getAsInt();
            SurfaceFactory.fill(OasColors.pressedFill(ac)).draw(graphics, mouseX, mouseY, x0, y0, width, height);
            drawBorder(graphics, (int) x0, (int) y0, width, height, ac);
            drawGlyph(graphics, mouseX, mouseY, x0, y0, width, height, glyph, ac);
        });
        return btn;
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        SurfaceFactory.fill(color).draw(graphics, 0, 0, x, y, width, 1);
        SurfaceFactory.fill(color).draw(graphics, 0, 0, x, y + height - 1, width, 1);
        SurfaceFactory.fill(color).draw(graphics, 0, 0, x, y, 1, height);
        SurfaceFactory.fill(color).draw(graphics, 0, 0, x + width - 1, y, 1, height);
    }

    private static void drawGlyph(GuiGraphics graphics, int mouseX, int mouseY, float x0, float y0, int width, int height, IGuiTexture glyph, int fallbackColor) {
        int glyphX = (int) x0 + 2;
        int glyphY = (int) y0 + 2;
        int glyphW = Math.max(1, width - 4);
        int glyphH = Math.max(1, height - 4);
        if (glyph != null) {
            glyph.draw(graphics, mouseX, mouseY, glyphX, glyphY, glyphW, glyphH);
            return;
        }
        int cx = (int) x0 + width / 2;
        int cy = (int) y0 + height / 2;
        SurfaceFactory.fill(fallbackColor).draw(graphics, 0, 0, cx - 3, cy, 7, 1);
        SurfaceFactory.fill(fallbackColor).draw(graphics, 0, 0, cx, cy - 3, 1, 7);
    }
}
