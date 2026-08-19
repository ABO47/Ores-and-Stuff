package com.abo47.oresandstuff.client.ui.controls;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;

public final class IconAtlas {
    private static final String BASE = "textures/gui/icons/";
    private static final Map<String, ResourceTexture> ICON_TEXTURE_CACHE = new HashMap<>();

    private IconAtlas() {
    }

    public static ResourceTexture iconTexture(String fileName) {
        String clean = normalizeKey(fileName);
        if (clean.isBlank()) {
            return null;
        }
        ResourceTexture cached = ICON_TEXTURE_CACHE.get(clean);
        if (cached != null) {
            return cached;
        }
        ResourceLocation id = ResourceLocation.tryBuild("oresandstuff", BASE + clean + ".png");
        if (id == null) {
            return null;
        }
        ResourceTexture texture = new ResourceTexture(id).setDynamicColor(() -> colorFor(clean));
        ICON_TEXTURE_CACHE.put(clean, texture);
        return texture;
    }

    private static String normalizeKey(String fileName) {
        if (fileName == null) {
            return "";
        }
        String clean = fileName.trim().toLowerCase(Locale.ROOT);
        if (clean.endsWith(".png")) {
            clean = clean.substring(0, clean.length() - 4);
        }
        return clean;
    }

    private static int colorFor(String key) {
        if (key.equals("close")) {
            return OasColors.ERROR;
        }
        if (key.equals("variant")) {
            return OasColors.INTERACTIVE;
        }
        return OasColors.TEXT_SECONDARY;
    }
}
