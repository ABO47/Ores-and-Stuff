package com.abo47.oresandstuff.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScannerFxTypes {
    private ScannerFxTypes() {
    }

    public record LayerSpec(float radiusOffset, float bandThickness, float r, float g, float b, float alphaMul, float yOffset) {
    }

    public static final LayerSpec[] WAVE_LAYERS = new LayerSpec[] {
            new LayerSpec(0f, 12f, 0.10f, 0.80f, 0.75f, 0.55f, 0.02f),
            new LayerSpec(-1.0f, 5f, 0.15f, 0.95f, 0.90f, 0.90f, 0.03f),
            new LayerSpec(-2.0f, 1.2f, 0.60f, 1.00f, 1.00f, 1.40f, 0.05f),
            new LayerSpec(-2.5f, 0.4f, 0.95f, 1.00f, 1.00f, 1.80f, 0.06f),
    };

    public static class ScanPulse {
        final double originX;
        final double originY;
        final double originZ;
        final long startMs;
        final long durationMs;
        int cachedRadiusStep = Integer.MIN_VALUE;
        final List<PulseTile> cachedTiles = new ArrayList<>();
        final Map<Long, Long> columnHitTime = new HashMap<>();
        final Map<Long, Float> alphaSmoothing = new HashMap<>();
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

    public static class PulseTile {
        final int dx;
        final int dz;
        final float y;
        final float shell;
        final float noise;
        final float yNorth;
        final float ySouth;
        final float yEast;
        final float yWest;

        public PulseTile(int dx, int dz, float y, float shell, float noise, float yNorth, float ySouth, float yEast, float yWest) {
            this.dx = dx;
            this.dz = dz;
            this.y = y;
            this.shell = shell;
            this.noise = noise;
            this.yNorth = yNorth;
            this.ySouth = ySouth;
            this.yEast = yEast;
            this.yWest = yWest;
        }
    }

    public static class FlashFx {
        final int x;
        final int y;
        final int z;
        long expiresAt;

        public FlashFx(int x, int y, int z, long expiresAt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.expiresAt = expiresAt;
        }
    }

    public static class TargetMarker {
        public final BlockPos pos;
        public float distance;
        public final long revealAtMs;
        public float smoothOffset;
        public boolean initialized;

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
