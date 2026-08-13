package com.abo47.oresandstuff.client.theme.tokens;

public final class OasColors {
    private OasColors() {
    }

    public static final int SURFACE_BASE = 0xFF171C21;
    public static final int SURFACE_PANEL = 0xFF202933;
    public static final int SURFACE_PANEL_ALT = 0xFF2C3742;

    public static final int BORDER_BASE = 0xFF546170;
    public static final int BORDER_ACCENT = 0xFF65B7C8;

    public static final int TEXT_PRIMARY = 0xFFEAF1F4;
    public static final int TEXT_SECONDARY = 0xFFB8C7CE;
    public static final int TEXT_MUTED = 0xFF88979F;

    public static final int SUCCESS = 0xFF66D38D;
    public static final int WARNING = 0xFFE5B44A;
    public static final int ERROR = 0xFFE06F73;
    public static final int INTERACTIVE = 0xFF64C3D2;
    public static final int GLOW = 0xFF64C3D2;
    public static final int SELECTION = 0xFF6BA8FF;
    public static final int SCROLL_TRACK = BORDER_BASE;
    public static final int SCROLL_THUMB = INTERACTIVE;

    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int TRANSPARENT = 0x00000000;
    public static final int DIM_OVERLAY = 0x60000000;

    public static final int BG_0 = SURFACE_BASE;
    public static final int BG_1 = SURFACE_PANEL;
    public static final int BG_2 = SURFACE_PANEL_ALT;
    public static final int BG_3 = 0xFF3F4953;
    public static final int BORDER = BORDER_BASE;
    public static final int BORDER_STRONG = BORDER_ACCENT;

    public static final int ACCENT_PRIMARY = INTERACTIVE;
    public static final int ACCENT_SOFT = 0xFF84CEDA;
    public static final int ACCENT_MINT = SUCCESS;

    public static final int VANILLA_BG = 0xFFC6C6C6;
    public static final int VANILLA_PANEL = 0xFF8B8B8B;
    public static final int VANILLA_PANEL_DARK = 0xFF747474;
    public static final int VANILLA_BORDER_LIGHT = 0xFFEDEDED;
    public static final int VANILLA_BORDER_DARK = 0xFF4A4A4A;
    public static final int VANILLA_TEXT = 0xFF2D2D2D;

    public static int elevatedSurface() {
        return mix(SURFACE_PANEL_ALT, TEXT_PRIMARY, 10);
    }

    public static int recessedSurface() {
        return mix(SURFACE_BASE, BLACK, 12);
    }

    public static int subtleBorder() {
        return mix(BORDER_BASE, SURFACE_BASE, 28);
    }

    public static int focusBorder() {
        return mix(BORDER_ACCENT, TEXT_PRIMARY, 10);
    }

    public static int hoverFill(int accent) {
        return withAlpha(accent, 46);
    }

    public static int pressedFill(int accent) {
        return withAlpha(accent, 76);
    }

    public static int scrollTrack(boolean active) {
        return withAlpha(SCROLL_TRACK, active ? 190 : 140);
    }

    public static int scrollThumb(boolean active) {
        return withAlpha(SCROLL_THUMB, active ? 255 : 220);
    }

    public static float rf(int argb) {
        return ((argb >> 16) & 0xFF) / 255f;
    }

    public static float gf(int argb) {
        return ((argb >> 8) & 0xFF) / 255f;
    }

    public static float bf(int argb) {
        return (argb & 0xFF) / 255f;
    }

    public static int withAlpha(int rgbOrArgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgbOrArgb & 0x00FFFFFF);
    }

    private static int mix(int color, int other, int otherPercent) {
        int p = Math.max(0, Math.min(100, otherPercent));
        int inv = 100 - p;
        int a = (((color >>> 24) & 0xFF) * inv + ((other >>> 24) & 0xFF) * p) / 100;
        int r = (((color >>> 16) & 0xFF) * inv + ((other >>> 16) & 0xFF) * p) / 100;
        int g = (((color >>> 8) & 0xFF) * inv + ((other >>> 8) & 0xFF) * p) / 100;
        int b = ((color & 0xFF) * inv + (other & 0xFF) * p) / 100;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
