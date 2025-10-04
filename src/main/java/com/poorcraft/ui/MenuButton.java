package com.poorcraft.ui;

/**
 * Simple, reliable menu button with a classic stone background.
 * 
 * Inspired by the vintage Minecraft menus: muted greys with a subtle highlight
 * that brightens on hover and darkens when pressed. No gradients, no surprises –
 * just a clean rectangle that is easy to see and interact with at any scale.
 */
public class MenuButton extends UIComponent {
    
    // Classic stone palette – neutral base with subtle hover bloom
    private static final float[] BG_NORMAL = {0.36f, 0.36f, 0.36f, 0.96f};      // Stone grey
    private static final float[] BG_HOVER = {0.46f, 0.46f, 0.46f, 0.98f};       // Lightened
    private static final float[] BG_PRESSED = {0.26f, 0.26f, 0.26f, 0.98f};     // Darkened
    private static final float[] BG_DISABLED = {0.18f, 0.18f, 0.18f, 0.6f};     // Dimmed
    
    // Borders pick up the classic bevel highlight
    private static final float[] BORDER_NORMAL = {0.82f, 0.82f, 0.82f, 0.9f};   // Soft highlight
    private static final float[] BORDER_HOVER = {0.95f, 0.95f, 0.95f, 0.95f};   // Brighter edge
    private static final float[] BORDER_DISABLED = {0.45f, 0.45f, 0.45f, 0.5f}; // Muted
    
    private static final float[] TEXT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};         // White
    private static final float[] TEXT_DISABLED = {0.5f, 0.5f, 0.5f, 0.6f};      // Gray
    
    private String text;
    private Runnable onClick;
    private boolean pressed;
    private float hoverTransition;  // 0.0 = normal, 1.0 = hover
    
    /**
     * Creates a new menu button.
     * 
     * @param x X position (top-left corner)
     * @param y Y position (top-left corner)
     * @param width Button width
     * @param height Button height
     * @param text Button text
     * @param onClick Click callback
     */
    public MenuButton(float x, float y, float width, float height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
        this.pressed = false;
        this.hoverTransition = 0.0f;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) return;
        
        // Pick colors based on state
        float[] bgColor, borderColor, textColor;
        
        if (!enabled) {
            bgColor = BG_DISABLED;
            borderColor = BORDER_DISABLED;
            textColor = TEXT_DISABLED;
        } else if (pressed) {
            bgColor = BG_PRESSED;
            borderColor = BORDER_HOVER;
            textColor = TEXT_COLOR;
        } else {
            // Smooth transition between normal and hover
            bgColor = lerp(BG_NORMAL, BG_HOVER, hoverTransition);
            borderColor = lerp(BORDER_NORMAL, BORDER_HOVER, hoverTransition);
            textColor = TEXT_COLOR;
        }
        
        // Draw background rectangle - THIS WILL DEFINITELY SHOW UP
        renderer.drawRect(x, y, width, height, 
            bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
        
        // Draw border (4 rectangles forming a frame)
        float borderWidth = 3.0f;
        
        // Top border
        renderer.drawRect(x, y, width, borderWidth,
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        
        // Bottom border
        renderer.drawRect(x, y + height - borderWidth, width, borderWidth,
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        
        // Left border
        renderer.drawRect(x, y, borderWidth, height,
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        
        // Right border
        renderer.drawRect(x + width - borderWidth, y, borderWidth, height,
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        
        // Draw text shadow for readability
        if (text != null && !text.isEmpty()) {
            float textWidth = fontRenderer.getTextWidth(text);
            float textHeight = fontRenderer.getTextHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height - textHeight) / 2 + textHeight * 0.7f;
            
            // Shadow
            fontRenderer.drawText(text, textX + 2, textY + 2, 0.0f, 0.0f, 0.0f, 0.6f);
            
            // Main text
            fontRenderer.drawText(text, textX, textY,
                textColor[0], textColor[1], textColor[2], textColor[3]);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // Smooth hover transition
        float target = hovered ? 1.0f : 0.0f;
        float speed = 10.0f;
        
        hoverTransition += (target - hoverTransition) * deltaTime * speed;
        
        // Clamp to avoid overshoot
        if (Math.abs(hoverTransition - target) < 0.01f) {
            hoverTransition = target;
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
            pressed = false;
            // Only trigger callback if mouse is still over button
            if (enabled && isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.run();
            }
        }
    }
    
    /**
     * Linear interpolation between two colors.
     * 
     * @param a First color
     * @param b Second color
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated color
     */
    private float[] lerp(float[] a, float[] b, float t) {
        float[] result = new float[4];
        for (int i = 0; i < 4; i++) {
            result[i] = a[i] + (b[i] - a[i]) * t;
        }
        return result;
    }
    
    /**
     * Sets the button text.
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Gets the button text.
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the click callback.
     */
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }
}
