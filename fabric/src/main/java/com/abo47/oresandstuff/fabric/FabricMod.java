package com.abo47.oresandstuff.fabric;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModFeatures;
import com.abo47.oresandstuff.platform.Services;

import net.fabricmc.api.ModInitializer;

public final class FabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Services.setPlatform(new FabricPlatformService());
        Services.setHooks(new FabricPlatformHooks());
        FabricContent.register();
        FabricWorldgen.registerFeature();
        OresAndStuffMod.bootstrap();
        FabricWorldgen.register();
    }
}
