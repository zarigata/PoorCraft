package com.poorcraft.core;

import com.poorcraft.ai.AICompanionManager;
import com.poorcraft.camera.Camera;
import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.crafting.CraftingManager;
import com.poorcraft.crafting.RecipeRegistry;
import com.poorcraft.discord.DiscordRichPresenceManager;
import com.poorcraft.input.InputHandler;
import com.poorcraft.inventory.Inventory;
import com.poorcraft.modding.LuaModContainer;
import com.poorcraft.modding.LuaModLoader;
import com.poorcraft.player.SkinManager;
import com.poorcraft.render.BlurRenderer;
import com.poorcraft.render.BlockHighlightRenderer;
import com.poorcraft.render.BlockPreviewRenderer;
import com.poorcraft.render.ChunkRenderer;
import com.poorcraft.render.Frustum;
import com.poorcraft.render.GPUCapabilities;
import com.poorcraft.render.ItemDropRenderer;
import com.poorcraft.render.NPCRenderer;
import com.poorcraft.render.PerformanceMonitor;
import com.poorcraft.render.SkyRenderer;
import com.poorcraft.render.SunLight;
import com.poorcraft.render.TextureGenerator;
import com.poorcraft.ui.GameState;
import com.poorcraft.ui.UIManager;
import com.poorcraft.world.ChunkManager;
import com.poorcraft.world.LeafDecaySystem;
import com.poorcraft.world.TreeFellingSystem;
import com.poorcraft.world.World;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.generation.BiomeType;
import com.poorcraft.world.entity.DropManager;
import com.poorcraft.world.entity.NPCManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

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
    private BlockPreviewRenderer blockPreviewRenderer;
    private ItemDropRenderer itemDropRenderer;
    private NPCRenderer npcRenderer;
    private DropManager dropManager;
    private NPCManager npcManager;
    private AICompanionManager aiCompanionManager;
    private RecipeRegistry recipeRegistry;
    private CraftingManager craftingManager;
    private LuaModLoader modLoader;
    private DiscordRichPresenceManager discordRPC;
    private SkinManager skinManager;
    private GameMode currentGameMode;
    private boolean highlightRendererInitialized;
    private SunLight sunLight;
    private float timeOfDay;
    private static final float DAY_LENGTH_SECONDS = 600.0f;
    private boolean externalTimeControlEnabled;
    private GPUCapabilities gpuCapabilities;
    private PerformanceMonitor performanceMonitor;
    private boolean chunkRendererInitialized;
    private boolean chunkRendererUnavailableWarned;
    private TreeFellingSystem treeFellingSystem;
    private LeafDecaySystem leafDecaySystem;
    
    // Main thread task queue for async operations
    private final Queue<Runnable> mainThreadTasks;
    
    // Blur effect resources
    private BlurRenderer blurRenderer;
    private int blurFbo;
    private int blurTexture;
    private int blurFbo2;
    private int blurTexture2;
    private int blurWidth;
    private int blurHeight;
    private boolean blurSupported;
    
    private boolean running;
    private boolean worldLoaded;  // Track if world is loaded
    private boolean multiplayerMode;  // Flag indicating if game is in multiplayer mode
    private GameState lastGameState;  // Track last game state for Discord presence updates
    private BiomeType lastBiome;  // Track last biome for Discord presence updates
    private float discordUpdateTimer;  // Timer for periodic Discord updates
    private long lastBlurResizeTime = 0L;
    private int pendingBlurWidth = 0;
    private int pendingBlurHeight = 0;
    private boolean blurResizePending = false;
    private static final long BLUR_RESIZE_DEBOUNCE_MS = 150L;
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
        this.blockPreviewRenderer = new BlockPreviewRenderer();
        this.itemDropRenderer = new ItemDropRenderer();
        this.npcRenderer = new NPCRenderer();
        this.dropManager = new DropManager();
        this.npcManager = new NPCManager();
        this.aiCompanionManager = null;
        this.recipeRegistry = new RecipeRegistry();
        this.craftingManager = new CraftingManager(this.recipeRegistry);
        this.miningSystem.setDropManager(this.dropManager);
        this.highlightRendererInitialized = false;
        this.selectedHotbarSlot = 0;
        this.hotbarScrollRemainder = 0.0;
        this.sunLight = new SunLight();
        this.timeOfDay = 0.25f; // Start early morning
        this.externalTimeControlEnabled = false;
        this.performanceMonitor = null;
        this.gpuCapabilities = null;
        this.chunkRendererInitialized = false;
        this.chunkRendererUnavailableWarned = false;
        this.treeFellingSystem = null;
        this.leafDecaySystem = null;
        this.blurRenderer = null;
        this.blurFbo = 0;
        this.blurTexture = 0;
        this.blurFbo2 = 0;
        this.blurTexture2 = 0;
        this.blurSupported = false;
        this.mainThreadTasks = new ConcurrentLinkedQueue<>();
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public RecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    public UIManager getUIManager() {
        return uiManager;
    }
    
    /**
     * Initializes all game subsystems.
     * Creates window, input handler, camera, etc.
     */
    public void init() {
        System.out.println("[Game] Initializing...");
        
        // Create and initialize window
        try {
            window = new Window(
                settings.window.width,
                settings.window.height,
                settings.window.title,
                settings.window.vsync
            );
            window.create();
        } catch (Exception ex) {
            System.err.println("[Game] Failed to create window: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Unable to create game window", ex);
        }

        GPUCapabilities caps;
        try {
            caps = GPUCapabilities.detect();
        } catch (Exception ex) {
            System.err.println("[Game] GPU capability detection failed: " + ex.getMessage());
            ex.printStackTrace();
            caps = GPUCapabilities.createFallbackCapabilities();
        }
        this.gpuCapabilities = caps;

        PerformanceMonitor perfMon = new PerformanceMonitor();
        this.performanceMonitor = perfMon;

        chunkRenderer = new ChunkRenderer();
        chunkRenderer.setGPUCapabilities(caps);
        chunkRenderer.setPerformanceMonitor(perfMon);
        try {
            chunkRenderer.init();
            chunkRendererInitialized = true;
            chunkRendererUnavailableWarned = false;
            System.out.println("[Game] Chunk renderer initialized early for menu rendering");
        } catch (Exception ex) {
            chunkRendererInitialized = false;
            System.err.println("[Game] Chunk renderer failed to initialize: " + ex.getMessage());
            ex.printStackTrace();
        }

        if (chunkRendererInitialized) {
            try {
                blockPreviewRenderer.init();
                blockPreviewRenderer.setTextureAtlas(chunkRenderer.getTextureAtlas());
                System.out.println("[Game] Block preview renderer initialized");
            } catch (Exception ex) {
                System.err.println("[Game] Block preview renderer failed to initialize: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

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
        uiManager.setBlockPreviewRenderer(blockPreviewRenderer);
        
        // Connect window resize events to UI manager
        window.setResizeCallback((width, height) -> {
            System.out.println("[Game] Window resized to: " + width + "x" + height);
            uiManager.onResize(width, height);
            if (width < 1 || height < 1) {
                System.out.println("[Game] Ignoring blur resize with invalid dimensions: " + width + "x" + height);
                return;
            }
            pendingBlurWidth = width;
            pendingBlurHeight = height;
            blurResizePending = true;
            lastBlurResizeTime = System.currentTimeMillis();
        });
        
        System.out.println("[Game] UI Manager initialized");

        try {
            int recipeCount = recipeRegistry.loadDefaultRecipes();
            System.out.println("[Game] Loaded " + recipeCount + " crafting recipes");
        } catch (Exception ex) {
            System.err.println("[Game] Failed to load crafting recipes: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Initialize mod loader to enable Lua mods
        try {
            modLoader = new LuaModLoader(this);
            modLoader.init();
            System.out.println("[Game] Lua mod loader initialized with " + modLoader.getLoadedMods().size() + " mods");
        } catch (Exception e) {
            System.err.println("[Game] Failed to initialize mod loader: " + e.getMessage());
            e.printStackTrace();
            System.err.println("[Game] Continuing without mods...");
            modLoader = null;
        }

        if (settings != null && settings.ai != null) {
            aiCompanionManager = new AICompanionManager();
            aiCompanionManager.init(settings.ai, npcManager, this::postToMainThread);
        }

        if (modLoader != null) {
            List<LuaModContainer> mods = modLoader.getLoadedMods();
            int totalMods = mods.size();
            long enabledMods = mods.stream()
                .filter(m -> m.getState() == LuaModContainer.ModState.ENABLED)
                .count();
            long errorMods = mods.stream()
                .filter(m -> m.getState() == LuaModContainer.ModState.ERROR)
                .count();

            System.out.println("[Game] Mod summary: " + enabledMods + " enabled, " + errorMods + " failed, " + totalMods + " total");

            if (errorMods > 0) {
                System.err.println("[Game] Warning: Some mods failed to load. Check logs above for details.");
            }
        }

        skinManager = SkinManager.getInstance();
        try {
            skinManager.init(settings);
            System.out.println("[Game] Skin manager initialized with " + skinManager.getAllSkins().size() + " skins");
        } catch (Exception ex) {
            System.err.println("[Game] Failed to initialize skin manager: " + ex.getMessage());
            ex.printStackTrace();
            System.err.println("[Game] Continuing without skin manager...");
        }

        skyRenderer = new SkyRenderer();
        try {
            skyRenderer.init();
        } catch (Exception ex) {
            System.err.println("[Game] Failed to initialise sky renderer: " + ex.getMessage());
            ex.printStackTrace();
            skyRenderer = null;
        }

        // Initialize blur effect resources
        try {
            initBlurResources(window.getWidth(), window.getHeight());
        } catch (Exception ex) {
            System.err.println("[Game] Failed to initialise blur resources: " + ex.getMessage());
            ex.printStackTrace();
            blurSupported = false;
        }

        // Generate baseline textures before mods start tinkering with them
        Map<String, ByteBuffer> generatedTextures = Map.of();
        try {
            generatedTextures = TextureGenerator.ensureDefaultBlockTextures();
            TextureGenerator.ensureAuxiliaryTextures();
        } catch (Exception ex) {
            System.err.println("[Game] Texture generation failed: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Share generated textures with ModAPI so chunk renderer can combine them with mod assets
        if (modLoader != null && modLoader.getModAPI() != null) {
            generatedTextures.forEach((name, buffer) -> {
                if (buffer != null) {
                    byte[] rgba = new byte[buffer.remaining()];
                    buffer.duplicate().get(rgba);
                    modLoader.getModAPI().addProceduralTexture(name, rgba);
                }
            });
        }

        // Set up input callbacks for UI
        // Use only the new keyEventCallback API to avoid double-dispatch
        inputHandler.setKeyEventCallback((key, action) -> uiManager.onKeyEvent(key, action));
        inputHandler.setCharInputCallback(character -> uiManager.onCharInput(character));
        inputHandler.setMouseClickCallback(button -> {
            uiManager.onMouseClick((float)inputHandler.getMouseX(), (float)inputHandler.getMouseY(), button);
        });
        inputHandler.setMouseReleaseCallback(button -> {
            uiManager.onMouseRelease((float)inputHandler.getMouseX(), (float)inputHandler.getMouseY(), button);
        });
        inputHandler.setScrollCallback(yOffset -> {
            if (shouldForwardScrollToUI()) {
                inputHandler.consumeScrollOffset();
                uiManager.onScroll(yOffset);
            }
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
            inputHandler.update(deltaTime);
            
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
        // Process main thread tasks from async operations
        processMainThreadTasks();
        processBlurResizeDebounce();
        
        // Update UI
        uiManager.update(deltaTime);
        
        // Update time of day (unless external control is enabled)
        if (!externalTimeControlEnabled) {
            timeOfDay = (timeOfDay + (deltaTime / DAY_LENGTH_SECONDS)) % 1.0f;
        }
        updateSkyLighting(timeOfDay);
        
        // Update mods after time advancement so they can override time each frame
        if (modLoader != null) {
            modLoader.update(deltaTime);
        }
        
        // Track biome changes for mods
        if (worldLoaded && playerController != null && world != null && modLoader != null) {
            Vector3f pos = playerController.getPosition();
            BiomeType newBiome = world.getBiome((int)pos.x, (int)pos.z);
            
            if (lastBiome != newBiome && lastBiome != null) {
                // Biome changed, fire event
                if (modLoader.getEventBus() != null) {
                    modLoader.getEventBus().fire(new com.poorcraft.modding.events.BiomeChangeEvent(
                        0,  // Player ID (0 for local player)
                        lastBiome.toString(),
                        newBiome.toString(),
                        (int)pos.x,
                        (int)pos.z
                    ));
                }
            }
            
            lastBiome = newBiome;
        }

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
        boolean monitorActive = performanceMonitor != null;
        if (monitorActive) {
            performanceMonitor.update(deltaTime);
            performanceMonitor.beginZone("Update");
        }

        if (uiManager.getCurrentState() != GameState.IN_GAME) {
            if (monitorActive) {
                performanceMonitor.endZone();
            }
            return;
        }
        
        // Process mouse movement (only if input is not captured by UI)
        if (!uiManager.isInputCaptured()) {
            camera.processMouseMovement(
                (float) inputHandler.getMouseDeltaX(),
                (float) inputHandler.getMouseDeltaY()
            );
        }

        // Handle hotbar scroll (only if input is not captured by UI)
        if (!uiManager.isInputCaptured()) {
            handleHotbarScroll();
        }

        // Update player controller (only if input is not captured by UI)
        if (!uiManager.isInputCaptured() && playerController != null) {
            playerController.update(world, inputHandler, settings, camera, deltaTime);
            camera.setPosition(playerController.getEyePosition());
        }

        // Update mining system (only if input is not captured by UI)
        if (!uiManager.isInputCaptured() && miningSystem != null) {
            miningSystem.update(world, camera, inputHandler, deltaTime);
        }

        if (dropManager != null && inventory != null && playerController != null) {
            dropManager.update(world, playerController.getPosition(), inventory, deltaTime);
        }

        if (leafDecaySystem != null) {
            leafDecaySystem.update(deltaTime);
        }

        if (npcManager != null && playerController != null) {
            npcManager.update(world, playerController.getPosition(), deltaTime);
        }

        if (aiCompanionManager != null) {
            aiCompanionManager.update(deltaTime);
        }
        
        // Update chunk manager with camera position
        // This handles dynamic chunk loading/unloading as player moves
        // In multiplayer mode, chunk loading is handled by network client, not ChunkManager
        if (worldLoaded && !multiplayerMode && chunkManager != null) {
            if (monitorActive) {
                performanceMonitor.beginZone("ChunkUpdate");
            }
            Vector3f trackingPosition = playerController != null
                ? playerController.getPosition()
                : camera.getPosition();
            Vector3f viewDirection = camera != null ? camera.getFront() : new Vector3f(0, 0, -1);
            Frustum activeFrustum = chunkRenderer != null ? chunkRenderer.getFrustum() : null;
            chunkManager.update(trackingPosition, viewDirection, activeFrustum, deltaTime);
            if (monitorActive) {
                performanceMonitor.endZone();
            }
        }

        if (monitorActive) {
            performanceMonitor.endZone();
        }
    }

    private boolean shouldForwardScrollToUI() {
        if (uiManager == null) {
            return true;
        }

        if (uiManager.isInputCaptured()) {
            return true;
        }

        if (uiManager.getConsoleOverlay() != null && uiManager.getConsoleOverlay().isVisible()) {
            return true;
        }

        if (uiManager.getChatOverlay() != null && uiManager.getChatOverlay().isVisible()) {
            return true;
        }

        return uiManager.getCurrentState() != GameState.IN_GAME;
    }

    private void processBlurResizeDebounce() {
        if (!blurResizePending) {
            return;
        }

        long elapsed = System.currentTimeMillis() - lastBlurResizeTime;
        if (elapsed < BLUR_RESIZE_DEBOUNCE_MS) {
            return;
        }

        int width = pendingBlurWidth;
        int height = pendingBlurHeight;

        if (width < 1 || height < 1) {
            System.out.println("[Game] Waiting for valid blur dimensions before processing resize: " + width + "x" + height);
            return;
        }

        resizeBlurTargets(width, height);

        System.out.println("[Game] Processing debounced blur resize: " + width + "x" + height);
        blurResizePending = false;
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
        boolean isPaused = uiManager.getCurrentState() == GameState.PAUSED;
        boolean shouldBlur = isPaused && settings.graphics.pauseMenuBlur && blurSupported && blurRenderer != null;
        
        if (shouldBlur) {
            // Bind first FBO for world rendering
            glBindFramebuffer(GL_FRAMEBUFFER, blurFbo);
            glViewport(0, 0, blurWidth, blurHeight);
        }
        
        // Clear color and depth buffers
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Render world if loaded and in appropriate state
        if (worldLoaded && (uiManager.getCurrentState() == GameState.IN_GAME || isPaused)) {
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
            if (chunkRenderer != null && chunkRendererInitialized) {
                chunkRenderer.render(loadedChunks, view, projection);
            } else if (!chunkRendererUnavailableWarned) {
                System.err.println("[Game] Skipping chunk rendering because renderer is unavailable.");
                chunkRendererUnavailableWarned = true;
            }

            if (highlightRendererInitialized && miningSystem != null) {
                miningSystem.getAimedTarget().ifPresent(target ->
                    blockHighlightRenderer.render(target, miningSystem.getBreakProgress(), view, projection)
                );
            }

            if (itemDropRenderer != null && dropManager != null) {
                itemDropRenderer.render(dropManager.getDrops(), camera, view, projection);
            }
            
            if (npcRenderer != null && npcManager != null) {
                npcRenderer.render(npcManager.getAllNPCs(), camera, view, projection);
            }
        }
        
        // Apply blur if paused
        if (shouldBlur) {
            // Horizontal blur pass: blurTexture -> blurFbo2
            blurRenderer.renderBlurPass(blurTexture, blurFbo2, true, blurWidth, blurHeight);
            
            // Vertical blur pass: blurTexture2 -> screen
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, window.getWidth(), window.getHeight());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            blurRenderer.renderBlurPass(blurTexture2, 0, false, window.getWidth(), window.getHeight());
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
    public void cleanup() {
        System.out.println("[Game] Cleaning up resources...");
        
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
            chunkRenderer = null;
        }
        if (blockPreviewRenderer != null) {
            blockPreviewRenderer.cleanup();
            blockPreviewRenderer = null;
        }

        if (skyRenderer != null) {
            skyRenderer.cleanup();
        }

        // Shutdown world system
        if (chunkManager != null) {
            chunkManager.shutdown();
            System.out.println("[Game] World cleaned up");
        }

        if (leafDecaySystem != null) {
            leafDecaySystem.clear();
            leafDecaySystem = null;
        }

        treeFellingSystem = null;

        if (highlightRendererInitialized && blockHighlightRenderer != null) {
            blockHighlightRenderer.cleanup();
            highlightRendererInitialized = false;
        }
        
        if (npcRenderer != null) {
            npcRenderer.cleanup();
            System.out.println("[Game] NPC renderer cleaned up");
        }
        
        if (aiCompanionManager != null) {
            aiCompanionManager.shutdown();
        }

        window.destroy();
        
        // Terminate GLFW only on final shutdown
        Window.terminateGLFW();
        
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
            if (settings != null && settings.world != null) {
                world.setDebugLogging(settings.world.debugLogging);
            }
            if (modLoader != null) {
                world.setEventBus(modLoader.getEventBus());
            }
            chunkManager = new ChunkManager(
                world,
                settings.world.chunkLoadDistance,
                settings.world.chunkUnloadDistance,
                performanceMonitor
            );
            Vector3f initialView = camera != null ? camera.getFront() : new Vector3f(0, 0, -1);
            Frustum initialFrustum = chunkRenderer != null ? chunkRenderer.getFrustum() : null;
            chunkManager.update(camera.getPosition(), initialView, initialFrustum, 0f);
            System.out.println("[Game] World initialized with seed: " + world.getSeed());
        } else if (world == null) {
            System.err.println("[Game] Warning: multiplayer world not provided before createWorld call");
            return;
        }

        if (chunkRenderer == null) {
            chunkRenderer = new ChunkRenderer();
        }

        chunkRenderer.setGPUCapabilities(gpuCapabilities);
        chunkRenderer.setPerformanceMonitor(performanceMonitor);
        chunkRenderer.setModLoader(modLoader);
        chunkRenderer.setSettings(settings);
        chunkRenderer.setSunLight(sunLight);

        if (!chunkRendererInitialized) {
            try {
                chunkRenderer.init();
                chunkRendererInitialized = true;
                chunkRendererUnavailableWarned = false;
                System.out.println("[Game] Chunk renderer initialized");
            } catch (Exception ex) {
                chunkRendererInitialized = false;
                System.err.println("[Game] Chunk renderer failed to initialize during world setup: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        if (world != null) {
            world.setAuthoritative(!multiplayerMode);
            world.setChunkUnloadCallback(pos -> chunkRenderer.onChunkUnloaded(pos));
        }

        initializeTreeSystems();

        updateSkyLighting(0.0f);

        itemDropRenderer.init();
        if (chunkRenderer != null && chunkRendererInitialized && chunkRenderer.getTextureAtlas() != null) {
            itemDropRenderer.setTextureAtlas(chunkRenderer.getTextureAtlas());
        }
        dropManager.clear();
        
        npcRenderer.init();
        npcRenderer.setSkinAtlas(skinManager.getAtlas());
        npcManager.clear();
        System.out.println("[Game] NPC renderer initialized");

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

        if (aiCompanionManager != null && settings != null && settings.ai != null
            && settings.ai.aiEnabled && settings.ai.spawnOnStart) {
            boolean spawned = aiCompanionManager.spawnCompanion(playerController != null
                ? playerController.getPosition() : null, settings.ai);
            if (spawned) {
                System.out.println("[AI] Companion spawned on world start.");
            }
        }

        System.out.println("[Game] World creation complete!");
    }

    /**
     * Initializes tree-related simulation systems for authoritative worlds only.
     * <p>
     * Multiplayer clients render server-provided state and must not run tree mutations locally.
     */
    private void initializeTreeSystems() {
        if (world == null) {
            return;
        }

        if (multiplayerMode) {
            world.setAuthoritative(false);
            return;
        }

        if (leafDecaySystem != null) {
            leafDecaySystem.clear();
        }

        Settings.GameplaySettings gameplaySettings = settings != null ? settings.gameplay : null;
        if (gameplaySettings == null) {
            gameplaySettings = new Settings.GameplaySettings();
        }
        gameplaySettings.ensureNestedDefaults();

        Settings.LeafDecaySettings leafSettings = gameplaySettings.leafDecay;
        Settings.TreeFellingSettings treeSettings = gameplaySettings.treeFelling;

        leafDecaySystem = new LeafDecaySystem(world, dropManager, leafSettings);
        treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem, treeSettings);
        if (settings != null && settings.world != null) {
            world.setDebugLogging(settings.world.debugLogging);
        }
        world.setAuthoritative(true);
        world.setTreeFellingSystem(treeFellingSystem);
        world.setLeafDecaySystem(leafDecaySystem);
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk != null) {
                leafDecaySystem.onChunkLoaded(chunk);
            }
        }
        System.out.println("[Game] Tree felling and leaf decay systems initialized");
    }

    public SunLight getSunLight() {
        return sunLight;
    }
    
    /**
     * Gets the sky renderer instance.
     * 
     * @return Sky renderer
     */
    public SkyRenderer getSkyRenderer() {
        return skyRenderer;
    }
    
    /**
     * Gets the GPU capabilities.
     * 
     * @return GPU capabilities
     */
    public GPUCapabilities getGPUCapabilities() {
        return gpuCapabilities;
    }
    
    /**
     * Gets the window instance.
     * 
     * @return Window
     */
    public Window getWindow() {
        return window;
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
        if (modLoader != null) {
            world.setEventBus(modLoader.getEventBus());
        }
        ensureHighlightRendererInitialized();
        if (miningSystem != null) {
            miningSystem.reset();
        }

        if (chunkRenderer == null) {
            chunkRenderer = new ChunkRenderer();
            chunkRenderer.setModLoader(modLoader);
            chunkRenderer.setSettings(settings);
            chunkRenderer.setSunLight(sunLight);
            try {
                chunkRenderer.init();
                chunkRendererInitialized = true;
                chunkRendererUnavailableWarned = false;
                System.out.println("[Game] Chunk renderer initialized");
            } catch (Exception ex) {
                chunkRendererInitialized = false;
                System.err.println("[Game] Chunk renderer failed to initialize: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            chunkRenderer.setSettings(settings);
            chunkRenderer.setSunLight(sunLight);
            chunkRenderer.setModLoader(modLoader);
        }

        if (world != null) {
            world.setAuthoritative(false);
            world.setChunkUnloadCallback(pos -> chunkRenderer.onChunkUnloaded(pos));
        }

        treeFellingSystem = null;
        leafDecaySystem = null;

        updateSkyLighting(0.0f);

        itemDropRenderer.init();
        if (chunkRenderer != null && chunkRendererInitialized && chunkRenderer.getTextureAtlas() != null) {
            itemDropRenderer.setTextureAtlas(chunkRenderer.getTextureAtlas());
        }
        dropManager.clear();
        
        npcRenderer.init();
        npcRenderer.setSkinAtlas(skinManager.getAtlas());
        npcManager.clear();

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
     * Gets the mod loader.
     * 
     * @return The Lua mod loader, or null if mod loading failed
     */
    public LuaModLoader getModLoader() {
        return modLoader;
    }
    
    /**
     * Gets the NPC manager instance.
     * 
     * @return The NPC manager
     */
    public NPCManager getNPCManager() {
        return npcManager;
    }

    public AICompanionManager getAICompanionManager() {
        return aiCompanionManager;
    }

    public Settings getSettings() {
        return settings;
    }
    
    /**
     * Gets the current game time of day.
     * 
     * @return Time of day value (0.0-1.0 range)
     */
    public float getTimeOfDay() {
        return timeOfDay;
    }

    /**
     * Sets the game time of day and updates lighting.
     * 
     * @param time Time of day (0.0-1.0 range, clamped)
     */
    public void setTimeOfDay(float time) {
        // Clamp to 0.0-1.0 range
        this.timeOfDay = Math.max(0.0f, Math.min(1.0f, time));
        // Update lighting to match new time
        updateSkyLighting(this.timeOfDay);
    }

    public void setExternalTimeControlEnabled(boolean enabled) {
        this.externalTimeControlEnabled = enabled;
    }

    public boolean isExternalTimeControlEnabled() {
        return externalTimeControlEnabled;
    }

    public boolean isWorldLoaded() {
        return worldLoaded && world != null;
    }

    /**
     * Gets the player's current position in the world.
     * 
     * @return Player position, or null if player controller is null
     */
    public Vector3f getPlayerPosition() {
        if (playerController != null) {
            return playerController.getPosition();
        }
        return null;
    }

    public PlayerController getPlayerController() {
        return playerController;
    }

    /**
     * Updates Discord Rich Presence based on current game state.
     * Called periodically from the update loop.
     * 
     * This is where the magic happens - we tell Discord what we're doing.
{{ ... }}
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
            case AI_COMPANION_SETTINGS:
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

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    private void ensureHighlightRendererInitialized() {
        if (!highlightRendererInitialized && blockHighlightRenderer != null) {
            blockHighlightRenderer.init();
            highlightRendererInitialized = true;
        }
    }
    
    /**
     * Initializes blur effect resources (FBOs and textures).
     * Called during init() and on window resize.
     * 
     * @param width Blur buffer width
     * @param height Blur buffer height
     */
    private void initBlurResources(int width, int height) {
        try {
            // Use half-resolution for performance
            blurWidth = Math.max(1, width / 2);
            blurHeight = Math.max(1, height / 2);
            
            // Initialize blur renderer if needed
            if (blurRenderer == null) {
                blurRenderer = new BlurRenderer();
                blurRenderer.init();
            }
            
            // Create first FBO and texture
            blurFbo = glGenFramebuffers();
            blurTexture = glGenTextures();
            
            glBindTexture(GL_TEXTURE_2D, blurTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, blurWidth, blurHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            
            glBindFramebuffer(GL_FRAMEBUFFER, blurFbo);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, blurTexture, 0);
            
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                System.err.println("[Game] Blur FBO 1 is not complete!");
                cleanupBlurResources();
                blurSupported = false;
                return;
            }
            
            // Create second FBO and texture
            blurFbo2 = glGenFramebuffers();
            blurTexture2 = glGenTextures();
            
            glBindTexture(GL_TEXTURE_2D, blurTexture2);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, blurWidth, blurHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            
            glBindFramebuffer(GL_FRAMEBUFFER, blurFbo2);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, blurTexture2, 0);
            
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                System.err.println("[Game] Blur FBO 2 is not complete!");
                cleanupBlurResources();
                blurSupported = false;
                return;
            }
            
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glBindTexture(GL_TEXTURE_2D, 0);
            
            blurSupported = true;
            System.out.println("[Game] Blur resources initialized at " + blurWidth + "x" + blurHeight);
            
        } catch (Exception e) {
            System.err.println("[Game] Failed to initialize blur resources: " + e.getMessage());
            cleanupBlurResources();
            blurSupported = false;
        }
    }
    
    /**
     * Resizes blur FBOs when window is resized.
     * 
     * @param width New window width
     * @param height New window height
     */
    private void resizeBlurTargets(int width, int height) {
        if (!blurSupported || blurRenderer == null) {
            return;
        }
        try {
            initBlurResources(width, height);
        } catch (Exception e) {
            System.err.println("[Game] Failed to resize blur resources: " + e.getMessage());
        }
    }
    
    /**
     * Cleans up blur effect resources.
     */
    private void cleanupBlurResources() {
        if (blurFbo != 0) {
            glDeleteFramebuffers(blurFbo);
            blurFbo = 0;
        }
        if (blurTexture != 0) {
            glDeleteTextures(blurTexture);
            blurTexture = 0;
        }
        if (blurFbo2 != 0) {
            glDeleteFramebuffers(blurFbo2);
            blurFbo2 = 0;
        }
        if (blurTexture2 != 0) {
            glDeleteTextures(blurTexture2);
            blurTexture2 = 0;
        }
    }
    
    /**
     * Posts a task to be executed on the main game thread.
     * Thread-safe method for async operations to schedule work on the main thread.
     * 
     * @param task Runnable to execute on main thread
     */
    public void postToMainThread(Runnable task) {
        if (task != null) {
            mainThreadTasks.offer(task);
        }
    }
    
    /**
     * Processes all pending main thread tasks.
     * Called once per frame from the update loop.
     */
    private void processMainThreadTasks() {
        Runnable task;
        while ((task = mainThreadTasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("[Game] Error executing main thread task: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
