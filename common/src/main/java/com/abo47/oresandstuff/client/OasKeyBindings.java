package com.abo47.oresandstuff.client;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

public final class OasKeyBindings {
    public static final String CATEGORY = "key.categories.oresandstuff";

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.oresandstuff.open_settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    private OasKeyBindings() {
    }

    public static void registerKeyMappings(Consumer<KeyMapping> registrar) {
        registrar.accept(OPEN_SETTINGS);
    }
}
