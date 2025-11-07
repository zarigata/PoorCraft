package com.poorcraft.crafting;

import com.poorcraft.inventory.ItemStack;
import com.poorcraft.world.block.BlockType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Crafting recipe where arrangement of ingredients does not matter.
 */
public class ShapelessRecipe extends Recipe {

    private final Map<BlockType, Integer> ingredients;

    public ShapelessRecipe(String id, Map<BlockType, Integer> ingredients, ItemStack result) {
        super(id, result.copy(), 2, 2);
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Shapeless recipe requires at least one ingredient");
        }
        this.ingredients = Collections.unmodifiableMap(new HashMap<>(ingredients));
    }

    @Override
    public boolean matches(ItemStack[] inputs) {
        if (inputs == null) {
            return false;
        }
        Map<BlockType, Integer> counts = new HashMap<>();
        int totalNonEmpty = 0;
        for (ItemStack stack : inputs) {
            if (!isEmptySlot(stack)) {
                counts.merge(stack.getBlockType(), 1, Integer::sum);
                totalNonEmpty++;
            }
        }
        int requiredTotal = ingredients.values().stream().mapToInt(Integer::intValue).sum();
        if (totalNonEmpty == 0 || requiredTotal != totalNonEmpty) {
            return false;
        }
        for (Map.Entry<BlockType, Integer> entry : ingredients.entrySet()) {
            int available = counts.getOrDefault(entry.getKey(), 0);
            if (available != entry.getValue()) {
                return false;
            }
        }
        return counts.size() == ingredients.size();
    }

    @Override
    public ItemStack getResult() {
        return getBaseResult().copy();
    }

    @Override
    protected Map<BlockType, Integer> getIngredientCounts() {
        return ingredients;
    }
}
