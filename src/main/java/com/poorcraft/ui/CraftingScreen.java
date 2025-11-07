package com.poorcraft.ui;

import com.poorcraft.core.Game;
import com.poorcraft.core.PlayerController;
import com.poorcraft.crafting.CraftingManager;
import com.poorcraft.input.InputHandler;
import com.poorcraft.inventory.Inventory;
import com.poorcraft.inventory.ItemStack;
import com.poorcraft.render.BlockPreviewRenderer;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.entity.DropManager;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * Dedicated screen for managing the 2x2 crafting grid and crafting output.
 */
public class CraftingScreen extends UIScreen {

    private static final float OVERLAY_ALPHA = 0.55f;

    private final UIManager uiManager;

    private Game game;
    private Inventory inventory;
    private CraftingManager craftingManager;

    private float slotSize;
    private float gridX;
    private float gridY;
    private float gridWidth;
    private float gridHeight;

    private float craftingGridX;
    private float craftingGridY;
    private float craftingSlotSize;
    private float craftingOutputX;
    private float craftingOutputY;
    private float craftingGridWidth;
    private float craftingGridHeight;
    private float totalContentWidth;
    private float totalContentHeight;

    private int hoveredRow = -1;
    private int hoveredColumn = -1;
    private int hoveredCraftingSlot = -1;
    private int selectedRow;
    private int selectedColumn;
    private BlockPreviewRenderer blockPreviewRenderer;
    private ItemStack cursorStack;

