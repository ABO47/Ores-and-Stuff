package com.abo47.oresandstuff.forge;

import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.command.DevCommands;
import com.abo47.oresandstuff.network.NetworkChannels;
import com.abo47.oresandstuff.world.ManualNodeMiningHandler;
import com.abo47.oresandstuff.world.NodeProtectionHandler;
import com.abo47.oresandstuff.content.ModBlockEntities;
import com.abo47.oresandstuff.content.ModBlocks;
import com.abo47.oresandstuff.miner.InfiniteBatteryBlockEntity;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ForgePlatformHooks implements PlatformHooks {
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OresAndStuffMod.MOD_ID);
    private final RegistryObject<BlockEntityType<?>> oreNode =
            BE_TYPES.register("ore_node", () -> BlockEntityType.Builder.of(OreNodeBlockEntity::new, ModBlocks.ORE_NODE).build(null));
    private final RegistryObject<BlockEntityType<?>> minerMk1 =
            BE_TYPES.register("miner_mk1", () -> BlockEntityType.Builder.of(MinerBlockEntity::new, ModBlocks.MINER_MK1).build(null));
    private final RegistryObject<BlockEntityType<?>> infiniteBattery =
            BE_TYPES.register("infinite_battery", () -> BlockEntityType.Builder.of(InfiniteBatteryBlockEntity::new, ModBlocks.INFINITE_BATTERY).build(null));

    private final List<SimpleJsonResourceReloadListener> reloadListeners = new ArrayList<>();

    public ForgePlatformHooks() {
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        BE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
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
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent event) -> DevCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.level.BlockEvent.BreakEvent event) -> {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && NodeProtectionHandler.isProtected(level, event.getPos(), event.getState().getBlock(), event.getPlayer())) {
                event.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) -> {
            if (ManualNodeMiningHandler.handle(event.getLevel(), event.getEntity(), event.getPos())) {
                event.setCanceled(true);
            }
        });
    }

    @Override
    public void registerNetwork() {
        NetworkChannels.register();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerBlockEntities() {
        ModBlockEntities.ORE_NODE = (BlockEntityType<OreNodeBlockEntity>) oreNode.get();
        ModBlockEntities.MINER_MK1 = (BlockEntityType<MinerBlockEntity>) minerMk1.get();
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

    private static ICapabilityProvider energyProvider(Supplier<com.abo47.oresandstuff.energy.EnergyStorage> storage) {
        return new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> c, Direction side) {
                if (c != ForgeCapabilities.ENERGY) {
                    return LazyOptional.empty();
                }
                com.abo47.oresandstuff.energy.EnergyStorage value = storage.get();
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
