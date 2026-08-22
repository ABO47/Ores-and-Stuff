package com.abo47.oresandstuff.miner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

public class CommonItemTransfer implements IItemTransfer {
    public static final int SLOT_COUNT = 3;

    private final List<ItemStack> stacks = new ArrayList<>(SLOT_COUNT);

    public CommonItemTransfer() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            stacks.add(ItemStack.EMPTY);
        }
    }

    public ItemStack getStack(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? stacks.get(slot) : ItemStack.EMPTY;
    }

    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            stacks.set(slot, stack);
        }
    }

    @Override
    public int getSlots() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return getStack(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        setStack(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate, boolean notify) {
        if (slot < 0 || slot >= SLOT_COUNT || toInsert.isEmpty() || !isItemValid(slot, toInsert)) {
            return toInsert;
        }
        ItemStack current = stacks.get(slot);
        if (current.isEmpty()) {
            int limit = Math.min(toInsert.getCount(), getSlotLimit(slot));
            if (!simulate) {
                stacks.set(slot, toInsert.copyWithCount(limit));
            }
            return toInsert.copyWithCount(toInsert.getCount() - limit);
        }
        if (ItemStack.isSameItemSameTags(current, toInsert)) {
            int space = getSlotLimit(slot) - current.getCount();
            if (space <= 0) {
                return toInsert;
            }
            int moved = Math.min(space, toInsert.getCount());
            if (!simulate) {
                current.grow(moved);
            }
            return toInsert.copyWithCount(toInsert.getCount() - moved);
        }
        return toInsert;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate, boolean notify) {
        if (slot < 0 || slot >= SLOT_COUNT || stacks.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = stacks.get(slot);
        int taken = Math.min(current.getCount(), amount);
        ItemStack result = current.copyWithCount(taken);
        if (!simulate) {
            current.shrink(taken);
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
        return new ArrayList<>(stacks);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreFromSnapshot(Object snapshot) {
        stacks.clear();
        stacks.addAll((List<ItemStack>) snapshot);
    }

    /** Whether every given stack can be fully inserted across the slots. */
    public boolean canFitAll(List<ItemStack> toInsert) {
        List<ItemStack> remaining = new ArrayList<>(toInsert);
        for (int slot = 0; slot < SLOT_COUNT && !remaining.isEmpty(); slot++) {
            List<ItemStack> next = new ArrayList<>();
            for (ItemStack stack : remaining) {
                ItemStack leftover = insertItem(slot, stack, true, false);
                if (!leftover.isEmpty()) {
                    next.add(leftover);
                }
            }
            remaining = next;
        }
        return remaining.isEmpty();
    }

    /** Inserts every stack, returning false if any could not fit (nothing is committed then). */
    public boolean insertAll(List<ItemStack> toInsert) {
        if (!canFitAll(toInsert)) {
            return false;
        }
        for (ItemStack stack : toInsert) {
            ItemStack remaining = stack;
            for (int slot = 0; slot < SLOT_COUNT && !remaining.isEmpty(); slot++) {
                remaining = insertItem(slot, remaining, false, false);
            }
        }
        return true;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            list.add(stack.save(new CompoundTag()));
        }
        tag.put("Items", list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        stacks.clear();
        for (int i = 0; i < SLOT_COUNT; i++) {
            stacks.add(ItemStack.EMPTY);
        }
        if (tag.contains("Items")) {
            ListTag list = tag.getList("Items", 10);
            for (int i = 0; i < Math.min(SLOT_COUNT, list.size()); i++) {
                stacks.set(i, ItemStack.of(list.getCompound(i)));
            }
        }
    }
}