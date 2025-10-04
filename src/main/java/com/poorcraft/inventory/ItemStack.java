package com.poorcraft.inventory;

import com.poorcraft.world.block.BlockType;

/**
 * Represents a stack of identical block items in the player's inventory.
 */
public class ItemStack {

    public static final int MAX_STACK_SIZE = 1024;

    private final BlockType blockType;
    private int count;

    /**
     * Creates a new stack containing the specified number of blocks.
     *
     * @param blockType block type
     * @param count initial count (clamped to {@link #MAX_STACK_SIZE})
     */
    public ItemStack(BlockType blockType, int count) {
        this.blockType = blockType;
        this.count = Math.min(Math.max(count, 0), MAX_STACK_SIZE);
    }

    /**
     * @return the block type stored in this stack
     */
    public BlockType getBlockType() {
        return blockType;
    }

    /**
     * @return number of blocks in this stack
     */
    public int getCount() {
        return count;
    }

    /**
     * Adds up to {@code amount} blocks to this stack.
     *
     * @param amount desired amount to add
     * @return number of blocks that could not be added because the stack is full
     */
    public int add(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int space = MAX_STACK_SIZE - count;
        int toAdd = Math.min(space, amount);
        count += toAdd;
        return amount - toAdd;
    }

    /**
     * Removes up to {@code amount} blocks from this stack.
     *
     * @param amount amount to remove
     * @return actual amount removed
     */
    public int remove(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int removed = Math.min(count, amount);
        count -= removed;
        return removed;
    }

    /**
     * @return true if this stack is empty
     */
    public boolean isEmpty() {
        return count <= 0;
    }

    /**
     * Creates a deep copy of this stack.
     */
    public ItemStack copy() {
        return new ItemStack(blockType, count);
    }
}
