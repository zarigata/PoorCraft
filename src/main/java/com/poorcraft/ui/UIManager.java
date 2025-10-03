package com.poorcraft.ui;

import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * UI manager that handles state transitions and screen rendering.
 * 
 * This is the central controller for all UI. It manages:
 * - Game state transitions (main menu -> world creation -> in-game, etc.)
 * - Screen lifecycle (initialization, rendering, input forwarding)
 * - UI rendering (2D renderer, font renderer)
 * - Input routing (keyboard, mouse)
 * 
 * The UIManager is created by Game and receives all input events.
 * It forwards events to the appropriate screen based on the current state.
 * 
 * This is basically an MVC controller. The screens are views, the game is the model,
 * and this is the glue that holds it all together. Classic architecture.
 */
public class UIManager {
    
    private Object game;  // Reference to game instance (will be cast as needed)
    private GameState currentState;
    private GameState previousState;
    
    private UIRenderer uiRenderer;
    private FontRenderer fontRenderer;
    
    private Map<GameState, UIScreen> screens;
    private UIScreen hudScreen;  // Special screen for in-game HUD
    
    private Settings settings;
    private ConfigManager configManager;
    
    // Input handling for cursor management
    private Object inputHandler;  // Will be set by Game
    private long windowHandle;    // GLFW window handle
    
    /**
     * Creates a new UI manager.
     * 
     * @param game Game instance
     * @param settings Game settings
     * @param configManager Configuration manager
     */
    public UIManager(Object game, Settings settings, ConfigManager configManager) {
        this.game = game;
        this.settings = settings;
        this.configManager = configManager;
        this.screens = new HashMap<>();
        this.currentState = GameState.MAIN_MENU;
        this.previousState = null;
        this.inputHandler = null;
        this.windowHandle = 0;
    }
    
    /**
     * Sets the input handler and window handle for cursor management.
     * This should be called by Game after creating the UIManager.
     * 
     * @param inputHandler InputHandler instance
     * @param windowHandle GLFW window handle
     */
    public void setInputHandler(Object inputHandler, long windowHandle) {
        this.inputHandler = inputHandler;
        this.windowHandle = windowHandle;
    }
    
    /**
     * Initializes the UI manager.
     * Creates renderers, loads fonts, and initializes all screens.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     */
    public void init(int windowWidth, int windowHeight) {
        System.out.println("[UIManager] Initializing UI system...");
        
        // Initialize renderers
        uiRenderer = new UIRenderer();
        uiRenderer.init(windowWidth, windowHeight);
        
        fontRenderer = new FontRenderer(uiRenderer, 18);
        try {
            // Try to load default font
            fontRenderer.init("/fonts/default.ttf");
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to load default font, using fallback: " + e.getMessage());
            // Font renderer will handle fallback internally
        }
        
        // Create all screens
        System.out.println("[UIManager] Creating UI screens...");
        
        // Main menu
        MainMenuScreen mainMenuScreen = new MainMenuScreen(windowWidth, windowHeight, this);
        screens.put(GameState.MAIN_MENU, mainMenuScreen);
        
        // Settings menu
        SettingsScreen settingsScreen = new SettingsScreen(windowWidth, windowHeight, 
            this, settings, configManager);
        screens.put(GameState.SETTINGS_MENU, settingsScreen);
        
        // World creation
        WorldCreationScreen worldCreationScreen = new WorldCreationScreen(windowWidth, windowHeight, this);
        screens.put(GameState.WORLD_CREATION, worldCreationScreen);
        
        // Pause menu
        PauseScreen pauseScreen = new PauseScreen(windowWidth, windowHeight, this);
        screens.put(GameState.PAUSED, pauseScreen);
        
        // HUD
        hudScreen = new HUD(windowWidth, windowHeight, game);
        
        System.out.println("[UIManager] Created " + screens.size() + " screens");
        
        // Set initial state
        setState(GameState.MAIN_MENU);
        
        System.out.println("[UIManager] UI system initialized");
    }
    
    /**
     * Sets the current game state and transitions to the appropriate screen.
     * Also manages cursor grabbing based on the state.
     * 
     * @param newState New game state
     */
    public void setState(GameState newState) {
        System.out.println("[UIManager] State transition: " + currentState + " -> " + newState);
        
        previousState = currentState;
        currentState = newState;
        
        // Get screen for new state
        UIScreen screen = screens.get(newState);
        if (screen != null) {
            screen.init();
        }
        
        // Handle cursor grabbing based on state
        // IN_GAME captures mouse, all menu states release it
        if (inputHandler != null && windowHandle != 0) {
            try {
                var inputHandlerClass = inputHandler.getClass();
                var method = inputHandlerClass.getMethod("setCursorGrabbed", long.class, boolean.class);
                
                if (newState.capturesMouse()) {
                    // Grab cursor for in-game state
                    method.invoke(inputHandler, windowHandle, true);
                    System.out.println("[UIManager] Cursor grabbed for gameplay");
                } else {
                    // Release cursor for menu states
                    method.invoke(inputHandler, windowHandle, false);
                    System.out.println("[UIManager] Cursor released for menu");
                }
            } catch (Exception e) {
                System.err.println("[UIManager] Failed to set cursor grabbed state: " + e.getMessage());
                // Not fatal, just log it. The game can still run, just with wonky cursor behavior.
            }
        }
    }
    
