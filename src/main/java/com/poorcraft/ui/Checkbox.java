package com.poorcraft.ui;

import java.util.function.Consumer;

/**
 * Checkbox UI component for boolean settings.
 * 
 * A clickable box with a checkmark when checked.
 * Used for on/off settings like vsync, fullscreen, generate structures, etc.
 * 
 * Drawing the checkmark was harder than I thought. I ended up just drawing
 * two lines in an X shape. Good enough for government work.
 */
public class Checkbox extends UIComponent {
    
    private static final float[] BOX_COLOR = {0.2f, 0.2f, 0.2f, 0.8f};
    private static final float[] BOX_HOVER_COLOR = {0.3f, 0.3f, 0.3f, 0.9f};
    private static final float[] CHECK_COLOR = {0.8f, 0.8f, 0.8f, 1.0f};
    private static final float[] TEXT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    
    private boolean checked;
    private String label;
    private Consumer<Boolean> onChange;
    private float boxSize;
    
    /**
     * Creates a new checkbox.
     * 
     * @param x X position
     * @param y Y position
     * @param size Checkbox box size
     * @param label Checkbox label
     * @param initialState Initial checked state
     * @param onChange Callback when state changes
     */
    public Checkbox(float x, float y, float size, String label, boolean initialState, Consumer<Boolean> onChange) {
        super(x, y, size, size);
        this.boxSize = size;
        this.label = label;
        this.checked = initialState;
        this.onChange = onChange;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) return;
        
        // Draw checkbox box
        float[] boxColor = hovered ? BOX_HOVER_COLOR : BOX_COLOR;
        renderer.drawRect(x, y, boxSize, boxSize, boxColor[0], boxColor[1], boxColor[2], boxColor[3]);
        
        // Draw border
        float borderWidth = 2;
        float borderBrightness = 0.2f;
        renderer.drawRect(x, y, boxSize, borderWidth, 
            boxColor[0] + borderBrightness, boxColor[1] + borderBrightness, 
            boxColor[2] + borderBrightness, boxColor[3]);
        renderer.drawRect(x, y + boxSize - borderWidth, boxSize, borderWidth, 
            boxColor[0] + borderBrightness, boxColor[1] + borderBrightness, 
            boxColor[2] + borderBrightness, boxColor[3]);
        renderer.drawRect(x, y, borderWidth, boxSize, 
            boxColor[0] + borderBrightness, boxColor[1] + borderBrightness, 
            boxColor[2] + borderBrightness, boxColor[3]);
        renderer.drawRect(x + boxSize - borderWidth, y, borderWidth, boxSize, 
            boxColor[0] + borderBrightness, boxColor[1] + borderBrightness, 
            boxColor[2] + borderBrightness, boxColor[3]);
        
        // Draw checkmark if checked
        if (checked) {
            float padding = boxSize * 0.2f;
            float checkWidth = 3;
            
            // Draw X shape (two diagonal lines)
            // Line 1: top-left to bottom-right
            float x1 = x + padding;
            float y1 = y + padding;
            float x2 = x + boxSize - padding;
            float y2 = y + boxSize - padding;
            drawLine(renderer, x1, y1, x2, y2, checkWidth, CHECK_COLOR);
            
            // Line 2: top-right to bottom-left
            float x3 = x + boxSize - padding;
            float y3 = y + padding;
            float x4 = x + padding;
            float y4 = y + boxSize - padding;
            drawLine(renderer, x3, y3, x4, y4, checkWidth, CHECK_COLOR);
        }
        
        // Draw label
        if (label != null && !label.isEmpty()) {
            float labelX = x + boxSize + 10;
            float labelY = y + boxSize / 2 + fontRenderer.getTextHeight() * 0.3f;
            fontRenderer.drawText(label, labelX, labelY, TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);
        }
    }
    
    /**
     * Draws a line between two points.
     * This is a simple approximation using a rotated rectangle.
     */
    private void drawLine(UIRenderer renderer, float x1, float y1, float x2, float y2, float width, float[] color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        
        // For simplicity, just draw a thick line as a series of small rectangles
        // Not the most efficient, but good enough for a checkbox
        int steps = (int) length;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float x = x1 + dx * t;
            float y = y1 + dy * t;
            renderer.drawRect(x - width / 2, y - width / 2, width, width, 
                color[0], color[1], color[2], color[3]);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // No animation needed
    }
    
    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            toggle();
        }
    }
    
    /**
     * Toggles the checkbox state.
     */
    public void toggle() {
        checked = !checked;
        if (onChange != null) {
            onChange.accept(checked);
        }
    }
    
    /**
     * Gets the checked state.
     * 
     * @return True if checked
     */
    public boolean isChecked() {
        return checked;
    }
    
    /**
     * Sets the checked state.
     * 
     * @param checked New checked state
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
