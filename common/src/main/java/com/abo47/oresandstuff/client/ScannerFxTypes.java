package com.abo47.oresandstuff.client;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class ScannerFxTypes {
    private ScannerFxTypes() {
    }

    public static class ScanPulse {
        final double originX;
        final double originY;
        final double originZ;
        final long startMs;
        final long durationMs;
        final Set<Long> pingedNodeKeys = new HashSet<>();

        public ScanPulse(double originX, double originY, double originZ, long durationMs) {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.startMs = System.currentTimeMillis();
            this.durationMs = durationMs;
        }

        float age01(long now) {
            return Mth.clamp((now - startMs) / (float) durationMs, 0f, 1f);
        }

        boolean done(long now) {
            return now - startMs > (long) (durationMs * 1.2f);
        }
    }

    public static class FlashFx {
        final int x;
        final int y;
        final int z;
        long expiresAt;
        final int color;

        public FlashFx(int x, int y, int z, long expiresAt, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.expiresAt = expiresAt;
            this.color = color;
        }
    }

    public static class TargetMarker {
        public final BlockPos pos;
        public float distance;
        public final long revealAtMs;
        public float smoothOffset;
        public boolean initialized;
        public int side;
        public boolean up;

        public TargetMarker(BlockPos pos, float distance, long revealAtMs) {
            this.pos = pos;
            this.distance = distance;
            this.revealAtMs = revealAtMs;
        }

        public boolean visible(long now) {
            return now >= revealAtMs;
        }
    }
}
