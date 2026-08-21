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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.OresAndStuffMod;
import com.abo47.oresandstuff.api.MinerExtractEvent;
import com.abo47.oresandstuff.api.MinerHandle;
import com.abo47.oresandstuff.api.MiningEvents;
import com.abo47.oresandstuff.api.OreNodeHandle;
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
    private final MinerTierConfig.MinerTier tier;
    private final EnergyStorage energy;
    private final int fePerTick;
    private final int maxReceiveFe;
    private final CommonItemTransfer output = new CommonItemTransfer();

    private double progress;
    private float displayProgress;
    private MinerStatus status = MinerStatus.NO_NODE;
    private ResourceLocation nodeTypeId = new ResourceLocation(OresAndStuffMod.MOD_ID, "iron");
    private double nodeQuality = 100.0;
    private boolean enabled = true;
    private long placedTick = -1;

    // Caches to avoid per-tick 19³ BE scans (I HOPE IT WORKS)
    private static final int MINER_LIMIT_CACHE_TICKS = 20;
    private static final int NODE_CACHE_TICKS = 10;
    private long lastMinerLimitCheckTick = Long.MIN_VALUE;
    private UUID lastLimitNodeId;
    private boolean lastLimitResult;
    private int lastLimitMax = -1;
    private long cachedNodeCheckTick = Long.MIN_VALUE;
    private BlockPos cachedNodePos;
    private UUID cachedNodeId;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINER, pos, state);
        MinerTierConfig.MinerTier tier = MinerTierConfig.get(MinerBlock.tierId(state)).orElse(MinerTierConfig.defaultTier());
        this.tier = tier;
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
        if (placedTick == -1) {
            placedTick = level.getGameTime();
            setChanged();
        }

        pullPowerFromNeighbors();
        ejectOutputToNeighbors();

        if (!enabled) {
            status = MinerStatus.STOPPED;
            displayProgress = 0.0F;
            progress = 0.0;
            setChanged();
            return;
        }

        OreNodeBlockEntity node = getAttachedNode();
        if (node == null) {
            status = MinerStatus.NO_NODE;
            nodeTypeId = new ResourceLocation("minecraft", "air");
            displayProgress = 0.0F;
            progress = 0.0;
            setChanged();
            return;
        }

        nodeTypeId = node.getNodeTypeId();
        nodeQuality = node.getQualityPercent();

        if (atMinerLimit(node)) {
            status = MinerStatus.MAX_MINERS;
            displayProgress = 0.0F;
            progress = 0.0;
            setChanged();
            return;
        }

        if (energy.getEnergyStored() < fePerTick) {
            status = MinerStatus.NO_POWER;
            displayProgress = 0.0F;
            progress = 0.0;
            setChanged();
            return;
        }

        java.util.List<ItemStack> rolled = ExtractionRateService.buildDrops(node, 1, 1.0);
        if (rolled.isEmpty()) {
            status = MinerStatus.NO_NODE;
            displayProgress = 0.0F;
            progress = 0.0;
            setChanged();
            return;
        }

        double ratePerTick = ExtractionRateService.minerItemsPerSecond(node, tier.rateMultiplier()) / 20.0;
        status = MinerStatus.RUNNING;

        int cycleTicks = (int) Math.min(60.0, Math.max(4.0, Math.round(20.0 / ratePerTick)));
        float step = 1.0F / cycleTicks;
        int unitsPerCycle = (int) Math.round(ratePerTick * cycleTicks);
        if (unitsPerCycle < 1) {
            unitsPerCycle = 1;
        }
        int maxUnits = (CommonItemTransfer.SLOT_COUNT * 64) / Math.max(1, rolled.size());
        if (unitsPerCycle > maxUnits) {
            unitsPerCycle = maxUnits;
        }

        displayProgress += step;
        boolean produced = false;
        if (displayProgress >= 1.0F) {
            displayProgress -= 1.0F;
            java.util.List<ItemStack> batch = new java.util.ArrayList<>();
            for (ItemStack stack : rolled) {
                batch.add(stack.copyWithCount(stack.getCount() * unitsPerCycle));
            }
            if (output.canFitAll(batch)) {
                output.insertAll(batch);
                produced = true;
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    MiningEvents.fire(new MinerExtractEvent(serverLevel,
                            new MinerHandle(this),
                            new OreNodeHandle(node.getBlockPos(), node.getNodeTypeId(), node.getQualityPercent(), node.getNodeId()),
                            unitsPerCycle,
                            batch));
                }
            } else {
                status = MinerStatus.OUTPUT_FULL;
                displayProgress = 1.0F;
            }
        }

        if (status != MinerStatus.OUTPUT_FULL) {
            energy.extractEnergy(fePerTick, false);
        }
        progress = displayProgress;
        ejectOutputToNeighbors();
        setChanged();
    }

    public OreNodeBlockEntity getAttachedNode() {
        if (level == null) {
            return null;
        }
        long now = level.getGameTime();
        // fast-path: positive cache still valid
        if (cachedNodePos != null && now - cachedNodeCheckTick < NODE_CACHE_TICKS) {
            BlockEntity be = level.getBlockEntity(cachedNodePos);
            if (be instanceof OreNodeBlockEntity node && node.getNodeId().equals(cachedNodeId)) {
                return node;
            }
        }
        // fast-path: negative cache (recently found no node)
        if (cachedNodePos == null && cachedNodeId == null && cachedNodeCheckTick != Long.MIN_VALUE
                && now - cachedNodeCheckTick < NODE_CACHE_TICKS) {
            return null;
        }
        for (Direction dir : Direction.values()) {
            BlockPos p = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof OreNodeBlockEntity node) {
                cachedNodePos = p.immutable();
                cachedNodeId = node.getNodeId();
                cachedNodeCheckTick = now;
                return node;
            }
        }
        cachedNodePos = null;
        cachedNodeId = null;
        cachedNodeCheckTick = now;
        return null;
    }

    public void invalidateNodeCache() {
        cachedNodeCheckTick = Long.MIN_VALUE;
        cachedNodePos = null;
        cachedNodeId = null;
    }

    public void invalidateMinerLimitCache() {
        lastMinerLimitCheckTick = Long.MIN_VALUE;
        lastLimitNodeId = null;
    }

    public void invalidateMinerCaches() {
        invalidateNodeCache();
        invalidateMinerLimitCache();
    }

    /**
     * Invalidate cached limit/node lookups for miners near {@code pos}.
     * Called from {@link com.abo47.oresandstuff.block.MinerBlock} on placement/removal
     * so a newly placed miner is visible to neighbours without waiting for the TTL to expire.
     */
    public static void invalidateNearbyCaches(Level lvl, BlockPos pos) {
        if (lvl == null) {
            return;
        }
        int rXZ = 10;
        int rY = 5;
        for (int dx = -rXZ; dx <= rXZ; dx++) {
            for (int dy = -rY; dy <= rY; dy++) {
                for (int dz = -rXZ; dz <= rXZ; dz++) {
                    BlockEntity be = lvl.getBlockEntity(pos.offset(dx, dy, dz));
                    if (be instanceof MinerBlockEntity miner) {
                        miner.invalidateMinerCaches();
                    }
                }
            }
        }
    }

    /**
     * Whether the connected node already has its configured maximum number of
     * miners attached. Earlier-placed miners keep working; only the newest
     * miners beyond {@code maxMinersPerNode} are blocked. Miners attached to
     * opposite sides of the same cluster can be several blocks apart, so the
     * scan radius is derived from the cluster radius (2 * radius + 1) to always
     * cover every miner on the node.
     * <p>
     * The result is cached for {@value #MINER_LIMIT_CACHE_TICKS} ticks and is
     * eagerly invalidated when nearby miners are placed/removed via
     * {@link #invalidateNearbyCaches(Level, BlockPos)}.
     */
    private boolean atMinerLimit(OreNodeBlockEntity node) {
        if (level == null || node == null) {
            return false;
        }
        long gameTime = level.getGameTime();
        UUID nodeId = node.getNodeId();
        var type = OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).orElse(null);
        int max = type != null ? type.maxMinersPerNode() : 1;
        if (lastLimitNodeId != null && lastLimitNodeId.equals(nodeId)
                && gameTime - lastMinerLimitCheckTick < MINER_LIMIT_CACHE_TICKS
                && lastLimitMax == max) {
            return lastLimitResult;
        }
        int scanXZ = Math.min(9, Math.max(4, 2 * (type != null ? type.clusterRadius() : 2) + 1));
        List<MinerBlockEntity> attachedMiners = new ArrayList<>();
        attachedMiners.add(this);
        for (int dx = -scanXZ; dx <= scanXZ; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -scanXZ; dz <= scanXZ; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockEntity be = level.getBlockEntity(worldPosition.offset(dx, dy, dz));
                    if (be instanceof MinerBlockEntity other && other != this) {
                        OreNodeBlockEntity otherNode = other.getAttachedNode();
                        if (otherNode != null && otherNode.getNodeId().equals(nodeId)) {
                            attachedMiners.add(other);
                        }
                    }
                }
            }
        }
        if (attachedMiners.size() <= max) {
            lastLimitNodeId = nodeId;
            lastMinerLimitCheckTick = gameTime;
            lastLimitResult = false;
            lastLimitMax = max;
            return false;
        }
        // Sort by placement time so earliest miners keep working; excess newest miners are blocked.
        attachedMiners.sort(Comparator
                .comparingLong((MinerBlockEntity m) -> m.placedTick == -1 ? gameTime : m.placedTick)
                .thenComparingInt(m -> m.worldPosition.getX())
                .thenComparingInt(m -> m.worldPosition.getY())
                .thenComparingInt(m -> m.worldPosition.getZ()));
        int myIndex = attachedMiners.indexOf(this);
        boolean result = myIndex >= max;
        lastLimitNodeId = nodeId;
        lastMinerLimitCheckTick = gameTime;
        lastLimitResult = result;
        lastLimitMax = max;
        return result;
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

    public float getDisplayProgress() {
        return displayProgress;
    }

    public double getRatePerSecond() {
        OreNodeBlockEntity node = getAttachedNode();
        if (node == null) {
            return 0.0;
        }
        double rate = ExtractionRateService.minerItemsPerSecond(node, tier.rateMultiplier());
        int entries = OreNodeDataManager.INSTANCE.getNodeType(node.getNodeTypeId()).map(t -> t.drops().size()).orElse(0);
        return entries > 0 ? rate * entries : rate;
    }

    public MinerTierConfig.MinerTier getTier() {
        return tier;
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

    public long getPlacedTick() {
        return placedTick;
    }

    public void setPlacedTick(long tick) {
        this.placedTick = tick;
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
        tag.putLong("PlacedTick", placedTick);
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
        if (tag.contains("PlacedTick")) {
            placedTick = tag.getLong("PlacedTick");
        } else {
            placedTick = -1;
        }
    }
}
