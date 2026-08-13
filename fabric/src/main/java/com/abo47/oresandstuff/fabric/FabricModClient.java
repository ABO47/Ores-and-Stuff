package com.abo47.oresandstuff.fabric;

import com.abo47.oresandstuff.platform.Services;

import net.fabricmc.api.ClientModInitializer;

public final class FabricModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Services.hooks().onClientInit();
    }
}
