package com.abo47.oresandstuff.node;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.abo47.oresandstuff.content.ModBlockEntities;

public class OreNodeBlockEntity extends BlockEntity {
    private ResourceLocation nodeTypeId = new ResourceLocation("oresandstuff", "iron");
    private double qualityPercent = 100.0;
    private UUID nodeId = UUID.randomUUID();

    public OreNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_NODE, pos, state);
    }

    public ResourceLocation getNodeTypeId() {
        return nodeTypeId;
    }

    public void setNodeTypeId(ResourceLocation nodeTypeId) {
        this.nodeTypeId = nodeTypeId;
        setChanged();
    }

    public double getQualityPercent() {
        return qualityPercent;
    }

    public void setQualityPercent(double qualityPercent) {
        this.qualityPercent = qualityPercent;
        setChanged();
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("NodeType", nodeTypeId.toString());
        tag.putDouble("QualityPercent", qualityPercent);
        tag.putUUID("NodeId", nodeId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nodeTypeId = new ResourceLocation(tag.getString("NodeType"));
        if (tag.contains("QualityPercent")) {
            qualityPercent = NodeQuality.clamp(tag.getDouble("QualityPercent"));
        } else if (tag.contains("PurityPercent")) {
            qualityPercent = NodeQuality.clamp(tag.getDouble("PurityPercent"));
        } else if (tag.contains("Purity")) {
            qualityPercent = switch (tag.getString("Purity")) {
                case "IMPURE" -> 50.0;
                case "PURE" -> 200.0;
                default -> 100.0;
            };
        }
        if (tag.hasUUID("NodeId")) {
            nodeId = tag.getUUID("NodeId");
        }
    }
}
