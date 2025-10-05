package com.poorcraft.core;

import com.poorcraft.camera.Camera;
import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.discord.DiscordRichPresenceManager;
import com.poorcraft.input.InputHandler;
import com.poorcraft.modding.ModLoader;
import com.poorcraft.inventory.Inventory;
import com.poorcraft.player.SkinManager;
import com.poorcraft.render.BlockHighlightRenderer;
import com.poorcraft.render.ChunkRenderer;
import com.poorcraft.render.ItemDropRenderer;
import com.poorcraft.render.SkyRenderer;
import com.poorcraft.render.SunLight;
import com.poorcraft.render.TextureGenerator;
import com.poorcraft.ui.GameState;
import com.poorcraft.ui.UIManager;
import com.poorcraft.world.ChunkManager;
import com.poorcraft.world.World;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.generation.BiomeType;
import com.poorcraft.world.entity.DropManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

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
    private PlayerController playerController;
    private MiningSystem miningSystem;
    private Inventory inventory;
    private Settings settings;
    private ConfigManager configManager;
    private UIManager uiManager;
    private World world;
    private ChunkManager chunkManager;
    private ChunkRenderer chunkRenderer;
    private SkyRenderer skyRenderer;
    private BlockHighlightRenderer blockHighlightRenderer;
    private ItemDropRenderer itemDropRenderer;
    private DropManager dropManager;
    private ModLoader modLoader;
    private DiscordRichPresenceManager discordRPC;
    private SkinManager skinManager;
    private GameMode currentGameMode;
    private boolean highlightRendererInitialized;
    private SunLight sunLight;
    private float timeOfDay;
    private static final float DAY_LENGTH_SECONDS = 600.0f;
    
    private boolean running;
    private boolean worldLoaded;  // Track if world is loaded
    private boolean multiplayerMode;  // Flag indicating if game is in multiplayer mode
    private GameState lastGameState;  // Track last game state for Discord presence updates
    private BiomeType lastBiome;  // Track last biome for Discord presence updates
    private float discordUpdateTimer;  // Timer for periodic Discord updates
    private int selectedHotbarSlot;
    private double hotbarScrollRemainder;
    
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
        this.lastGameState = null;
        this.lastBiome = null;
        this.discordUpdateTimer = 0.0f;
        this.currentGameMode = GameMode.SURVIVAL;
        this.miningSystem = new MiningSystem();
        this.inventory = new Inventory();
        this.blockHighlightRenderer = new BlockHighlightRenderer();
        this.itemDropRenderer = new ItemDropRenderer();
        this.dropManager = new DropManager();
        this.miningSystem.setDropManager(this.dropManager);
        this.highlightRendererInitialized = false;
        this.selectedHotbarSlot = 0;
        this.hotbarScrollRemainder = 0.0;
        this.sunLight = new SunLight();
        this.timeOfDay = 0.25f; // Start early morning
    }

    public SkinManager getSkinManager() {
        return skinManager;
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
        Vector3f initialPosition = new Vector3f(0.5f, 70.0f, 0.5f);
        camera = new Camera(
            new Vector3f(initialPosition),
            settings.camera.moveSpeed,
            settings.controls.mouseSensitivity
        );
        playerController = new PlayerController(initialPosition);
        camera.setPosition(playerController.getEyePosition());
        
        // Initialize UI manager
        uiManager = new UIManager(this, settings, configManager);
        uiManager.init(window.getWidth(), window.getHeight());
        // Connect input handler to UI manager for cursor management
        uiManager.setInputHandler(inputHandler, window.getHandle());
        
        // Connect window resize events to UI manager
        window.setResizeCallback((width, height) -> {
            System.out.println("[Game] Window resized to: " + width + "x" + height);
            uiManager.onResize(width, height);
        });
        
        System.out.println("[Game] UI Manager initialized");
        
        // Initialize mod loader to enable Python mods
        modLoader = new ModLoader(this);
        modLoader.init();
        System.out.println("[Game] Mod loader initialized");

        skinManager = SkinManager.getInstance();
        skinManager.init(settings);
        System.out.println("[Game] Skin manager initialized with " + skinManager.getAllSkins().size() + " skins");

        skyRenderer = new SkyRenderer();
        skyRenderer.init();

        // Generate baseline textures before mods start tinkering with them
        Map<String, ByteBuffer> generatedTextures = TextureGenerator.ensureDefaultBlockTextures();
        TextureGenerator.ensureAuxiliaryTextures();

        // Share generated textures with ModAPI so chunk renderer can combine them with mod assets
        if (modLoader.getModAPI() != null) {
            generatedTextures.forEach((name, buffer) -> {
                if (buffer != null) {
                    byte[] rgba = new byte[buffer.remaining()];
                    buffer.duplicate().get(rgba);
                    modLoader.getModAPI().addProceduralTexture(name, rgba);
                }
            });
        }

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
        
        // Initialize Discord Rich Presence
        // This is where we tell Discord we're cool enough to have Rich Presence
        // Spoiler: we're not, but Discord doesn't need to know that
        discordRPC = new DiscordRichPresenceManager();
        if (discordRPC.init()) {
            System.out.println("[Game] Discord Rich Presence enabled");
            discordRPC.updateMainMenu();
        } else {
            System.out.println("[Game] Discord Rich Presence disabled (Discord not running or init failed)");
        }
        
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

        timeOfDay = (timeOfDay + (deltaTime / DAY_LENGTH_SECONDS)) % 1.0f;
        updateSkyLighting(timeOfDay);

        if (skyRenderer != null) {
            skyRenderer.update(deltaTime);
        }

        // Update Discord Rich Presence callbacks
        // Discord needs regular updates or it'll think we froze
        // Kind of like a needy friend who needs constant attention
        if (discordRPC != null && discordRPC.isInitialized()) {
            discordRPC.runCallbacks();
            
            // Update Discord presence based on game state (but not every frame, that's overkill)
            discordUpdateTimer += deltaTime;
            if (discordUpdateTimer >= 2.0f) {  // Update every 2 seconds
                updateDiscordPresence();
                discordUpdateTimer = 0.0f;
            }
        }
        
        // Only update gameplay if in IN_GAME state
        if (uiManager.getCurrentState() != GameState.IN_GAME) {
            return;
        }
        
        // Process mouse movement (always in IN_GAME state)
        camera.processMouseMovement(
            (float) inputHandler.getMouseDeltaX(),
            (float) inputHandler.getMouseDeltaY()
        );

        handleHotbarScroll();

        if (playerController != null) {
            playerController.update(world, inputHandler, settings, camera, deltaTime);
            camera.setPosition(playerController.getEyePosition());
        }

        if (miningSystem != null) {
            miningSystem.update(world, camera, inputHandler, deltaTime);
        }

        if (dropManager != null && inventory != null && playerController != null) {
            dropManager.update(world, playerController.getPosition(), inventory, deltaTime);
        }
        
        // Update chunk manager with camera position
        // This handles dynamic chunk loading/unloading as player moves
        // In multiplayer mode, chunk loading is handled by network client, not ChunkManager
        if (worldLoaded && !multiplayerMode && chunkManager != null) {
            Vector3f trackingPosition = playerController != null
                ? playerController.getPosition()
                : camera.getPosition();
            chunkManager.update(trackingPosition);
        }
    }
    
    /**
     * Updates sky lighting based on time of day.
     * 
     * @param timeOfDay Time of day (0.0 = midnight, 0.5 = noon, 1.0 = midnight)
     */
    private void updateSkyLighting(float timeOfDay) {
        Vector3f focusPoint = camera != null ? camera.getPosition() : new Vector3f();
        float coverage = settings != null ? settings.world.chunkLoadDistance * Chunk.CHUNK_SIZE : 128f;
        sunLight.update(timeOfDay, focusPoint, coverage);
        if (chunkRenderer != null) {
            chunkRenderer.setSunLight(sunLight);
        }
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
            // Enable depth testing for 3D world rendering
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            
            // Get matrices from camera
            Matrix4f view = camera.getViewMatrix();
            Matrix4f projection = camera.getProjectionMatrix(
                settings.graphics.fov,
                (float) window.getWidth() / window.getHeight(),
                0.1f,   // Near plane
                1000.0f // Far plane
            );
            
            if (skyRenderer != null && sunLight != null) {
                float aspect = (float) window.getWidth() / window.getHeight();
                skyRenderer.render(camera, settings.graphics.fov, aspect, sunLight.getDirection());
            }

            // Render all loaded chunks
            Collection<Chunk> loadedChunks = world.getLoadedChunks();
            chunkRenderer.render(loadedChunks, view, projection);

            if (highlightRendererInitialized && miningSystem != null) {
                miningSystem.getAimedTarget().ifPresent(target ->
                    blockHighlightRenderer.render(target, miningSystem.getBreakProgress(), view, projection)
                );
            }

            if (itemDropRenderer != null && dropManager != null) {
                itemDropRenderer.render(dropManager.getDrops(), camera, view, projection);
            }
        }
        
        // Reset OpenGL state before UI rendering
        // This is crucial! The 3D world might leave weird state that breaks 2D UI
        // I learned this the hard way after spending 2 hours debugging invisible menus
        glDisable(GL_DEPTH_TEST);  // UI doesn't need depth testing
        glDisable(GL_CULL_FACE);   // Make sure back-face culling is off
        glEnable(GL_BLEND);        // Need blending for transparency
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Render UI on top (UIRenderer.begin() will set up the rest)
        uiManager.render();
    }
    
    /**
     * Cleans up resources and shuts down subsystems.
     */
    private void cleanup() {
        System.out.println("[Game] Cleaning up...");
        
        // Shutdown Discord Rich Presence
        // Important: Do this first so Discord knows we're leaving
        // Otherwise your friends will think you rage quit
        if (discordRPC != null) {
            discordRPC.shutdown();
        }

        if (skinManager != null) {
            skinManager.cleanup();
            System.out.println("[Game] Skin manager cleaned up");
        }
        
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

        if (skyRenderer != null) {
            skyRenderer.cleanup();
        }
        
        // Shutdown world system
        if (chunkManager != null) {
            chunkManager.shutdown();
            System.out.println("[Game] World cleaned up");
        }

        if (highlightRendererInitialized && blockHighlightRenderer != null) {
            blockHighlightRenderer.cleanup();
            highlightRendererInitialized = false;
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
     * Exposes the mining system for HUD and other systems.
     *
     * @return active MiningSystem instance
     */
    public MiningSystem getMiningSystem() {
        return miningSystem;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public void selectHotbarSlot(int slot) {
        if (inventory == null) {
            selectedHotbarSlot = 0;
            return;
        }
        int normalized = ((slot % 16) + 16) % 16;
        selectedHotbarSlot = normalized;
    }

    private void handleHotbarScroll() {
        double scrollDelta = inputHandler.consumeScrollOffset();
        if (scrollDelta == 0.0) {
            return;
        }
        hotbarScrollRemainder += scrollDelta;
        int steps = (int) hotbarScrollRemainder;
        if (steps != 0) {
            selectHotbarSlot(selectedHotbarSlot - steps);
            hotbarScrollRemainder -= steps;
        }
    }

    
    /**
     * Creates a new world with the specified parameters.
     * Called from the UI when player clicks "Create World".
     * 
     * @param seed World seed (0 for random)
     * @param generateStructures Whether to generate structures
     * @param gameMode Game mode (SURVIVAL, CREATIVE, ADVENTURE)
     */
    public void createWorld(long seed, boolean generateStructures, GameMode gameMode) {
        System.out.println("[Game] Creating world with seed: " + seed);
        GameMode mode = gameMode != null ? gameMode : GameMode.SURVIVAL;
        currentGameMode = mode;

        if (!multiplayerMode) {
            world = new World(seed, generateStructures);
            world.setEventBus(modLoader.getEventBus());
            chunkManager = new ChunkManager(
                world,
                settings.world.chunkLoadDistance,
                settings.world.chunkUnloadDistance
            );
            chunkManager.update(camera.getPosition());
            System.out.println("[Game] World initialized with seed: " + world.getSeed());
        } else if (world == null) {
            System.err.println("[Game] Warning: multiplayer world not provided before createWorld call");
            return;
        }

        if (chunkRenderer == null) {
            chunkRenderer = new ChunkRenderer();
            chunkRenderer.setModLoader(modLoader);
            chunkRenderer.setSettings(settings);
            chunkRenderer.setSunLight(sunLight);
            chunkRenderer.init();
            System.out.println("[Game] Chunk renderer initialized");
        } else {
            chunkRenderer.setSettings(settings);
            chunkRenderer.setSunLight(sunLight);
        }

        if (world != null) {
            world.setChunkUnloadCallback(pos -> chunkRenderer.onChunkUnloaded(pos));
        }

        updateSkyLighting(0.0f);

        itemDropRenderer.init();
        itemDropRenderer.setTextureAtlas(chunkRenderer.getTextureAtlas());
        dropManager.clear();

        ensureHighlightRendererInitialized();

        if (miningSystem != null) {
            miningSystem.reset();
        }

        worldLoaded = true;

        if (playerController != null) {
            playerController.respawn(world);
            playerController.setGameMode(currentGameMode);
            camera.setPosition(playerController.getEyePosition());
        }

        System.out.println("[Game] World creation complete!");
    }

    public SunLight getSunLight() {
        return sunLight;
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
        this.currentGameMode = GameMode.SURVIVAL;

        // Set EventBus on world so mods can receive world/block/chunk events
        // Even in multiplayer, mods need to know about world changes
        world.setEventBus(modLoader.getEventBus());
        ensureHighlightRendererInitialized();
        if (miningSystem != null) {
            miningSystem.reset();
        }

        if (chunkRenderer == null) {
            chunkRenderer = new ChunkRenderer();
            chunkRenderer.setModLoader(modLoader);
            chunkRenderer.setSettings(settings);
            chunkRenderer.setSunLight(sunLight);
            chunkRenderer.init();
            System.out.println("[Game] Chunk renderer initialized");
        } else {
            chunkRenderer.setSettings(settings);
            chunkRenderer.setSunLight(sunLight);
        }

        if (world != null) {
            world.setChunkUnloadCallback(pos -> chunkRenderer.onChunkUnloaded(pos));
        }

        updateSkyLighting(0.0f);

        itemDropRenderer.init();
        itemDropRenderer.setTextureAtlas(chunkRenderer.getTextureAtlas());
        dropManager.clear();

        ensureHighlightRendererInitialized();

        if (miningSystem != null) {
            miningSystem.reset();
        }

        worldLoaded = true;

        System.out.println("[Game] Remote world set for multiplayer");
    }

    /**
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
    
    /**
     * Updates Discord Rich Presence based on current game state.
     * Called periodically from the update loop.
     * 
     * This is where the magic happens - we tell Discord what we're doing.
     * Or what we want Discord to think we're doing. Same thing really.
     */
    private void updateDiscordPresence() {
        if (discordRPC == null || !discordRPC.isInitialized()) {
            return;
        }
        
        GameState currentState = uiManager.getCurrentState();
        
        // Check if state changed
        boolean stateChanged = (lastGameState != currentState);
        lastGameState = currentState;
        
        switch (currentState) {
            case MAIN_MENU:
                if (stateChanged) {
                    discordRPC.updateMainMenu();
                }
                break;
                
            case WORLD_CREATION:
                if (stateChanged) {
                    discordRPC.updateCreatingWorld();
                }
                break;
                
            case SETTINGS_MENU:
            case MULTIPLAYER_MENU:
                if (stateChanged) {
                    discordRPC.updateMainMenu();
                }
                break;
                
            case CONNECTING:
            case HOSTING:
                if (stateChanged) {
                    discordRPC.updateConnectingMultiplayer("server");
                }
                break;
                
            case PAUSED:
                if (stateChanged) {
                    discordRPC.updatePaused();
                }
                break;

            case INVENTORY:
                if (stateChanged) {
                    discordRPC.updatePaused();
                }
                break;

            case SKIN_MANAGER:
            case SKIN_EDITOR:
                if (stateChanged) {
                    discordRPC.updatePaused();
                }
                break;

            case IN_GAME:
                // Update biome info if in game
                if (world != null && playerController != null) {
                    Vector3f pos = playerController.getPosition();
                    BiomeType currentBiome = world.getBiome((int)pos.x, (int)pos.z);
                    // Update if biome changed or state changed
                    if (stateChanged || currentBiome != lastBiome) {
                        discordRPC.updateInGame(currentBiome, multiplayerMode, world.getSeed());
                        lastBiome = currentBiome;
                    }
                }
                break;
        }
    }
    
    /**
     * Gets the Discord RPC manager.
     * 
     * @return Discord RPC manager instance
     */
    public DiscordRichPresenceManager getDiscordRPC() {
        return discordRPC;
    }

    private void ensureHighlightRendererInitialized() {
        if (!highlightRendererInitialized && blockHighlightRenderer != null) {
            blockHighlightRenderer.init();
            highlightRendererInitialized = true;
        }
    }
}
