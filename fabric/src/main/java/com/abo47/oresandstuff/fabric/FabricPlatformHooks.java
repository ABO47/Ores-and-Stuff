package com.abo47.oresandstuff.fabric;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.client.OasClient;
import com.abo47.oresandstuff.command.DevCommands;
import com.abo47.oresandstuff.content.ModBlockEntities;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.content.ModContent;
import com.abo47.oresandstuff.miner.InfiniteBatteryBlockEntity;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.network.NetworkChannels;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.platform.PlatformHooks;
import com.abo47.oresandstuff.world.ManualNodeMiningHandler;
import com.abo47.oresandstuff.world.NodeProtectionHandler;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;

public final class FabricPlatformHooks implements PlatformHooks {
    @Override
    public void registerGameplayEvents() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> DevCommands.register(dispatcher));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel level && NodeProtectionHandler.isProtected(level, pos, state.getBlock(), player)) {
                return false;
            }
            return true;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world instanceof ServerLevel && ManualNodeMiningHandler.handle(world, player, pos)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }


    @Override
    public void registerBlockEntities() {
        ModBlockEntities.ORE_NODE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(OresAndStuffMod.MOD_ID, "ore_node"),
                BlockEntityType.Builder.of(OreNodeBlockEntity::new, ModBlocks.ORE_NODE).build(null));
        ModBlockEntities.MINER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(OresAndStuffMod.MOD_ID, "miner"),
                BlockEntityType.Builder.of(MinerBlockEntity::new, ModContent.minerBlocksArray()).build(null));
        ModBlockEntities.INFINITE_BATTERY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(OresAndStuffMod.MOD_ID, "infinite_battery"),
                BlockEntityType.Builder.of(InfiniteBatteryBlockEntity::new, ModBlocks.INFINITE_BATTERY).build(null));
    }

    @Override
    public void registerEnergy() {
        EnergyStorage.SIDED.registerForBlockEntity(FabricPlatformHooks::minerEnergy, ModBlockEntities.MINER);
        EnergyStorage.SIDED.registerForBlockEntity(FabricPlatformHooks::batteryEnergy, ModBlockEntities.INFINITE_BATTERY);
    }

    private static EnergyStorage minerEnergy(BlockEntity be, Direction dir) {
        return be instanceof MinerBlockEntity miner ? new FabricEnergyCap(miner.getEnergyStorage()) : null;
    }

    private static EnergyStorage batteryEnergy(BlockEntity be, Direction dir) {
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
    public void registerNetwork() {
        NetworkChannels.register();
    }

    @Override
    public void onClientInit() {
        OasClient.init(Minecraft.getInstance());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> OasClient.clientTick());
        HudRenderCallback.EVENT.register((g, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            OasClient.renderHud(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        });
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
