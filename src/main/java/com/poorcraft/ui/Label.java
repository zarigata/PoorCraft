package com.poorcraft.ui;

/**
 * Text label UI component.
 *
 * <p>A simple non-interactive text display element. Supports custom colours,
 * optional horizontal centring, scaling, and configurable text shadows for
 * improved readability on complex backgrounds.</p>
 */
public class Label extends UIComponent {

    private String text;
    private float r;
    private float g;
    private float b;
    private float a;
    private boolean centered;
    private float scale;
    private boolean useTextShadow;
    private float shadowOffset;
    private float shadowAlpha;

    /**
     * Creates a label with default white colour.
     *
     * @param x    X position
     * @param y    Y position
     * @param text label text
     */
    public Label(float x, float y, String text) {
        this(x, y, text, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Creates a label with custom colour.
     *
     * @param x    X position
     * @param y    Y position
     * @param text label text
     * @param r    red component (0.0 to 1.0)
     * @param g    green component (0.0 to 1.0)
     * @param b    blue component (0.0 to 1.0)
     * @param a    alpha component (0.0 to 1.0)
     */
    public Label(float x, float y, String text, float r, float g, float b, float a) {
        super(x, y, 0f, 0f);
        this.text = text;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.centered = false;
        this.scale = 1.0f;
        this.useTextShadow = false;
        this.shadowOffset = 2.0f;
        this.shadowAlpha = 0.6f;
    }

    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible || text == null || text.isEmpty()) {
            return;
        }

        float textWidth = fontRenderer.getTextWidth(text) * scale;
        float drawX = centered ? x - (textWidth / 2f) : x;

        if (useTextShadow) {
            fontRenderer.drawTextWithShadow(text, drawX, y, scale, r, g, b, a, shadowOffset, shadowAlpha);
        } else {
            fontRenderer.drawText(text, drawX, y, scale, r, g, b, a);
        }
    }

    @Override
    public void update(float deltaTime) {
        // Labels are static, no per-frame updates required.
    }

    /**
     * Sets the label text.
     *
     * @param text new text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Updates the label position.
     *
     * @param x new X coordinate
     * @param y new Y coordinate
     */
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
    }

    /**
     * Gets the label text.
     *
     * @return label text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text colour.
     *
     * @param r red component (0.0 to 1.0)
     * @param g green component (0.0 to 1.0)
     * @param b blue component (0.0 to 1.0)
     * @param a alpha component (0.0 to 1.0)
     */
    public void setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * Sets whether the text should be centred horizontally.
     *
     * @param centered {@code true} to centre text
     */
    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    /**
     * Checks whether the text is centred.
     *
     * @return {@code true} if centred
     */
    public boolean isCentered() {
        return centered;
    }

    /**
     * Sets the font scale relative to the base font size.
     *
     * @param scale scale factor (values &lt;= 0 fall back to 1)
     */
    public void setScale(float scale) {
        this.scale = scale <= 0f ? 1.0f : scale;
    }

    /**
     * Gets the font scale.
     *
     * @return current scale factor
     */
    public float getScale() {
        return scale;
    }

    /**
     * Enables or disables text shadow rendering.
     *
     * @param useTextShadow {@code true} to enable shadows
     */
    public void setUseTextShadow(boolean useTextShadow) {
        this.useTextShadow = useTextShadow;
    }

    /**
     * Sets the distance between the shadow and the text.
     *
     * @param shadowOffset offset in pixels
     */
    public void setShadowOffset(float shadowOffset) {
        this.shadowOffset = shadowOffset;
    }

    /**
     * Sets the shadow alpha multiplier.
     *
     * @param shadowAlpha shadow opacity (0.0 to 1.0)
     */
    public void setShadowAlpha(float shadowAlpha) {
        this.shadowAlpha = shadowAlpha;
    }
}
