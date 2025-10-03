package com.poorcraft.core;

import com.poorcraft.camera.Camera;
import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.input.InputHandler;
import com.poorcraft.modding.ModLoader;
import com.poorcraft.render.ChunkRenderer;
import com.poorcraft.ui.GameState;
import com.poorcraft.ui.UIManager;
import com.poorcraft.world.ChunkManager;
import com.poorcraft.world.World;
import com.poorcraft.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Main game class that orchestrates the game loop and all subsystems.
 * 
 * This is the heart of the engine. The conductor of the orchestra.
 * The big cheese. The head honcho. You get the idea.
 */
public class Game {
    
    private Window window;
    private Timer timer;
    private InputHandler inputHandler;
    private Camera camera;
    private Settings settings;
    private ConfigManager configManager;
    private UIManager uiManager;
    private World world;
    private ChunkManager chunkManager;
    private ChunkRenderer chunkRenderer;
    private ModLoader modLoader;
    
    private boolean running;
    private boolean worldLoaded;  // Track if world is loaded
    private boolean multiplayerMode;  // Flag indicating if game is in multiplayer mode
    
    private static final float FIXED_TIME_STEP = 1.0f / 60.0f;  // 60 updates per second
    
    /**
     * Creates a new game instance with the given settings.
     * 
     * @param settings Game settings
     * @param configManager Configuration manager
     */
    public Game(Settings settings, ConfigManager configManager) {
        this.settings = settings;
        this.configManager = configManager;
        this.running = false;
        this.worldLoaded = false;
        this.multiplayerMode = false;
    }
    
    /**
     * Initializes all game subsystems.
     * Creates window, input handler, camera, etc.
     */
    public void init() {
        System.out.println("[Game] Initializing...");
        
        // Create and initialize window
        window = new Window(
            settings.window.width,
            settings.window.height,
            settings.window.title,
            settings.window.vsync
        );
        window.create();
        
        // Initialize timer
        timer = new Timer();
        
        // Initialize input handler
        inputHandler = new InputHandler();
        inputHandler.init(window.getHandle());
        
        // Initialize camera at spawn height (Y=70, typical ground level)
        camera = new Camera(
            new Vector3f(0, 70, 0),
            settings.camera.moveSpeed,
            settings.controls.mouseSensitivity
        );
        
        // Initialize UI manager
        uiManager = new UIManager(this, settings, configManager);
        uiManager.init(window.getWidth(), window.getHeight());
        
        // Connect input handler to UI manager for cursor management
        uiManager.setInputHandler(inputHandler, window.getHandle());
        
        System.out.println("[Game] UI Manager initialized");
        
        // Initialize mod loader to enable Python mods
        modLoader = new ModLoader(this);
        modLoader.init();
        System.out.println("[Game] Mod loader initialized");
        
        // Set up input callbacks for UI
        inputHandler.setKeyPressCallback(key -> uiManager.onKeyPress(key, 0));
        inputHandler.setCharInputCallback(character -> uiManager.onCharInput(character));
        inputHandler.setMouseClickCallback(button -> {
            uiManager.onMouseClick((float)inputHandler.getMouseX(), (float)inputHandler.getMouseY(), button);
        });
        inputHandler.setMouseReleaseCallback(button -> {
            uiManager.onMouseRelease((float)inputHandler.getMouseX(), (float)inputHandler.getMouseY(), button);
        });
        
        // Don't create world yet - wait for player to click "Create World" in UI
        // World creation moved to createWorld() method
        
        running = true;
        
        System.out.println("[Game] Initialization complete!");
        System.out.println("[Game] Starting in main menu. Use UI to create a world.");
    }
    
    /**
     * Main game loop.
     * Handles timing, input, updates, and rendering.
     * 
     * This is where the magic happens. Or where everything breaks. One of the two.
     */
    public void run() {
        init();
        
        // Main game loop
        while (running && !window.shouldClose()) {
            // Update timing
            timer.update();
            float deltaTime = timer.getDeltaTimeFloat();
            
            // Update input
            inputHandler.update();
            
            // Forward mouse movement to UI manager
            uiManager.onMouseMove((float)inputHandler.getMouseX(), (float)inputHandler.getMouseY());
            
            // Update game state
            update(deltaTime);
            
            // Render frame
            render();
            
            // Update window (swap buffers, poll events)
            window.update();
        }
        
        cleanup();
    }
    
