package com.poorcraft.crafting;

import com.poorcraft.inventory.Inventory;
import com.poorcraft.inventory.ItemStack;
import com.poorcraft.world.block.BlockType;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all crafting recipes.
 */
public abstract class Recipe {

    private final String id;
    private final ItemStack result;
    private final int width;
    private final int height;

    protected Recipe(String id, ItemStack result, int width, int height) {
        this.id = id;
        this.result = result;
        this.width = width;
        this.height = height;
    }

    public String getId() {
        return id;
    }

    protected ItemStack getBaseResult() {
        return result;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public abstract boolean matches(ItemStack[] inputs);

    public abstract ItemStack getResult();

    protected boolean isEmptySlot(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    protected abstract Map<BlockType, Integer> getIngredientCounts();

    public boolean canCraftFromInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        Map<BlockType, Integer> requirements = getIngredientCounts();
        if (requirements.isEmpty()) {
            return false;
        }

        ItemStack[] snapshot = inventory.getSlotsSnapshot();
        Map<BlockType, Integer> available = new HashMap<>();
        for (ItemStack stack : snapshot) {
            if (stack != null && !stack.isEmpty()) {
                available.merge(stack.getBlockType(), stack.getCount(), Integer::sum);
            }
        }

        for (Map.Entry<BlockType, Integer> entry : requirements.entrySet()) {
            int have = available.getOrDefault(entry.getKey(), 0);
            if (have < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
}
