package com.abo47.oresandstuff.forge;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.client.OasKeyBindings;
import com.abo47.oresandstuff.data.config.RuntimeAssetPack;
import com.abo47.oresandstuff.platform.Services;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.AddPackFindersEvent;
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
        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onAddPackFinders);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        event.addRepositorySource(consumer -> {
            Path packPath = RuntimeAssetPack.packFolder();
            if (!Files.isDirectory(packPath)) {
                return;
            }
            Pack pack = Pack.readMetaAndCreate(
                    "oresandstuff_runtime",
                    Component.literal("Ores and Stuff runtime assets"),
                    true,
                    new Pack.ResourcesSupplier() {
                        @Override
                        public PackResources open(String id) {
                            return new PathPackResources(id, packPath, false);
                        }
                    },
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.DEFAULT
            );
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OasKeyBindings.registerKeyMappings(event::register);
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
