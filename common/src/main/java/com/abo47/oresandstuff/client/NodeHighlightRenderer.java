package com.abo47.oresandstuff.client;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;

final class NodeHighlightRenderer {
    private NodeHighlightRenderer() {
    }

    static void draw(PoseStack pose, Minecraft minecraft, Map<Long, ScannerFxTypes.FlashFx> flashes, Map<Long, Long> bursts, long nowMs) {
        drawNodeFlashes(pose, minecraft, flashes, nowMs);
        drawClusterBeacons(pose, flashes, nowMs);
    }

    private static void drawNodeFlashes(PoseStack pose, Minecraft minecraft, Map<Long, ScannerFxTypes.FlashFx> flashes, long nowMs) {
        if (minecraft == null || minecraft.level == null || flashes.isEmpty()) return;
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        flashes.entrySet().removeIf(e -> nowMs > e.getValue().expiresAt);
        Set<Long> active = new HashSet<>(flashes.keySet());

        for (ScannerFxTypes.FlashFx fx : flashes.values()) {
            float t = Mth.clamp((fx.expiresAt - nowMs) / 8000f, 0f, 1f);
            float tIn = Mth.clamp((1.1f - t) * 8f, 0f, 1f);
            float breathe = 0.65f + 0.35f * Mth.sin((float) (nowMs * 0.0045f));
            float alpha = tIn * t * breathe * 0.62f;
            float y = fx.y + 1.015f;

            float huePulse = 0.5f + 0.5f * Mth.sin((float) (nowMs * 0.0035f));
            int tint = fx.color != 0 ? fx.color : OasColors.ACCENT_PRIMARY;
            float brighten = 0.92f + 0.16f * huePulse;
            float r = OasColors.rf(tint) * brighten;
            float g = OasColors.gf(tint) * brighten;
            float b = OasColors.bf(tint);
            float x0 = fx.x, x1 = fx.x + 1f, z0 = fx.z, z1 = fx.z + 1f;
            ScannerRenderUtil.drawTopFill(bb, mat, x0, x1, z0, z1, y, r, g, b, alpha * 0.85f);
            drawNodeOuterRim(bb, mat, fx.x, fx.y, fx.z, active, y + 0.02f, r, g, b, alpha);
            drawNodeOuterRim(bb, mat, fx.x, fx.y, fx.z, active, y + 0.03f, r, g, b, alpha * 0.45f);

            float wallTop = y;
            float wallBottom = y - 1.5f;
            float wallAlpha = alpha * 0.38f;
            if (!hasFlashNeighbor(active, fx.x, fx.y, fx.z - 1)) drawNodeSide(bb, mat, x0, x1, z0, z0, wallTop, wallBottom, r, g, b, wallAlpha);
            if (!hasFlashNeighbor(active, fx.x, fx.y, fx.z + 1)) drawNodeSide(bb, mat, x0, x1, z1, z1, wallTop, wallBottom, r, g, b, wallAlpha);
            if (!hasFlashNeighbor(active, fx.x - 1, fx.y, fx.z)) drawNodeSide(bb, mat, x0, x0, z0, z1, wallTop, wallBottom, r, g, b, wallAlpha);
            if (!hasFlashNeighbor(active, fx.x + 1, fx.y, fx.z)) drawNodeSide(bb, mat, x1, x1, z0, z1, wallTop, wallBottom, r, g, b, wallAlpha);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void drawClusterBeacons(PoseStack pose, Map<Long, ScannerFxTypes.FlashFx> flashes, long nowMs) {
        if (flashes.isEmpty()) return;
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Set<Long> active = new HashSet<>(flashes.keySet());
        Set<Long> seen = new HashSet<>();
        for (Long start : active) {
            if (!seen.add(start)) continue;
            ArrayDeque<Long> q = new ArrayDeque<>();
            q.add(start);
            int count = 0;
            double sx = 0, sz = 0;
            int maxY = Integer.MIN_VALUE;
            long maxExpiry = 0L;
            int clusterColor = 0;

            while (!q.isEmpty()) {
                long k = q.poll();
                ScannerFxTypes.FlashFx fx = flashes.get(k);
                if (fx == null) continue;
                count++;
                if (clusterColor == 0 && fx.color != 0) {
                    clusterColor = fx.color;
                }
                sx += fx.x + 0.5;
                sz += fx.z + 0.5;
                maxY = Math.max(maxY, fx.y);
                maxExpiry = Math.max(maxExpiry, fx.expiresAt);
                int x = fx.x, y = fx.y, z = fx.z;
                for (long nk : active) {
                    if (seen.contains(nk)) continue;
                    int nx = unpackX(nk), ny = unpackY(nk), nz = unpackZ(nk);
                    if (Math.abs(nx - x) <= 2 && Math.abs(ny - y) <= 2 && Math.abs(nz - z) <= 2) {
                        seen.add(nk);
                        q.add(nk);
                    }
                }
            }

            if (count <= 0) continue;
            double cx = sx / count;
            double cz = sz / count;
            float t = Mth.clamp((maxExpiry - nowMs) / 8000f, 0f, 1f);
            float alpha = (0.22f + 0.30f * t) * (0.72f + 0.20f * Mth.sin((float) (nowMs * 0.0045f)));
            float y0 = maxY + 1.05f;
            float y2 = y0 + 46f;
            float w = 0.18f;
            float wTop = 0.05f;
            int tint = clusterColor != 0 ? clusterColor : OasColors.ACCENT_PRIMARY;
            float r = OasColors.rf(tint);
            float g = OasColors.gf(tint);
            float b = OasColors.bf(tint);
            float x0 = (float) cx - w, x1 = (float) cx + w, z0 = (float) cz - w, z1 = (float) cz + w;
            float x0t = (float) cx - wTop, x1t = (float) cx + wTop, z0t = (float) cz - wTop, z1t = (float) cz + wTop;

            bb.vertex(mat, x0t, y2, z0t).color(r, g, b, 0.0f).endVertex();
            bb.vertex(mat, x1t, y2, z0t).color(r, g, b, 0.0f).endVertex();
            bb.vertex(mat, x1, y0, z0).color(r, g, b, alpha).endVertex();
            bb.vertex(mat, x0, y0, z0).color(r, g, b, alpha).endVertex();

            bb.vertex(mat, x0t, y2, z1t).color(r, g, b, 0.0f).endVertex();
            bb.vertex(mat, x1t, y2, z1t).color(r, g, b, 0.0f).endVertex();
            bb.vertex(mat, x1, y0, z1).color(r, g, b, alpha).endVertex();
            bb.vertex(mat, x0, y0, z1).color(r, g, b, alpha).endVertex();

            bb.vertex(mat, x0t, y2, z0t).color(r, g, b, 0.0f).endVertex();
            bb.vertex(mat, x0t, y2, z1t).color(r, g, b, 0.0f).endVertex();
            bb.vertex(mat, x0, y0, z1).color(r, g, b, alpha).endVertex();
            bb.vertex(mat, x0, y0, z0).color(r, g, b, alpha).endVertex();

            bb.vertex(mat, x1t, y2, z0t).color(r, g, b, 0.0f).endVertex();
            bb.vertex(mat, x1t, y2, z1t).color(r, g, b, 0.0f).endVertex();
            bb.vertex(mat, x1, y0, z1).color(r, g, b, alpha).endVertex();
            bb.vertex(mat, x1, y0, z0).color(r, g, b, alpha).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void drawNodeSide(BufferBuilder bb, Matrix4f mat, float x0, float x1, float z0, float z1, float wallTop, float wallBottom, float r, float g, float b, float wallAlpha) {
        bb.vertex(mat, x0, wallTop, z0).color(r, g, b, wallAlpha).endVertex();
        bb.vertex(mat, x1, wallTop, z1).color(r, g, b, wallAlpha).endVertex();
        bb.vertex(mat, x1, wallBottom, z1).color(r, g, b, 0f).endVertex();
        bb.vertex(mat, x0, wallBottom, z0).color(r, g, b, 0f).endVertex();
    }

    private static void drawNodeOuterRim(BufferBuilder bb, Matrix4f mat, int x, int y, int z, Set<Long> active, float yDraw, float r, float g, float b, float alpha) {
        float x0 = x, x1 = x + 1f, z0 = z, z1 = z + 1f;
        if (!hasFlashNeighbor(active, x, y, z - 1)) ScannerRenderUtil.drawSingleEdge(bb, mat, x0, x1, z0, z1, yDraw, 0, r, g, b, alpha);
        if (!hasFlashNeighbor(active, x, y, z + 1)) ScannerRenderUtil.drawSingleEdge(bb, mat, x0, x1, z0, z1, yDraw, 1, r, g, b, alpha);
        if (!hasFlashNeighbor(active, x - 1, y, z)) ScannerRenderUtil.drawSingleEdge(bb, mat, x0, x1, z0, z1, yDraw, 2, r, g, b, alpha);
        if (!hasFlashNeighbor(active, x + 1, y, z)) ScannerRenderUtil.drawSingleEdge(bb, mat, x0, x1, z0, z1, yDraw, 3, r, g, b, alpha);
    }

    private static boolean hasFlashNeighbor(Set<Long> active, int x, int y, int z) {
        return active.contains(ScannerRenderUtil.packPosKey(x, y, z));
    }

    private static int unpackX(long key) {
        int x = (int) ((key >> 43) & 0x1FFFFF);
        return x >= 0x100000 ? x - 0x200000 : x;
    }

    private static int unpackY(long key) {
        int y = (int) ((key >> 21) & 0x3FFFFF);
        return y >= 0x200000 ? y - 0x400000 : y;
    }

    private static int unpackZ(long key) {
        int z = (int) (key & 0x1FFFFF);
        return z >= 0x100000 ? z - 0x200000 : z;
    }
}
