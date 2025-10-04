package com.poorcraft.ui;

/**
 * Text label UI component.
 * 
 * A simple non-interactive text display element.
 * Supports custom colors and optional horizontal centering.
 * 
 * Labels are the bread and butter of UI. Simple, reliable, boring.
 * Just how I like my UI components.
 */
public class Label extends UIComponent {
    
    private String text;
    private float r, g, b, a;
    private boolean centered;
    private float scale;
    
    /**
     * Creates a label with default white color.
     * 
     * @param x X position
     * @param y Y position
     * @param text Label text
     */
    public Label(float x, float y, String text) {
        this(x, y, text, 1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    /**
     * Creates a label with custom color.
     * 
     * @param x X position
     * @param y Y position
     * @param text Label text
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     */
    public Label(float x, float y, String text, float r, float g, float b, float a) {
        super(x, y, 0, 0); // Width/height calculated from text
        this.text = text;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.centered = false;
        this.scale = 1.0f;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible || text == null || text.isEmpty()) {
            return;
        }
        
        float textWidth = fontRenderer.getTextWidth(text) * scale;
        float drawX = centered ? x - textWidth / 2 : x;
        
        fontRenderer.drawText(text, drawX, y, scale, r, g, b, a);
    }
    
    @Override
    public void update(float deltaTime) {
        // Labels are static, no updates needed
    }
    
    /**
     * Sets the label text.
     * 
     * @param text New text
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Gets the label text.
     * 
     * @return Label text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the text color.
     * 
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     */
    public void setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
    
    /**
     * Sets whether the text should be centered horizontally.
     * 
     * @param centered True to center text
     */
    public void setCentered(boolean centered) {
        this.centered = centered;
    }
    
    /**
     * Gets whether the text is centered.
     * 
     * @return True if centered
     */
    public boolean isCentered() {
        return centered;
    }

    /**
     * Sets the font scale relative to the base font size.
     *
     * @param scale scale factor (values <= 0 fall back to 1)
     */
    public void setScale(float scale) {
        this.scale = scale <= 0 ? 1.0f : scale;
    }

    /**
     * Gets the font scale.
     *
     * @return current scale factor
     */
    public float getScale() {
        return scale;
    }
}
