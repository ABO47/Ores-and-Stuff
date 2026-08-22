package com.abo47.oresandstuff.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.AABB;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;

public final class ScannerPostProcessFx {
    private ScannerPostProcessFx() {
    }

    private static TextureTarget depthCopyTarget;
    private static boolean warnedMissingShader;

    public static boolean isShaderReady() {
        return OasShaders.isTerrainReady();
    }

    public static boolean renderTerrainSweep(ScannerFxTypes.ScanPulse pulse, float radius, Matrix4f invView, Matrix4f invProj) {
        var cfg = OresAndStuffConfig.scanner();
        return renderTerrainSweep(pulse, radius, invView, invProj, cfg.pulseWidthBlocks, cfg.experimentalVisualMode, null);
    }

    public static boolean renderTerrainSweep(ScannerFxTypes.ScanPulse pulse, float radius, Matrix4f invView, Matrix4f invProj, float ringWidth, int distanceMode, AABB bounds) {
        ShaderInstance shader = OasShaders.terrainScanShader();
        if (shader == null) {
            if (!warnedMissingShader) {
                warnedMissingShader = true;
                OresAndStuffMod.LOGGER.error("Holographic Sweep shader is not loaded. Check terrain_scan shader resources.");
            }
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        ensureDepthTarget(main.width, main.height);
        if (depthCopyTarget != null) {
            depthCopyTarget.copyDepthFrom(main);
        }
        main.bindWrite(false);

        shader.setSampler("depthTex", depthCopyTarget.getDepthTextureId());
        shader.safeGetUniform("center").set((float) pulse.originX, (float) pulse.originY, (float) pulse.originZ);
        shader.safeGetUniform("invViewMat").set(invView);
        shader.safeGetUniform("invProjMat").set(invProj);
        shader.safeGetUniform("radius").set(radius);
        shader.safeGetUniform("ringWidth").set(ringWidth);
        shader.safeGetUniform("holoWidth").set((float) OresAndStuffConfig.scanner().holographicWidthBlocks);
        shader.safeGetUniform("scanlineStrength").set((float) OresAndStuffConfig.scanner().holographicScanlineStrength);
        shader.safeGetUniform("quality").set((float) OresAndStuffConfig.scanner().qualityPreset);
        shader.safeGetUniform("use3dDistance").set(distanceMode == 1 ? 1f : 0f);
        shader.safeGetUniform("distanceMode").set((float) distanceMode);
        if (bounds != null) {
            shader.safeGetUniform("useBounds").set(1f);
            shader.safeGetUniform("boundMin").set((float) bounds.minX, (float) bounds.minY, (float) bounds.minZ);
            shader.safeGetUniform("boundMax").set((float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ);
        } else {
            shader.safeGetUniform("useBounds").set(0f);
        }

        blitFullscreen(main, shader);
        return true;
    }

    private static void ensureDepthTarget(int width, int height) {
        if (depthCopyTarget == null) {
            depthCopyTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            return;
        }
        if (depthCopyTarget.width != width || depthCopyTarget.height != height) {
            depthCopyTarget.resize(width, height, Minecraft.ON_OSX);
        }
    }

    private static void blitFullscreen(RenderTarget target, ShaderInstance shader) {
        int width = target.width;
        int height = target.height;

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ShaderInstance old = RenderSystem.getShader();
        RenderSystem.setShader(() -> shader);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, 0, height, 1, 100), VertexSorting.ORTHOGRAPHIC_Z);

        Tesselator t = Tesselator.getInstance();
        BufferBuilder bb = t.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(0, 0, -50).uv(0, 0).endVertex();
        bb.vertex(width, 0, -50).uv(1, 0).endVertex();
        bb.vertex(width, height, -50).uv(1, 1).endVertex();
        bb.vertex(0, height, -50).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(bb.end());

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.setShader(() -> old);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
