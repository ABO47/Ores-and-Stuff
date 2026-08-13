package com.abo47.oresandstuff.client;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

final class BioScanRenderTypes extends RenderType {
    private static final Map<RenderType, RenderType> SCAN_WRAP_CACHE = new IdentityHashMap<>();
    private static final Map<RenderType, RenderType> DEPTH_WRAP_CACHE = new IdentityHashMap<>();

    static ShaderInstance SCAN_ENTITY_SHADER;
    private static final ShaderStateShard SCAN_ENTITY_STATE = new ShaderStateShard(() -> SCAN_ENTITY_SHADER);

    private BioScanRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    static RenderType wrapForDepth(RenderType original) {
        return DEPTH_WRAP_CACHE.computeIfAbsent(original, type -> new BioScanRenderTypes(
                OresAndStuffMod.MOD_ID + "_bio_depth_wrap",
                type.format(),
                type.mode(),
                type.bufferSize(),
                type.affectsCrumbling(),
                false,
                () -> {
                    type.setupRenderState();
                    SCAN_ENTITY_STATE.setupRenderState();
                    DEPTH_WRITE.setupRenderState();
                },
                () -> {
                    DEPTH_WRITE.clearRenderState();
                    SCAN_ENTITY_STATE.clearRenderState();
                    type.clearRenderState();
                }
        ));
    }

    static RenderType wrapForScan(RenderType original) {
        return SCAN_WRAP_CACHE.computeIfAbsent(original, type -> new BioScanRenderTypes(
                OresAndStuffMod.MOD_ID + "_bio_scan_wrap",
                type.format(),
                type.mode(),
                type.bufferSize(),
                type.affectsCrumbling(),
                false,
                () -> {
                    type.setupRenderState();
                    SCAN_ENTITY_STATE.setupRenderState();
                    TRANSLUCENT_TRANSPARENCY.setupRenderState();
                    COLOR_WRITE.setupRenderState();
                    new DepthTestStateShard("equal_depth", GL11.GL_EQUAL).setupRenderState();
                },
                () -> {
                    new DepthTestStateShard("equal_depth", GL11.GL_EQUAL).clearRenderState();
                    COLOR_WRITE.clearRenderState();
                    TRANSLUCENT_TRANSPARENCY.clearRenderState();
                    SCAN_ENTITY_STATE.clearRenderState();
                    type.clearRenderState();
                }
        ));
    }

    static ShaderInstance makeEntityScanShader(ResourceProvider resourceProvider) throws IOException {
        ResourceProvider provider = location -> resourceProvider
                .getResource(new ResourceLocation(OresAndStuffMod.MOD_ID, location.getPath()))
                .or(() -> resourceProvider.getResource(location));
        return new ShaderInstance(provider, "bio_entity_scan", DefaultVertexFormat.NEW_ENTITY);
    }
}
