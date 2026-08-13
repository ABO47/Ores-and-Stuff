package com.abo47.oresandstuff;

import com.abo47.oresandstuff.content.ModRegistries;
import com.abo47.oresandstuff.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OresAndStuffMod {
    public static final String MOD_ID = "oresandstuff";
    public static final String MOD_NAME = "Ores and Stuff";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private OresAndStuffMod() {
    }

    public static void bootstrap() {
        LOGGER.info("{} bootstrapped", MOD_NAME);
        Services.hooks().onCommonInit();
        ModRegistries.bootstrap();
        Services.hooks().registerEnergy();
        Services.hooks().registerBlockEntities();
        OresAndStuffConfig.load();
    }
}