    /**
     * Updates game state based on input and delta time.
     * Handles camera movement with speed multipliers.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    private void update(float deltaTime) {
        // Update UI
        uiManager.update(deltaTime);
        
        // Only update gameplay if in IN_GAME state
        if (uiManager.getCurrentState() != GameState.IN_GAME) {
            return;
        }
        
        float speedMultiplier = getCurrentSpeedMultiplier();
        float adjustedDelta = deltaTime * speedMultiplier;
        
        // Process camera movement based on keybinds
        // Using safe getKeybind() to avoid NPE when config is missing keys
        // Because crashing on a missing keybind is so 2009
        if (inputHandler.isKeyPressed(settings.controls.getKeybind("forward", 87))) {  // W
            camera.processKeyboard(Camera.FORWARD, adjustedDelta);
        }
        if (inputHandler.isKeyPressed(settings.controls.getKeybind("backward", 83))) {  // S
            camera.processKeyboard(Camera.BACKWARD, adjustedDelta);
        }
        if (inputHandler.isKeyPressed(settings.controls.getKeybind("left", 65))) {  // A
            camera.processKeyboard(Camera.LEFT, adjustedDelta);
        }
        if (inputHandler.isKeyPressed(settings.controls.getKeybind("right", 68))) {  // D
            camera.processKeyboard(Camera.RIGHT, adjustedDelta);
        }
        if (inputHandler.isKeyPressed(settings.controls.getKeybind("jump", 32))) {  // Space
            camera.processKeyboard(Camera.UP, adjustedDelta);
        }
        if (inputHandler.isKeyPressed(settings.controls.getKeybind("sneak", 340))) {  // Left Shift
            camera.processKeyboard(Camera.DOWN, adjustedDelta);
        }
        
        // Process mouse movement (always in IN_GAME state)
        camera.processMouseMovement(
            (float) inputHandler.getMouseDeltaX(),
            (float) inputHandler.getMouseDeltaY()
        );
        
        // Update chunk manager with camera position
        // This handles dynamic chunk loading/unloading as player moves
        // In multiplayer mode, chunk loading is handled by network client, not ChunkManager
        if (worldLoaded && !multiplayerMode && chunkManager != null) {
            chunkManager.update(camera.getPosition());
        }
    }
    
    /**
     * Returns current movement speed multiplier based on input.
     * Sprint = faster, sneak = slower, normal = 1.0x
     * 
     * @return Speed multiplier
     */
    private float getCurrentSpeedMultiplier() {
        // Check sprint first (higher priority)
        if (inputHandler.isKeyPressed(settings.controls.getKeybind("sprint", 341))) {  // Left Control
            return settings.camera.sprintMultiplier;
        }
        
        // Check sneak
        if (inputHandler.isKeyPressed(settings.controls.getKeybind("sneak", 340))) {  // Left Shift
            return settings.camera.sneakMultiplier;
        }
        
        // Normal speed
        return 1.0f;
    }
    
    /**
     * Renders the current frame.
     * Renders all loaded chunks with frustum culling and lighting.
     */
    private void render() {
        // Clear color and depth buffers
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Render world if loaded and in appropriate state
        if (worldLoaded && (uiManager.getCurrentState() == GameState.IN_GAME || 
                           uiManager.getCurrentState() == GameState.PAUSED)) {
            // Get matrices from camera
            Matrix4f view = camera.getViewMatrix();
            Matrix4f projection = camera.getProjectionMatrix(
                settings.graphics.fov,
                (float) window.getWidth() / window.getHeight(),
                0.1f,   // Near plane
                1000.0f // Far plane
            );
            
            // Render all loaded chunks
            Collection<Chunk> loadedChunks = world.getLoadedChunks();
            chunkRenderer.render(loadedChunks, view, projection);
        }
        
        // Render UI on top
        uiManager.render();
    }
    
