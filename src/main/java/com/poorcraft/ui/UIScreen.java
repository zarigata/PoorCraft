package com.poorcraft.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for UI screens.
 * 
 * Screens are containers for UI components. Each screen (main menu, settings, etc.)
 * extends this class and adds its own components.
 * 
 * The screen manages component lifecycle, input forwarding, and rendering.
 * Components are rendered in the order they were added.
 * 
 * This is a pretty standard UI framework pattern. Nothing revolutionary here,
 * just good old-fashioned object-oriented design.
 */
public abstract class UIScreen {
    
    protected List<UIComponent> components;
    protected int windowWidth;
    protected int windowHeight;
    protected UIComponent focusedComponent;
    protected UIScaleManager scaleManager;
    protected float lastMouseX;
    protected float lastMouseY;
    private boolean lastMousePositionInitialized;
    
    /**
     * Creates a new UI screen.
     * 
     * @param windowWidth Screen width in pixels
     * @param windowHeight Screen height in pixels
     * @param scaleManager UI scale manager for consistent scaling
     */
    public UIScreen(int windowWidth, int windowHeight, UIScaleManager scaleManager) {
        this.components = new ArrayList<>();
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.scaleManager = scaleManager;
        this.focusedComponent = null;
        this.lastMousePositionInitialized = false;
    }
    
    /**
     * Creates a new UI screen without scale manager (for backward compatibility).
     * 
     * @param windowWidth Screen width in pixels
     * @param windowHeight Screen height in pixels
     */
    public UIScreen(int windowWidth, int windowHeight) {
        this(windowWidth, windowHeight, null);
    }
    
    void setLastMousePosition(float mouseX, float mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.lastMousePositionInitialized = true;
    }

    /**
     * Initializes the screen.
     * Called when the screen is shown. Subclasses should create their components here.
     */
    public abstract void init();
    
    /**
     * Called when the window is resized.
     * Subclasses should update component positions/sizes here.
     * 
     * @param width New window width
     * @param height New window height
     */
    public abstract void onResize(int width, int height);
    
