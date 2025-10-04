package com.poorcraft.ui;

import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.core.Game;
import com.poorcraft.core.GameMode;
import com.poorcraft.network.client.GameClient;
import com.poorcraft.network.server.GameServer;

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
    
    // Networking
    private GameClient gameClient;  // Network client (null when not connected)
    private GameServer gameServer;  // Integrated server (null when not hosting)
    private float uiScale;
    
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
        this.uiScale = 1.0f;
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
        
        // Initialize font renderer with Silkscreen font for that retro vibe
        // Base size 80 keeps text crisp; global scaling handles responsiveness
        fontRenderer = new FontRenderer(uiRenderer, 80);
        try {
            // Try to load Silkscreen font (the one we just added!)
            fontRenderer.init("src/main/resources/fonts/Silkscreen-Regular.ttf");
            System.out.println("[UIManager] Loaded Silkscreen font - looking retro!");
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to load Silkscreen font, using fallback: " + e.getMessage());
            // Font renderer will handle fallback internally (probably system font)
        }

        // Compute initial UI scale relative to window size
        uiScale = LayoutUtils.computeScale(windowWidth, windowHeight);
        fontRenderer.setGlobalScale(uiScale);
        
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
        
        // Multiplayer screens
        MultiplayerMenuScreen multiplayerMenuScreen = new MultiplayerMenuScreen(windowWidth, windowHeight, this);
        screens.put(GameState.MULTIPLAYER_MENU, multiplayerMenuScreen);
        ConnectingScreen connectingScreen = new ConnectingScreen(windowWidth, windowHeight, this);
        screens.put(GameState.CONNECTING, connectingScreen);
        
        HostingScreen hostingScreen = new HostingScreen(windowWidth, windowHeight, this);
        screens.put(GameState.HOSTING, hostingScreen);

        InventoryScreen inventoryScreen = new InventoryScreen(windowWidth, windowHeight, this);
        screens.put(GameState.INVENTORY, inventoryScreen);
        
        // HUD
        hudScreen = new HUD(windowWidth, windowHeight, game);

        applyGlobalScale(uiScale);

        if (hudScreen != null) {
            hudScreen.init();
        }

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
        } else if (currentState == GameState.INVENTORY) {
            if (hudScreen != null) {
                hudScreen.render(uiRenderer, fontRenderer);
            }
            UIScreen inventoryScreen = screens.get(GameState.INVENTORY);
            if (inventoryScreen != null) {
                inventoryScreen.render(uiRenderer, fontRenderer);
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
        // Update network client if connected
        if (gameClient != null && gameClient.isConnected()) {
            gameClient.tick();
            
            // Send player movement if in game
            if (currentState == GameState.IN_GAME) {
                try {
                    var gameClass = game.getClass();
                    var cameraMethod = gameClass.getMethod("getCamera");
                    var camera = cameraMethod.invoke(game);
                    
                    var cameraClass = camera.getClass();
                    var posMethod = cameraClass.getMethod("getPosition");
                    var yawMethod = cameraClass.getMethod("getYaw");
                    var pitchMethod = cameraClass.getMethod("getPitch");
                    
                    var pos = (org.joml.Vector3f) posMethod.invoke(camera);
                    float yaw = (float) yawMethod.invoke(camera);
                    float pitch = (float) pitchMethod.invoke(camera);
                    
                    gameClient.sendMovement(pos.x, pos.y, pos.z, yaw, pitch, false);
                } catch (Exception e) {
                    // Silently ignore - not critical
                }
            }
        }
        
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
        int inventoryKey = settings != null && settings.controls != null
            ? settings.controls.getKeybind("inventory", GLFW_KEY_E)
            : GLFW_KEY_E;

        // Handle global hotkeys
        if (key == GLFW_KEY_F3 && (currentState == GameState.IN_GAME || currentState == GameState.PAUSED)) {
            // Toggle debug info
            if (hudScreen instanceof HUD) {
                ((HUD) hudScreen).toggleDebug();
            }
        } else if (key == inventoryKey) {
            if (currentState == GameState.IN_GAME) {
                setState(GameState.INVENTORY);
                return;
            } else if (currentState == GameState.INVENTORY) {
                setState(GameState.IN_GAME);
                return;
            }
        } else if (key == GLFW_KEY_ESCAPE) {
            // Handle ESC key
            if (currentState == GameState.IN_GAME) {
                setState(GameState.PAUSED);
            } else if (currentState == GameState.PAUSED) {
                setState(GameState.IN_GAME);
            } else if (currentState == GameState.INVENTORY) {
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

        uiScale = LayoutUtils.computeScale(width, height);
        fontRenderer.setGlobalScale(uiScale);
        applyGlobalScale(uiScale);
        
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
    public void createWorld(long seed, boolean generateStructures, GameMode gameMode) {
        System.out.println("[UIManager] Creating world with seed: " + seed);
        
        // Call game method to create world
        // This will be implemented when we integrate with Game.java
        try {
            var gameClass = game.getClass();
            var method = gameClass.getMethod("createWorld", long.class, boolean.class, GameMode.class);
            method.invoke(game, seed, generateStructures, gameMode);
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

    private void applyGlobalScale(float scale) {
        for (UIScreen screen : screens.values()) {
            screen.setUIScale(scale);
        }
        if (hudScreen != null) {
            hudScreen.setUIScale(scale);
        }
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
     * Connects to a multiplayer server.
     * 
     * @param host Server hostname/IP
     * @param port Server port
     */
    public void connectToServer(String host, int port) {
        System.out.println("[UIManager] Connecting to " + host + ":" + port);
        
        // Create game client
        String username = settings.multiplayer.username;
        gameClient = new GameClient(host, port, username);
        
        // Set disconnect callback
        gameClient.setDisconnectCallback(reason -> onConnectionFailed(reason));
        
        // Transition to connecting state
        setState(GameState.CONNECTING);
        
        // Update status
        ConnectingScreen screen = (ConnectingScreen) screens.get(GameState.CONNECTING);
        if (screen != null) {
            screen.setStatus("Establishing connection...");
        }
        
        // Connect in background thread
        new Thread(() -> {
            try {
                gameClient.connect();
                // Wait a bit for login response
                Thread.sleep(1000);
                
                if (gameClient.isConnected() && gameClient.getRemoteWorld() != null) {
                    onConnectionSuccess();
                }
            } catch (Exception e) {
                onConnectionFailed("Connection failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Hosts an integrated server.
     * 
     * @param port Server port
     * @param seed World seed
     * @param generateStructures Generate structures flag
     */
    public void hostServer(int port, long seed, boolean generateStructures) {
        System.out.println("[UIManager] Hosting server on port " + port);
        
        // Create game server
        gameServer = new GameServer(port, settings);
        
        // Hook up the modding event bus to the integrated server
        // This ensures server-side events (block/player/chunk) reach mods
        // Without this, mods never get notified of server-side changes. Classic mistake!
        try {
            Game gameInstance = (Game) game;
            if (gameInstance.getModLoader() != null) {
                gameServer.setEventBus(gameInstance.getModLoader().getEventBus());
                System.out.println("[UIManager] EventBus hooked up to integrated server");
            }
        } catch (Exception e) {
            // Not fatal - server can still run without mod support
            System.err.println("[UIManager] Failed to hook up EventBus: " + e.getMessage());
        }
        
        // Transition to hosting state
        setState(GameState.HOSTING);
        
        // Update status
        HostingScreen screen = (HostingScreen) screens.get(GameState.HOSTING);
        if (screen != null) {
            screen.setStatus("Initializing server...");
        }
        
        // Start server in background thread
        new Thread(() -> {
            try {
                gameServer.start(seed, generateStructures);
                
                if (screen != null) {
                    screen.setStatus("Connecting to local server...");
                }
                
                // Connect client to localhost
                Thread.sleep(500);  // Give server time to start
                connectToServer("localhost", port);
                
            } catch (Exception e) {
                onHostingFailed("Server startup failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Called when connection succeeds.
     */
    private void onConnectionSuccess() {
        System.out.println("[UIManager] Connection successful");
        
        // Get remote world from client
        try {
            var world = gameClient.getRemoteWorld();
            
            // Pass remote world to game
            var gameClass = game.getClass();
            var method = gameClass.getMethod("setWorld", com.poorcraft.world.World.class);
            method.invoke(game, world);
            
            // Transition to in-game state
            setState(GameState.IN_GAME);
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to set remote world: " + e.getMessage());
            e.printStackTrace();
            onConnectionFailed("Failed to load world");
        }
    }
    
    /**
     * Called when connection fails.
     * 
     * @param reason Failure reason
     */
    private void onConnectionFailed(String reason) {
        System.err.println("[UIManager] Connection failed: " + reason);
        
        // Disconnect client if connected
        if (gameClient != null) {
            gameClient.disconnect();
            gameClient = null;
        }
        
        // Stop server if running
        if (gameServer != null) {
            gameServer.stop();
            gameServer = null;
        }
        
        // Return to multiplayer menu
        setState(GameState.MULTIPLAYER_MENU);
        
        // TODO: Show error dialog
        System.err.println("[UIManager] Error: " + reason);
    }
    
    /**
     * Called when hosting fails.
     * 
     * @param reason Failure reason
     */
    private void onHostingFailed(String reason) {
        System.err.println("[UIManager] Hosting failed: " + reason);
        
        // Stop server if running
        if (gameServer != null) {
            gameServer.stop();
            gameServer = null;
        }
        
        // Disconnect client if connected
        if (gameClient != null) {
            gameClient.disconnect();
            gameClient = null;
        }
        
        // Return to multiplayer menu
        setState(GameState.MULTIPLAYER_MENU);
        
        // TODO: Show error dialog
        System.err.println("[UIManager] Error: " + reason);
    }
    
    /**
     * Disconnects from server and stops integrated server if running.
     */
    public void disconnectFromServer() {
        if (gameClient != null && gameClient.isConnected()) {
            gameClient.disconnect();
            gameClient = null;
        }
        
        if (gameServer != null && gameServer.isRunning()) {
            gameServer.stop();
            gameServer = null;
        }
        
        setState(GameState.MAIN_MENU);
    }
    
    /**
     * Gets the game client.
     * 
     * @return Game client (may be null)
     */
    public GameClient getGameClient() {
        return gameClient;
    }
    
    /**
     * Gets the game server.
     * 
     * @return Game server (may be null)
     */
    public GameServer getGameServer() {
        return gameServer;
    }
    
    /**
     * Checks if in multiplayer mode.
     * 
     * @return True if connected to a server
     */
    public boolean isMultiplayer() {
        return gameClient != null && gameClient.isConnected();
    }
    
    /**
     * Checks if hosting a server.
     * 
     * @return True if server is running
     */
    public boolean isHosting() {
        return gameServer != null && gameServer.isRunning();
    }
    
    /**
     * Cleans up UI resources.
     */
    public void cleanup() {
        System.out.println("[UIManager] Cleaning up UI system");
        
        // Disconnect from server
        disconnectFromServer();
        
        if (uiRenderer != null) {
            uiRenderer.cleanup();
        }
        
        if (fontRenderer != null) {
            fontRenderer.cleanup();
        }
    }
}
