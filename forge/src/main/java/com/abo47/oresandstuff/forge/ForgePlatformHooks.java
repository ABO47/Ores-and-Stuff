package com.abo47.oresandstuff.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.client.OasClient;
import com.abo47.oresandstuff.client.OasShaders;
import com.abo47.oresandstuff.command.DevCommands;
import com.abo47.oresandstuff.content.ModBlockEntities;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.content.ModContent;
import com.abo47.oresandstuff.data.config.MinerTierConfig;
import com.abo47.oresandstuff.energy.EnergyStorage;
import com.abo47.oresandstuff.miner.InfiniteBatteryBlockEntity;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.network.NetworkChannels;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.platform.PlatformHooks;
import com.abo47.oresandstuff.world.ManualNodeMiningHandler;
import com.abo47.oresandstuff.world.NodeProtectionHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ForgePlatformHooks implements PlatformHooks {
    private final DeferredRegister<BlockEntityType<?>> beTypes;
    private final RegistryObject<BlockEntityType<?>> oreNode;
    private final RegistryObject<BlockEntityType<?>> miner;
    private final RegistryObject<BlockEntityType<?>> infiniteBattery;

    private final List<SimpleJsonResourceReloadListener> reloadListeners = new ArrayList<>();

    public ForgePlatformHooks() {
        MinerTierConfig.ensureLoaded();
        ModContent.registerMiners();
        beTypes = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OresAndStuffMod.MOD_ID);
        oreNode = beTypes.register("ore_node", () -> BlockEntityType.Builder.of(OreNodeBlockEntity::new, ModBlocks.ORE_NODE).build(null));
        miner = beTypes.register("miner", () -> BlockEntityType.Builder.of(MinerBlockEntity::new, ModContent.minerBlocksArray()).build(null));
        infiniteBattery = beTypes.register("infinite_battery", () -> BlockEntityType.Builder.of(InfiniteBatteryBlockEntity::new, ModBlocks.INFINITE_BATTERY).build(null));
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        beTypes.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterShaders);
    }

    private void onRegisterShaders(RegisterShadersEvent event) {
        OasShaders.init(event.getResourceProvider());
    }

    @Override
    public int pullEnergyFrom(Level level, BlockPos pos, Direction side, int max) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return 0;
        }
        var cap = be.getCapability(ForgeCapabilities.ENERGY, side);
        if (!cap.isPresent()) {
            return 0;
        }
        IEnergyStorage source = cap.orElse(null);
        if (source == null || !source.canExtract()) {
            return 0;
        }
        return source.extractEnergy(max, false);
    }

    @Override
    public void registerGameplayEvents() {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> DevCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((BreakEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level && NodeProtectionHandler.isProtected(level, event.getPos(), event.getState().getBlock(), event.getPlayer())) {
                event.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LeftClickBlock event) -> {
            if (ManualNodeMiningHandler.handle(event.getLevel(), event.getEntity(), event.getPos())) {
                event.setCanceled(true);
            }
        });
    }

    @Override
    public void onClientInit() {
        OasClient.init(Minecraft.getInstance());
        MinecraftForge.EVENT_BUS.addListener(this::onRenderLevel);
        MinecraftForge.EVENT_BUS.addListener(this::onRenderHud);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        OasClient.renderLevel(event.getPoseStack(), event.getPartialTick(), event.getProjectionMatrix());
    }

    private void onRenderHud(RenderGuiOverlayEvent.Post event) {
        OasClient.renderHud(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            OasClient.clientTick();
        }
    }

    @Override
    public void registerNetwork() {
        NetworkChannels.register();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerBlockEntities() {
        ModBlockEntities.ORE_NODE = (BlockEntityType<OreNodeBlockEntity>) oreNode.get();
        ModBlockEntities.MINER = (BlockEntityType<MinerBlockEntity>) miner.get();
        ModBlockEntities.INFINITE_BATTERY = (BlockEntityType<InfiniteBatteryBlockEntity>) infiniteBattery.get();
    }

    @Override
    public void registerEnergy() {
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, this::onAttachCapabilities);
    }

    private void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity be = event.getObject();
        if (be instanceof MinerBlockEntity miner) {
            event.addCapability(new ResourceLocation(OresAndStuffMod.MOD_ID, "miner_energy"), energyProvider(miner::getEnergyStorage));
        } else if (be instanceof InfiniteBatteryBlockEntity battery) {
            event.addCapability(new ResourceLocation(OresAndStuffMod.MOD_ID, "battery_energy"), energyProvider(battery::getEnergyStorage));
        }
    }

    private static ICapabilityProvider energyProvider(Supplier<EnergyStorage> storage) {
        return new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> c, Direction side) {
                if (c != ForgeCapabilities.ENERGY) {
                    return LazyOptional.empty();
                }
                EnergyStorage value = storage.get();
                return value == null ? LazyOptional.empty() : LazyOptional.of(() -> new ForgeEnergyCap(value)).cast();
            }
        };
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        reloadListeners.forEach(event::addListener);
    }

    @Override
    public void registerReloadListener(SimpleJsonResourceReloadListener listener) {
        reloadListeners.add(listener);
    }
}
