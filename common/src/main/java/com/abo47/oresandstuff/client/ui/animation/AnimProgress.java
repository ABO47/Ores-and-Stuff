package com.abo47.oresandstuff.client.ui.animation;

public final class AnimProgress {
    private AnimProgress() {
    }

    public static boolean running(long startMs, long durationMs) {
        return running(startMs, durationMs, System.currentTimeMillis());
    }

    public static boolean running(long startMs, long durationMs, long nowMs) {
        long elapsed = nowMs - startMs;
        return startMs > 0L && durationMs > 0L && elapsed >= 0L && elapsed < durationMs;
    }

    public static float easedProgress(long startMs, long durationMs) {
        return easedProgress(startMs, durationMs, System.currentTimeMillis());
    }

    public static float easedProgress(long startMs, long durationMs, long nowMs) {
        float t = linearProgress(startMs, durationMs, nowMs);
        return t * t * (3.0f - 2.0f * t);
    }

    public static float cubicOutProgress(long startMs, long durationMs) {
        return cubicOutProgress(startMs, durationMs, System.currentTimeMillis());
    }

    public static float cubicOutProgress(long startMs, long durationMs, long nowMs) {
        return cubicOut(linearProgress(startMs, durationMs, nowMs));
    }

    public static float linearProgress(long startMs, long durationMs) {
        return linearProgress(startMs, durationMs, System.currentTimeMillis());
    }

    public static float linearProgress(long startMs, long durationMs, long nowMs) {
        if (durationMs <= 0L) {
            return 1.0f;
        }
        float t = (nowMs - startMs) / (float) durationMs;
        return Math.max(0.0f, Math.min(1.0f, t));
    }

    public static float cubicOut(float progress) {
        float t = Math.max(0.0f, Math.min(1.0f, progress));
        float inverse = 1.0f - t;
        return 1.0f - inverse * inverse * inverse;
    }

    public static float openProgress(boolean open, boolean animationFromClosed, long startMs, long durationMs) {
        return openProgress(open, animationFromClosed, startMs, durationMs, System.currentTimeMillis());
    }

    public static float openProgress(boolean open, boolean animationFromClosed, long startMs, long durationMs, long nowMs) {
        if (!running(startMs, durationMs, nowMs)) {
            return open ? 1.0f : 0.0f;
        }
        float eased = easedProgress(startMs, durationMs, nowMs);
        return animationFromClosed ? eased : 1.0f - eased;
    }

    public static int interpolate(int from, int to, float progress) {
        return Math.round(interpolate((float) from, (float) to, progress));
    }

    public static float interpolate(float from, float to, float progress) {
        float clamped = Math.max(0.0f, Math.min(1.0f, progress));
        return from + (to - from) * clamped;
    }
}