    public CraftingScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.game = resolveGameReference();
        this.inventory = game != null ? game.getInventory() : null;
        this.craftingManager = game != null ? game.getCraftingManager() : null;
        this.selectedRow = Inventory.HEIGHT - 1;
        this.selectedColumn = 0;
    }

    @Override
    public void init() {
        this.game = resolveGameReference();
        this.inventory = game != null ? game.getInventory() : null;
        this.craftingManager = game != null ? game.getCraftingManager() : null;
        if (game != null) {
            selectedColumn = game.getSelectedHotbarSlot();
            selectedRow = Inventory.HEIGHT - 1;
        }
        cursorStack = null;
        hoveredCraftingSlot = -1;
        recalculateLayout();
        if (craftingManager != null) {
            craftingManager.updateCraftingResult();
        }
    }

    @Override
    public void onClose() {
        flushCraftingGrid(game);
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
        float contentWidth = totalContentWidth > 0f ? totalContentWidth : gridWidth;
        float contentHeight = totalContentHeight > 0f ? totalContentHeight : gridHeight;
        float panelWidth = contentWidth + panelPadding * 2f;
        float panelHeight = contentHeight + panelPadding * 2f + infoAreaHeight;
        float panelX = (windowWidth - panelWidth) / 2f;
        float panelY = gridY - panelPadding - titleOffset;

        renderer.drawRect(panelX, panelY, panelWidth, panelHeight, 0.08f, 0.08f, 0.1f, 0.92f);
        float borderSize = scaleDimension(2f);
        renderer.drawRect(panelX, panelY, panelWidth, borderSize, 0.3f, 0.3f, 0.4f, 1.0f);
        renderer.drawRect(panelX, panelY + panelHeight - borderSize, panelWidth, borderSize, 0.3f, 0.3f, 0.4f, 1.0f);

        String title = "Crafting";
        float textScale = getTextScale(fontRenderer);
        float titleWidth = fontRenderer.getTextWidth(title) * textScale;
        fontRenderer.drawText(title, panelX + (panelWidth - titleWidth) / 2f, panelY + scaleDimension(28f), textScale, 0.95f, 0.95f, 0.95f, 1.0f);

        drawInventoryGrid(renderer, fontRenderer);
        drawCraftingGrid(renderer, fontRenderer);
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
        hoveredCraftingSlot = findCraftingSlotAt(mouseX, mouseY);
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
            handleInventorySlotClick(position.row, position.column);
            return;
        }

        int craftingSlot = findCraftingSlotAt(mouseX, mouseY);
        if (craftingSlot >= 0 && craftingSlot <= 3) {
            handleCraftingSlotClick(craftingSlot);
        } else if (craftingSlot == 4) {
            handleCraftingOutputClick();
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
                    boolean renderedPreview = false;
                    if (blockPreviewRenderer != null && blockType != null && blockType != BlockType.AIR) {
                        float previewSize = slotSize * 0.7f;
                        float previewX = cellX + (slotSize - previewSize) / 2f;
                        float previewY = cellY + (slotSize - previewSize) / 2f - scaleDimension(3f);
                        blockPreviewRenderer.renderBlockPreview(blockType, previewX, previewY, previewSize, windowWidth, windowHeight);
                        renderedPreview = true;
                    }

                    if (!renderedPreview) {
                        String label = formatBlockLabel(blockType);
                        if (!label.isEmpty()) {
                            float baseText = getTextScale(fontRenderer);
                            float labelScale = Math.min(0.55f * baseText, (slotSize - scaleDimension(6f)) / Math.max(fontRenderer.getTextWidth(label), 1f));
                            float labelWidth = fontRenderer.getTextWidth(label) * labelScale;
                            float labelX = cellX + (slotSize - labelWidth) / 2f;
                            float labelY = cellY + slotSize / 2f + fontRenderer.getTextHeight() * (labelScale - baseText) * 0.5f;
                            fontRenderer.drawText(label, labelX, labelY, labelScale, 0.92f, 0.92f, 0.95f, 1.0f);
                        }
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
        int craftingKey = getCraftingKeyCode();
        String craftingKeyName = formatKeyName(craftingKey, "C");
        String instructions = "Press " + craftingKeyName + " or Esc to close • Shift-click output to auto-craft";
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

        this.craftingSlotSize = slotSize;
        float craftingSpacing = slotSpacing;
        this.craftingGridWidth = craftingSlotSize * 2f + craftingSpacing;
        this.craftingGridHeight = craftingSlotSize * 2f + craftingSpacing;
        float craftingOffset = scaleDimension(40f);
        float outputOffset = scaleDimension(48f);
        this.craftingGridX = gridX + gridWidth + craftingOffset;
        this.craftingGridY = gridY;
        this.craftingOutputX = craftingGridX + craftingGridWidth + outputOffset;
        this.craftingOutputY = craftingGridY + (craftingGridHeight - craftingSlotSize) / 2f;

        float totalWidth = (craftingOutputX + craftingSlotSize) - gridX;
        float desiredLeft = (windowWidth - totalWidth) / 2f;
        float offset = desiredLeft - gridX;
        this.gridX += offset;
        this.craftingGridX += offset;
        this.craftingOutputX += offset;
        this.totalContentWidth = totalWidth;
        this.totalContentHeight = Math.max(gridHeight, craftingGridHeight);
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

    private InputHandler resolveInputHandler() {
        if (uiManager == null) {
            return null;
        }
        try {
            var field = uiManager.getClass().getDeclaredField("inputHandler");
            field.setAccessible(true);
            Object value = field.get(uiManager);
            return (value instanceof InputHandler) ? (InputHandler) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isShiftDown() {
        InputHandler handler = resolveInputHandler();
        if (handler == null) {
            return false;
        }
        return handler.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)
            || handler.isKeyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void handleInventorySlotClick(int row, int column) {
        if (inventory == null) {
            return;
        }

        ItemStack slotStack = inventory.getSlot(row, column);
        if (cursorStack == null) {
            if (slotStack != null) {
                cursorStack = slotStack;
                inventory.setSlot(row, column, null);
            }
            return;
        }

        if (slotStack == null) {
            inventory.setSlot(row, column, cursorStack);
            cursorStack = null;
            return;
        }

        if (slotStack.getBlockType() == cursorStack.getBlockType() && slotStack.getCount() < ItemStack.MAX_STACK_SIZE) {
            int transfer = Math.min(cursorStack.getCount(), slotStack.remainingCapacity());
            if (transfer > 0) {
                slotStack.add(transfer);
                cursorStack.remove(transfer);
                if (cursorStack.isEmpty()) {
                    cursorStack = null;
                }
            }
        } else {
            inventory.setSlot(row, column, cursorStack);
            cursorStack = slotStack;
        }
    }

    private void handleCraftingSlotClick(int index) {
        if (craftingManager == null) {
            return;
        }

        ItemStack slotStack = craftingManager.getCraftingSlot(index);
        if (cursorStack == null) {
            if (slotStack != null) {
                cursorStack = slotStack;
                craftingManager.setCraftingSlot(index, null);
            }
            return;
        }

        if (slotStack == null) {
            craftingManager.setCraftingSlot(index, cursorStack);
            cursorStack = null;
            return;
        }

        if (slotStack.getBlockType() == cursorStack.getBlockType() && slotStack.getCount() < ItemStack.MAX_STACK_SIZE) {
            int transfer = Math.min(cursorStack.getCount(), slotStack.remainingCapacity());
            if (transfer > 0) {
                slotStack.add(transfer);
                cursorStack.remove(transfer);
                if (cursorStack.isEmpty()) {
                    cursorStack = null;
                }
                craftingManager.updateCraftingResult();
            }
        } else {
            craftingManager.setCraftingSlot(index, cursorStack);
            cursorStack = slotStack;
        }
    }

    private void handleCraftingOutputClick() {
        if (craftingManager == null || inventory == null) {
            return;
        }

        ItemStack result = craftingManager.getCraftingResult();
        if (result == null) {
            return;
        }

        boolean shift = isShiftDown();
        if (shift) {
            while (true) {
                ItemStack currentResult = craftingManager.getCraftingResult();
                if (currentResult == null) {
                    break;
                }
                int remaining = inventory.add(currentResult.getBlockType(), currentResult.getCount());
                if (remaining > 0) {
                    DropManager dropManager = game != null ? game.getDropManager() : null;
                    Vector3f position = getPlayerPosition(game);
                    if (dropManager != null && position != null) {
                        dropManager.spawn(currentResult.getBlockType(), position.x, position.y, position.z, remaining);
                    }
                }
                boolean crafted = craftingManager.craft();
                if (!crafted) {
                    break;
                }
            }
        } else {
            if (cursorStack == null) {
                cursorStack = result.copy();
                craftingManager.craft();
                return;
            }

            if (cursorStack.getBlockType() == result.getBlockType() && cursorStack.getCount() < ItemStack.MAX_STACK_SIZE) {
                int space = cursorStack.remainingCapacity();
                if (space > 0) {
                    int toCursor = Math.min(space, result.getCount());
                    int remainder = result.getCount() - toCursor;
                    boolean canStoreRemainder = remainder == 0 || canInventoryAccept(result.getBlockType(), remainder);
                    if (canStoreRemainder) {
                        cursorStack.add(toCursor);
                        if (remainder > 0) {
                            inventory.add(result.getBlockType(), remainder);
                        }
                        craftingManager.craft();
                        return;
                    }
                }
            }

            int remaining = inventory.add(result.getBlockType(), result.getCount());
            if (remaining > 0) {
                DropManager dropManager = game != null ? game.getDropManager() : null;
                Vector3f position = getPlayerPosition(game);
                if (dropManager != null && position != null) {
                    dropManager.spawn(result.getBlockType(), position.x, position.y, position.z, remaining);
                }
            }
            craftingManager.craft();
        }
    }

    private boolean canInventoryAccept(BlockType blockType, int amount) {
        if (inventory == null || blockType == null || amount <= 0) {
            return amount <= 0;
        }

        int remaining = amount;
        for (int index = 0; index < Inventory.SLOT_COUNT && remaining > 0; index++) {
            ItemStack slot = inventory.getSlot(index);
            if (slot == null) {
                remaining -= ItemStack.MAX_STACK_SIZE;
            } else if (slot.getBlockType() == blockType) {
                remaining -= slot.remainingCapacity();
            }
        }
        return remaining <= 0;
    }

    private int findCraftingSlotAt(float mouseX, float mouseY) {
        if (craftingManager == null) {
            return -1;
        }

        float slotSpacing = scaleDimension(4f);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                float cellX = craftingGridX + column * (craftingSlotSize + slotSpacing);
                float cellY = craftingGridY + row * (craftingSlotSize + slotSpacing);
                if (mouseX >= cellX && mouseX <= cellX + craftingSlotSize
                    && mouseY >= cellY && mouseY <= cellY + craftingSlotSize) {
                    return row * 2 + column;
                }
            }
        }

        float outputX = craftingOutputX;
        float outputY = craftingOutputY;
        if (mouseX >= outputX && mouseX <= outputX + craftingSlotSize
            && mouseY >= outputY && mouseY <= outputY + craftingSlotSize) {
            return 4;
        }
        return -1;
    }

    private void drawCraftingGrid(UIRenderer renderer, FontRenderer fontRenderer) {
        if (craftingManager == null) {
            return;
        }

        float slotSpacing = scaleDimension(4f);
        float labelScale = getTextScale(fontRenderer);
        String label = "Crafting";
        float labelWidth = fontRenderer.getTextWidth(label) * labelScale;
        fontRenderer.drawText(label, craftingGridX + (craftingGridWidth - labelWidth) / 2f,
            craftingGridY - scaleDimension(18f), labelScale, 0.9f, 0.92f, 0.95f, 1f);

        for (int index = 0; index < 4; index++) {
            int row = index / 2;
            int column = index % 2;
            float cellX = craftingGridX + column * (craftingSlotSize + slotSpacing);
            float cellY = craftingGridY + row * (craftingSlotSize + slotSpacing);

            boolean isHovered = hoveredCraftingSlot == index;

            float r = 0.17f;
            float g = 0.17f;
            float b = 0.22f;
            if (isHovered) {
                r += 0.08f;
                g += 0.08f;
                b += 0.08f;
            }
            renderer.drawRect(cellX, cellY, craftingSlotSize, craftingSlotSize, r, g, b, 0.88f);
            renderer.drawRect(cellX, cellY, craftingSlotSize, 1f, 0.05f, 0.05f, 0.06f, 0.95f);
            renderer.drawRect(cellX, cellY + craftingSlotSize - 1f, craftingSlotSize, 1f, 0.05f, 0.05f, 0.06f, 0.95f);
            renderer.drawRect(cellX, cellY, 1f, craftingSlotSize, 0.05f, 0.05f, 0.06f, 0.95f);
            renderer.drawRect(cellX + craftingSlotSize - 1f, cellY, 1f, craftingSlotSize, 0.05f, 0.05f, 0.06f, 0.95f);

            ItemStack stack = craftingManager.getCraftingSlot(index);
            if (stack != null && !stack.isEmpty()) {
                BlockType blockType = stack.getBlockType();
                boolean rendered = false;
                if (blockPreviewRenderer != null && blockType != null && blockType != BlockType.AIR) {
                    float previewSize = craftingSlotSize * 0.7f;
                    float previewX = cellX + (craftingSlotSize - previewSize) / 2f;
                    float previewY = cellY + (craftingSlotSize - previewSize) / 2f - scaleDimension(2f);
                    blockPreviewRenderer.renderBlockPreview(blockType, previewX, previewY, previewSize, windowWidth, windowHeight);
                    rendered = true;
                }
                if (!rendered) {
                    String blockLabel = formatBlockLabel(blockType);
                    if (!blockLabel.isEmpty()) {
                        float baseText = getTextScale(fontRenderer);
                        float textScale = Math.min(0.55f * baseText,
                            (craftingSlotSize - scaleDimension(6f)) / Math.max(fontRenderer.getTextWidth(blockLabel), 1f));
                        float textWidth = fontRenderer.getTextWidth(blockLabel) * textScale;
                        float textX = cellX + (craftingSlotSize - textWidth) / 2f;
                        float textY = cellY + craftingSlotSize / 2f + fontRenderer.getTextHeight() * (textScale - baseText) * 0.5f;
                        fontRenderer.drawText(blockLabel, textX, textY, textScale, 0.9f, 0.92f, 0.95f, 1f);
                    }
                }

                int count = stack.getCount();
                if (count > 0) {
                    String countText = formatCount(count);
                    float baseText = getTextScale(fontRenderer);
                    float countScale = 0.45f * baseText;
                    float countWidth = fontRenderer.getTextWidth(countText) * countScale;
                    float countX = cellX + craftingSlotSize - countWidth - scaleDimension(5f);
                    float countY = cellY + craftingSlotSize - scaleDimension(6f);
                    fontRenderer.drawText(countText, countX, countY, countScale, 1f, 1f, 1f, 1f);
                }
            }
        }

        float arrowX = craftingGridX + craftingGridWidth + scaleDimension(16f);
        float arrowY = craftingGridY + craftingSlotSize * 0.5f;
        fontRenderer.drawText("→", arrowX, arrowY, labelScale, 0.9f, 0.92f, 0.95f, 1f);

        boolean outputHovered = hoveredCraftingSlot == 4;
        float outR = outputHovered ? 0.42f : 0.32f;
        float outG = outputHovered ? 0.34f : 0.28f;
        float outB = outputHovered ? 0.18f : 0.14f;
        renderer.drawRect(craftingOutputX, craftingOutputY, craftingSlotSize, craftingSlotSize, outR, outG, outB, 0.92f);
        renderer.drawRect(craftingOutputX, craftingOutputY, craftingSlotSize, 1f, 0.05f, 0.05f, 0.06f, 0.95f);
        renderer.drawRect(craftingOutputX, craftingOutputY + craftingSlotSize - 1f, craftingSlotSize, 1f, 0.05f, 0.05f, 0.06f, 0.95f);
        renderer.drawRect(craftingOutputX, craftingOutputY, 1f, craftingSlotSize, 0.05f, 0.05f, 0.06f, 0.95f);
        renderer.drawRect(craftingOutputX + craftingSlotSize - 1f, craftingOutputY, 1f, craftingSlotSize, 0.05f, 0.05f, 0.06f, 0.95f);

        ItemStack result = craftingManager.getCraftingResult();
        if (result != null && !result.isEmpty()) {
            BlockType blockType = result.getBlockType();
            boolean rendered = false;
            if (blockPreviewRenderer != null && blockType != null && blockType != BlockType.AIR) {
                float previewSize = craftingSlotSize * 0.75f;
                float previewX = craftingOutputX + (craftingSlotSize - previewSize) / 2f;
                float previewY = craftingOutputY + (craftingSlotSize - previewSize) / 2f - scaleDimension(2f);
                blockPreviewRenderer.renderBlockPreview(blockType, previewX, previewY, previewSize, windowWidth, windowHeight);
                rendered = true;
            }
            if (!rendered) {
                String blockLabel = formatBlockLabel(blockType);
                if (!blockLabel.isEmpty()) {
                    float baseText = getTextScale(fontRenderer);
                    float textScale = Math.min(0.6f * baseText,
                        (craftingSlotSize - scaleDimension(6f)) / Math.max(fontRenderer.getTextWidth(blockLabel), 1f));
                    float textWidth = fontRenderer.getTextWidth(blockLabel) * textScale;
                    float textX = craftingOutputX + (craftingSlotSize - textWidth) / 2f;
                    float textY = craftingOutputY + craftingSlotSize / 2f + fontRenderer.getTextHeight() * (textScale - baseText) * 0.5f;
                    fontRenderer.drawText(blockLabel, textX, textY, textScale, 0.95f, 0.95f, 0.95f, 1f);
                }
            }

            int count = result.getCount();
            if (count > 0) {
                String countText = formatCount(count);
                float baseText = getTextScale(fontRenderer);
                float countScale = 0.48f * baseText;
                float countWidth = fontRenderer.getTextWidth(countText) * countScale;
                float countX = craftingOutputX + craftingSlotSize - countWidth - scaleDimension(6f);
                float countY = craftingOutputY + craftingSlotSize - scaleDimension(6f);
                fontRenderer.drawText(countText, countX, countY, countScale, 1f, 1f, 1f, 1f);
            }
        }
    }

    private int getCraftingKeyCode() {
        if (uiManager == null || uiManager.getSettings() == null || uiManager.getSettings().controls == null) {
            return GLFW.GLFW_KEY_C;
        }
        return uiManager.getSettings().controls.getKeybind("crafting", GLFW.GLFW_KEY_C);
    }

    private String formatKeyName(int keyCode, String fallback) {
        String name = GLFW.glfwGetKeyName(keyCode, GLFW.glfwGetKeyScancode(keyCode));
        if (name == null || name.isBlank()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE:
                    return "Esc";
                case GLFW.GLFW_KEY_ENTER:
                    return "Enter";
                case GLFW.GLFW_KEY_TAB:
                    return "Tab";
                case GLFW.GLFW_KEY_LEFT_SHIFT:
                case GLFW.GLFW_KEY_RIGHT_SHIFT:
                    return "Shift";
                default:
                    return fallback;
            }
        }
        return name.toUpperCase();
    }

    private static Vector3f getPlayerPosition(Game game) {
        if (game == null) {
            return null;
        }
        PlayerController controller = game.getPlayerController();
        return controller != null ? controller.getPosition() : null;
    }

    public void setBlockPreviewRenderer(BlockPreviewRenderer renderer) {
        this.blockPreviewRenderer = renderer;
    }

    private static final class SlotPosition {
        final int row;
        final int column;

        SlotPosition(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    public static void flushCraftingGrid(Game game) {
        if (game == null) {
            return;
        }
        CraftingManager manager = game.getCraftingManager();
        if (manager == null) {
            return;
        }
        Inventory inventory = game.getInventory();
        DropManager dropManager = game.getDropManager();
        Vector3f position = getPlayerPosition(game);

        boolean updated = false;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = manager.getCraftingSlot(i);
            if (stack == null || stack.isEmpty()) {
                manager.setCraftingSlot(i, null);
                continue;
            }

            int leftover = stack.getCount();
            if (inventory != null) {
                leftover = inventory.add(stack.getBlockType(), stack.getCount());
            }

            if (leftover > 0 && dropManager != null) {
                Vector3f dropPosition = position != null ? position : new Vector3f(0f, 72f, 0f);
                dropManager.spawn(stack.getBlockType(), dropPosition.x, dropPosition.y, dropPosition.z, leftover);
                leftover = 0;
            }

            if (leftover > 0) {
                manager.setCraftingSlot(i, new ItemStack(stack.getBlockType(), leftover));
            } else {
                manager.setCraftingSlot(i, null);
            }
            updated = true;
        }

        if (updated) {
            manager.updateCraftingResult();
        }
    }
}
