package com.poorcraft.crafting;

import com.poorcraft.inventory.ItemStack;
import com.poorcraft.world.block.BlockType;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Crafting recipe where the arrangement of ingredients matters.
 */
public class ShapedRecipe extends Recipe {

    private final BlockType[][] pattern;
    private final Map<BlockType, Integer> ingredientCounts;

    public ShapedRecipe(String id, String[] patternRows, Map<Character, BlockType> key, ItemStack result) {
        super(id, result.copy(), patternRows.length == 0 ? 0 : patternRows[0].length(), patternRows.length);
        if (patternRows.length == 0) {
            throw new IllegalArgumentException("Shaped recipe must have at least one row");
        }
        int expectedWidth = patternRows[0].length();
        if (expectedWidth == 0) {
            throw new IllegalArgumentException("Shaped recipe rows must not be empty");
        }
        this.pattern = new BlockType[getHeight()][getWidth()];
        Map<BlockType, Integer> counts = new HashMap<>();
        for (int y = 0; y < patternRows.length; y++) {
            String row = patternRows[y];
            if (row.length() != expectedWidth) {
                throw new IllegalArgumentException("Inconsistent row width in shaped recipe '" + id + "'");
            }
            for (int x = 0; x < row.length(); x++) {
                char symbol = row.charAt(x);
                if (symbol == ' ') {
                    this.pattern[y][x] = null;
                } else {
                    BlockType mapped = key.get(symbol);
                    if (mapped == null) {
                        throw new IllegalArgumentException("Missing key mapping for symbol '" + symbol + "' in shaped recipe '" + id + "'");
                    }
                    this.pattern[y][x] = mapped;
                    counts.merge(mapped, 1, Integer::sum);
                }
            }
        }
        this.ingredientCounts = Collections.unmodifiableMap(counts);
    }

    @Override
    public boolean matches(ItemStack[] inputs) {
        if (inputs == null) {
            return false;
        }
        int slotCount = inputs.length;
        if (slotCount == 0) {
            return false;
        }
        int gridSize = (int) Math.round(Math.sqrt(slotCount));
        if (gridSize * gridSize != slotCount) {
            return false;
        }
        if (gridSize < getWidth() || gridSize < getHeight()) {
            return false;
        }

        for (int offsetY = 0; offsetY <= gridSize - getHeight(); offsetY++) {
            for (int offsetX = 0; offsetX <= gridSize - getWidth(); offsetX++) {
                if (matchesAtOffset(inputs, gridSize, offsetX, offsetY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAtOffset(ItemStack[] inputs, int gridSize, int offsetX, int offsetY) {
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                int index = y * gridSize + x;
                ItemStack stack = inputs[index];
                boolean withinPattern = x >= offsetX && x < offsetX + getWidth()
                    && y >= offsetY && y < offsetY + getHeight();

                if (withinPattern) {
                    BlockType required = pattern[y - offsetY][x - offsetX];
                    if (required == null) {
                        if (!isEmptySlot(stack)) {
                            return false;
                        }
                    } else {
                        if (isEmptySlot(stack) || stack.getBlockType() != required) {
                            return false;
                        }
                    }
                } else {
                    if (!isEmptySlot(stack)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack getResult() {
        return getBaseResult().copy();
    }

    @Override
    protected Map<BlockType, Integer> getIngredientCounts() {
        return ingredientCounts;
    }
}
