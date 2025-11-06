package com.poorcraft.ui;

/**
 * Abstract base class for all UI components.
 * 
 * Components are positioned in screen space (pixels from top-left).
 * The coordinate system has origin at (0,0) in the top-left corner,
 * with X increasing right and Y increasing down.
 * 
 * All UI components support visibility, enabled state, and hover detection.
 * Subclasses implement rendering and interaction logic.
 * 
 * This is basically how every UI framework works. Swing, JavaFX, HTML... all the same idea.
 * If it ain't broke, don't fix it!
 */
public abstract class UIComponent {
    
    protected float x, y;           // Position (top-left corner)
    protected float width, height;  // Dimensions
    protected boolean visible;      // Whether component is visible
    protected boolean enabled;      // Whether component can be interacted with
    protected boolean hovered;      // Whether mouse is over component
    
    /**
     * Creates a new UI component.
     * 
     * @param x X position (pixels from left)
     * @param y Y position (pixels from top)
     * @param width Component width in pixels
     * @param height Component height in pixels
     */
    public UIComponent(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = true;
        this.enabled = true;
        this.hovered = false;
    }
    
    /**
     * Renders the component.
     * Called every frame when the component is visible.
     * 
     * @param renderer UI renderer for drawing primitives
     * @param fontRenderer Font renderer for drawing text
     */
    public abstract void render(UIRenderer renderer, FontRenderer fontRenderer);
    
    /**
     * Updates the component state.
     * Called every frame for animations, timers, etc.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public abstract void update(float deltaTime);
    
    /**
     * Checks if mouse coordinates are within component bounds.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @return True if mouse is over component
     */
    public boolean isMouseOver(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Called when mouse moves.
     * Updates hover state by default. Override for custom hover effects.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    public void onMouseMove(float mouseX, float mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
    }
    
    /**
     * Called when mouse button is pressed.
     * Override in subclasses to handle clicks.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button (0=left, 1=right, 2=middle)
     */
    public void onMouseClick(float mouseX, float mouseY, int button) {
        // Override in subclasses
    }
    
    /**
     * Called when mouse button is released.
     * Override in subclasses to handle click completion.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     */
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        // Override in subclasses
    }
    
    /**
     * Called when a key is pressed.
     * Override in subclasses for keyboard input (text fields, etc.).
     * 
     * @param key GLFW key code
     * @param mods Key modifiers (shift, ctrl, alt)
     */
    public void onKeyPress(int key, int mods) {
        // Override in subclasses
    }
    
    /**
     * Called when a character is typed.
     * Override in subclasses for text input.
     * 
     * @param character Typed character
     */
    public void onCharInput(char character) {
        // Override in subclasses
    }
    
    // Getters and setters
    
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    
    public void setPosition(float x, float y) {
        if (x < -10000f || y < -10000f) {
            System.err.println("[UIComponent] Warning: extreme negative position: (" + x + ", " + y + ")");
        }
        this.x = x;
        this.y = y;
    }

    public void setSize(float width, float height) {
        if (width < 0f || height < 0f) {
            System.err.println("[UIComponent] Clamping negative size: " + width + "x" + height);
        }
        this.width = Math.max(0f, width);
        this.height = Math.max(0f, height);
    }

    public void setBounds(float x, float y, float width, float height) {
        setPosition(x, y);
        setSize(width, height);
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isHovered() { return hovered; }
}
