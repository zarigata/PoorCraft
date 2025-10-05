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
    
    // Improved color constants with better contrast and depth
    private static final float[] NORMAL_TOP = {0.25f, 0.25f, 0.25f, 0.9f};
    private static final float[] NORMAL_BOTTOM = {0.15f, 0.15f, 0.15f, 0.9f};
    private static final float[] HOVER_TOP = {0.35f, 0.35f, 0.38f, 0.95f};
    private static final float[] HOVER_BOTTOM = {0.22f, 0.22f, 0.25f, 0.95f};
    private static final float[] PRESSED_TOP = {0.12f, 0.12f, 0.12f, 0.95f};
    private static final float[] PRESSED_BOTTOM = {0.18f, 0.18f, 0.18f, 0.95f};
    private static final float[] DISABLED_COLOR = {0.1f, 0.1f, 0.1f, 0.5f};
    private static final float[] TEXT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] DISABLED_TEXT_COLOR = {0.5f, 0.5f, 0.5f, 0.5f};
    private static final float[] TEXT_SHADOW_COLOR = {0.0f, 0.0f, 0.0f, 0.6f};
    
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
        
        // Choose colors based on state with gradient
        float[] topColor;
        float[] bottomColor;
        float[] textColor;
        
        if (!enabled) {
            topColor = DISABLED_COLOR;
            bottomColor = DISABLED_COLOR;
            textColor = DISABLED_TEXT_COLOR;
        } else if (pressed) {
            topColor = PRESSED_TOP;
            bottomColor = PRESSED_BOTTOM;
            textColor = TEXT_COLOR;
        } else if (hovered) {
            // Interpolate between normal and hover colors
            topColor = new float[4];
            bottomColor = new float[4];
            for (int i = 0; i < 4; i++) {
                topColor[i] = NORMAL_TOP[i] + (HOVER_TOP[i] - NORMAL_TOP[i]) * hoverAlpha;
                bottomColor[i] = NORMAL_BOTTOM[i] + (HOVER_BOTTOM[i] - NORMAL_BOTTOM[i]) * hoverAlpha;
            }
            textColor = TEXT_COLOR;
        } else {
            topColor = NORMAL_TOP;
            bottomColor = NORMAL_BOTTOM;
            textColor = TEXT_COLOR;
        }
        
        // Draw gradient background (top half lighter, bottom half darker)
        renderer.drawRect(x, y, width, height / 2, 
            topColor[0], topColor[1], topColor[2], topColor[3]);
        renderer.drawRect(x, y + height / 2, width, height / 2, 
            bottomColor[0], bottomColor[1], bottomColor[2], bottomColor[3]);
        
        // Draw 3D borders
        float borderWidth = 2.0f;
        float highlightMul = pressed ? 0.5f : 1.0f;
        float shadowMul = pressed ? 1.3f : 1.0f;
        
        // Top border (highlight)
        renderer.drawRect(x, y, width, borderWidth, 
            topColor[0] + 0.15f * highlightMul, topColor[1] + 0.15f * highlightMul, 
            topColor[2] + 0.15f * highlightMul, topColor[3]);
        // Left border (highlight)
        renderer.drawRect(x, y, borderWidth, height, 
            topColor[0] + 0.12f * highlightMul, topColor[1] + 0.12f * highlightMul, 
            topColor[2] + 0.12f * highlightMul, topColor[3]);
        // Bottom border (shadow)
        renderer.drawRect(x, y + height - borderWidth, width, borderWidth, 
            bottomColor[0] - 0.08f * shadowMul, bottomColor[1] - 0.08f * shadowMul, 
            bottomColor[2] - 0.08f * shadowMul, bottomColor[3]);
        // Right border (shadow)
        renderer.drawRect(x + width - borderWidth, y, borderWidth, height, 
            bottomColor[0] - 0.08f * shadowMul, bottomColor[1] - 0.08f * shadowMul, 
            bottomColor[2] - 0.08f * shadowMul, bottomColor[3]);
        
        // Draw text (centered) with shadow
        if (text != null && !text.isEmpty()) {
            float textWidth = fontRenderer.getTextWidth(text);
            float textHeight = fontRenderer.getTextHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height - textHeight) / 2 + textHeight * 0.7f; // Adjust for baseline
            
            // Draw shadow
            fontRenderer.drawText(text, textX + 1.5f, textY + 1.5f, 
                TEXT_SHADOW_COLOR[0], TEXT_SHADOW_COLOR[1], TEXT_SHADOW_COLOR[2], TEXT_SHADOW_COLOR[3]);
            
            // Draw main text
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
