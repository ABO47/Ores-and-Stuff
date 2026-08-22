package com.abo47.oresandstuff.client.ui.theme.render;

import java.awt.Rectangle;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ShaderTexture;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;

public final class GlowShaderHelper {
    private static final ResourceLocation GLOW_SHADER = new ResourceLocation("oresandstuff", "glow");

    private GlowShaderHelper() {
    }

    private static ShaderTexture shader() {
        return ShaderTexture.createShader(GLOW_SHADER);
    }

    public static IGuiTexture hoverGlow() {
        return hoverGlow(OasColors.GLOW);
    }

    public static IGuiTexture hoverGlow(int glowColor) {
        return (graphics, mouseX, mouseY, x, y, w, h) ->
                drawGlow(graphics, mouseX, mouseY, (int) x, (int) y, w, h, glowColor);
    }

    public static void drawGlow(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h) {
        drawGlow(graphics, mouseX, mouseY, x, y, w, h, OasColors.GLOW);
    }

    public static void drawGlow(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, int glowColor) {
        ShaderTexture s = shader();
        if (s == null) return;
        float r = FastColor.ARGB32.red(glowColor) / 255f;
        float g = FastColor.ARGB32.green(glowColor) / 255f;
        float b = FastColor.ARGB32.blue(glowColor) / 255f;
        s.setUniformCache(cache -> cache.glUniform4F("uGlowColor", r, g, b, 1f));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        s.draw(graphics, mouseX, mouseY, x, y, w, h);
    }

    public static void drawGlowClipped(GuiGraphics graphics, int mouseX, int mouseY,
                                       int wx, int wy, int ww, int wh,
                                       int glowColor, List<Rectangle> ancestors) {
        if (ww <= 0 || wh <= 0) return;
        Rectangle clip = ancestorClip(wx, wy, ww, wh, ancestors);
        if (clip == null || clip.width <= 0 || clip.height <= 0) return;
        graphics.enableScissor(clip.x, clip.y, clip.x + clip.width, clip.y + clip.height);
        drawGlow(graphics, mouseX, mouseY, wx, wy, ww, wh, glowColor);
        graphics.disableScissor();
    }

    private static Rectangle ancestorClip(int wx, int wy, int ww, int wh, List<Rectangle> ancestors) {
        int sx = wx;
        int sy = wy;
        int sw = ww;
        int sh = wh;
        for (Rectangle a : ancestors) {
            int ar = a.x + a.width;
            int ab = a.y + a.height;
            int r = sx + sw;
            int b = sy + sh;
            sx = Math.max(sx, a.x);
            sy = Math.max(sy, a.y);
            sw = Math.min(r, ar) - sx;
            sh = Math.min(b, ab) - sy;
            if (sw <= 0 || sh <= 0) return null;
        }
        return new Rectangle(sx, sy, sw, sh);
    }

    public static void drawGlowOccluded(GuiGraphics graphics, int mouseX, int mouseY,
                                         int wx, int wy, int ww, int wh,
                                         int glowColor, List<Rectangle> ancestors,
                                         List<Rectangle> excludes) {
        if (ww <= 0 || wh <= 0) return;

        Rectangle base = ancestorClip(wx, wy, ww, wh, ancestors);
        if (base == null || base.width <= 0 || base.height <= 0) return;

        if (excludes == null || excludes.isEmpty()) {
            graphics.enableScissor(base.x, base.y, base.x + base.width, base.y + base.height);
            drawGlow(graphics, mouseX, mouseY, wx, wy, ww, wh, glowColor);
            graphics.disableScissor();
            return;
        }

        List<Rectangle> free = new java.util.ArrayList<>();
        free.add(base);

        for (Rectangle hole : excludes) {
            Rectangle h = base.intersection(hole);
            if (h.isEmpty()) continue;

            List<Rectangle> next = new java.util.ArrayList<>();
            for (Rectangle r : free) {
                subtract(r, h, next);
            }
            free = next;
        }

        for (Rectangle piece : free) {
            if (piece.width <= 0 || piece.height <= 0) continue;
            graphics.enableScissor(piece.x, piece.y, piece.x + piece.width, piece.y + piece.height);
            drawGlow(graphics, mouseX, mouseY, wx, wy, ww, wh, glowColor);
            graphics.disableScissor();
        }
    }

    private static void subtract(Rectangle rect, Rectangle hole, List<Rectangle> out) {
        Rectangle h = rect.intersection(hole);
        if (h.isEmpty()) {
            out.add(rect);
            return;
        }

        if (h.y > rect.y) {
            out.add(new Rectangle(rect.x, rect.y, rect.width, h.y - rect.y));
        }
        int rectBottom = rect.y + rect.height;
        int holeBottom = h.y + h.height;
        if (holeBottom < rectBottom) {
            out.add(new Rectangle(rect.x, holeBottom, rect.width, rectBottom - holeBottom));
        }
        if (h.x > rect.x) {
            out.add(new Rectangle(rect.x, h.y, h.x - rect.x, h.height));
        }
        int rectRight = rect.x + rect.width;
        int holeRight = h.x + h.width;
        if (holeRight < rectRight) {
            out.add(new Rectangle(holeRight, h.y, rectRight - holeRight, h.height));
        }
    }
}
