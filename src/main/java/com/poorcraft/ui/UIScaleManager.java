package com.poorcraft.ui;

/**
 * Centralized UI scaling manager that calculates scale factors based on window dimensions.
 * 
 * <p>This manager provides a unified approach to UI scaling across all screens and components.
 * Instead of each screen calculating its own ad-hoc scale factors, all scaling decisions
 * flow through this manager, ensuring consistency.</p>
 * 
 * <h2>Scaling Philosophy</h2>
 * <ul>
 *   <li><b>Window-based scaling:</b> Scale factors are calculated from the current window size,
 *       not the screen resolution. This ensures UI remains readable when the window is resized.</li>
 *   <li><b>Reference resolution:</b> 1920x1080 is used as the baseline. At this resolution,
 *       baseScale = 1.0. Smaller windows get smaller scales, larger windows get larger scales.</li>
 *   <li><b>User preference:</b> The userScale multiplier (from Settings.graphics.uiScale) allows
 *       players to adjust UI size to their preference, independent of window size.</li>
 *   <li><b>Effective scale:</b> The final scale used for rendering is baseScale * userScale,
 *       clamped to reasonable bounds to prevent extreme values.</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <pre>
 * // Get the combined scale factor for sizing UI elements
 * float scale = scaleManager.getEffectiveScale();
 * float buttonWidth = 200f * scale;
 * 
 * // Scale individual dimensions
 * float padding = scaleManager.scaleDimension(12f);
 * 
 * // Use percentage-based sizing
 * float panelWidth = scaleManager.scaleWidth(0.5f);  // 50% of window width
 * 
 * // Get recommended font size for FontRenderer
 * int fontSize = scaleManager.getFontSize();  // Returns 16, 20, 24, or 32
 * 
 * // Get fine-grained text scale for FontRenderer.drawText()
 * float textScale = scaleManager.getTextScale();
 * </pre>
 */
public class UIScaleManager {
    
    // Reference resolution for scale calculations (1920x1080 is the baseline)
    private static final int REFERENCE_WIDTH = 1920;
    private static final int REFERENCE_HEIGHT = 1080;
    
    // Scale bounds to prevent extreme values
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;
    
    // Font size thresholds for discrete atlas selection
    private static final float FONT_SIZE_16_THRESHOLD = 0.8f;
    private static final float FONT_SIZE_20_THRESHOLD = 1.2f;
    private static final float FONT_SIZE_24_THRESHOLD = 1.8f;
    
    private int windowWidth;
    private int windowHeight;
    private float baseScale;
    private float userScale;
    private float effectiveScale;
    
