package com.poorcraft.core;

import com.poorcraft.camera.Camera;
import com.poorcraft.config.Settings;
import com.poorcraft.input.InputHandler;
import com.poorcraft.render.ChunkRenderer;
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
    private World world;
    private ChunkManager chunkManager;
    private ChunkRenderer chunkRenderer;
    
    private boolean running;
    private boolean escapePressed;  // Track ESC key state for toggle
    
    private static final float FIXED_TIME_STEP = 1.0f / 60.0f;  // 60 updates per second
    
    /**
     * Creates a new game instance with the given settings.
     * 
     * @param settings Game settings
     */
    public Game(Settings settings) {
        this.settings = settings;
        this.running = false;
        this.escapePressed = false;
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
        
        // Initialize world system
        world = new World(settings.world.seed, settings.world.generateStructures);
        chunkManager = new ChunkManager(
            world,
            settings.world.chunkLoadDistance,
            settings.world.chunkUnloadDistance
        );
        
        // Initial chunk load around spawn
        chunkManager.update(camera.getPosition());
        System.out.println("[Game] World initialized with seed: " + world.getSeed());
        
        // Initialize chunk renderer
        chunkRenderer = new ChunkRenderer();
        chunkRenderer.init();
        System.out.println("[Game] Chunk renderer initialized");
        
        // Set up chunk unload callback
        world.setChunkUnloadCallback(pos -> chunkRenderer.onChunkUnloaded(pos));
        
        running = true;
        
        System.out.println("[Game] Initialization complete!");
        System.out.println("[Game] Controls: WASD to move, Mouse to look, ESC to toggle cursor");
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
            
            // Handle ESC key for cursor toggle (with debounce)
            if (inputHandler.isKeyPressed(GLFW_KEY_ESCAPE)) {
                if (!escapePressed) {
                    // Toggle cursor grabbed state
                    inputHandler.setCursorGrabbed(
                        window.getHandle(),
                        !inputHandler.isCursorGrabbed()
                    );
                    escapePressed = true;
                }
            } else {
                escapePressed = false;
            }
            
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
        
        // Process mouse movement if cursor is grabbed
        if (inputHandler.isCursorGrabbed()) {
            camera.processMouseMovement(
                (float) inputHandler.getMouseDeltaX(),
                (float) inputHandler.getMouseDeltaY()
            );
        }
        
        // Update chunk manager with camera position
        // This handles dynamic chunk loading/unloading as player moves
        chunkManager.update(camera.getPosition());
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
    
    /**
     * Cleans up resources and shuts down subsystems.
     */
    private void cleanup() {
        System.out.println("[Game] Cleaning up...");
        
        // Cleanup chunk renderer
        chunkRenderer.cleanup();
        System.out.println("[Game] Chunk renderer cleaned up");
        
        // Shutdown world system
        chunkManager.shutdown();
        System.out.println("[Game] World cleaned up");
        
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
}
