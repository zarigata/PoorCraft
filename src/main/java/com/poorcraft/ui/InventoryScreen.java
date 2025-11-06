package com.poorcraft.ui;

import com.poorcraft.core.Game;
import com.poorcraft.inventory.Inventory;
import com.poorcraft.inventory.ItemStack;
import com.poorcraft.world.block.BlockType;

/**
 * Overlay screen for managing the player's 16x16 inventory grid.
 */
public class InventoryScreen extends UIScreen {

    private static final float OVERLAY_ALPHA = 0.55f;

    private final UIManager uiManager;

    private Game game;
    private Inventory inventory;

    private float slotSize;
    private float gridX;
    private float gridY;
    private float gridWidth;
    private float gridHeight;

    private int hoveredRow = -1;
    private int hoveredColumn = -1;
    private int selectedRow;
    private int selectedColumn;

    public InventoryScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.game = resolveGameReference();
        this.inventory = game != null ? game.getInventory() : null;
        this.selectedRow = Inventory.HEIGHT - 1;
        this.selectedColumn = 0;
    }

    @Override
    public void init() {
        this.game = resolveGameReference();
        this.inventory = game != null ? game.getInventory() : null;
        if (game != null) {
            selectedColumn = game.getSelectedHotbarSlot();
            selectedRow = Inventory.HEIGHT - 1;
        }
        recalculateLayout();
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        recalculateLayout();
    }

    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (inventory == null && game != null) {
            inventory = game.getInventory();
        }

        renderer.drawRect(0, 0, windowWidth, windowHeight, 0f, 0f, 0f, OVERLAY_ALPHA);

        float panelPadding = scaleDimension(32f);
        float titleOffset = scaleDimension(36f);
        float infoAreaHeight = scaleDimension(80f);
        float panelWidth = gridWidth + panelPadding * 2f;
        float panelHeight = gridHeight + panelPadding * 2f + infoAreaHeight;
        float panelX = (windowWidth - panelWidth) / 2f;
        float panelY = gridY - panelPadding - titleOffset;

        renderer.drawRect(panelX, panelY, panelWidth, panelHeight, 0.08f, 0.08f, 0.1f, 0.92f);
        float borderSize = scaleDimension(2f);
        renderer.drawRect(panelX, panelY, panelWidth, borderSize, 0.3f, 0.3f, 0.4f, 1.0f);
        renderer.drawRect(panelX, panelY + panelHeight - borderSize, panelWidth, borderSize, 0.3f, 0.3f, 0.4f, 1.0f);

        String title = "Inventory";
        float textScale = getTextScale(fontRenderer);
        float titleWidth = fontRenderer.getTextWidth(title) * textScale;
        fontRenderer.drawText(title, panelX + (panelWidth - titleWidth) / 2f, panelY + scaleDimension(28f), textScale, 0.95f, 0.95f, 0.95f, 1.0f);

        drawInventoryGrid(renderer, fontRenderer);
        drawSelectionDetails(renderer, fontRenderer, panelX, panelY + panelHeight - infoAreaHeight, panelWidth);
        drawInstructions(fontRenderer, panelX, panelY + panelHeight - scaleDimension(26f), panelWidth);
    }

    @Override
    public void update(float deltaTime) {
        // No animation yet.
    }

    @Override
    public void onMouseMove(float mouseX, float mouseY) {
        SlotPosition position = findSlotAt(mouseX, mouseY);
        if (position != null) {
            hoveredRow = position.row;
            hoveredColumn = position.column;
        } else {
            hoveredRow = -1;
            hoveredColumn = -1;
        }
    }

    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (button != 0) {
            return;
        }
        SlotPosition position = findSlotAt(mouseX, mouseY);
        if (position != null) {
            selectedRow = position.row;
            selectedColumn = position.column;
            if (game != null && position.row == Inventory.HEIGHT - 1) {
                game.selectHotbarSlot(position.column);
            }
        }
    }

    private void drawInventoryGrid(UIRenderer renderer, FontRenderer fontRenderer) {
        float slotSpacing = scaleDimension(4f);
        for (int row = 0; row < Inventory.HEIGHT; row++) {
            for (int column = 0; column < Inventory.WIDTH; column++) {
                float cellX = gridX + column * (slotSize + slotSpacing);
                float cellY = gridY + row * (slotSize + slotSpacing);

                boolean isHotbarRow = row == Inventory.HEIGHT - 1;
                boolean isSelected = row == selectedRow && column == selectedColumn;
                boolean isHovered = row == hoveredRow && column == hoveredColumn;
                boolean isHotbarSelected = isHotbarRow && game != null && column == game.getSelectedHotbarSlot();

                float r = 0.15f;
                float g = 0.15f;
                float b = 0.18f;
                float a = 0.85f;

                if (isHotbarRow) {
                    r = 0.2f;
                    g = 0.18f;
                    b = 0.23f;
                }
                if (isHotbarSelected) {
                    r = 0.35f;
                    g = 0.32f;
                    b = 0.18f;
                }
                if (isSelected) {
                    r = 0.5f;
                    g = 0.45f;
                    b = 0.2f;
                } else if (isHovered) {
                    r += 0.08f;
                    g += 0.08f;
                    b += 0.08f;
                }

                renderer.drawRect(cellX, cellY, slotSize, slotSize, r, g, b, a);
                renderer.drawRect(cellX, cellY, slotSize, 1f, 0.05f, 0.05f, 0.06f, 0.95f);
                renderer.drawRect(cellX, cellY + slotSize - 1f, slotSize, 1f, 0.05f, 0.05f, 0.06f, 0.95f);
                renderer.drawRect(cellX, cellY, 1f, slotSize, 0.05f, 0.05f, 0.06f, 0.95f);
                renderer.drawRect(cellX + slotSize - 1f, cellY, 1f, slotSize, 0.05f, 0.05f, 0.06f, 0.95f);

                ItemStack stack = inventory != null ? inventory.getSlot(row, column) : null;
                if (stack != null && !stack.isEmpty()) {
                    BlockType blockType = stack.getBlockType();
                    String label = formatBlockLabel(blockType);
                    if (!label.isEmpty()) {
                        float baseText = getTextScale(fontRenderer);
                        float labelScale = Math.min(0.55f * baseText, (slotSize - scaleDimension(6f)) / Math.max(fontRenderer.getTextWidth(label), 1f));
                        float labelWidth = fontRenderer.getTextWidth(label) * labelScale;
                        float labelX = cellX + (slotSize - labelWidth) / 2f;
                        float labelY = cellY + slotSize / 2f + fontRenderer.getTextHeight() * (labelScale - baseText) * 0.5f;
                        fontRenderer.drawText(label, labelX, labelY, labelScale, 0.92f, 0.92f, 0.95f, 1.0f);
                    }

                    int count = stack.getCount();
                    if (count > 0) {
                        String countText = formatCount(count);
                        float baseText = getTextScale(fontRenderer);
                        float countScale = 0.45f * baseText;
                        float countWidth = fontRenderer.getTextWidth(countText) * countScale;
                        float countX = cellX + slotSize - countWidth - scaleDimension(5f);
                        float countY = cellY + slotSize - scaleDimension(6f);
                        fontRenderer.drawText(countText, countX, countY, countScale, 1f, 1f, 1f, 1f);
                    }
                }
            }
        }
    }

    private void drawSelectionDetails(UIRenderer renderer, FontRenderer fontRenderer, float panelX, float infoY, float panelWidth) {
        float infoAreaHeight = scaleDimension(80f);
        renderer.drawRect(panelX + scaleDimension(12f), infoY + scaleDimension(10f), panelWidth - scaleDimension(24f), infoAreaHeight - scaleDimension(20f), 0.1f, 0.1f, 0.14f, 0.8f);

        ItemStack selectedStack = inventory != null ? inventory.getSlot(selectedRow, selectedColumn) : null;
        String labelText;
        String countText;
        if (selectedStack != null && !selectedStack.isEmpty()) {
            BlockType type = selectedStack.getBlockType();
            labelText = formatFullName(type);
            countText = "Quantity: " + selectedStack.getCount();
        } else {
            labelText = "Empty Slot";
            countText = "";
        }

        float textScale = getTextScale(fontRenderer);
        fontRenderer.drawText(labelText, panelX + scaleDimension(28f), infoY + scaleDimension(38f), 0.9f * textScale, 0.94f, 0.94f, 0.94f, 1f);
        if (!countText.isEmpty()) {
            fontRenderer.drawText(countText, panelX + scaleDimension(28f), infoY + scaleDimension(38f) + fontRenderer.getTextHeight() * 0.8f * textScale,
                0.8f * textScale, 0.8f, 0.85f, 0.9f, 1f);
        }
    }

    private void drawInstructions(FontRenderer fontRenderer, float panelX, float textY, float panelWidth) {
        String instructions = "Press E or Esc to close • Click a hotbar slot to equip";
        float textScale = getTextScale(fontRenderer);
        float instrScale = 0.8f * textScale;
        float textWidth = fontRenderer.getTextWidth(instructions) * instrScale;
        float textX = panelX + (panelWidth - textWidth) / 2f;
        fontRenderer.drawText(instructions, textX, textY, instrScale, 0.75f, 0.75f, 0.78f, 1f);
    }

    private SlotPosition findSlotAt(float mouseX, float mouseY) {
        float slotSpacing = scaleDimension(4f);
        for (int row = 0; row < Inventory.HEIGHT; row++) {
            float cellY = gridY + row * (slotSize + slotSpacing);
            if (mouseY < cellY || mouseY > cellY + slotSize) {
                continue;
            }
            for (int column = 0; column < Inventory.WIDTH; column++) {
                float cellX = gridX + column * (slotSize + slotSpacing);
                if (mouseX >= cellX && mouseX <= cellX + slotSize) {
                    return new SlotPosition(row, column);
                }
            }
        }
        return null;
    }

    private void recalculateLayout() {
        float maxGridWidth = windowWidth * 0.82f;
        float maxGridHeight = windowHeight * 0.72f;
        float slotSpacing = scaleDimension(4f);
        float titleOffset = scaleDimension(36f);

        float slotSizeByWidth = (maxGridWidth - (Inventory.WIDTH - 1) * slotSpacing) / Inventory.WIDTH;
        float slotSizeByHeight = (maxGridHeight - (Inventory.HEIGHT - 1) * slotSpacing) / Inventory.HEIGHT;
        this.slotSize = clamp(Math.min(slotSizeByWidth, slotSizeByHeight), scaleDimension(28f), scaleDimension(56f));

        this.gridWidth = Inventory.WIDTH * slotSize + (Inventory.WIDTH - 1) * slotSpacing;
        this.gridHeight = Inventory.HEIGHT * slotSize + (Inventory.HEIGHT - 1) * slotSpacing;
        this.gridX = (windowWidth - gridWidth) / 2f;
        this.gridY = (windowHeight - gridHeight) / 2f + titleOffset;
    }

    private String formatBlockLabel(BlockType blockType) {
        if (blockType == null || blockType == BlockType.AIR) {
            return "";
        }
        String name = blockType.name().replace('_', ' ').toLowerCase();
        if (name.length() <= 10) {
            return capitalizeWords(name);
        }
        String[] parts = name.split(" ");
        StringBuilder acronym = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                acronym.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return acronym.toString();
    }

    private String formatFullName(BlockType blockType) {
        if (blockType == null || blockType == BlockType.AIR) {
            return "Empty";
        }
        return capitalizeWords(blockType.name().replace('_', ' ').toLowerCase());
    }

    private String formatCount(int count) {
        if (count >= 1000) {
            return (count / 1000) + "k";
        }
        return Integer.toString(count);
    }

    private String capitalizeWords(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                builder.append(c);
            } else if (capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private Game resolveGameReference() {
        if (uiManager == null) {
            return null;
        }
        try {
            var field = uiManager.getClass().getDeclaredField("game");
            field.setAccessible(true);
            Object value = field.get(uiManager);
            return (value instanceof Game) ? (Game) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static final class SlotPosition {
        final int row;
        final int column;

        SlotPosition(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }
}
