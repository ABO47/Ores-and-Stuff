package com.abo47.oresandstuff.client;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

final class BioScanVisualRenderer {
    private BioScanVisualRenderer() {
    }

    private static final Map<Integer, Vec3> STABLE_END = new HashMap<>();

    public static void render(PoseStack pose, Minecraft minecraft, Entity target, double rx, double ry, double rz, float partialTick, float progress, long nowMs, long scanStartedMs, Matrix4f invView, Matrix4f invProj) {
        Plane plane = buildPlane(target, rx, ry, rz, progress);
        renderTriangleRay(pose, minecraft, target, rx, ry, rz, partialTick, progress, plane, nowMs);
    }

    private static Plane buildPlane(Entity target, double rx, double ry, double rz, float progress) {
        float w = Math.min(target.getBbWidth(), 8.0f);
        float h = Math.min(target.getBbHeight(), 8.0f);
        float x = (float) rx;
        float y = (float) ry;
        float z = (float) rz;
        float pad = Math.max(0.08f, w * 0.24f);
        float x0 = x - (w * 0.5f) - pad;
        float x1 = x + (w * 0.5f) + pad;
        float z0 = z - (w * 0.5f) - pad;
        float z1 = z + (w * 0.5f) + pad;
        float y0 = y + 0.02f;
        float y1 = y + h + 0.20f;

        float scanY = y0 + (y1 - y0) * Mth.clamp(progress, 0f, 1f);
        return new Plane(x0, x1, z0, z1, y0, y1, scanY);
    }

    private static void renderTriangleRay(PoseStack pose, Minecraft minecraft, Entity target, double rx, double ry, double rz, float partialTick, float progress, Plane plane, long nowMs) {
        if (minecraft == null || minecraft.player == null) return;

        float bbW = Math.min(target.getBbWidth(), 3.0f);
        float rayStyle = OasVisuals.BIO_SCAN_RAY_STYLE;

        double px = Mth.lerp(partialTick, minecraft.player.xo, minecraft.player.getX());
        double py = Mth.lerp(partialTick, minecraft.player.yo, minecraft.player.getY());
        double pz = Mth.lerp(partialTick, minecraft.player.zo, minecraft.player.getZ());
        Vec3 eye = new Vec3(px, py + minecraft.player.getEyeHeight(), pz);
        Vec3 look = minecraft.player.getLookAngle().normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 rightBase = look.cross(worldUp);
        if (rightBase.lengthSqr() < 1.0e-6) rightBase = new Vec3(1, 0, 0);
        Vec3 right = rightBase.normalize();
        HumanoidArm arm = minecraft.player.getMainArm();
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        Vec3 start = eye
                .add(look.scale(0.36))
                .add(right.scale(0.23 * side))
                .add(0.0, -0.18, 0.0);

        float clampedW = Math.min(target.getBbWidth(), 3.0f);
        float clampedH = Math.min(target.getBbHeight(), 4.0f);
        float visualRadius = Math.min(target.getBbWidth(), 8.0f) > 8.0f || target.getBbHeight() > 8.0f
                ? 0.42f
                : Mth.clamp(Math.max(clampedW * 0.24f, clampedH * 0.10f), 0.38f, 0.90f);
        Vec3 rawEnd = new Vec3(plane.x0 + (plane.x1 - plane.x0) * 0.5, plane.scanY, plane.z0 + (plane.z1 - plane.z0) * 0.5);
        Vec3 prev = STABLE_END.get(target.getId());
        Vec3 end = prev == null ? rawEnd : prev.lerp(rawEnd, 0.20);
        STABLE_END.put(target.getId(), end);

        float modelRadius = Math.max(clampedW * 0.55f, clampedH * 0.26f);
        float endHalf = Math.max(0.08f, modelRadius * (0.32f + 0.52f * Mth.clamp(progress, 0f, 1f)));
        endHalf = Math.min(endHalf, 1.05f);
        float startHalf = Math.max(0.012f, bbW * 0.03f * rayStyle);
        Vec3 dir = end.subtract(start).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 sideVec = dir.cross(up);
        if (sideVec.lengthSqr() < 1.0e-6) sideVec = new Vec3(1, 0, 0);
        Vec3 sideN = sideVec.normalize();

        Vec3 sL = start.subtract(sideN.scale(startHalf));
        Vec3 sR = start.add(sideN.scale(startHalf));
        Vec3 eL = end.subtract(sideN.scale(endHalf));
        Vec3 eR = end.add(sideN.scale(endHalf));
        Vec3 sC = start;
        Vec3 eC = end;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11_SRC_ALPHA(), GL11_ONE(), GL11_ONE(), GL11_ONE_MINUS_SRC_ALPHA());
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float t = (nowMs % 1000000L) / 1000.0f;
        float pulse = 0.74f + 0.26f * Mth.sin(t * 5.6f);
        float noiseA = 0.5f + 0.5f * Mth.sin(t * 13.0f + (float) target.getId() * 0.37f);
        float noiseB = 0.5f + 0.5f * Mth.sin(t * 9.0f + (float) target.getId() * 0.19f + 1.7f);
        float dissolve = 0.35f + 0.65f * (1.0f - Mth.clamp(progress, 0f, 1f));
        float endFade = (0.14f + 0.22f * noiseB) * dissolve;
        float baseCore = (0.155f + 0.030f * noiseA) * pulse;
        float coreStartA = baseCore;
        float coreEndA = baseCore * (0.32f + 0.28f * noiseB) * endFade;
        float edgeStartA = coreStartA * 0.22f;
        float edgeEndA = coreEndA * 0.24f;
        float sr = OasColors.rf(OasColors.ACCENT_PRIMARY);
        float sg = OasColors.gf(OasColors.ACCENT_PRIMARY);
        float sb = OasColors.bf(OasColors.ACCENT_PRIMARY);
        float er = sr, eg = sg, eb = sb;

