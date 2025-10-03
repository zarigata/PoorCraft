package com.poorcraft.ui;

/**
 * Vaporwave-styled button with gradient effects.
 * 
 * This button brings that sweet 80s aesthetic to PoorCraft.
 * Features smooth gradient backgrounds, animated hover effects, and that
 * cyberpunk vibe that makes everything look cooler.
 * 
 * I spent way too much time getting these colors just right, but honestly?
 * Worth it. This menu is gonna look sick.
 */
public class VaporwaveButton extends UIComponent {
    
    // Vaporwave color palette - these colors are *chef's kiss*
    // Pink/Purple gradient for normal state
    private static final float[] COLOR_1_NORMAL = {0.98f, 0.26f, 0.63f, 0.9f}; // Hot pink
    private static final float[] COLOR_2_NORMAL = {0.4f, 0.22f, 0.72f, 0.9f};  // Deep purple
    
    // Cyan/Blue gradient for hover state
    private static final float[] COLOR_1_HOVER = {0.0f, 0.95f, 0.95f, 0.95f};  // Electric cyan
    private static final float[] COLOR_2_HOVER = {0.29f, 0.0f, 0.51f, 0.95f};  // Indigo
    
    // Disabled state - desaturated
    private static final float[] COLOR_1_DISABLED = {0.3f, 0.3f, 0.3f, 0.5f};
    private static final float[] COLOR_2_DISABLED = {0.2f, 0.2f, 0.2f, 0.5f};
    
    // Border glow colors
    private static final float[] BORDER_NORMAL = {0.98f, 0.26f, 0.63f, 0.8f};
    private static final float[] BORDER_HOVER = {0.0f, 0.95f, 0.95f, 1.0f};
    private static final float[] BORDER_DISABLED = {0.4f, 0.4f, 0.4f, 0.4f};
    
    private static final float[] TEXT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] TEXT_DISABLED = {0.5f, 0.5f, 0.5f, 0.5f};
    
    private String text;
    private Runnable onClick;
    private boolean pressed;
    private float hoverAlpha;  // Smooth transition between normal and hover
    private float pulseTimer;  // For subtle pulsing animation
    
    /**
     * Creates a new Vaporwave button.
     * 
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param height Button height
     * @param text Button label text
     * @param onClick Callback when button is clicked
     */
    public VaporwaveButton(float x, float y, float width, float height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
        this.pressed = false;
        this.hoverAlpha = 0.0f;
        this.pulseTimer = 0.0f;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) return;
        
        // Choose colors based on state
        float[] color1, color2, borderColor;
        float[] textColor;
        
        if (!enabled) {
            color1 = COLOR_1_DISABLED;
            color2 = COLOR_2_DISABLED;
            borderColor = BORDER_DISABLED;
            textColor = TEXT_DISABLED;
        } else {
            // Interpolate between normal and hover colors
            color1 = interpolateColor(COLOR_1_NORMAL, COLOR_1_HOVER, hoverAlpha);
            color2 = interpolateColor(COLOR_2_NORMAL, COLOR_2_HOVER, hoverAlpha);
            borderColor = interpolateColor(BORDER_NORMAL, BORDER_HOVER, hoverAlpha);
            textColor = TEXT_COLOR;
            
            // Add subtle pulse effect when hovering
            if (hoverAlpha > 0.5f) {
                float pulse = (float) Math.sin(pulseTimer * 3.0) * 0.1f + 0.9f;
                borderColor[3] *= pulse;
            }
        }
        
        // Draw gradient background (simulate with multiple rectangles)
        // This ain't perfect but it looks decent enough for our vaporwave vibe
        int gradientSteps = 20;
        float stepHeight = height / gradientSteps;
        
        for (int i = 0; i < gradientSteps; i++) {
            float t = i / (float) gradientSteps;
            float[] stepColor = interpolateColor(color1, color2, t);
            
            renderer.drawRect(x, y + i * stepHeight, width, stepHeight + 1, // +1 to avoid gaps
                stepColor[0], stepColor[1], stepColor[2], stepColor[3]);
        }
        
        // Draw outer glow border (multiple passes for blur effect)
        float glowWidth = pressed ? 2.0f : (3.0f + hoverAlpha * 2.0f);
        float glowAlpha = borderColor[3];
        
        // Outer glow layers
        for (int i = 0; i < 3; i++) {
            float layerAlpha = glowAlpha * (0.3f - i * 0.1f);
            float offset = i * glowWidth;
            
            // Top
            renderer.drawRect(x - offset, y - offset, width + offset * 2, glowWidth,
                borderColor[0], borderColor[1], borderColor[2], layerAlpha);
            // Bottom
            renderer.drawRect(x - offset, y + height - glowWidth + offset, width + offset * 2, glowWidth,
                borderColor[0], borderColor[1], borderColor[2], layerAlpha);
            // Left
            renderer.drawRect(x - offset, y - offset, glowWidth, height + offset * 2,
                borderColor[0], borderColor[1], borderColor[2], layerAlpha);
            // Right
            renderer.drawRect(x + width - glowWidth + offset, y - offset, glowWidth, height + offset * 2,
                borderColor[0], borderColor[1], borderColor[2], layerAlpha);
        }
        
        // Draw solid border
        float borderWidth = 2.0f;
        // Top
        renderer.drawRect(x, y, width, borderWidth,
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        // Bottom
        renderer.drawRect(x, y + height - borderWidth, width, borderWidth,
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        // Left
        renderer.drawRect(x, y, borderWidth, height,
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        // Right
        renderer.drawRect(x + width - borderWidth, y, borderWidth, height,
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        
        // Draw text (centered)
        if (text != null && !text.isEmpty()) {
            float textWidth = fontRenderer.getTextWidth(text);
            float textHeight = fontRenderer.getTextHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height - textHeight) / 2 + textHeight * 0.7f;
            
            // Draw text shadow for better readability
            fontRenderer.drawText(text, textX + 2, textY + 2, 0.0f, 0.0f, 0.0f, 0.5f);
            
            // Draw main text
            fontRenderer.drawText(text, textX, textY,
                textColor[0], textColor[1], textColor[2], textColor[3]);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // Animate hover effect smoothly
        float targetAlpha = hovered ? 1.0f : 0.0f;
        float speed = 8.0f; // Smooth but responsive
        hoverAlpha += (targetAlpha - hoverAlpha) * deltaTime * speed;
        
        // Clamp to prevent overshoot
        if (Math.abs(hoverAlpha - targetAlpha) < 0.01f) {
            hoverAlpha = targetAlpha;
        }
        
        // Update pulse timer
        pulseTimer += deltaTime;
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
     * Interpolates between two colors.
     * 
     * @param color1 First color (RGBA)
     * @param color2 Second color (RGBA)
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated color
     */
    private float[] interpolateColor(float[] color1, float[] color2, float t) {
        float[] result = new float[4];
        for (int i = 0; i < 4; i++) {
            result[i] = color1[i] + (color2[i] - color1[i]) * t;
        }
        return result;
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
