package com.poorcraft.input;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Input handler for keyboard and mouse state tracking.
 * Uses GLFW callbacks to capture input and caches state for polling.
 * 
 * Why callbacks AND polling? Because sometimes you need immediate response (callbacks)
 * and sometimes you need to check state in your update loop (polling).
 * Best of both worlds!
 */
public class InputHandler {
    
    private final boolean[] keys;
    private final boolean[] mouseButtons;
    private double mouseX;
    private double mouseY;
    private double lastMouseX;
    private double lastMouseY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    private boolean firstMouse;
    private boolean cursorGrabbed;
    private double scrollOffset;
    
    // Callbacks for UI integration
    private Consumer<Integer> keyPressCallback;
    private Consumer<Character> charInputCallback;
    private Consumer<Integer> mouseClickCallback;
    private Consumer<Integer> mouseReleaseCallback;
    
    /**
     * Creates a new input handler.
     * Call init() after this to register callbacks.
     */
    public InputHandler() {
        this.keys = new boolean[512];  // GLFW key codes go up to ~350, 512 is safe
        this.mouseButtons = new boolean[8];  // Support 8 mouse buttons
        this.firstMouse = true;
        this.cursorGrabbed = false;
        this.scrollOffset = 0.0;
    }
    
    /**
     * Initializes input callbacks for the given window.
     * Sets up keyboard and mouse tracking, grabs cursor by default.
     * 
     * @param windowHandle GLFW window handle
     */
    public void init(long windowHandle) {
        // Keyboard callback - tracks key press/release
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) {
                keys[key] = (action != GLFW_RELEASE);
                
                // Fire key press callback
                if (action == GLFW_PRESS && keyPressCallback != null) {
                    keyPressCallback.accept(key);
                }
            }
        });
        
        // Character input callback - for text input
        glfwSetCharCallback(windowHandle, (window, codepoint) -> {
            if (charInputCallback != null) {
                charInputCallback.accept((char) codepoint);
            }
        });
        
        // Mouse button callback
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button >= 0 && button < mouseButtons.length) {
                mouseButtons[button] = (action != GLFW_RELEASE);
                
                if (action == GLFW_PRESS && mouseClickCallback != null) {
                    mouseClickCallback.accept(button);
                }
                if (action == GLFW_RELEASE && mouseReleaseCallback != null) {
                    mouseReleaseCallback.accept(button);
                }
            }
        });
        
        // Mouse position callback - tracks cursor movement
        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });
        
        // Scroll callback - accumulates scroll offset for hotbar selection and UI
        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            scrollOffset += yoffset;
        });
        
        // Initialize last mouse position to window center
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(windowHandle, width, height);
        lastMouseX = width[0] / 2.0;
        lastMouseY = height[0] / 2.0;
        
        // Don't grab cursor by default - let Game/UIManager handle it based on state
        // This way menus can actually be used. Revolutionary concept, I know.
        
        System.out.println("[InputHandler] Initialized");
    }
    
    /**
     * Updates mouse delta calculations.
     * Call this once per frame before processing input.
     */
    public void update() {
        if (firstMouse) {
            // First frame: don't calculate delta to avoid jump
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
            mouseDeltaX = 0.0;
            mouseDeltaY = 0.0;
        } else {
            // Calculate mouse movement since last frame
            mouseDeltaX = mouseX - lastMouseX;
            mouseDeltaY = lastMouseY - mouseY;  // Inverted Y for FPS controls
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }
    
    /**
     * Checks if a key is currently pressed.
     * 
     * @param keyCode GLFW key code
     * @return true if key is pressed
     */
    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < keys.length) {
            return keys[keyCode];
        }
        return false;
    }
    
    /**
     * Returns horizontal mouse movement since last frame.
     * 
     * @return Mouse delta X
     */
    public double getMouseDeltaX() {
        return mouseDeltaX;
    }
    
    /**
     * Returns vertical mouse movement since last frame.
     * 
     * @return Mouse delta Y
     */
    public double getMouseDeltaY() {
        return mouseDeltaY;
    }
    
    /**
     * Grabs or releases the cursor.
     * Grabbed cursor is hidden and locked to window (FPS style).
     * Released cursor is visible and free to move.
     * 
     * @param windowHandle GLFW window handle
     * @param grabbed true to grab cursor, false to release
     */
    public void setCursorGrabbed(long windowHandle, boolean grabbed) {
        this.cursorGrabbed = grabbed;
        glfwSetInputMode(windowHandle, GLFW_CURSOR, 
                        grabbed ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
        
        // Reset first mouse flag to prevent jump when re-grabbing
        if (grabbed) {
            firstMouse = true;
        }
    }
    
    /**
     * Checks if cursor is currently grabbed.
     * 
     * @return true if cursor is grabbed
     */
    public boolean isCursorGrabbed() {
        return cursorGrabbed;
    }
    
    /**
     * Resets mouse deltas to zero.
     * Useful when cursor is released to prevent jump when re-grabbed.
     */
    public void resetMouseDeltas() {
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }

    /**
     * Retrieves accumulated scroll offset since last call and resets it.
     * Positive values indicate scrolling up, negative indicate scrolling down.
     * 
     * @return scroll offset
     */
    public double consumeScrollOffset() {
        double offset = scrollOffset;
        scrollOffset = 0.0;
        return offset;
    }
    
    /**
     * Gets current mouse X position.
     * 
     * @return Mouse X position
     */
    public double getMouseX() {
        return mouseX;
    }
    
    /**
     * Gets current mouse Y position.
     * 
     * @return Mouse Y position
     */
    public double getMouseY() {
        return mouseY;
    }
    
    /**
     * Checks if a mouse button is pressed.
     * 
     * @param button Mouse button (0=left, 1=right, 2=middle)
     * @return True if button is pressed
     */
    public boolean isMouseButtonPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }
    
    /**
     * Sets the key press callback.
     * 
     * @param callback Callback to invoke when a key is pressed
     */
    public void setKeyPressCallback(Consumer<Integer> callback) {
        this.keyPressCallback = callback;
    }
    
    /**
     * Sets the character input callback.
     * 
     * @param callback Callback to invoke when a character is typed
     */
    public void setCharInputCallback(Consumer<Character> callback) {
        this.charInputCallback = callback;
    }
    
    /**
     * Sets the mouse click callback.
     * 
     * @param callback Callback to invoke when a mouse button is clicked
     */
    public void setMouseClickCallback(Consumer<Integer> callback) {
        this.mouseClickCallback = callback;
    }
    
    /**
     * Sets the mouse release callback.
     * 
     * @param callback Callback to invoke when a mouse button is released
     */
    public void setMouseReleaseCallback(Consumer<Integer> callback) {
        this.mouseReleaseCallback = callback;
    }
}
