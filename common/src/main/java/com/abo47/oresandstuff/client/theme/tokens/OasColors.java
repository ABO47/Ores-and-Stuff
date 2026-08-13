package com.abo47.oresandstuff.client.theme.tokens;

public final class OasColors {
    private OasColors() {
    }

    public static final int BG_0 = 0xFF0B1118;
    public static final int BG_1 = 0xFF111923;
    public static final int BG_2 = 0xFF1A2433;
    public static final int BG_3 = 0xFF223249;
    public static final int BORDER = 0xFF2A4060;
    public static final int BORDER_STRONG = 0xFF3F699A;

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFC9D9E8;
    public static final int TEXT_MUTED = 0xFF88A9C2;

    public static final int ACCENT_PRIMARY = 0xFF33D6FF;
    public static final int ACCENT_SOFT = 0xFF66E6FF;
    public static final int ACCENT_MINT = 0xFF95FFD5;

    public static final int SUCCESS = 0xFF4ADE80;
    public static final int WARNING = 0xFFFFD580;
    public static final int ERROR = 0xFFF87171;

    public static final int VANILLA_BG = 0xFFC6C6C6;
    public static final int VANILLA_PANEL = 0xFF8B8B8B;
    public static final int VANILLA_PANEL_DARK = 0xFF747474;
    public static final int VANILLA_BORDER_LIGHT = 0xFFEDEDED;
    public static final int VANILLA_BORDER_DARK = 0xFF4A4A4A;
    public static final int VANILLA_TEXT = 0xFF2D2D2D;

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
}
