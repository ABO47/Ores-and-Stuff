package com.abo47.oresandstuff.content;

import com.abo47.oresandstuff.data.EntityScanDataManager;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.platform.Services;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void bootstrap() {
        Services.hooks().registerReloadListener(OreNodeDataManager.INSTANCE);
        Services.hooks().registerReloadListener(EntityScanDataManager.INSTANCE);
    }
}
