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
    private Purity purity = Purity.NORMAL;
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

    public Purity getPurity() {
        return purity;
    }

    public void setPurity(Purity purity) {
        this.purity = purity;
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
        tag.putString("Purity", purity.name());
        tag.putUUID("NodeId", nodeId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nodeTypeId = new ResourceLocation(tag.getString("NodeType"));
        purity = Purity.valueOf(tag.getString("Purity"));
        if (tag.hasUUID("NodeId")) {
            nodeId = tag.getUUID("NodeId");
        }
    }
}
