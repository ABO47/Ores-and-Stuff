package com.abo47.oresandstuff.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;

final class ScannerRenderUtil {
    private ScannerRenderUtil() {
    }

    static float sampleSurfaceY(Minecraft minecraft, ClientLevel level, double x, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        if (!level.hasChunkAt(new BlockPos(bx, level.getSeaLevel(), bz))) return (float) minecraft.player.getY();
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz);
    }

    static void drawTopFill(BufferBuilder bb, Matrix4f mat, float x0, float x1, float z0, float z1, float y, float r, float g, float b, float alpha) {
        bb.vertex(mat, x0, y, z0).color(r, g, b, alpha).endVertex();
        bb.vertex(mat, x1, y, z0).color(r, g, b, alpha).endVertex();
        bb.vertex(mat, x1, y, z1).color(r, g, b, alpha).endVertex();
        bb.vertex(mat, x0, y, z1).color(r, g, b, alpha).endVertex();
    }

    static void drawSingleEdge(BufferBuilder bb, Matrix4f mat, float x0, float x1, float z0, float z1, float y, int edge, float r, float g, float b, float alpha) {
        float w = 0.07f;
        switch (edge) {
            case 0 -> {
                bb.vertex(mat, x0, y, z0).color(r, g, b, alpha).endVertex();
                bb.vertex(mat, x1, y, z0).color(r, g, b, alpha).endVertex();
                bb.vertex(mat, x1, y, z0 + w).color(r, g, b, 0f).endVertex();
                bb.vertex(mat, x0, y, z0 + w).color(r, g, b, 0f).endVertex();
            }
            case 1 -> {
                bb.vertex(mat, x0, y, z1 - w).color(r, g, b, 0f).endVertex();
                bb.vertex(mat, x1, y, z1 - w).color(r, g, b, 0f).endVertex();
                bb.vertex(mat, x1, y, z1).color(r, g, b, alpha).endVertex();
                bb.vertex(mat, x0, y, z1).color(r, g, b, alpha).endVertex();
            }
            case 2 -> {
                bb.vertex(mat, x0, y, z0).color(r, g, b, alpha).endVertex();
                bb.vertex(mat, x0 + w, y, z0).color(r, g, b, 0f).endVertex();
                bb.vertex(mat, x0 + w, y, z1).color(r, g, b, 0f).endVertex();
                bb.vertex(mat, x0, y, z1).color(r, g, b, alpha).endVertex();
            }
            case 3 -> {
                bb.vertex(mat, x1 - w, y, z0).color(r, g, b, 0f).endVertex();
                bb.vertex(mat, x1, y, z0).color(r, g, b, alpha).endVertex();
                bb.vertex(mat, x1, y, z1).color(r, g, b, alpha).endVertex();
                bb.vertex(mat, x1 - w, y, z1).color(r, g, b, 0f).endVertex();
            }
        }
    }

    static long packPosKey(int x, int y, int z) {
        long lx = ((long) x & 0x1FFFFFL) << 43;
        long ly = ((long) y & 0x3FFFFFL) << 21;
        long lz = ((long) z & 0x1FFFFFL);
        return lx | ly | lz;
    }
}
