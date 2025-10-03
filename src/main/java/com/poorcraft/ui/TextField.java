package com.poorcraft.ui;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Text input field UI component.
 * 
 * Supports keyboard input, cursor positioning, and text selection.
 * Click to focus, type to enter text, backspace/delete to remove characters.
 * 
 * Text fields are surprisingly complex. This is a simplified version that
 * handles the basics. No fancy stuff like copy/paste or undo/redo.
 * Maybe in version 2.0... or never. Probably never.
 */
public class TextField extends UIComponent {
    
    private static final float[] BG_COLOR = {0.1f, 0.1f, 0.1f, 0.9f};
    private static final float[] BG_FOCUSED_COLOR = {0.15f, 0.15f, 0.15f, 0.95f};
    private static final float[] BORDER_COLOR = {0.3f, 0.3f, 0.3f, 1.0f};
    private static final float[] BORDER_FOCUSED_COLOR = {0.5f, 0.5f, 0.5f, 1.0f};
    private static final float[] TEXT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] PLACEHOLDER_COLOR = {0.5f, 0.5f, 0.5f, 0.7f};
    private static final float[] CURSOR_COLOR = {1.0f, 1.0f, 1.0f, 0.9f};
    
    private String text;
    private String placeholder;
    private boolean focused;
    private int cursorPosition;
    private float cursorBlinkTimer;
    private int maxLength;
    
    /**
     * Creates a new text field.
     * 
     * @param x X position
     * @param y Y position
     * @param width Field width
     * @param height Field height
     * @param placeholder Placeholder text when empty
     */
    public TextField(float x, float y, float width, float height, String placeholder) {
        super(x, y, width, height);
        this.text = "";
        this.placeholder = placeholder;
        this.focused = false;
        this.cursorPosition = 0;
        this.cursorBlinkTimer = 0;
        this.maxLength = 32;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) return;
        
        // Draw background
        float[] bgColor = focused ? BG_FOCUSED_COLOR : BG_COLOR;
        renderer.drawRect(x, y, width, height, bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
        
        // Draw border
        float[] borderColor = focused ? BORDER_FOCUSED_COLOR : BORDER_COLOR;
        float borderWidth = 2;
        renderer.drawRect(x, y, width, borderWidth, 
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        renderer.drawRect(x, y + height - borderWidth, width, borderWidth, 
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        renderer.drawRect(x, y, borderWidth, height, 
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        renderer.drawRect(x + width - borderWidth, y, borderWidth, height, 
            borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        
        // Draw text or placeholder
        float textX = x + 8;
        float textY = y + height / 2 + fontRenderer.getTextHeight() * 0.3f;
        
        if (text.isEmpty() && !focused) {
            // Draw placeholder
            fontRenderer.drawText(placeholder, textX, textY, 
                PLACEHOLDER_COLOR[0], PLACEHOLDER_COLOR[1], PLACEHOLDER_COLOR[2], PLACEHOLDER_COLOR[3]);
        } else {
            // Draw text
            fontRenderer.drawText(text, textX, textY, 
                TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);
            
            // Draw cursor if focused and visible
            if (focused && (cursorBlinkTimer % 1.0f) < 0.5f) {
                String textBeforeCursor = text.substring(0, cursorPosition);
                float cursorX = textX + fontRenderer.getTextWidth(textBeforeCursor);
                float cursorY = y + 6;
                float cursorHeight = height - 12;
                
                renderer.drawRect(cursorX, cursorY, 2, cursorHeight, 
                    CURSOR_COLOR[0], CURSOR_COLOR[1], CURSOR_COLOR[2], CURSOR_COLOR[3]);
            }
        }
    }
    
    @Override
    public void update(float deltaTime) {
        if (focused) {
            cursorBlinkTimer += deltaTime;
        }
    }
    
    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (button == 0) {
            boolean wasInside = isMouseOver(mouseX, mouseY);
            setFocused(wasInside);
            
            if (wasInside && !text.isEmpty()) {
                // Calculate cursor position from click location
                // This is a simple approximation - just snap to nearest character
                float clickX = mouseX - (x + 8);
                float bestDistance = Float.MAX_VALUE;
                int bestPosition = 0;
                
                for (int i = 0; i <= text.length(); i++) {
                    String substring = text.substring(0, i);
                    float charX = 0; // fontRenderer.getTextWidth(substring);
                    float distance = Math.abs(charX - clickX);
                    
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPosition = i;
                    }
                }
                
                cursorPosition = bestPosition;
            }
        }
    }
    
    @Override
    public void onKeyPress(int key, int mods) {
        if (!focused) return;
        
        if (key == GLFW_KEY_BACKSPACE) {
            // Remove character before cursor
            if (cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
            }
        } else if (key == GLFW_KEY_DELETE) {
            // Remove character after cursor
            if (cursorPosition < text.length()) {
                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
            }
        } else if (key == GLFW_KEY_LEFT) {
            // Move cursor left
            if (cursorPosition > 0) {
                cursorPosition--;
            }
        } else if (key == GLFW_KEY_RIGHT) {
            // Move cursor right
            if (cursorPosition < text.length()) {
                cursorPosition++;
            }
        } else if (key == GLFW_KEY_HOME) {
            // Move cursor to start
            cursorPosition = 0;
        } else if (key == GLFW_KEY_END) {
            // Move cursor to end
            cursorPosition = text.length();
        }
        
        // Reset cursor blink timer on any key press
        cursorBlinkTimer = 0;
    }
    
    @Override
    public void onCharInput(char character) {
        if (!focused) return;
        
        // Only accept printable characters
        if (character >= 32 && character < 127) {
            if (text.length() < maxLength) {
                // Insert character at cursor position
                text = text.substring(0, cursorPosition) + character + text.substring(cursorPosition);
                cursorPosition++;
                cursorBlinkTimer = 0;
            }
        }
    }
    
    /**
     * Gets the current text.
     * 
     * @return Text field content
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the text.
     * 
     * @param text New text
     */
    public void setText(String text) {
        this.text = text == null ? "" : text;
        this.cursorPosition = Math.min(cursorPosition, this.text.length());
    }
    
    /**
     * Sets the focus state.
     * 
     * @param focused True to focus field
     */
    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            cursorBlinkTimer = 0;
        }
    }
    
    /**
     * Gets the focus state.
     * 
     * @return True if focused
     */
    public boolean isFocused() {
        return focused;
    }
    
    /**
     * Sets the maximum text length.
     * 
     * @param maxLength Maximum number of characters
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
    
    /**
     * Gets the maximum text length.
     * 
     * @return Maximum number of characters
     */
    public int getMaxLength() {
        return maxLength;
    }
}
