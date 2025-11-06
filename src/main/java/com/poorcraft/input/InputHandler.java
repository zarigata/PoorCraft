package com.poorcraft.input;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

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
    
    /**
     * Key action types for distinguishing press, repeat, and release events.
     */
    public enum KeyAction {
        PRESS,
        REPEAT,
        RELEASE
    }
    
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
    
    // Key repeat tracking
    private Map<Integer, Float> keyPressTimes;
    private Map<Integer, Boolean> keyRepeating;
    private static final float KEY_REPEAT_INITIAL_DELAY = 0.5f;
    private static final float KEY_REPEAT_INTERVAL = 0.03f;
    
    // Printable character repeat tracking (independent from key-event repeat)
    private Map<Integer, Float> charPressTimes;
    private Map<Integer, Boolean> charRepeating;
    
    // Callbacks for UI integration
    private Consumer<Integer> keyPressCallback;
    private BiConsumer<Integer, KeyAction> keyEventCallback;
    private Consumer<Character> charInputCallback;
    private Consumer<Integer> mouseClickCallback;
    private Consumer<Integer> mouseReleaseCallback;
    private Consumer<Double> scrollCallback;
    
    // Whitelist of keys that should repeat (editing/navigation keys)
    private static final Set<Integer> REPEATABLE_KEYS = new HashSet<>();
    static {
        REPEATABLE_KEYS.add(GLFW_KEY_BACKSPACE);
        REPEATABLE_KEYS.add(GLFW_KEY_DELETE);
        REPEATABLE_KEYS.add(GLFW_KEY_LEFT);
        REPEATABLE_KEYS.add(GLFW_KEY_RIGHT);
        REPEATABLE_KEYS.add(GLFW_KEY_UP);
        REPEATABLE_KEYS.add(GLFW_KEY_DOWN);
        REPEATABLE_KEYS.add(GLFW_KEY_HOME);
        REPEATABLE_KEYS.add(GLFW_KEY_END);
    }
    
    // Track modifier keys for character mapping
    private boolean shiftPressed;
    private boolean capsLockActive;
    
    // Synthetic character repeat fallback (disabled by default)
    private boolean forceCharRepeat = false;
    
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
        this.keyPressTimes = new HashMap<>();
        this.keyRepeating = new HashMap<>();
        this.charPressTimes = new HashMap<>();
        this.charRepeating = new HashMap<>();
        this.shiftPressed = false;
        this.capsLockActive = false;
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
                
                // Track modifier keys
                if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT) {
                    shiftPressed = (action != GLFW_RELEASE);
                }
                if (key == GLFW_KEY_CAPS_LOCK && action == GLFW_PRESS) {
                    capsLockActive = !capsLockActive;
                }
                
                // Fire key event callback (new API)
                if (keyEventCallback != null) {
                    if (action == GLFW_PRESS) {
                        keyEventCallback.accept(key, KeyAction.PRESS);
                    } else if (action == GLFW_RELEASE) {
                        keyEventCallback.accept(key, KeyAction.RELEASE);
                    }
                }
                
                // Fire key press callback (backward compatibility)
                // Only invoke if keyEventCallback is not set to prevent double-dispatch
                if (action == GLFW_PRESS && keyPressCallback != null && keyEventCallback == null) {
                    keyPressCallback.accept(key);
                }
                
                // Clean up key repeat state on release
                if (action == GLFW_RELEASE) {
                    keyPressTimes.remove(key);
                    keyRepeating.remove(key);
                    // Also clear printable char repeat state
                    charPressTimes.remove(key);
                    charRepeating.remove(key);
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
            
            // Fire scroll callback for UI overlays
            if (scrollCallback != null) {
                scrollCallback.accept(yoffset);
            }
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
     * Updates mouse delta calculations and key repeat logic.
     * Call this once per frame before processing input.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public void update(float deltaTime) {
        // Update mouse deltas
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
        
        // Update key repeat logic
        for (int key = 0; key < keys.length; key++) {
            if (keys[key]) {
                // Only process repeat for whitelisted keys
                if (!REPEATABLE_KEYS.contains(key)) {
                    continue;
                }
                
                // Key is currently pressed
                float pressTime = keyPressTimes.getOrDefault(key, 0.0f);
                pressTime += deltaTime;
                keyPressTimes.put(key, pressTime);
                
                // Check if we should start repeating or fire a repeat event
                if (pressTime >= KEY_REPEAT_INITIAL_DELAY) {
                    boolean wasRepeating = keyRepeating.getOrDefault(key, false);
                    
                    if (!wasRepeating) {
                        // Just crossed the initial delay threshold - start repeating
                        keyRepeating.put(key, true);
                        
                        // Fire repeat event
                        if (keyEventCallback != null) {
                            keyEventCallback.accept(key, KeyAction.REPEAT);
                        }
                        // Only invoke legacy callback if new API is not set
                        if (keyPressCallback != null && keyEventCallback == null) {
                            keyPressCallback.accept(key);
                        }
                        
                        // Generate character repeat for printable keys
                        generateCharacterRepeat(key);
                    } else {
                        // Already repeating - check if we should fire another repeat
                        float timeSinceInitial = pressTime - KEY_REPEAT_INITIAL_DELAY;
                        
                        // Calculate how many intervals have passed
                        int currentInterval = (int)(timeSinceInitial / KEY_REPEAT_INTERVAL);
                        int lastInterval = (int)((pressTime - deltaTime - KEY_REPEAT_INITIAL_DELAY) / KEY_REPEAT_INTERVAL);
                        
                        if (currentInterval > lastInterval) {
                            // Fire repeat event
                            if (keyEventCallback != null) {
                                keyEventCallback.accept(key, KeyAction.REPEAT);
                            }
                            // Only invoke legacy callback if new API is not set
                            if (keyPressCallback != null && keyEventCallback == null) {
                                keyPressCallback.accept(key);
                            }
                            
                            // Generate character repeat for printable keys
                            generateCharacterRepeat(key);
                        }
                    }
                }
            }
        }
        
        // Synthetic printable character repeat (independent from key-event repeat)
        // Only active when forceCharRepeat is enabled
        if (forceCharRepeat && charInputCallback != null) {
            for (int key = GLFW_KEY_SPACE; key < keys.length; key++) {
                if (!keys[key]) {
                    // Not pressed: ensure state cleared (also handled on release)
                    continue;
                }
                
                // Map to a printable character using existing mapKeyToCharacter(key)
                Character ch = mapKeyToCharacter(key);
                if (ch == null) {
                    continue; // skip non-printables and modifiers
                }
                
                float t = charPressTimes.getOrDefault(key, 0.0f) + deltaTime;
                charPressTimes.put(key, t);
                
                if (t >= KEY_REPEAT_INITIAL_DELAY) {
                    boolean started = charRepeating.getOrDefault(key, false);
                    if (!started) {
                        charRepeating.put(key, true);
                        charInputCallback.accept(ch); // first repeat after delay
                    } else {
                        float since = t - KEY_REPEAT_INITIAL_DELAY;
                        int curr = (int) (since / KEY_REPEAT_INTERVAL);
                        int prev = (int) ((t - deltaTime - KEY_REPEAT_INITIAL_DELAY) / KEY_REPEAT_INTERVAL);
                        if (curr > prev) {
                            charInputCallback.accept(ch);
                        }
                    }
                }
            }
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
     * Generates character repeat for printable keys during repeat events.
     * This ensures text fields get repeated characters for editing keys.
     * If forceCharRepeat is enabled, synthesizes character events for printable keys.
     * 
     * @param key GLFW key code
     */
    private void generateCharacterRepeat(int key) {
        if (charInputCallback == null || !forceCharRepeat) {
            return;
        }
        
        // Map key to character using US layout with shift/caps modifiers
        Character ch = mapKeyToCharacter(key);
        if (ch != null) {
            charInputCallback.accept(ch);
        }
    }
    
    /**
     * Maps a GLFW key code to a character using US keyboard layout.
     * Takes into account Shift and Caps Lock modifiers.
     * 
     * @param key GLFW key code
     * @return Mapped character, or null if key is not printable
     */
    private Character mapKeyToCharacter(int key) {
        // Letters A-Z
        if (key >= GLFW_KEY_A && key <= GLFW_KEY_Z) {
            char base = (char)('a' + (key - GLFW_KEY_A));
            // Uppercase if Shift XOR Caps Lock
            if (shiftPressed ^ capsLockActive) {
                return Character.toUpperCase(base);
            }
            return base;
        }
        
        // Numbers and their shift variants
        if (!shiftPressed) {
            switch (key) {
                case GLFW_KEY_0: return '0';
                case GLFW_KEY_1: return '1';
                case GLFW_KEY_2: return '2';
                case GLFW_KEY_3: return '3';
                case GLFW_KEY_4: return '4';
                case GLFW_KEY_5: return '5';
                case GLFW_KEY_6: return '6';
                case GLFW_KEY_7: return '7';
                case GLFW_KEY_8: return '8';
                case GLFW_KEY_9: return '9';
            }
        } else {
            switch (key) {
                case GLFW_KEY_0: return ')';
                case GLFW_KEY_1: return '!';
                case GLFW_KEY_2: return '@';
                case GLFW_KEY_3: return '#';
                case GLFW_KEY_4: return '$';
                case GLFW_KEY_5: return '%';
                case GLFW_KEY_6: return '^';
                case GLFW_KEY_7: return '&';
                case GLFW_KEY_8: return '*';
                case GLFW_KEY_9: return '(';
            }
        }
        
        // Punctuation and symbols
        if (!shiftPressed) {
            switch (key) {
                case GLFW_KEY_SPACE: return ' ';
                case GLFW_KEY_MINUS: return '-';
                case GLFW_KEY_EQUAL: return '=';
                case GLFW_KEY_LEFT_BRACKET: return '[';
                case GLFW_KEY_RIGHT_BRACKET: return ']';
                case GLFW_KEY_BACKSLASH: return '\\';
                case GLFW_KEY_SEMICOLON: return ';';
                case GLFW_KEY_APOSTROPHE: return '\'';
                case GLFW_KEY_COMMA: return ',';
                case GLFW_KEY_PERIOD: return '.';
                case GLFW_KEY_SLASH: return '/';
                case GLFW_KEY_GRAVE_ACCENT: return '`';
            }
        } else {
            switch (key) {
                case GLFW_KEY_SPACE: return ' ';
                case GLFW_KEY_MINUS: return '_';
                case GLFW_KEY_EQUAL: return '+';
                case GLFW_KEY_LEFT_BRACKET: return '{';
                case GLFW_KEY_RIGHT_BRACKET: return '}';
                case GLFW_KEY_BACKSLASH: return '|';
                case GLFW_KEY_SEMICOLON: return ':';
                case GLFW_KEY_APOSTROPHE: return '"';
                case GLFW_KEY_COMMA: return '<';
                case GLFW_KEY_PERIOD: return '>';
                case GLFW_KEY_SLASH: return '?';
                case GLFW_KEY_GRAVE_ACCENT: return '~';
            }
        }
        
        return null;
    }
    
    /**
     * Sets the key press callback (backward compatibility).
     * NOTE: If keyEventCallback is set, this callback will not be invoked
     * to prevent double-dispatch. Use keyEventCallback exclusively for new code.
     * 
     * @param callback Callback to invoke when a key is pressed
     */
    public void setKeyPressCallback(Consumer<Integer> callback) {
        this.keyPressCallback = callback;
    }
    
    /**
     * Sets the key event callback with action type.
     * This is the preferred API for handling key events with PRESS/REPEAT/RELEASE distinction.
     * 
     * @param callback Callback to invoke with key and action type
     */
    public void setKeyEventCallback(BiConsumer<Integer, KeyAction> callback) {
        this.keyEventCallback = callback;
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
    
    /**
     * Sets the scroll callback.
     * 
     * @param callback Callback to invoke when mouse wheel is scrolled
     */
    public void setScrollCallback(Consumer<Double> callback) {
        this.scrollCallback = callback;
    }
    
    /**
     * Enables or disables synthetic character repeat fallback.
     * When enabled, printable keys will generate character events during repeat.
     * This is useful on platforms where GLFW doesn't emit repeated char events.
     * 
     * @param enabled true to enable synthetic repeat, false to rely on GLFW
     */
    public void setForceCharRepeat(boolean enabled) {
        this.forceCharRepeat = enabled;
    }
    
    /**
     * Checks if synthetic character repeat is enabled.
     * 
     * @return true if synthetic repeat is enabled
     */
    public boolean isForceCharRepeatEnabled() {
        return forceCharRepeat;
    }
}
