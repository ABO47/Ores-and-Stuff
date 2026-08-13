package com.abo47.oresandstuff.client.ui.controls;

public final class ScrollMath {
    private ScrollMath() {
    }

    public static boolean hit(int mouseX, int mouseY, int trackX, int trackY, int trackW, int trackH) {
        return mouseX >= trackX && mouseX <= trackX + trackW && mouseY >= trackY && mouseY <= trackY + trackH;
    }

    public static int clamp(int value, int max) {
        return Math.max(0, Math.min(Math.max(0, max), value));
    }

    public static int wheel(int current, int max, int step, double wheelDelta) {
        int next = current + (wheelDelta > 0 ? -step : step);
        return clamp(next, max);
    }

    public static int byMouse(int mouseY, int trackTop, int trackHeight, int knobHeight, int maxValue) {
        if (maxValue <= 0 || trackHeight <= 0) {
            return 0;
        }
        int span = Math.max(1, trackHeight - knobHeight);
        int target = mouseY - trackTop - knobHeight / 2;
        target = Math.max(0, Math.min(span, target));
        float t = (float) target / (float) span;
        return Math.round(t * maxValue);
    }

    public static int listRows(int innerHeight, int rowHeight, int lastRowSlack) {
        if (rowHeight <= 0) {
            return 1;
        }
        return Math.max(1, (innerHeight + Math.max(0, lastRowSlack)) / rowHeight);
    }
}
