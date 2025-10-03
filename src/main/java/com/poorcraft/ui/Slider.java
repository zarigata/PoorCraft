package com.poorcraft.ui;

import java.util.function.Consumer;

/**
 * Slider UI component for numeric values.
 * 
 * A draggable slider with a track and handle.
 * Useful for settings like volume, FOV, render distance, etc.
 * 
 * The math here is simple but I still managed to mess it up the first time.
 * Remember: normalize to 0-1, then map to min-max range. Not the other way around.
 * I don't know why I keep forgetting this.
 */
public class Slider extends UIComponent {
    
    private static final float[] TRACK_COLOR = {0.2f, 0.2f, 0.2f, 0.8f};
    private static final float[] HANDLE_COLOR = {0.5f, 0.5f, 0.5f, 1.0f};
    private static final float[] HANDLE_HOVER_COLOR = {0.6f, 0.6f, 0.6f, 1.0f};
    private static final float[] TEXT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    
    private static final float HANDLE_WIDTH = 12;
    private static final float HANDLE_HEIGHT = 20;
    private static final float TRACK_HEIGHT = 4;
    
    private String label;
    private float value;        // Normalized value (0.0 to 1.0)
    private float minValue;
    private float maxValue;
    private boolean dragging;
    private Consumer<Float> onChange;
    private int decimalPlaces;
    
    /**
     * Creates a new slider.
     * 
     * @param x X position
     * @param y Y position
     * @param width Slider width
     * @param height Slider height
     * @param label Slider label
     * @param minValue Minimum value
     * @param maxValue Maximum value
     * @param initialValue Initial value
     * @param onChange Callback when value changes
     */
    public Slider(float x, float y, float width, float height, String label, 
                  float minValue, float maxValue, float initialValue, Consumer<Float> onChange) {
        super(x, y, width, height);
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.onChange = onChange;
        this.dragging = false;
        this.decimalPlaces = 2;
        
        // Normalize initial value to 0-1 range
        setValue(initialValue);
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) return;
        
        // Draw label
        float labelY = y;
        fontRenderer.drawText(label, x, labelY, TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);
        
        // Calculate track position (below label)
        float trackY = y + fontRenderer.getTextHeight() + 8;
        float trackX = x;
        float trackWidth = width - 60; // Leave space for value display
        
        // Draw track
        float trackCenterY = trackY + (HANDLE_HEIGHT - TRACK_HEIGHT) / 2;
        renderer.drawRect(trackX, trackCenterY, trackWidth, TRACK_HEIGHT, 
            TRACK_COLOR[0], TRACK_COLOR[1], TRACK_COLOR[2], TRACK_COLOR[3]);
        
        // Calculate handle position
        float handleX = trackX + (trackWidth - HANDLE_WIDTH) * value;
        float handleY = trackY;
        
        // Draw handle
        float[] handleColor = (hovered || dragging) ? HANDLE_HOVER_COLOR : HANDLE_COLOR;
        renderer.drawRect(handleX, handleY, HANDLE_WIDTH, HANDLE_HEIGHT, 
            handleColor[0], handleColor[1], handleColor[2], handleColor[3]);
        
        // Draw current value
        float actualValue = minValue + value * (maxValue - minValue);
        String valueText = formatValue(actualValue);
        float valueX = trackX + trackWidth + 10;
        float valueY = trackY + HANDLE_HEIGHT / 2 + fontRenderer.getTextHeight() * 0.3f;
        fontRenderer.drawText(valueText, valueX, valueY, TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);
    }
    
    @Override
    public void update(float deltaTime) {
        // No animation needed
    }
    
    @Override
    public void onMouseMove(float mouseX, float mouseY) {
        super.onMouseMove(mouseX, mouseY);
        
        if (dragging) {
            updateValueFromMouse(mouseX);
        }
    }
    
    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            dragging = true;
            updateValueFromMouse(mouseX);
        }
    }
    
    @Override
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }
    
    /**
     * Updates the slider value based on mouse X position.
     */
    private void updateValueFromMouse(float mouseX) {
        float trackX = x;
        float trackWidth = width - 60;
        
        // Calculate normalized value from mouse position
        float newValue = (mouseX - trackX) / trackWidth;
        newValue = Math.max(0.0f, Math.min(1.0f, newValue)); // Clamp to 0-1
        
        if (newValue != value) {
            value = newValue;
            
            // Call onChange callback with actual value
            if (onChange != null) {
                float actualValue = minValue + value * (maxValue - minValue);
                onChange.accept(actualValue);
            }
        }
    }
    
    /**
     * Formats a value for display.
     */
    private String formatValue(float value) {
        if (decimalPlaces == 0) {
            return String.valueOf((int) value);
        } else {
            return String.format("%." + decimalPlaces + "f", value);
        }
    }
    
    /**
     * Gets the current value (mapped to min-max range).
     * 
     * @return Current value
     */
    public float getValue() {
        return minValue + value * (maxValue - minValue);
    }
    
    /**
     * Sets the value (will be clamped to min-max range).
     * 
     * @param value New value
     */
    public void setValue(float value) {
        // Normalize to 0-1 range
        this.value = (value - minValue) / (maxValue - minValue);
        this.value = Math.max(0.0f, Math.min(1.0f, this.value));
    }
    
    /**
     * Sets the number of decimal places to display.
     * 
     * @param decimalPlaces Number of decimal places (0 for integers)
     */
    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }
}
