package com.poorcraft.ui;

import com.poorcraft.core.Game;
import com.poorcraft.inventory.Inventory;
import com.poorcraft.inventory.ItemStack;
import com.poorcraft.world.block.BlockType;

/**
 * Overlay screen for managing the player's 16x16 inventory grid.
 */
public class InventoryScreen extends UIScreen {

    private static final float PANEL_PADDING = 32f;
    private static final float TITLE_OFFSET = 36f;
    private static final float INFO_AREA_HEIGHT = 80f;
    private static final float SLOT_SPACING = 4f;
    private static final float OVERLAY_ALPHA = 0.55f;

    private final UIManager uiManager;

    private Game game;
    private Inventory inventory;

    private float slotSize;
    private float gridX;
    private float gridY;
    private float gridWidth;
    private float gridHeight;
    private float currentSlotSpacing;

    private int hoveredRow = -1;
    private int hoveredColumn = -1;
    private int selectedRow;
    private int selectedColumn;

    public InventoryScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
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

        float panelPadding = scale(PANEL_PADDING);
        float infoHeight = scale(INFO_AREA_HEIGHT);
        float titleOffset = scale(TITLE_OFFSET);

        float panelWidth = gridWidth + panelPadding * 2f;
        float panelHeight = gridHeight + panelPadding * 2f + infoHeight;
        float panelX = (windowWidth - panelWidth) / 2f;
        float panelY = gridY - panelPadding - titleOffset;

        renderer.drawRect(panelX, panelY, panelWidth, panelHeight, 0.08f, 0.08f, 0.1f, 0.92f);
        float borderThickness = Math.max(1.5f, scale(2f));
        renderer.drawRect(panelX, panelY, panelWidth, borderThickness, 0.3f, 0.3f, 0.4f, 1.0f);
        renderer.drawRect(panelX, panelY + panelHeight - borderThickness, panelWidth, borderThickness, 0.3f, 0.3f, 0.4f, 1.0f);

        String title = "Inventory";
        float titleScale = getUiScale();
        float titleWidth = fontRenderer.getTextWidth(title) * titleScale;
        fontRenderer.drawText(title,
            panelX + (panelWidth - titleWidth) / 2f,
            panelY + scale(28f),
            titleScale,
            0.95f, 0.95f, 0.95f, 1.0f);

        drawInventoryGrid(renderer, fontRenderer);
        drawSelectionDetails(renderer, fontRenderer, panelX, panelY + panelHeight - infoHeight, panelWidth, infoHeight);
        drawInstructions(fontRenderer, panelX, panelY + panelHeight - scale(26f), panelWidth);
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
        for (int row = 0; row < Inventory.HEIGHT; row++) {
            for (int column = 0; column < Inventory.WIDTH; column++) {
                float cellX = gridX + column * (slotSize + currentSlotSpacing);
                float cellY = gridY + row * (slotSize + currentSlotSpacing);

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
                        float labelScale = Math.min(1.0f, (slotSize - scale(6f)) / Math.max(fontRenderer.getTextWidth(label), 1f));
                        float labelWidth = fontRenderer.getTextWidth(label) * labelScale;
                        float labelX = cellX + (slotSize - labelWidth) / 2f;
                        float labelY = cellY + slotSize / 2f + fontRenderer.getTextHeight() * (labelScale - 1f) * 0.25f;
                        fontRenderer.drawText(label, labelX, labelY, labelScale, 0.92f, 0.92f, 0.95f, 1.0f);
                    }

                    int count = stack.getCount();
                    if (count > 0) {
                        String countText = formatCount(count);
                        float countScale = getUiScale() * 0.6f;
                        float countWidth = fontRenderer.getTextWidth(countText) * countScale;
                        float countX = cellX + slotSize - countWidth - scale(5f);
                        float countY = cellY + slotSize - scale(6f);
                        fontRenderer.drawText(countText, countX, countY, countScale, 1f, 1f, 1f, 1f);
                    }
                }
            }
        }
    }

    private void drawSelectionDetails(UIRenderer renderer, FontRenderer fontRenderer,
                                      float panelX, float infoY, float panelWidth, float infoHeight) {
        float innerPadding = scale(12f);
        renderer.drawRect(panelX + innerPadding, infoY + scale(10f),
            panelWidth - innerPadding * 2f,
            infoHeight - scale(20f),
            0.1f, 0.1f, 0.14f, 0.8f);

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

        float primaryScale = getUiScale();
        fontRenderer.drawText(labelText,
            panelX + scale(28f),
            infoY + scale(38f),
            primaryScale,
            0.94f, 0.94f, 0.94f, 1f);
        if (!countText.isEmpty()) {
            fontRenderer.drawText(countText,
                panelX + scale(28f),
                infoY + scale(38f) + fontRenderer.getTextHeight() * (primaryScale * 0.85f),
                primaryScale * 0.85f,
                0.8f, 0.8f, 0.85f, 0.9f);
        }
    }

    private void drawInstructions(FontRenderer fontRenderer, float panelX, float textY, float panelWidth) {
        String instructions = "Press E or Esc to close • Click a hotbar slot to equip";
        float scaleFactor = getUiScale() * 0.85f;
        float textWidth = fontRenderer.getTextWidth(instructions) * scaleFactor;
        float textX = panelX + (panelWidth - textWidth) / 2f;
        fontRenderer.drawText(instructions, textX, textY, scaleFactor, 0.75f, 0.75f, 0.78f, 1f);
    }

    private SlotPosition findSlotAt(float mouseX, float mouseY) {
        for (int row = 0; row < Inventory.HEIGHT; row++) {
            float cellY = gridY + row * (slotSize + currentSlotSpacing);
            if (mouseY < cellY || mouseY > cellY + slotSize) {
                continue;
            }
            for (int column = 0; column < Inventory.WIDTH; column++) {
                float cellX = gridX + column * (slotSize + currentSlotSpacing);
                if (mouseX >= cellX && mouseX <= cellX + slotSize) {
                    return new SlotPosition(row, column);
                }
            }
        }
        return null;
    }

    private void recalculateLayout() {
        float spacing = scale(SLOT_SPACING);
        this.currentSlotSpacing = spacing;

        float maxGridWidth = windowWidth * 0.82f;
        float maxGridHeight = windowHeight * 0.72f;

        float slotSizeByWidth = (maxGridWidth - (Inventory.WIDTH - 1) * spacing) / Inventory.WIDTH;
        float slotSizeByHeight = (maxGridHeight - (Inventory.HEIGHT - 1) * spacing) / Inventory.HEIGHT;
        this.slotSize = Math.max(0f, Math.min(slotSizeByWidth, slotSizeByHeight));

        this.gridWidth = Inventory.WIDTH * slotSize + (Inventory.WIDTH - 1) * spacing;
        this.gridHeight = Inventory.HEIGHT * slotSize + (Inventory.HEIGHT - 1) * spacing;
        this.gridX = (windowWidth - gridWidth) / 2f;
        this.gridY = (windowHeight - gridHeight) / 2f + scale(TITLE_OFFSET);
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
