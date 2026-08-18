package com.abo47.oresandstuff.miner;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

public class CommonItemTransfer implements IItemTransfer {
    private ItemStack stack = ItemStack.EMPTY;

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? stack : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            this.stack = stack;
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate, boolean notify) {
        if (slot != 0 || toInsert.isEmpty()) {
            return toInsert;
        }
        if (!isItemValid(slot, toInsert)) {
            return toInsert;
        }
        if (stack.isEmpty()) {
            int limit = Math.min(toInsert.getCount(), getSlotLimit(slot));
            if (!simulate) {
                this.stack = toInsert.copyWithCount(limit);
            }
            return toInsert.copyWithCount(toInsert.getCount() - limit);
        }
        if (ItemStack.isSameItemSameTags(stack, toInsert)) {
            int space = getSlotLimit(slot) - stack.getCount();
            if (space <= 0) {
                return toInsert;
            }
            int moved = Math.min(space, toInsert.getCount());
            if (!simulate) {
                stack.grow(moved);
            }
            return toInsert.copyWithCount(toInsert.getCount() - moved);
        }
        return toInsert;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate, boolean notify) {
        if (slot != 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(stack.getCount(), amount);
        ItemStack result = stack.copyWithCount(taken);
        if (!simulate) {
            stack.shrink(taken);
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public Object createSnapshot() {
        return stack.copy();
    }

    @Override
    public void restoreFromSnapshot(Object snapshot) {
        this.stack = (ItemStack) snapshot;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Item", stack.save(new CompoundTag()));
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        stack = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
    }
}
