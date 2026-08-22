package com.abo47.oresandstuff.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

final class BioScanEntityOverlayRenderer {
    private BioScanEntityOverlayRenderer() {
    }

    static void render(PoseStack poseStack, float partialTick, MultiBufferSource.BufferSource bufferSource, Entity target, float progress) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || target == null || BioScanRenderTypes.SCAN_ENTITY_SHADER == null) return;

        float p = Mth.clamp(progress, 0f, 1f);
        double entityHeight = target.getBbHeight();
        float scanLimitY = p >= 1.0f ? 10000.0f : (float) (entityHeight * p * 1.35f);
        float red = 0.22f;
        float green = 0.88f;
        float blue = 1.0f;
        float alpha = 0.72f;

        if (BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("ScanLimitY") != null) {
            BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("ScanLimitY").set(scanLimitY);
        }
        if (BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("ScanTime") != null) {
            BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("ScanTime").set((System.currentTimeMillis() % 1000000L) / 1000.0f);
        }
        Matrix4f invModelView = new Matrix4f(poseStack.last().pose()).invert();
        invModelView.mul(new Matrix4f(RenderSystem.getModelViewMatrix()).invert());
        if (BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("InverseModelViewMat") != null) {
            BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("InverseModelViewMat").set(invModelView);
        }
        if (BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator") != null) {
            BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator").set(1f, 1f, 1f, 1f);
        }

        float yaw = Mth.lerp(partialTick, target.yRotO, target.getYRot());
        MultiBufferSource depthSource = requestedType -> tint(bufferSource.getBuffer(BioScanRenderTypes.wrapForDepth(requestedType)), 1f, 1f, 1f, 1f);
        mc.getEntityRenderDispatcher().render(target, 0.0D, 0.0D, 0.0D, yaw, partialTick, poseStack, depthSource, 15728880);
        bufferSource.endBatch();

        if (BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator") != null) {
            BioScanRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator").set(red, green, blue, alpha);
        }
        MultiBufferSource colorSource = requestedType -> tint(bufferSource.getBuffer(BioScanRenderTypes.wrapForScan(requestedType)), red, green, blue, alpha);
        mc.getEntityRenderDispatcher().render(target, 0.0D, 0.0D, 0.0D, yaw, partialTick, poseStack, colorSource, 15728880);
        bufferSource.endBatch();
    }

    private static VertexConsumer tint(VertexConsumer delegate, float r, float g, float b, float a) {
        return new TintedVertexConsumer(delegate, r, g, b, a);
    }

    private record TintedVertexConsumer(VertexConsumer delegate, float r, float g, float b, float a) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) { return delegate.vertex(x, y, z); }
        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            int nr = Math.min(255, Math.max(0, (int) (red * r)));
            int ng = Math.min(255, Math.max(0, (int) (green * g)));
            int nb = Math.min(255, Math.max(0, (int) (blue * b)));
            int na = Math.min(255, Math.max(0, (int) (alpha * a)));
            return delegate.color(nr, ng, nb, na);
        }
        @Override
        public VertexConsumer uv(float u, float v) { return delegate.uv(u, v); }
        @Override
        public VertexConsumer overlayCoords(int u, int v) { return delegate.overlayCoords(u, v); }
        @Override
        public VertexConsumer uv2(int u, int v) { return delegate.uv2(u, v); }
        @Override
        public VertexConsumer normal(float x, float y, float z) { return delegate.normal(x, y, z); }
        @Override
        public void endVertex() { delegate.endVertex(); }
        @Override
        public void defaultColor(int red, int green, int blue, int alpha) { delegate.defaultColor(red, green, blue, alpha); }
        @Override
        public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
