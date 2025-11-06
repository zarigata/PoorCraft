package com.poorcraft.ui;

/**
 * Tooltip component that displays contextual information when hovering over UI elements.
 * 
 * Features smooth fade in/out animations, automatic positioning near the mouse cursor,
 * text wrapping for long content, and a modern dark theme with high contrast.
 */
public class Tooltip extends UIComponent {
    
    private String text;
    private float fadeAlpha;
    private float targetAlpha;
    private float fadeSpeed;
    private float paddingX;
    private float paddingY;
    private float maxWidth;
    
    // Styling constants
    private static final float BG_R = 0.08f;
    private static final float BG_G = 0.08f;
    private static final float BG_B = 0.12f;
    private static final float BG_A = 0.92f;
    
    private static final float BORDER_R = 0.7f;
    private static final float BORDER_G = 0.85f;
    private static final float BORDER_B = 0.95f;
    private static final float BORDER_A = 0.8f;
    
    private static final float TEXT_R = 0.95f;
    private static final float TEXT_G = 0.95f;
    private static final float TEXT_B = 1.0f;
    
    private static final float TEXT_SCALE = 1.0f;
    private static final float SHADOW_OFFSET = 2.0f;
    private static final float SHADOW_ALPHA = 0.7f;
    
    /**
     * Creates a new tooltip.
     * 
     * @param x Initial X position
     * @param y Initial Y position
     * @param text Tooltip text content
     */
    public Tooltip(float x, float y, String text) {
        super(x, y, 0f, 0f);
        this.text = text;
        this.fadeAlpha = 0.0f;
        this.targetAlpha = 0.0f;
        this.fadeSpeed = 8.0f;
        this.paddingX = 12f;
        this.paddingY = 8f;
        this.maxWidth = 300f;
    }
    
    /**
     * Shows the tooltip with a fade-in animation.
     */
    public void show() {
        targetAlpha = 1.0f;
    }
    
    /**
     * Hides the tooltip with a fade-out animation.
     */
    public void hide() {
        targetAlpha = 0.0f;
    }
    
    /**
     * Checks if the tooltip is currently visible.
     * 
     * @return True if tooltip has any opacity
     */
    public boolean isVisible() {
        return fadeAlpha > 0.01f;
    }
    
    /**
     * Updates the tooltip text content.
     * 
     * @param text New text content
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Sets the maximum width before text wrapping.
     * 
     * @param width Maximum width in pixels
     */
    public void setMaxWidth(float width) {
        this.maxWidth = width;
    }
    
    /**
     * Updates the tooltip position.
     * 
     * @param x New X position
     * @param y New Y position
     */
    @Override
    public void setX(float x) {
        this.x = x;
    }
    
    @Override
    public void setY(float y) {
        this.y = y;
    }
    
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public void update(float deltaTime) {
        // Smooth fade animation
        fadeAlpha += (targetAlpha - fadeAlpha) * deltaTime * fadeSpeed;
        
        // Clamp alpha
        if (fadeAlpha < 0.0f) fadeAlpha = 0.0f;
        if (fadeAlpha > 1.0f) fadeAlpha = 1.0f;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (fadeAlpha < 0.01f || text == null || text.isEmpty()) {
            return;
        }
        
        // Split text by newlines
        String[] rawLines = text.split("\n");
        java.util.List<String> lines = new java.util.ArrayList<>();
        
        // Process each line for wrapping
        float baseLineHeight = fontRenderer.getTextHeight() * TEXT_SCALE;
        float lineHeight = baseLineHeight * 1.25f;
        
        for (String rawLine : rawLines) {
            float lineWidth = fontRenderer.getTextWidth(rawLine) * TEXT_SCALE;
            
            if (lineWidth <= maxWidth) {
                // Line fits, add as-is
                lines.add(rawLine);
            } else {
                // Naive word wrapping at word boundaries
                String[] words = rawLine.split(" ");
                StringBuilder currentLine = new StringBuilder();
                
                for (String word : words) {
                    String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                    float testWidth = fontRenderer.getTextWidth(testLine) * TEXT_SCALE;
                    
                    if (testWidth <= maxWidth) {
                        if (currentLine.length() > 0) {
                            currentLine.append(" ");
                        }
                        currentLine.append(word);
                    } else {
                        // Current line is full, start new line
                        if (currentLine.length() > 0) {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder(word);
                        } else {
                            // Single word exceeds maxWidth, add it anyway
                            lines.add(word);
                        }
                    }
                }
                
                // Add remaining text
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
            }
        }
        
        // Calculate max line width and box dimensions
        float maxLineWidth = 0f;
        for (String line : lines) {
            float lineWidth = fontRenderer.getTextWidth(line) * TEXT_SCALE;
            if (lineWidth > maxLineWidth) {
                maxLineWidth = lineWidth;
            }
        }
        
        float boxWidth = maxLineWidth + paddingX * 2f;
        float boxHeight = (lines.size() * lineHeight) + paddingY * 2f;
        
        // Update component dimensions for bounds checking
        this.width = boxWidth;
        this.height = boxHeight;
        
        // Draw drop shadow for depth
        float shadowX = x + 4f;
        float shadowY = y + 4f;
        renderer.drawRect(shadowX, shadowY, boxWidth, boxHeight, 
            0f, 0f, 0f, 0.5f * fadeAlpha);
        
        // Draw background
        renderer.drawRect(x, y, boxWidth, boxHeight, 
            BG_R, BG_G, BG_B, BG_A * fadeAlpha);
        
        // Draw border
        float borderWidth = 2f;
        float[] bgColor = {BG_R, BG_G, BG_B, BG_A * fadeAlpha};
        float[] borderColor = {BORDER_R, BORDER_G, BORDER_B, BORDER_A * fadeAlpha};
        renderer.drawBorderedRect(x, y, boxWidth, boxHeight, borderWidth, bgColor, borderColor);
        
        // Draw each line of text vertically stacked
        float textX = x + paddingX;
        float textY = y + paddingY;
        
        for (String line : lines) {
            fontRenderer.drawTextWithShadow(line, textX, textY, TEXT_SCALE, 
                TEXT_R, TEXT_G, TEXT_B, fadeAlpha, SHADOW_OFFSET, SHADOW_ALPHA);
            textY += lineHeight;
        }
    }
}
