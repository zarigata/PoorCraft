package com.poorcraft.ui;

/**
 * Button UI component.
 * 
 * A clickable button with text label, hover effects, and click callbacks.
 * Supports disabled state with visual feedback.
 * 
 * The hover animation is smooth and satisfying. I spent way too much time
 * tweaking the interpolation speed. 10.0f feels just right. Trust me.
 */
public class Button extends UIComponent {
    
    // Color constants
    private static final float[] NORMAL_COLOR = {0.2f, 0.2f, 0.2f, 0.8f};
    private static final float[] HOVER_COLOR = {0.3f, 0.3f, 0.3f, 0.9f};
    private static final float[] PRESSED_COLOR = {0.15f, 0.15f, 0.15f, 0.9f};
    private static final float[] DISABLED_COLOR = {0.1f, 0.1f, 0.1f, 0.5f};
    private static final float[] TEXT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] DISABLED_TEXT_COLOR = {0.5f, 0.5f, 0.5f, 0.5f};
    
    private String text;
    private Runnable onClick;
    private boolean pressed;
    private float hoverAlpha;
    
    /**
     * Creates a new button.
     * 
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param height Button height
     * @param text Button label text
     * @param onClick Callback when button is clicked
     */
    public Button(float x, float y, float width, float height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
        this.pressed = false;
        this.hoverAlpha = 0.0f;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) return;
        
        // Choose color based on state
        float[] bgColor;
        float[] textColor;
        
        if (!enabled) {
            bgColor = DISABLED_COLOR;
            textColor = DISABLED_TEXT_COLOR;
        } else if (pressed) {
            bgColor = PRESSED_COLOR;
            textColor = TEXT_COLOR;
        } else if (hovered) {
            // Interpolate between normal and hover color
            bgColor = new float[4];
            for (int i = 0; i < 4; i++) {
                bgColor[i] = NORMAL_COLOR[i] + (HOVER_COLOR[i] - NORMAL_COLOR[i]) * hoverAlpha;
            }
            textColor = TEXT_COLOR;
        } else {
            bgColor = NORMAL_COLOR;
            textColor = TEXT_COLOR;
        }
        
        // Draw background
        renderer.drawRect(x, y, width, height, bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
        
        // Draw border (slightly lighter)
        float borderBrightness = 0.1f;
        renderer.drawRect(x, y, width, 2, 
            bgColor[0] + borderBrightness, bgColor[1] + borderBrightness, 
            bgColor[2] + borderBrightness, bgColor[3]);
        renderer.drawRect(x, y + height - 2, width, 2, 
            bgColor[0] + borderBrightness, bgColor[1] + borderBrightness, 
            bgColor[2] + borderBrightness, bgColor[3]);
        renderer.drawRect(x, y, 2, height, 
            bgColor[0] + borderBrightness, bgColor[1] + borderBrightness, 
            bgColor[2] + borderBrightness, bgColor[3]);
        renderer.drawRect(x + width - 2, y, 2, height, 
            bgColor[0] + borderBrightness, bgColor[1] + borderBrightness, 
            bgColor[2] + borderBrightness, bgColor[3]);
        
        // Draw text (centered)
        if (text != null && !text.isEmpty()) {
            float textWidth = fontRenderer.getTextWidth(text);
            float textHeight = fontRenderer.getTextHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height - textHeight) / 2 + textHeight * 0.7f; // Adjust for baseline
            
            fontRenderer.drawText(text, textX, textY, 
                textColor[0], textColor[1], textColor[2], textColor[3]);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // Animate hover effect
        float targetAlpha = hovered ? 1.0f : 0.0f;
        hoverAlpha += (targetAlpha - hoverAlpha) * deltaTime * 10.0f;
        
        // Clamp to prevent overshoot
        if (Math.abs(hoverAlpha - targetAlpha) < 0.01f) {
            hoverAlpha = targetAlpha;
        }
    }
    
    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (enabled && isMouseOver(mouseX, mouseY) && button == 0) {
            pressed = true;
        }
    }
    
    @Override
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        if (pressed && button == 0) {
            if (isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.run();
            }
            pressed = false;
        }
    }
    
    /**
     * Sets the button text.
     * 
     * @param text New button text
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Gets the button text.
     * 
     * @return Button text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the click callback.
     * 
     * @param onClick New click callback
     */
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }
}
