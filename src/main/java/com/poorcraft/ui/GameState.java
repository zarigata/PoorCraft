package com.poorcraft.ui;

/**
 * Enum defining all possible game states.
 * 
 * The game state determines which screen is rendered and how input is handled.
 * This is a simple state machine - only one state is active at a time.
 * 
 * State transitions:
 * - MAIN_MENU -> WORLD_CREATION (Singleplayer button)
 * - MAIN_MENU -> MULTIPLAYER_MENU (Multiplayer button)
 * - MAIN_MENU -> SETTINGS_MENU (Settings button)
 * - WORLD_CREATION -> IN_GAME (Create World button)
 * - MULTIPLAYER_MENU -> CONNECTING (Join Server button)
 * - MULTIPLAYER_MENU -> HOSTING (Host Game button)
 * - CONNECTING -> IN_GAME (connection success)
 * - HOSTING -> IN_GAME (server started)
 * - CONNECTING/HOSTING -> MAIN_MENU (connection failed)
 * - IN_GAME -> PAUSED (ESC key)
 * - PAUSED -> IN_GAME (Resume button)
 * - PAUSED -> SETTINGS_MENU (Settings button)
 * - SETTINGS_MENU -> previous state (Apply/Cancel)
 * 
 * This is basically how every game works. Main menu, gameplay, pause menu.
 * Nothing fancy, just solid fundamentals.
 */
public enum GameState {
    
    /**
     * Main menu screen.
     * Shows Singleplayer, Multiplayer, Settings, and Quit buttons.
     */
    MAIN_MENU,
    
    /**
     * Settings configuration screen.
     * Allows changing graphics, audio, controls, and AI settings.
     */
    SETTINGS_MENU,
    
    /**
     * World creation/setup screen.
     * Configure seed, world name, and generation options.
     */
    WORLD_CREATION,
    
    /**
     * Multiplayer menu screen.
     * Shows server list, direct connect, and host game options.
     */
    MULTIPLAYER_MENU,
    
    /**
     * Connecting to server loading screen.
     * Shown while establishing connection and logging in.
     */
    CONNECTING,
    
    /**
     * Hosting integrated server loading screen.
     * Shown while starting server and connecting to it.
     */
    HOSTING,
    
    /**
     * Active gameplay state.
     * World is loaded, player can move around, chunks are rendering.
     */
    IN_GAME,
    
    /**
     * In-game pause menu.
     * Shown when ESC is pressed during gameplay.
     * World is still visible but gameplay is paused.
     */
    PAUSED,

    /**
     * Inventory management screen.
     * Opens over gameplay to manage items.
     */
    INVENTORY;
    
    /**
     * Checks if this state is an in-game state.
     * 
     * @return True if state is IN_GAME or PAUSED
     */
    public boolean isInGame() {
        return this == IN_GAME || this == PAUSED || this == INVENTORY;
    }
    
    /**
     * Checks if this state requires a loaded world.
     * 
     * @return True if state needs an active world
     */
    public boolean requiresWorldLoaded() {
        return this == IN_GAME || this == PAUSED || this == INVENTORY;
    }
    
    /**
     * Checks if this state should capture the mouse cursor.
     * 
     * @return True if state should grab cursor (only IN_GAME)
     */
    public boolean capturesMouse() {
        return this == IN_GAME;
    }
    
    /**
     * Checks if this state is a multiplayer-related state.
     * 
     * @return True if state is MULTIPLAYER_MENU, CONNECTING, or HOSTING
     */
    public boolean isMultiplayer() {
        return this == MULTIPLAYER_MENU || this == CONNECTING || this == HOSTING;
    }
    
    /**
     * Checks if this state is a loading state.
     * 
     * @return True if state is CONNECTING or HOSTING
     */
    public boolean isLoading() {
        return this == CONNECTING || this == HOSTING;
    }
}
