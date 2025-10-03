package com.poorcraft.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dropdown/combobox UI component for selecting from a list of options.
 * 
 * Click to expand, click an option to select, click outside to collapse.
 * Used for settings like resolution, AI provider, game mode, etc.
 * 
 * Dropdowns are deceptively complex. This is a simplified version that works
 * well enough for our needs. No search, no keyboard navigation, no scrolling.
 * If you have more than 10 options, you're doing something wrong.
 */
public class Dropdown extends UIComponent {
    
    private static final float[] BG_COLOR = {0.2f, 0.2f, 0.2f, 0.8f};
    private static final float[] BG_HOVER_COLOR = {0.3f, 0.3f, 0.3f, 0.9f};
    private static final float[] OPTION_HOVER_COLOR = {0.4f, 0.4f, 0.4f, 0.95f};
    private static final float[] TEXT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] ARROW_COLOR = {0.7f, 0.7f, 0.7f, 1.0f};
    
    private List<String> options;
    private int selectedIndex;
    private boolean expanded;
    private Consumer<Integer> onChange;
    private int hoveredOptionIndex;
    
    /**
     * Creates a new dropdown.
     * 
     * @param x X position
     * @param y Y position
     * @param width Dropdown width
     * @param height Dropdown height (for main box)
     * @param options List of options
     * @param initialIndex Initially selected option index
     * @param onChange Callback when selection changes
     */
    public Dropdown(float x, float y, float width, float height, List<String> options, 
                    int initialIndex, Consumer<Integer> onChange) {
        super(x, y, width, height);
        this.options = new ArrayList<>(options);
        this.selectedIndex = initialIndex;
        this.onChange = onChange;
        this.expanded = false;
        this.hoveredOptionIndex = -1;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) return;
        
        // Draw main dropdown box
        float[] bgColor = hovered ? BG_HOVER_COLOR : BG_COLOR;
        renderer.drawRect(x, y, width, height, bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
        
        // Draw border
        float borderWidth = 2;
        float borderBrightness = 0.2f;
        renderer.drawRect(x, y, width, borderWidth, 
            bgColor[0] + borderBrightness, bgColor[1] + borderBrightness, 
            bgColor[2] + borderBrightness, bgColor[3]);
        renderer.drawRect(x, y + height - borderWidth, width, borderWidth, 
            bgColor[0] + borderBrightness, bgColor[1] + borderBrightness, 
            bgColor[2] + borderBrightness, bgColor[3]);
        renderer.drawRect(x, y, borderWidth, height, 
            bgColor[0] + borderBrightness, bgColor[1] + borderBrightness, 
            bgColor[2] + borderBrightness, bgColor[3]);
        renderer.drawRect(x + width - borderWidth, y, borderWidth, height, 
            bgColor[0] + borderBrightness, bgColor[1] + borderBrightness, 
            bgColor[2] + borderBrightness, bgColor[3]);
        
        // Draw selected option text
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            String selectedText = options.get(selectedIndex);
            float textX = x + 8;
            float textY = y + height / 2 + fontRenderer.getTextHeight() * 0.3f;
            fontRenderer.drawText(selectedText, textX, textY, TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);
        }
        
        // Draw arrow indicator
        float arrowSize = 6;
        float arrowX = x + width - 20;
        float arrowY = y + height / 2;
        drawArrow(renderer, arrowX, arrowY, arrowSize, expanded);
        
        // Draw dropdown list if expanded
        if (expanded) {
            float listY = y + height + 2;
            float optionHeight = height;
            
            for (int i = 0; i < options.size(); i++) {
                float optionY = listY + i * optionHeight;
                
                // Draw option background
                float[] optionBgColor = (i == hoveredOptionIndex) ? OPTION_HOVER_COLOR : BG_COLOR;
                renderer.drawRect(x, optionY, width, optionHeight, 
                    optionBgColor[0], optionBgColor[1], optionBgColor[2], optionBgColor[3]);
                
                // Draw option border
                renderer.drawRect(x, optionY, width, borderWidth, 
                    optionBgColor[0] + borderBrightness, optionBgColor[1] + borderBrightness, 
                    optionBgColor[2] + borderBrightness, optionBgColor[3]);
                renderer.drawRect(x, optionY + optionHeight - borderWidth, width, borderWidth, 
                    optionBgColor[0] + borderBrightness, optionBgColor[1] + borderBrightness, 
                    optionBgColor[2] + borderBrightness, optionBgColor[3]);
                renderer.drawRect(x, optionY, borderWidth, optionHeight, 
                    optionBgColor[0] + borderBrightness, optionBgColor[1] + borderBrightness, 
                    optionBgColor[2] + borderBrightness, optionBgColor[3]);
                renderer.drawRect(x + width - borderWidth, optionY, borderWidth, optionHeight, 
                    optionBgColor[0] + borderBrightness, optionBgColor[1] + borderBrightness, 
                    optionBgColor[2] + borderBrightness, optionBgColor[3]);
                
                // Draw option text
                String optionText = options.get(i);
                float textX = x + 8;
                float textY = optionY + optionHeight / 2 + fontRenderer.getTextHeight() * 0.3f;
                fontRenderer.drawText(optionText, textX, textY, TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);
            }
        }
    }
    
    /**
     * Draws a simple arrow (triangle pointing down or up).
     */
    private void drawArrow(UIRenderer renderer, float x, float y, float size, boolean pointUp) {
        // Draw a simple triangle using rectangles (not perfect but good enough)
        float arrowWidth = 2;
        
        if (pointUp) {
            // Up arrow: draw lines forming ^
            for (int i = 0; i < (int)size; i++) {
                float offset = i * 0.7f;
                renderer.drawRect(x - offset, y + i, arrowWidth, arrowWidth, 
                    ARROW_COLOR[0], ARROW_COLOR[1], ARROW_COLOR[2], ARROW_COLOR[3]);
                renderer.drawRect(x + offset, y + i, arrowWidth, arrowWidth, 
                    ARROW_COLOR[0], ARROW_COLOR[1], ARROW_COLOR[2], ARROW_COLOR[3]);
            }
        } else {
            // Down arrow: draw lines forming v
            for (int i = 0; i < (int)size; i++) {
                float offset = i * 0.7f;
                renderer.drawRect(x - offset, y - i, arrowWidth, arrowWidth, 
                    ARROW_COLOR[0], ARROW_COLOR[1], ARROW_COLOR[2], ARROW_COLOR[3]);
                renderer.drawRect(x + offset, y - i, arrowWidth, arrowWidth, 
                    ARROW_COLOR[0], ARROW_COLOR[1], ARROW_COLOR[2], ARROW_COLOR[3]);
            }
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // No animation needed
    }
    
    @Override
    public void onMouseMove(float mouseX, float mouseY) {
        super.onMouseMove(mouseX, mouseY);
        
        if (expanded) {
            // Update hovered option
            hoveredOptionIndex = getOptionIndexAtPosition(mouseX, mouseY);
        }
    }
    
    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (button != 0) return;
        
        // Check if click is on main box
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            expanded = !expanded;
            return;
        }
        
        // Check if click is on an option (if expanded)
        if (expanded) {
            int optionIndex = getOptionIndexAtPosition(mouseX, mouseY);
            if (optionIndex >= 0) {
                selectedIndex = optionIndex;
                expanded = false;
                
                if (onChange != null) {
                    onChange.accept(selectedIndex);
                }
                return;
            }
        }
        
        // Click outside - collapse dropdown
        if (expanded) {
            expanded = false;
        }
    }
    
    /**
     * Gets the option index at the given mouse position.
     * Returns -1 if no option is at that position.
     */
    private int getOptionIndexAtPosition(float mouseX, float mouseY) {
        if (!expanded) return -1;
        
        float listY = y + height + 2;
        float optionHeight = height;
        
        if (mouseX < x || mouseX > x + width) return -1;
        if (mouseY < listY) return -1;
        
        int index = (int) ((mouseY - listY) / optionHeight);
        if (index >= 0 && index < options.size()) {
            return index;
        }
        
        return -1;
    }
    
    /**
     * Gets the currently selected option text.
     * 
     * @return Selected option text, or null if no selection
     */
    public String getSelectedOption() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            return options.get(selectedIndex);
        }
        return null;
    }
    
    /**
     * Gets the selected option index.
     * 
     * @return Selected index
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Sets the selected option index.
     * 
     * @param index New selected index
     */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            this.selectedIndex = index;
        }
    }
    
    /**
     * Sets the options list.
     * 
     * @param options New options list
     */
    public void setOptions(List<String> options) {
        this.options = new ArrayList<>(options);
        if (selectedIndex >= options.size()) {
            selectedIndex = Math.max(0, options.size() - 1);
        }
    }
}
