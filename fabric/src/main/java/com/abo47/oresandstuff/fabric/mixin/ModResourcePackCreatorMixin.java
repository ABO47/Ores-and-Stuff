package com.abo47.oresandstuff.fabric.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.impl.resource.loader.ModResourcePackCreator;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import com.abo47.oresandstuff.data.config.RuntimeAssetPack;

@Mixin(value = ModResourcePackCreator.class, remap = false)
public abstract class ModResourcePackCreatorMixin {
    @Shadow
    private PackType type;

    @Inject(method = "method_14453", at = @At("TAIL"))
    private void oresandstuff$addRuntimePack(Consumer<Pack> consumer, CallbackInfo ci) {
        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }
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
    }
}