    /**
     * Creates a new UI scale manager.
     * 
     * @param windowWidth Current window width in pixels
     * @param windowHeight Current window height in pixels
     * @param userScale User preference scale multiplier (from settings)
     */
    public UIScaleManager(int windowWidth, int windowHeight, float userScale) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.userScale = Math.max(0.5f, Math.min(2.0f, userScale));
        recalculateScales();
    }
    
    /**
     * Updates the window size and recalculates scale factors.
     * Called when the window is resized.
     * 
     * @param width New window width
     * @param height New window height
     */
    public void updateWindowSize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        recalculateScales();
    }
    
    /**
     * Updates the user preference scale multiplier.
     * Called when the user changes UI scale in settings.
     * 
     * @param scale New user scale (typically 0.5 to 2.0)
     */
    public void setUserScale(float scale) {
        this.userScale = Math.max(0.5f, Math.min(2.0f, scale));
        recalculateScales();
    }
    
    /**
     * Gets the effective scale factor (baseScale * userScale).
     * This is the primary scale factor used for sizing UI elements.
     * 
     * @return Combined scale factor
     */
    public float getEffectiveScale() {
        return effectiveScale;
    }
    
    /**
     * Gets the base scale factor calculated from window size only.
     * Useful for debugging or when you need window-based scaling without user preference.
     * 
     * @return Window-based scale factor
     */
    public float getBaseScale() {
        return baseScale;
    }
    
    /**
     * Scales a pixel dimension by the effective scale factor.
     * 
     * @param pixels Dimension in pixels at reference resolution
     * @return Scaled dimension
     */
    public float scaleDimension(float pixels) {
        return pixels * effectiveScale;
    }
    
    /**
     * Converts a percentage of window width to pixels.
     * 
     * @param percent Width percentage (0.0 to 1.0)
     * @return Width in pixels
     */
    public float scaleWidth(float percent) {
        return windowWidth * percent;
    }
    
    /**
     * Converts a percentage of window height to pixels.
     * 
     * @param percent Height percentage (0.0 to 1.0)
     * @return Height in pixels
     */
    public float scaleHeight(float percent) {
        return windowHeight * percent;
    }
    
    /**
     * Gets the recommended font size based on the current scale.
     * Returns a discrete size (16, 20, 24, or 32) for FontRenderer atlas selection.
     * 
     * @return Font size in pixels
     */
    public int getFontSize() {
        if (effectiveScale < FONT_SIZE_16_THRESHOLD) {
            return 16;
        } else if (effectiveScale < FONT_SIZE_20_THRESHOLD) {
            return 20;
        } else if (effectiveScale < FONT_SIZE_24_THRESHOLD) {
            return 24;
        } else {
            return 32;
        }
    }
    
    /**
     * Gets the fine-grained text scale multiplier for FontRenderer.
     * This provides smooth scaling between discrete font sizes.
     * 
     * <p>For example, if effectiveScale is 1.1 and we're using the 20px atlas,
     * this returns 1.1 so text can be scaled slightly larger than the base atlas size.</p>
     * 
     * @return Text scale multiplier
     * @deprecated Use getTextScaleForFontSize(int) to avoid double-scaling
     */
    @Deprecated
    public float getTextScale() {
        // Return the effective scale directly for fine-grained control
        // FontRenderer will apply this on top of the selected atlas size
        return effectiveScale;
    }
    
    /**
     * Gets the normalized text scale for a specific font atlas size.
     * This corrects for the atlas size selection to prevent double-scaling.
     * 
     * <p>The formula compensates for the discrete atlas size choice:
     * textScale = effectiveScale / (atlasPx / 20f)</p>
     * 
     * <p>For example, if effectiveScale is 1.5 and we selected the 24px atlas,
     * this returns 1.5 / (24/20) = 1.25, so the final rendered size is 24 * 1.25 = 30px,
     * which matches the intended effectiveScale * 20 = 30px.</p>
     * 
     * @param atlasPx The font atlas size in pixels (16, 20, 24, or 32)
     * @return Normalized text scale multiplier
     */
    public float getTextScaleForFontSize(int atlasPx) {
        if (atlasPx <= 0) {
            return effectiveScale;
        }
        // Normalize: effectiveScale / (atlasPx / 20f)
        // This ensures final size = atlasPx * scale = effectiveScale * 20
        return effectiveScale / (atlasPx / 20f);
    }
    
    /**
     * Scales an X coordinate by the effective scale factor.
     * Useful for positioning elements proportionally.
     * 
     * @param x X coordinate at reference resolution
     * @return Scaled X coordinate
     */
    public float scaleX(float x) {
        return x * effectiveScale;
    }
    
    /**
     * Scales a Y coordinate by the effective scale factor.
     * Useful for positioning elements proportionally.
     * 
     * @param y Y coordinate at reference resolution
     * @return Scaled Y coordinate
     */
    public float scaleY(float y) {
        return y * effectiveScale;
    }
    
    /**
     * Gets the current window width.
     * 
     * @return Window width in pixels
     */
    public int getWindowWidth() {
        return windowWidth;
    }
    
    /**
     * Gets the current window height.
     * 
     * @return Window height in pixels
     */
    public int getWindowHeight() {
        return windowHeight;
    }
    
    /**
     * Recalculates all scale factors based on current window size and user preference.
     */
    private void recalculateScales() {
        // Calculate base scale as average of width and height ratios
        float widthRatio = (float) windowWidth / REFERENCE_WIDTH;
        float heightRatio = (float) windowHeight / REFERENCE_HEIGHT;
        baseScale = (widthRatio + heightRatio) / 2.0f;
        
        // Clamp base scale to reasonable bounds
        baseScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, baseScale));
        
        // Calculate effective scale
        effectiveScale = baseScale * userScale;
        
        // Clamp effective scale to prevent extreme values
        effectiveScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, effectiveScale));
    }
}
