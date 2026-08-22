package com.abo47.oresandstuff.client.ui.theme.tokens;

public final class UiThemeTokens {
    public static final int GRID_1 = 1;
    public static final int GRID_2 = 2;
    public static final int GRID_3 = 3;
    public static final int GRID_4 = 4;
    public static final int GRID_5 = 5;
    public static final int GRID_6 = 6;
    public static final int GRID_7 = 7;
    public static final int GRID_8 = 8;
    public static final int GRID_9 = 9;
    public static final int GRID_10 = 10;
    public static final int GRID_12 = 12;
    public static final int GRID_14 = 14;
    public static final int GRID_15 = 15;
    public static final int GRID_16 = 16;
    public static final int GRID_18 = 18;
    public static final int GRID_20 = 20;
    public static final int GRID_24 = 24;
    public static final int GRID_26 = 26;
    public static final int GRID_30 = 30;
    public static final int GRID_32 = 32;
    public static final int GRID_34 = 34;
    public static final int GRID_48 = 48;
    public static final int GRID_64 = 64;
    public static final int GRID_96 = 96;

    public static final int ICON_10 = 10;
    public static final int ICON_12 = 12;
    public static final int ICON_16 = 16;
    public static final int ICON_18 = 18;
    public static final int ICON_22 = 22;
    public static final int ICON_24 = 24;
    public static final int ICON_48 = 48;

    public static final int ROW_H_12 = 12;
    public static final int ROW_H_14 = 14;
    public static final int ROW_H_16 = 16;
    public static final int ROW_H_18 = 18;
    public static final int ROW_H_20 = 20;
    public static final int ROW_H_26 = 26;
    public static final int ROW_H_32 = 32;

    public static final int BUTTON_16 = 16;
    public static final int BUTTON_18 = 18;
    public static final int BUTTON_20 = 20;
    public static final int BUTTON_H_14 = 14;

    public static final int PAD_1 = 1;
    public static final int PAD_2 = 2;
    public static final int PAD_3 = 3;
    public static final int PAD_4 = 4;
    public static final int PAD_5 = 5;
    public static final int PAD_6 = 6;
    public static final int PAD_7 = 7;
    public static final int PAD_8 = 8;
    public static final int PAD_9 = 9;
    public static final int PAD_10 = 10;
    public static final int PAD_12 = 12;
    public static final int PAD_14 = 14;
    public static final int PAD_16 = 16;

    public static final int CONTEXT_ROW_H = 12;

    private UiThemeTokens() {
    }

    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
