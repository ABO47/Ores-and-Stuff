package com.abo47.oresandstuff.fabric;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.content.ModBlockEntities;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.miner.InfiniteBatteryBlockEntity;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.platform.PlatformHooks;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class FabricPlatformHooks implements PlatformHooks {
    @Override
    public void registerBlockEntities() {
        ModBlockEntities.ORE_NODE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(OresAndStuffMod.MOD_ID, "ore_node"),
                BlockEntityType.Builder.of(OreNodeBlockEntity::new, ModBlocks.ORE_NODE).build(null));
        ModBlockEntities.MINER_MK1 = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(OresAndStuffMod.MOD_ID, "miner_mk1"),
                BlockEntityType.Builder.of(MinerBlockEntity::new, ModBlocks.MINER_MK1).build(null));
        ModBlockEntities.INFINITE_BATTERY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(OresAndStuffMod.MOD_ID, "infinite_battery"),
                BlockEntityType.Builder.of(InfiniteBatteryBlockEntity::new, ModBlocks.INFINITE_BATTERY).build(null));
    }

    @Override
    public void registerEnergy() {
        EnergyStorage.SIDED.registerForBlockEntity(FabricPlatformHooks::minerEnergy, ModBlockEntities.MINER_MK1);
        EnergyStorage.SIDED.registerForBlockEntity(FabricPlatformHooks::batteryEnergy, ModBlockEntities.INFINITE_BATTERY);
    }

    private static EnergyStorage minerEnergy(BlockEntity be, net.minecraft.core.Direction dir) {
        return be instanceof MinerBlockEntity miner ? new FabricEnergyCap(miner.getEnergyStorage()) : null;
    }

    private static EnergyStorage batteryEnergy(BlockEntity be, net.minecraft.core.Direction dir) {
        return be instanceof InfiniteBatteryBlockEntity battery ? new FabricEnergyCap(battery.getEnergyStorage()) : null;
    }

    @Override
    public int pullEnergyFrom(Level level, BlockPos pos, Direction side, int max) {
        EnergyStorage source = EnergyStorage.SIDED.find(level, pos, side);
        if (source == null || !source.supportsExtraction()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = source.extract(max, transaction);
            if (extracted > 0) {
                transaction.commit();
                return (int) Math.min(extracted, Integer.MAX_VALUE);
            }
        }
        return 0;
    }

    @Override
    public void registerReloadListener(SimpleJsonResourceReloadListener listener) {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation(OresAndStuffMod.MOD_ID, listener.getClass().getSimpleName().toLowerCase());
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                    ProfilerFiller prepProfiler, ProfilerFiller reloadProfiler, Executor bgExecutor, Executor gameExecutor) {
                return listener.reload(barrier, resourceManager, prepProfiler, reloadProfiler, bgExecutor, gameExecutor);
            }
        });
    }
}
