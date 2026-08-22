package com.abo47.oresandstuff.client;

import org.joml.Matrix4f;

final class ScannerRenderer {
    private ScannerRenderer() {
    }

    static void draw(ScannerFxTypes.ScanPulse pulse, float radius, Matrix4f invView, Matrix4f invProj) {
        if (OasShaders.isTerrainReady()) {
            ScannerPostProcessFx.renderTerrainSweep(pulse, radius, invView, invProj);
        }
    }
}
