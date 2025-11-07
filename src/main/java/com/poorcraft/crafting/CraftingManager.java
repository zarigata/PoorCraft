package com.poorcraft.crafting;

import com.poorcraft.inventory.ItemStack;

import java.util.Arrays;

/**
 * Handles crafting grid state and crafts items using registered recipes.
 */
public class CraftingManager {

    private final RecipeRegistry recipeRegistry;
    private final ItemStack[] craftingGrid;
    private ItemStack craftingResult;
    private Recipe currentRecipe;

    public CraftingManager(RecipeRegistry recipeRegistry) {
        this.recipeRegistry = recipeRegistry;
        this.craftingGrid = new ItemStack[4];
        this.craftingResult = null;
        this.currentRecipe = null;
    }

    public void setCraftingSlot(int index, ItemStack stack) {
        if (index < 0 || index >= craftingGrid.length) {
            throw new IndexOutOfBoundsException("Crafting slot index out of bounds: " + index);
        }
        craftingGrid[index] = stack;
        updateCraftingResult();
    }

    public ItemStack getCraftingSlot(int index) {
        if (index < 0 || index >= craftingGrid.length) {
            throw new IndexOutOfBoundsException("Crafting slot index out of bounds: " + index);
        }
        return craftingGrid[index];
    }

    public ItemStack[] getCraftingGrid() {
        return Arrays.copyOf(craftingGrid, craftingGrid.length);
    }

    public ItemStack getCraftingResult() {
        return craftingResult == null ? null : craftingResult.copy();
    }

    public Recipe getCurrentRecipe() {
        return currentRecipe;
    }

    public void updateCraftingResult() {
        Recipe recipe = recipeRegistry.findMatchingRecipe(craftingGrid);
        if (recipe != null) {
            currentRecipe = recipe;
            craftingResult = recipe.getResult();
        } else {
            currentRecipe = null;
            craftingResult = null;
        }
    }

    public boolean craft() {
        if (craftingResult == null || currentRecipe == null) {
            return false;
        }
        for (int i = 0; i < craftingGrid.length; i++) {
            ItemStack stack = craftingGrid[i];
            if (stack != null) {
                stack.remove(1);
                if (stack.isEmpty()) {
                    craftingGrid[i] = null;
                }
            }
        }
        updateCraftingResult();
        return true;
    }

    public void clearCraftingGrid() {
        Arrays.fill(craftingGrid, null);
        craftingResult = null;
        currentRecipe = null;
    }
}
