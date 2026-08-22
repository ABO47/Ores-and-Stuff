package com.abo47.oresandstuff.client;

import java.io.IOException;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import com.abo47.oresandstuff.OresAndStuffMod;

public final class OasShaders {
    private OasShaders() {
    }

    private static ShaderInstance terrainScanShader;
    private static ShaderInstance bioEntityShader;
    private static boolean warnedTerrain;
    private static boolean warnedBio;
    private static boolean initialized;

    public static ShaderInstance terrainScanShader() {
        return terrainScanShader;
    }

    public static ShaderInstance bioEntityShader() {
        return bioEntityShader;
    }

    public static boolean isTerrainReady() {
        return terrainScanShader != null;
    }

    public static boolean isBioReady() {
        return bioEntityShader != null;
    }

    public static void init(ResourceProvider provider) {
        if (initialized || provider == null) {
            return;
        }
        initialized = true;
        ResourceProvider redirect = location -> provider
                .getResource(new ResourceLocation(OresAndStuffMod.MOD_ID, location.getPath()))
                .or(() -> provider.getResource(location));
        try {
            terrainScanShader = new ShaderInstance(redirect, "terrain_scan", DefaultVertexFormat.POSITION_TEX);
        } catch (IOException e) {
            if (!warnedTerrain) {
                warnedTerrain = true;
                OresAndStuffMod.LOGGER.error("Failed to load terrain_scan shader", e);
            }
        }
        try {
            bioEntityShader = new ShaderInstance(redirect, "bio_entity_scan", DefaultVertexFormat.NEW_ENTITY);
        } catch (IOException e) {
            if (!warnedBio) {
                warnedBio = true;
                OresAndStuffMod.LOGGER.error("Failed to load bio_entity_scan shader", e);
            }
        }
        BioScanRenderTypes.SCAN_ENTITY_SHADER = bioEntityShader;
    }

    public static void ensureLoaded() {
        if (initialized) {
            return;
        }
        ResourceProvider provider = Minecraft.getInstance().getResourceManager();
        if (provider != null) {
            init(provider);
        }
    }
}