    /**
     * Renders all components on this screen.
     * Can be overridden to add custom background rendering.
     * 
     * @param renderer UI renderer
     * @param fontRenderer Font renderer
     */
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Use copy to avoid ConcurrentModificationException if components change during rendering
        for (UIComponent component : new ArrayList<>(components)) {
            if (component.isVisible()) {
                component.render(renderer, fontRenderer);
            }
        }
    }
    
    /**
     * Updates all components.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public void update(float deltaTime) {
        // Use copy to avoid ConcurrentModificationException if components change during update
        for (UIComponent component : new ArrayList<>(components)) {
            if (component.isVisible()) {
                component.update(deltaTime);
            }
        }
    }
    
    /**
     * Forwards mouse move event to all components.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    public void onMouseMove(float mouseX, float mouseY) {
        // Use copy to avoid ConcurrentModificationException if components change during event
        for (UIComponent component : new ArrayList<>(components)) {
            if (component.isVisible()) {
                component.onMouseMove(mouseX, mouseY);
            }
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastMousePositionInitialized = true;
    }
    
    /**
     * Forwards mouse click event to all components.
     * Also handles focus management for text fields.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     */
    public void onMouseClick(float mouseX, float mouseY, int button) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastMousePositionInitialized = true;
        // Use copy to avoid ConcurrentModificationException if components change during event
        List<UIComponent> componentsCopy = new ArrayList<>(components);
        
        // Update focused component
        UIComponent newFocus = null;
        for (UIComponent component : componentsCopy) {
            if (component.isVisible() && component.isMouseOver(mouseX, mouseY)) {
                if (component instanceof TextField) {
                    newFocus = component;
                }
            }
        }
        
        // Unfocus previous component
        if (focusedComponent != null && focusedComponent != newFocus) {
            if (focusedComponent instanceof TextField) {
                ((TextField) focusedComponent).setFocused(false);
            }
        }
        
        focusedComponent = newFocus;
        
        // Forward click to all components
        for (UIComponent component : componentsCopy) {
            if (component.isVisible()) {
                component.onMouseClick(mouseX, mouseY, button);
            }
        }
    }
    
    /**
     * Forwards mouse release event to all components.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     */
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        // Use copy to avoid ConcurrentModificationException if components change during event
        // This is critical because onClick callbacks might rebuild the screen (e.g., tab switches)
        for (UIComponent component : new ArrayList<>(components)) {
            if (component.isVisible()) {
                component.onMouseRelease(mouseX, mouseY, button);
            }
        }
    }

    /**
     * Forwards scroll events to the first scrollable container under the cursor.
     *
     * @param yOffset Scroll delta (positive = up, negative = down)
     */
    public void onScroll(double yOffset) {
        if (!lastMousePositionInitialized) {
            lastMouseX = windowWidth / 2.0f;
            lastMouseY = windowHeight / 2.0f;
            lastMousePositionInitialized = true;
        }
        List<UIComponent> componentsCopy = new ArrayList<>(components);
        for (int i = componentsCopy.size() - 1; i >= 0; i--) {
            UIComponent component = componentsCopy.get(i);
            if (!(component instanceof ScrollContainer container) || !component.isVisible()) {
                continue;
            }
            if (container.isMouseOver(lastMouseX, lastMouseY)) {
                container.onScroll(yOffset);
                break;
            }
        }
    }
    
    /**
     * Forwards key press event to focused component.
     * 
     * @param key GLFW key code
     * @param mods Key modifiers
     */
    public void onKeyPress(int key, int mods) {
        if (focusedComponent != null) {
            focusedComponent.onKeyPress(key, mods);
        }
    }
    
    /**
     * Forwards character input to focused component.
     * 
     * @param character Typed character
     */
    public void onCharInput(char character) {
        if (focusedComponent != null) {
            focusedComponent.onCharInput(character);
        }
    }
    
    /**
     * Adds a component to this screen.
     * 
     * @param component Component to add
     */
    protected void addComponent(UIComponent component) {
        components.add(component);
    }
    
    /**
     * Removes a component from this screen.
     * 
     * @param component Component to remove
     */
    protected void removeComponent(UIComponent component) {
        components.remove(component);
        if (focusedComponent == component) {
            focusedComponent = null;
        }
    }
    
    /**
     * Clears all components from this screen.
     * Useful for rebuilding the screen layout.
     */
    protected void clearComponents() {
        components.clear();
        focusedComponent = null;
    }
    
    /**
     * Gets the list of components.
     * 
     * @return Component list
     */
    protected List<UIComponent> getComponents() {
        return components;
    }
    
    /**
     * Gets the UI scale manager.
     * 
     * @return Scale manager instance (may be null)
     */
    public UIScaleManager getScaleManager() {
        return scaleManager;
    }
    
    /**
     * Scales an X coordinate using the scale manager.
     * 
     * @param x X coordinate at reference resolution
     * @return Scaled X coordinate
     */
    protected float scaleX(float x) {
        return scaleManager != null ? scaleManager.scaleX(x) : x;
    }
    
    /**
     * Scales a Y coordinate using the scale manager.
     * 
     * @param y Y coordinate at reference resolution
     * @return Scaled Y coordinate
     */
    protected float scaleY(float y) {
        return scaleManager != null ? scaleManager.scaleY(y) : y;
    }
    
    /**
     * Scales a dimension using the scale manager.
     * 
     * @param pixels Dimension in pixels at reference resolution
     * @return Scaled dimension
     */
    protected float scaleDimension(float pixels) {
        return scaleManager != null ? scaleManager.scaleDimension(pixels) : pixels;
    }
    
    /**
     * Gets the text scale multiplier for FontRenderer.
     * 
     * @return Text scale factor
     * @deprecated Use getTextScale(FontRenderer) to get normalized scale
     */
    @Deprecated
    protected float getTextScale() {
        return scaleManager != null ? scaleManager.getTextScale() : 1.0f;
    }
    
    /**
     * Gets the normalized text scale for the current font atlas size.
     * This prevents double-scaling by compensating for atlas size selection.
     * 
     * @param fontRenderer FontRenderer to get current atlas size from
     * @return Normalized text scale factor
     */
    protected float getTextScale(FontRenderer fontRenderer) {
        if (scaleManager == null || fontRenderer == null) {
            return 1.0f;
        }
        int atlasSize = fontRenderer.getFontSize();
        return scaleManager.getTextScaleForFontSize(atlasSize);
    }
}