        float u = Mth.clamp(progress, 0f, 1f);
        float stripe = 0.5f + 0.5f * Mth.sin(t * 22.0f + u * 28.0f + (float) target.getId() * 0.13f);
        float grain = 0.5f + 0.5f * Mth.sin(t * 47.0f + u * 61.0f + (float) target.getId() * 0.91f);
        float texBoost = 0.82f + 0.38f * stripe + 0.22f * grain;
        coreStartA *= texBoost;
        coreEndA *= texBoost;
        edgeStartA *= 0.75f + 0.25f * stripe;
        edgeEndA *= 0.75f + 0.25f * stripe;
        float tintJitter = 0.90f + 0.10f * grain;
        sr *= tintJitter;
        sg *= tintJitter;
        er *= tintJitter;
        eg *= tintJitter;

        bb.vertex(mat, (float) sL.x, (float) sL.y, (float) sL.z).color(sr, sg, sb, edgeStartA).endVertex();
        bb.vertex(mat, (float) sC.x, (float) sC.y, (float) sC.z).color(sr, sg, sb, coreStartA).endVertex();
        bb.vertex(mat, (float) eC.x, (float) eC.y, (float) eC.z).color(er, eg, eb, coreEndA).endVertex();
        bb.vertex(mat, (float) sL.x, (float) sL.y, (float) sL.z).color(sr, sg, sb, edgeStartA).endVertex();
        bb.vertex(mat, (float) eC.x, (float) eC.y, (float) eC.z).color(er, eg, eb, coreEndA).endVertex();
        bb.vertex(mat, (float) eL.x, (float) eL.y, (float) eL.z).color(er, eg, eb, edgeEndA).endVertex();

        bb.vertex(mat, (float) sC.x, (float) sC.y, (float) sC.z).color(sr, sg, sb, coreStartA).endVertex();
        bb.vertex(mat, (float) sR.x, (float) sR.y, (float) sR.z).color(sr, sg, sb, edgeStartA).endVertex();
        bb.vertex(mat, (float) eR.x, (float) eR.y, (float) eR.z).color(er, eg, eb, edgeEndA).endVertex();
        bb.vertex(mat, (float) sC.x, (float) sC.y, (float) sC.z).color(sr, sg, sb, coreStartA).endVertex();
        bb.vertex(mat, (float) eR.x, (float) eR.y, (float) eR.z).color(er, eg, eb, edgeEndA).endVertex();
        bb.vertex(mat, (float) eC.x, (float) eC.y, (float) eC.z).color(er, eg, eb, coreEndA).endVertex();

        BufferUploader.drawWithShader(bb.end());
        RenderSystem.defaultBlendFunc();
    }

    private static int GL11_SRC_ALPHA() { return org.lwjgl.opengl.GL11.GL_SRC_ALPHA; }
    private static int GL11_ONE() { return org.lwjgl.opengl.GL11.GL_ONE; }
    private static int GL11_ONE_MINUS_SRC_ALPHA() { return org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA; }

    private record Plane(float x0, float x1, float z0, float z1, float y0, float y1, float scanY) {
    }
}
