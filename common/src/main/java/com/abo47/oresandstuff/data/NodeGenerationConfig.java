package com.abo47.oresandstuff.data;

public record NodeGenerationConfig(int minSpacingBlocks, int placementAttempts, int scannerRadius) {
    public static NodeGenerationConfig defaults() {
        return new NodeGenerationConfig(220, 1, 192);
    }
}
