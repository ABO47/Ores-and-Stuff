package com.abo47.oresandstuff.client.ui.controls;

import static com.abo47.oresandstuff.client.ui.theme.tokens.UiThemeTokens.GRID_3;

public record TileGridLayout(
        int width,
        int height,
        int tileW,
        int tileH,
        int gap,
        int padX,
        int padY,
        int cols,
        int rows,
        int pageSize,
        boolean showScroll,
        int maxStart,
        int scrollStart,
        int entryCount
) {
    public static TileGridLayout calculate(
            int width,
            int height,
            int tileW,
            int tileH,
            int gap,
            int padX,
            int padY,
            int entryCount,
            int scrollStart
    ) {
        int safeTileW = Math.max(1, tileW);
        int safeTileH = Math.max(1, tileH);
        int safeGap = Math.max(0, gap);
        int safePadX = Math.max(0, padX);
        int safePadY = Math.max(0, padY);
        int safeCount = Math.max(0, entryCount);
        int contentH = Math.max(safeTileH, height - safePadY * 2);
        int rows = Math.max(1, (contentH + safeGap) / (safeTileH + safeGap));
        int baseContentW = Math.max(safeTileW, width - safePadX * 2);
        int baseCols = Math.max(1, (baseContentW + safeGap) / (safeTileW + safeGap));
        boolean showScroll = safeCount > baseCols * rows;
        int contentW = Math.max(safeTileW, width - safePadX * 2 - (showScroll ? DragScrollBarWidget.RESERVED_WIDTH : 0));
        int cols = Math.max(1, (contentW + safeGap) / (safeTileW + safeGap));
        int pageSize = Math.max(1, cols * rows);
        showScroll = safeCount > pageSize;
        int maxStart = Math.max(0, safeCount - pageSize);
        int safeStart = ScrollMath.clamp(scrollStart, maxStart);
        return new TileGridLayout(width, height, safeTileW, safeTileH, safeGap, safePadX, safePadY, cols, rows, pageSize, showScroll, maxStart, safeStart, safeCount);
    }

    public int wheelStep() {
        return Math.max(1, cols);
    }

    public int visibleEnd() {
        return Math.min(entryCount, scrollStart + pageSize);
    }

    public int tileX(int visibleIndex) {
        return padX + (visibleIndex % cols) * (tileW + gap);
    }

    public int tileY(int visibleIndex) {
        return padY + (visibleIndex / cols) * (tileH + gap);
    }

    public int scrollBarX() {
        return padX + cols * (tileW + gap) - gap + GRID_3;
    }

    public int scrollBarY() {
        return padY;
    }

    public int scrollBarH() {
        return Math.max(1, rows * tileH + Math.max(0, rows - 1) * gap);
    }

    public int knobH() {
        if (entryCount <= 0) {
            return scrollBarH();
        }
        return Math.max(12, Math.min(scrollBarH(), Math.round((float) pageSize / (float) entryCount * scrollBarH())));
    }
}
