package com.poorcraft.inventory;

import com.poorcraft.world.block.BlockType;

import java.util.Arrays;

/**
 * Represents the player's 16x16 inventory grid.
 */
public class Inventory {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 16;
    public static final int SLOT_COUNT = WIDTH * HEIGHT;

    private final ItemStack[] slots;

    public Inventory() {
        this.slots = new ItemStack[SLOT_COUNT];
    }

    public ItemStack getSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            return null;
        }
        return slots[index];
    }

    public ItemStack getSlot(int row, int column) {
        return getSlot(toIndex(row, column));
    }

    public void setSlot(int index, ItemStack stack) {
        if (index < 0 || index >= SLOT_COUNT) {
            return;
        }
        slots[index] = stack;
    }

    public void setSlot(int row, int column, ItemStack stack) {
        setSlot(toIndex(row, column), stack);
    }

    /**
     * Adds the specified number of blocks to the inventory.
     *
     * @param blockType block type to add
     * @param amount    number of blocks to add
     * @return amount that could not be inserted (0 if fully inserted)
     */
    public int add(BlockType blockType, int amount) {
        if (blockType == null || blockType == BlockType.AIR || amount <= 0) {
            return amount;
        }

        int remaining = amount;

        // First, try to top off existing stacks of the same block type.
        for (int i = 0; i < SLOT_COUNT && remaining > 0; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.getBlockType() == blockType && stack.getCount() < ItemStack.MAX_STACK_SIZE) {
                remaining = stack.add(remaining);
            }
        }

        // Then, fill empty slots with new stacks.
        for (int i = 0; i < SLOT_COUNT && remaining > 0; i++) {
            if (slots[i] == null) {
                int stackAmount = Math.min(ItemStack.MAX_STACK_SIZE, remaining);
                slots[i] = new ItemStack(blockType, stackAmount);
                remaining -= stackAmount;
            }
        }

        return remaining;
    }

    /**
     * Removes one block from the specified slot, deleting the stack if empty.
     *
     * @param index slot index
     * @return {@code true} if an item was removed
     */
    public boolean removeOne(int index) {
        ItemStack stack = getSlot(index);
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        stack.remove(1);
        if (stack.isEmpty()) {
            slots[index] = null;
        }
        return true;
    }

    public ItemStack[] getSlotsSnapshot() {
        return Arrays.copyOf(slots, slots.length);
    }

    private int toIndex(int row, int column) {
        int clampedRow = Math.max(0, Math.min(HEIGHT - 1, row));
        int clampedColumn = Math.max(0, Math.min(WIDTH - 1, column));
        return clampedRow * WIDTH + clampedColumn;
    }
}
