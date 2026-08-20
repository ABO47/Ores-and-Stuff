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
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.data.config.MinerTierConfig;
import com.abo47.oresandstuff.energy.EnergyStorage;
import com.abo47.oresandstuff.energy.SimpleEnergyStorage;
import com.abo47.oresandstuff.node.ExtractionRateService;
import com.abo47.oresandstuff.node.NodeQuality;
import com.abo47.oresandstuff.node.OreNodeBlockEntity;
import com.abo47.oresandstuff.node.OreNodeType;
import com.abo47.oresandstuff.platform.Services;

public class MinerBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI {
    private final EnergyStorage energy;
    private final int fePerTick;
    private final int maxReceiveFe;
    private final CommonItemTransfer output = new CommonItemTransfer();

    private double progress;
    private MinerStatus status = MinerStatus.NO_NODE;
    private ResourceLocation nodeTypeId = new ResourceLocation(OresAndStuffMod.MOD_ID, "iron");
    private double nodeQuality = 100.0;
    private boolean enabled = true;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINER, pos, state);
        MinerTierConfig.MinerTier tier = MinerTierConfig.get(MinerBlock.tierId(state)).orElse(MinerTierConfig.defaultTier());
        this.fePerTick = tier.fePerTick();
        this.maxReceiveFe = tier.maxReceiveFe();
        this.energy = new SimpleEnergyStorage(tier.bufferFe(), this.maxReceiveFe, this.fePerTick);
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
        nodeQuality = node.getQualityPercent();

        if (atMinerLimit(node)) {
            status = MinerStatus.MAX_MINERS;
            setChanged();
            return;
        }

        if (energy.getEnergyStored() < fePerTick) {
            status = MinerStatus.NO_POWER;
            setChanged();
            return;
        }

        java.util.List<ItemStack> rolled = ExtractionRateService.buildDrops(node, 1, 1.0);
        if (rolled.isEmpty()) {
            status = MinerStatus.NO_NODE;
            setChanged();
            return;
        }

        double ratePerTick = ExtractionRateService.minerItemsPerSecond(node, 1.0) / 20.0;
        progress += ratePerTick;

        if (progress >= 1.0) {
            int units = (int) progress;
            java.util.List<ItemStack> batch = new java.util.ArrayList<>();
            for (ItemStack stack : rolled) {
                batch.add(stack.copyWithCount(stack.getCount() * units));
            }
            if (!output.insertAll(batch)) {
                status = MinerStatus.OUTPUT_FULL;
                setChanged();
                return;
            }
            progress -= units;
            energy.extractEnergy(fePerTick, false);
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

    /**
     * Whether the connected node already has its configured maximum number of
     * miners attached. Miners attached to opposite sides of the same cluster
     * can be several blocks apart, so the scan radius is derived from the
     * cluster radius (2 * radius + 1) to always cover every miner on the node.
     */
    private boolean atMinerLimit(OreNodeBlockEntity node) {
        if (level == null || node == null) {
            return false;
        }
        var type = OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).orElse(null);
        int max = type != null ? type.maxMinersPerNode() : 1;
        int scanXZ = Math.min(9, Math.max(4, 2 * (type != null ? type.clusterRadius() : 2) + 1));
        java.util.UUID nodeId = node.getNodeId();
        int attached = 0;
        for (int dx = -scanXZ; dx <= scanXZ; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -scanXZ; dz <= scanXZ; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockEntity be = level.getBlockEntity(worldPosition.offset(dx, dy, dz));
                    if (be instanceof MinerBlockEntity other && other != this) {
                        OreNodeBlockEntity otherNode = other.findAttachedNode();
                        if (otherNode != null && otherNode.getNodeId().equals(nodeId)) {
                            attached++;
                        }
                    }
                }
            }
        }
        return attached >= max;
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
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < output.getSlots(); slot++) {
            ItemStack stack = output.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                IItemTransfer neighbor = ItemTransferHelper.getItemTransfer(level, worldPosition.relative(direction), direction.getOpposite());
                if (neighbor == null) {
                    continue;
                }
                ItemStack remaining = ItemTransferHelper.insertItem(neighbor, stack.copy(), false);
                if (remaining.getCount() != stack.getCount()) {
                    output.setStack(slot, remaining);
                    stack = remaining;
                    if (remaining.isEmpty()) {
                        break;
                    }
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

    public double getRatePerSecond() {
        OreNodeBlockEntity node = findAttachedNode();
        return node != null ? ExtractionRateService.minerItemsPerSecond(node, 1.0) : 0.0;
    }

    public MinerStatus getStatus() {
        return status;
    }

    public ResourceLocation getNodeTypeId() {
        return nodeTypeId;
    }

    public double getNodeQuality() {
        return nodeQuality;
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
        tag.putDouble("NodeQuality", nodeQuality);
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
        if (tag.contains("NodeQuality")) {
            nodeQuality = NodeQuality.clamp(tag.getDouble("NodeQuality"));
        } else if (tag.contains("NodePurity")) {
            nodeQuality = NodeQuality.clamp(switch (tag.getString("NodePurity")) {
                case "IMPURE" -> 50.0;
                case "PURE" -> 200.0;
                default -> 100.0;
            });
        }
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    }
}
