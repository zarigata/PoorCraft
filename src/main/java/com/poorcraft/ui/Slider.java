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
    private float labelScale = 1.0f;
    private float valueScale = 1.0f;
    
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
        
        float baseTextHeight = Math.max(1f, fontRenderer.getTextHeight());
        float scaledLabelHeight = baseTextHeight * labelScale;
        float labelBaseline = y + scaledLabelHeight;
        fontRenderer.drawText(label, x, labelBaseline, labelScale,
            TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);
        
        float trackX = x;
        float trackWidth = Math.max(width - 120f, 140f);
        float availableHeight = Math.max(height - scaledLabelHeight - 12f, HANDLE_HEIGHT);
        float handleHeight = Math.max(availableHeight, HANDLE_HEIGHT);
        float handleWidth = Math.max(handleHeight * 0.28f, HANDLE_WIDTH);
        float trackHeight = Math.max(handleHeight * 0.18f, TRACK_HEIGHT);
        float trackY = labelBaseline + 12f;
        float trackCenterY = trackY + (handleHeight - trackHeight) / 2f;
        
        renderer.drawRect(trackX, trackCenterY, trackWidth, trackHeight,
            TRACK_COLOR[0], TRACK_COLOR[1], TRACK_COLOR[2], TRACK_COLOR[3]);
        
        float handleX = trackX + (trackWidth - handleWidth) * value;
        float handleY = trackY;
        float[] handleColor = (hovered || dragging) ? HANDLE_HOVER_COLOR : HANDLE_COLOR;
        renderer.drawRect(handleX, handleY, handleWidth, handleHeight,
            handleColor[0], handleColor[1], handleColor[2], handleColor[3]);
        
        float actualValue = minValue + value * (maxValue - minValue);
        String valueText = formatValue(actualValue);
        float valueX = trackX + trackWidth + 16f;
        float valueBaseline = trackY + handleHeight * 0.65f;
        fontRenderer.drawText(valueText, valueX, valueBaseline, valueScale,
            TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);
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
    
    private void updateValueFromMouse(float mouseX) {
        float trackX = x;
        float trackWidth = Math.max(width - 120f, 140f);
        
        float newValue = (mouseX - trackX) / trackWidth;
        newValue = Math.max(0.0f, Math.min(1.0f, newValue));
        
        if (newValue != value) {
            value = newValue;
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
    
    /**
     * Sets the font scale for label and value text.
     * 
     * @param labelScale Scale for the label text
     * @param valueScale Scale for the value text
     */
    public void setFontScale(float labelScale, float valueScale) {
        this.labelScale = Math.max(0.6f, labelScale);
        this.valueScale = Math.max(0.6f, valueScale);
    }
}
