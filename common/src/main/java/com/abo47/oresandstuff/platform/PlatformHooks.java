package com.abo47.oresandstuff.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.level.Level;

public interface PlatformHooks {
    default void onCommonInit() {
    }

    default void onClientInit() {
    }

    default void registerReloadListener(SimpleJsonResourceReloadListener listener) {
    }

    default void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
    }

    default void registerGameplayEvents() {
    }

    default void registerNetwork() {
    }

    default void registerEnergy() {
    }

    default int pullEnergyFrom(Level level, BlockPos pos, Direction side, int max) {
        return 0;
    }

    default void registerBlockEntities() {
    }

    default void reload() {
    }
}