    /**
     * Cleans up resources and shuts down subsystems.
     */
    private void cleanup() {
        System.out.println("[Game] Cleaning up...");
        
        // Shutdown mod loader
        if (modLoader != null) {
            modLoader.shutdown();
            System.out.println("[Game] Mod loader shut down");
        }
        
        // Cleanup UI
        if (uiManager != null) {
            uiManager.cleanup();
        }
        
        // Cleanup chunk renderer
        if (chunkRenderer != null) {
            chunkRenderer.cleanup();
            System.out.println("[Game] Chunk renderer cleaned up");
        }
        
        // Shutdown world system
        if (chunkManager != null) {
            chunkManager.shutdown();
            System.out.println("[Game] World cleaned up");
        }
        
        window.destroy();
        System.out.println("[Game] Cleanup complete");
    }
    
    /**
     * Stops the game loop.
     * Game will shut down after current frame completes.
     */
    public void stop() {
        running = false;
    }
    
    /**
     * Gets the world instance.
     * Useful for future systems that need world access.
     * 
     * @return The game world
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * Gets the chunk manager instance.
     * Useful for debugging and monitoring chunk loading.
     * 
     * @return The chunk manager
     */
    public ChunkManager getChunkManager() {
        return chunkManager;
    }
    
    /**
     * Gets the chunk renderer instance.
     * Useful for debugging and monitoring rendering stats.
     * 
     * @return The chunk renderer
     */
    public ChunkRenderer getChunkRenderer() {
        return chunkRenderer;
    }
    
    /**
     * Creates a new world with the specified parameters.
     * Called from the UI when player clicks "Create World".
     * 
     * @param seed World seed (0 for random)
     * @param generateStructures Whether to generate structures
     */
    public void createWorld(long seed, boolean generateStructures) {
        System.out.println("[Game] Creating world with seed: " + seed);
        
        // Check if multiplayer mode
        if (multiplayerMode) {
            // In multiplayer, world comes from network client, skip local generation
            System.out.println("[Game] Multiplayer world loaded");
        } else {
            // Single-player: create local world
            // Initialize world
            world = new World(seed, generateStructures);
            
            // Initialize chunk manager
            chunkManager = new ChunkManager(
                world,
                settings.world.chunkLoadDistance,
                settings.world.chunkUnloadDistance
            );
            
            // Initial chunk load around spawn
            chunkManager.update(camera.getPosition());
            System.out.println("[Game] World initialized with seed: " + world.getSeed());
        }
        
        // Initialize chunk renderer if not already initialized
        if (chunkRenderer == null) {
            chunkRenderer = new ChunkRenderer();
            chunkRenderer.init();
            System.out.println("[Game] Chunk renderer initialized");
        }
        
        // Set up chunk unload callback
        if (world != null) {
            world.setChunkUnloadCallback(pos -> chunkRenderer.onChunkUnloaded(pos));
        }
        
        // Mark world as loaded
        worldLoaded = true;
        
        // Cursor grabbing is now handled by UIManager.setState() when transitioning to IN_GAME
        // No need to manually grab here anymore. One less thing to worry about!
        
        System.out.println("[Game] World creation complete!");
    }
    
    /**
     * Sets the world for multiplayer mode.
     * Called by UIManager when connecting to a server.
     * 
     * @param world Remote world from network client
     */
    public void setWorld(World world) {
        this.multiplayerMode = true;
        this.world = world;
        
        // Initialize chunk renderer if not already initialized
        if (chunkRenderer == null) {
            chunkRenderer = new ChunkRenderer();
            chunkRenderer.init();
            System.out.println("[Game] Chunk renderer initialized");
        }
        
        // Set up chunk unload callback
        world.setChunkUnloadCallback(pos -> chunkRenderer.onChunkUnloaded(pos));
        
        // Mark world as loaded
        worldLoaded = true;
        
        System.out.println("[Game] Remote world set for multiplayer");
    }
    
    /**
     * Checks if game is in multiplayer mode.
     * 
     * @return True if multiplayer mode
     */
    public boolean isMultiplayerMode() {
        return multiplayerMode;
    }
    
    /**
     * Gets the camera instance.
     * 
     * @return The camera
     */
    public Camera getCamera() {
        return camera;
    }
    
    /**
     * Gets the mod loader instance.
     * 
     * @return The mod loader
     */
    public ModLoader getModLoader() {
        return modLoader;
    }
}
