package com.abo47.oresandstuff.fabric.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;

import com.abo47.oresandstuff.client.OasClient;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererRenderMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void oresandstuff$onRenderLevel(PoseStack poseStack, float partialTick, long p_109515_, boolean p_109516_, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        OasClient.renderLevel(poseStack, partialTick, projectionMatrix);
    }
}