    /**
     * Renders the current screen and HUD.
     */
    public void render() {
        uiRenderer.begin();
        
        if (currentState == GameState.IN_GAME) {
            // Render HUD only
            if (hudScreen != null) {
                hudScreen.render(uiRenderer, fontRenderer);
            }
        } else if (currentState == GameState.PAUSED) {
            // Render HUD + pause screen
            if (hudScreen != null) {
                hudScreen.render(uiRenderer, fontRenderer);
            }
            UIScreen pauseScreen = screens.get(GameState.PAUSED);
            if (pauseScreen != null) {
                pauseScreen.render(uiRenderer, fontRenderer);
            }
        } else {
            // Render menu screen
            UIScreen screen = screens.get(currentState);
            if (screen != null) {
                screen.render(uiRenderer, fontRenderer);
            }
        }
        
        uiRenderer.end();
    }
    
    /**
     * Updates the current screen.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public void update(float deltaTime) {
        if (currentState == GameState.IN_GAME || currentState == GameState.PAUSED) {
            if (hudScreen != null) {
                hudScreen.update(deltaTime);
            }
        }
        
        UIScreen screen = screens.get(currentState);
        if (screen != null) {
            screen.update(deltaTime);
        }
    }
    
    /**
     * Forwards mouse move event to current screen.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    public void onMouseMove(float mouseX, float mouseY) {
        if (!currentState.capturesMouse()) {
            UIScreen screen = screens.get(currentState);
            if (screen != null) {
                screen.onMouseMove(mouseX, mouseY);
            }
        }
    }
    
    /**
     * Forwards mouse click event to current screen.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     */
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (!currentState.capturesMouse()) {
            UIScreen screen = screens.get(currentState);
            if (screen != null) {
                screen.onMouseClick(mouseX, mouseY, button);
            }
        }
    }
    
    /**
     * Forwards mouse release event to current screen.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     */
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        if (!currentState.capturesMouse()) {
            UIScreen screen = screens.get(currentState);
            if (screen != null) {
                screen.onMouseRelease(mouseX, mouseY, button);
            }
        }
    }
    
    /**
     * Forwards key press event to current screen.
     * Also handles global hotkeys like ESC and F3.
     * 
     * @param key GLFW key code
     * @param mods Key modifiers
     */
    public void onKeyPress(int key, int mods) {
        // Handle global hotkeys
        if (key == GLFW_KEY_F3 && (currentState == GameState.IN_GAME || currentState == GameState.PAUSED)) {
            // Toggle debug info
            if (hudScreen instanceof HUD) {
                ((HUD) hudScreen).toggleDebug();
            }
        } else if (key == GLFW_KEY_ESCAPE) {
            // Handle ESC key
            if (currentState == GameState.IN_GAME) {
                setState(GameState.PAUSED);
            } else if (currentState == GameState.PAUSED) {
                setState(GameState.IN_GAME);
            } else if (currentState == GameState.SETTINGS_MENU) {
                // Return to previous state
                if (previousState != null) {
                    setState(previousState);
                } else {
                    setState(GameState.MAIN_MENU);
                }
            }
        } else {
            // Forward to current screen
            UIScreen screen = screens.get(currentState);
            if (screen != null) {
                screen.onKeyPress(key, mods);
            }
        }
    }
    
    /**
     * Forwards character input to current screen.
     * 
     * @param character Typed character
     */
    public void onCharInput(char character) {
        UIScreen screen = screens.get(currentState);
        if (screen != null) {
            screen.onCharInput(character);
        }
    }
    
    /**
     * Called when window is resized.
     * Updates renderer projection and notifies all screens.
     * 
     * @param width New window width
     * @param height New window height
     */
    public void onResize(int width, int height) {
        uiRenderer.updateProjection(width, height);
        
        for (UIScreen screen : screens.values()) {
            screen.onResize(width, height);
        }
        
        if (hudScreen != null) {
            hudScreen.onResize(width, height);
        }
    }
    
    /**
     * Creates a new world and transitions to in-game state.
     * This is called from the world creation screen.
     * 
     * @param seed World seed
     * @param generateStructures Whether to generate structures
     */
    public void createWorld(long seed, boolean generateStructures) {
        System.out.println("[UIManager] Creating world with seed: " + seed);
        
        // Call game method to create world
        // This will be implemented when we integrate with Game.java
        try {
            var gameClass = game.getClass();
            var method = gameClass.getMethod("createWorld", long.class, boolean.class);
            method.invoke(game, seed, generateStructures);
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to create world: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Transition to in-game state
        setState(GameState.IN_GAME);
    }
    
    /**
     * Quits the game.
     * This is called from the main menu quit button.
     */
    public void quit() {
        System.out.println("[UIManager] Quitting game");
        
        // Call game stop method
        try {
            var gameClass = game.getClass();
            var method = gameClass.getMethod("stop");
            method.invoke(game);
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to stop game: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the current game state.
     * 
     * @return Current state
     */
    public GameState getCurrentState() {
        return currentState;
    }
    
    /**
     * Gets the previous game state.
     * 
     * @return Previous state
     */
    public GameState getPreviousState() {
        return previousState;
    }
    
    /**
     * Gets the settings.
     * 
     * @return Game settings
     */
    public Settings getSettings() {
        return settings;
    }
    
    /**
     * Gets the config manager.
     * 
     * @return Config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Registers a screen for a specific state.
     * 
     * @param state Game state
     * @param screen UI screen
     */
    public void registerScreen(GameState state, UIScreen screen) {
        screens.put(state, screen);
    }
    
    /**
     * Sets the HUD screen.
     * 
     * @param hud HUD screen
     */
    public void setHUD(UIScreen hud) {
        this.hudScreen = hud;
    }
    
    /**
     * Cleans up UI resources.
     */
    public void cleanup() {
        System.out.println("[UIManager] Cleaning up UI system");
        
        if (uiRenderer != null) {
            uiRenderer.cleanup();
        }
        
        if (fontRenderer != null) {
            fontRenderer.cleanup();
        }
    }
}
