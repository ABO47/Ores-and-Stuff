package com.abo47.oresandstuff.client.miner;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.miner.MinerStatus;
import com.abo47.oresandstuff.node.OreNodeType;

public class MinerUIState extends Widget {
    private final MinerBlockEntity be;

    private int energyStored;
    private int maxEnergy;
    private double progress;
    private double nodeQuality;
    private MinerStatus status;
    private ResourceLocation nodeTypeId;
    private boolean enabled;
    private boolean synced;

    public MinerUIState(MinerBlockEntity be) {
        super(0, 0, 1, 1);
        this.be = be;
        this.energyStored = be.getEnergyStored();
        this.maxEnergy = be.getMaxEnergyStored();
        this.progress = be.getProgress();
        this.nodeQuality = be.getNodeQuality();
        this.status = be.getStatus();
        this.nodeTypeId = be.getNodeTypeId();
        this.enabled = be.isEnabled();
    }

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        writeState(buffer);
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        readState(buffer);
        synced = true;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        writeUpdateInfo(1, this::writeState);
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == 1) {
            readState(buffer);
            synced = true;
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    private void writeState(FriendlyByteBuf buf) {
        buf.writeInt(be.getEnergyStored());
        buf.writeInt(be.getMaxEnergyStored());
        buf.writeDouble(be.getProgress());
        buf.writeDouble(be.getNodeQuality());
        buf.writeUtf(be.getStatus().name());
        buf.writeUtf(be.getNodeTypeId().toString());
        buf.writeBoolean(be.isEnabled());
    }

    private void readState(FriendlyByteBuf buf) {
        energyStored = buf.readInt();
        maxEnergy = buf.readInt();
        progress = buf.readDouble();
        nodeQuality = buf.readDouble();
        status = MinerStatus.valueOf(buf.readUtf());
        nodeTypeId = new ResourceLocation(buf.readUtf());
        enabled = buf.readBoolean();
    }

    private boolean live() {
        Level level = be.getLevel();
        return level == null || !level.isClientSide;
    }

    public int getEnergyStored() {
        return live() ? be.getEnergyStored() : energyStored;
    }

    public int getMaxEnergy() {
        return live() ? be.getMaxEnergyStored() : maxEnergy;
    }

    public double getProgress() {
        return live() ? be.getProgress() : progress;
    }

    public double getNodeQuality() {
        return live() ? be.getNodeQuality() : nodeQuality;
    }

    public MinerStatus getStatus() {
        return live() ? be.getStatus() : status;
    }

    public ResourceLocation getNodeTypeId() {
        return live() ? be.getNodeTypeId() : nodeTypeId;
    }

    public boolean isEnabled() {
        return live() ? be.isEnabled() : enabled;
    }

    public Level getLevel() {
        return be.getLevel();
    }

    public double getRatePerSecond() {
        double multiplier = be.getTier().rateMultiplier();
        return nodeQuality / 100.0 * multiplier
                * OreNodeDataManager.INSTANCE.getNodeType(nodeTypeId)
                        .map(OreNodeType::baseRatePerSecond).orElse(0.0);
    }

    public String getTierId() {
        return be.getTier().id();
    }

    public boolean isSynced() {
        return synced;
    }
}