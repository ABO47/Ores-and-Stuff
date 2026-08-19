package com.abo47.oresandstuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.data.config.BioLibraryConfig;
import com.abo47.oresandstuff.data.config.MinerTierConfig;
import com.abo47.oresandstuff.platform.Services;

public final class OresAndStuffMod {
    public static final String MOD_ID = "oresandstuff";
    public static final String MOD_NAME = "Ores and Stuff";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private OresAndStuffMod() {
    }

    public static void bootstrap() {
        LOGGER.info("{} bootstrapped", MOD_NAME);
        Services.hooks().onCommonInit();
        BioLibraryConfig.ensureGenerated();
        MinerTierConfig.ensureLoaded();
        OreNodeDataManager.INSTANCE.ensureLoaded();
        Services.hooks().registerBlockEntities();
        Services.hooks().registerGameplayEvents();
        Services.hooks().registerNetwork();
        Services.hooks().registerEnergy();
        OresAndStuffConfig.load();
    }
}