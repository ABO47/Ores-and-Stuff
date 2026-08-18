package com.abo47.oresandstuff.fabric;

import com.abo47.oresandstuff.client.OasKeyBindings;
import com.abo47.oresandstuff.platform.Services;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public final class FabricModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OasKeyBindings.registerKeyMappings(KeyBindingHelper::registerKeyBinding);
        Services.hooks().onClientInit();
    }
}
