package com.abo47.oresandstuff.forge;

import java.nio.file.Path;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import net.minecraftforge.fml.ModList;

import com.abo47.oresandstuff.platform.PlatformService;

public final class ForgePlatformService implements PlatformService {
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String loaderName() {
        return "forge";
    }

    @Override
    public String loaderVersion() {
        return FMLLoader.versionInfo().forgeVersion();
    }

    @Override
    public String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
