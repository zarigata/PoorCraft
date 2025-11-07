package com.poorcraft.crafting;

import com.poorcraft.inventory.ItemStack;
import com.poorcraft.world.block.BlockType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry that stores all crafting recipes.
 */
public class RecipeRegistry {

    private final Map<String, Recipe> recipes;
    private final List<Recipe> recipeList;

    public RecipeRegistry() {
        this.recipes = new LinkedHashMap<>();
        this.recipeList = new ArrayList<>();
    }

    public void registerRecipe(Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("Cannot register null recipe");
        }
        if (recipes.containsKey(recipe.getId())) {
            throw new IllegalStateException("Duplicate recipe id: " + recipe.getId());
        }
        recipes.put(recipe.getId(), recipe);
        recipeList.add(recipe);
        System.out.println("[RecipeRegistry] Registered recipe: " + recipe.getId());
    }

    public Recipe findMatchingRecipe(ItemStack[] inputs) {
        for (Recipe recipe : recipeList) {
            if (recipe.matches(inputs)) {
                return recipe;
            }
        }
        return null;
    }

    public List<Recipe> getAllRecipes() {
        return Collections.unmodifiableList(recipeList);
    }

    public Recipe getRecipeById(String id) {
        return recipes.get(id);
    }

    public int loadDefaultRecipes() {
        registerRecipe(new ShapelessRecipe(
            "planks_from_wood",
            Map.of(BlockType.WOOD, 1),
            new ItemStack(BlockType.WOOD, 4)
        ));

        registerRecipe(new ShapedRecipe(
            "sticks_from_planks",
            new String[]{"A", "A"},
            Map.of('A', BlockType.WOOD),
            new ItemStack(BlockType.WOOD, 4)
        ));

        return recipeList.size();
    }
}
