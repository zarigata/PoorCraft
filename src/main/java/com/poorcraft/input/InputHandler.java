package com.poorcraft.input;

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
    private double mouseX;
    private double mouseY;
    private double lastMouseX;
    private double lastMouseY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    private boolean firstMouse;
    private boolean cursorGrabbed;
    
    /**
     * Creates a new input handler.
     * Call init() after this to register callbacks.
     */
    public InputHandler() {
        this.keys = new boolean[512];  // GLFW key codes go up to ~350, 512 is safe
        this.firstMouse = true;
        this.cursorGrabbed = false;
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
            }
        });
        
        // Mouse position callback - tracks cursor movement
        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });
        
        // Initialize last mouse position to window center
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(windowHandle, width, height);
        lastMouseX = width[0] / 2.0;
        lastMouseY = height[0] / 2.0;
        
        // Grab cursor by default (FPS game style)
        setCursorGrabbed(windowHandle, true);
        
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
}
