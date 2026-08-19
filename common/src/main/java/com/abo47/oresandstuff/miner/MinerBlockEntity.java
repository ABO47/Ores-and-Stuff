package com.abo47.oresandstuff.miner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.block.MinerBlock;
import com.abo47.oresandstuff.client.miner.MinerScreen;
import com.abo47.oresandstuff.content.ModBlockEntities;
import com.abo47.oresandstuff.data.config.MinerTierConfig;
import com.abo47.oresandstuff.energy.EnergyStorage;
import com.abo47.oresandstuff.energy.SimpleEnergyStorage;
import com.abo47.oresandstuff.node.ExtractionRateService;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.node.Purity;
import com.abo47.oresandstuff.platform.Services;

public class MinerBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    private final EnergyStorage energy;
    private final int fePerTick;
    private final int maxReceiveFe;
    private final CommonItemTransfer output = new CommonItemTransfer();

    private double progress;
    private MinerStatus status = MinerStatus.NO_NODE;
    private ResourceLocation nodeTypeId = new ResourceLocation(OresAndStuffMod.MOD_ID, "iron");
    private Purity nodePurity = Purity.NORMAL;
    private boolean enabled = true;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINER, pos, state);
        MinerTierConfig.MinerTier tier = MinerTierConfig.get(MinerBlock.tierId(state)).orElse(null);
        this.fePerTick = tier != null ? tier.fePerTick() : OresAndStuffConfig.miner().fePerTick;
        this.maxReceiveFe = tier != null ? tier.maxReceiveFe() : OresAndStuffConfig.miner().maxReceiveFe;
        int buffer = tier != null ? tier.bufferFe() : OresAndStuffConfig.miner().bufferFe;
        this.energy = new SimpleEnergyStorage(buffer, this.maxReceiveFe, this.fePerTick);
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return MinerScreen.create(this, entityPlayer);
    }

    public void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (!enabled) {
            status = MinerStatus.STOPPED;
            setChanged();
            return;
        }

        pullPowerFromNeighbors();

        OreNodeBlockEntity node = findAttachedNode();
        if (node == null) {
            status = MinerStatus.NO_NODE;
            nodeTypeId = new ResourceLocation("minecraft", "air");
            setChanged();
            return;
        }

        nodeTypeId = node.getNodeTypeId();
        nodePurity = node.getPurity();

        if (energy.getEnergyStored() < fePerTick) {
            status = MinerStatus.NO_POWER;
            setChanged();
            return;
        }

        ItemStack drop = ExtractionRateService.buildDrop(node, 1);
        if (drop.isEmpty()) {
            status = MinerStatus.NO_NODE;
            setChanged();
            return;
        }

        double ratePerTick = ExtractionRateService.minerItemsPerSecond(node, 1.0) / 20.0;
        progress += ratePerTick;
        energy.extractEnergy(fePerTick, false);

        if (progress >= 1.0) {
            int toInsert = (int) progress;
            progress -= toInsert;
            drop.setCount(toInsert);
            ItemStack sim = output.insertItem(0, drop, true, false);
            if (!sim.isEmpty()) {
                status = MinerStatus.OUTPUT_FULL;
                progress += toInsert;
                setChanged();
                return;
            }
            output.insertItem(0, drop, false, false);
        }

        status = MinerStatus.RUNNING;
        ejectOutputToNeighbors();
        setChanged();
    }

    private OreNodeBlockEntity findAttachedNode() {
        if (level == null) {
            return null;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockEntity be = level.getBlockEntity(worldPosition.offset(dx, dy, dz));
                    if (be instanceof OreNodeBlockEntity node) {
                        return node;
                    }
                }
            }
        }
        return null;
    }

    private void pullPowerFromNeighbors() {
        if (level == null) {
            return;
        }
        int needed = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (needed <= 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            int pulled = Services.hooks().pullEnergyFrom(level, worldPosition.relative(direction), direction.getOpposite(), Math.min(needed, maxReceiveFe));
            if (pulled > 0) {
                energy.receiveEnergy(pulled, false);
                needed -= pulled;
                if (needed <= 0) {
                    return;
                }
            }
        }
    }

    private void ejectOutputToNeighbors() {
        ItemStack stack = output.getStack();
        if (stack.isEmpty() || level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            IItemTransfer neighbor = ItemTransferHelper.getItemTransfer(level, worldPosition.relative(direction), direction.getOpposite());
            if (neighbor == null) {
                continue;
            }
            ItemStack remaining = ItemTransferHelper.insertItem(neighbor, stack.copy(), false);
            if (remaining.getCount() != stack.getCount()) {
                output.setStack(remaining);
                stack = remaining;
                if (remaining.isEmpty()) {
                    return;
                }
            }
        }
    }

    public EnergyStorage getEnergyStorage() {
        return energy;
    }

    public IItemTransfer getOutputTransfer() {
        return output;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public double getProgress() {
        return progress;
    }

    public MinerStatus getStatus() {
        return status;
    }

    public ResourceLocation getNodeTypeId() {
        return nodeTypeId;
    }

    public Purity getNodePurity() {
        return nodePurity;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
    }

    public void toggleEnabled() {
        enabled = !enabled;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Energy", ((SimpleEnergyStorage) energy).serializeNBT());
        tag.put("Output", output.serializeNBT());
        tag.putDouble("Progress", progress);
        tag.putString("Status", status.name());
        tag.putString("NodeType", nodeTypeId.toString());
        tag.putString("NodePurity", nodePurity.name());
        tag.putBoolean("Enabled", enabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Energy")) {
            ((SimpleEnergyStorage) energy).deserializeNBT(tag.getCompound("Energy"));
        }
        if (tag.contains("Output")) {
            output.deserializeNBT(tag.getCompound("Output"));
        }
        progress = tag.getDouble("Progress");
        if (tag.contains("Status")) {
            try {
                status = MinerStatus.valueOf(tag.getString("Status"));
            } catch (Exception ignored) {
            }
        }
        if (tag.contains("NodeType")) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("NodeType"));
            if (parsed != null) {
                nodeTypeId = parsed;
            }
        }
        if (tag.contains("NodePurity")) {
            try {
                nodePurity = Purity.valueOf(tag.getString("NodePurity"));
            } catch (Exception ignored) {
            }
        }
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    }
}
