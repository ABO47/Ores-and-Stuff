package com.abo47.oresandstuff.forge;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.platform.Services;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OresAndStuffMod.MOD_ID)
public final class ForgeMod {
    public ForgeMod() {
        Services.setPlatform(new ForgePlatformService());
        Services.setHooks(new ForgePlatformHooks());

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeContent.register(modBus);
        ForgeWorldgen.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            OresAndStuffMod.bootstrap();
            ForgeWorldgen.bind();
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        Services.hooks().onClientInit();
    }
}
