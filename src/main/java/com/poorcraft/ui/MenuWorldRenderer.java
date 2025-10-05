package com.poorcraft.ui;

import com.poorcraft.camera.Camera;
import com.poorcraft.render.ChunkRenderer;
import com.poorcraft.render.SkyRenderer;
import com.poorcraft.render.SunLight;
import com.poorcraft.world.World;
import com.poorcraft.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders an animated 3D world background for menu screens.
 * 
 * Creates a small, pre-generated world chunk with interesting terrain
 * and slowly animates the camera around it for visual interest.
 * This gives menus that classic Minecraft feel with a living world background.
 */
public class MenuWorldRenderer {
    
    private static final long MENU_WORLD_SEED = 12345678L;
    private static final float CAMERA_DISTANCE = 20.0f;
    private static final float CAMERA_HEIGHT = 75.0f;
    private static final float ROTATION_SPEED = 0.05f; // Radians per second
    private static final float BOB_SPEED = 0.3f;
    private static final float BOB_AMPLITUDE = 1.5f;
    
    private World menuWorld;
    private Camera menuCamera;
    private ChunkRenderer chunkRenderer;
    private SkyRenderer skyRenderer;
    private SunLight sunLight;
    
    private float rotationAngle;
    private float bobTimer;
    private boolean initialized;
    private float animationSpeed;
    private boolean loading;
    private float fadeAlpha;
    
    /**
     * Creates a new menu world renderer.
     * 
     * @param chunkRenderer Chunk renderer from game
     * @param skyRenderer Sky renderer from game
     * @param sunLight Sun light from game
     * @param animationSpeed Speed multiplier for animations (1.0 = normal)
     */
    public MenuWorldRenderer(ChunkRenderer chunkRenderer, SkyRenderer skyRenderer, 
                            SunLight sunLight, float animationSpeed) {
        this.chunkRenderer = chunkRenderer;
        this.skyRenderer = skyRenderer;
        this.sunLight = sunLight;
        this.animationSpeed = animationSpeed;
        this.rotationAngle = 0.0f;
        this.bobTimer = 0.0f;
        this.initialized = false;
        this.loading = false;
        this.fadeAlpha = 0.0f;
    }
    
    /**
     * Initializes the menu world and camera.
     * Creates a small world with interesting terrain on a background thread.
     */
    public void init() {
        if (initialized || loading) {
            return;
        }
        
        loading = true;
        System.out.println("[MenuWorldRenderer] Starting async menu world initialization...");
        
        // Create camera immediately for smooth transition
        Vector3f cameraPos = new Vector3f(CAMERA_DISTANCE, CAMERA_HEIGHT, 0);
        menuCamera = new Camera(cameraPos, 0, 0);
        
        // Load world and chunks on background thread
        new Thread(() -> {
            try {
                // Create a small world for the menu background
                menuWorld = new World(MENU_WORLD_SEED, true);
                
                // Force load a few chunks around spawn for the background
                // We load a 3x3 area centered at origin
                for (int cx = -1; cx <= 1; cx++) {
                    for (int cz = -1; cz <= 1; cz++) {
                        try {
                            var worldClass = menuWorld.getClass();
                            var method = worldClass.getMethod("getOrCreateChunk", int.class, int.class);
                            method.invoke(menuWorld, cx, cz);
                        } catch (Exception e) {
                            System.err.println("[MenuWorldRenderer] Failed to load chunk: " + e.getMessage());
                        }
                    }
                }
                
                System.out.println("[MenuWorldRenderer] Menu world initialized with " + 
                                  menuWorld.getLoadedChunks().size() + " chunks");
                
                initialized = true;
                loading = false;
            } catch (Exception e) {
                System.err.println("[MenuWorldRenderer] Failed to initialize menu world: " + e.getMessage());
                loading = false;
            }
        }, "MenuWorldLoader").start();
    }
    
    /**
     * Updates the camera animation and fade-in effect.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public void update(float deltaTime) {
        // Fade in when initialized
        if (initialized && fadeAlpha < 1.0f) {
            fadeAlpha = Math.min(1.0f, fadeAlpha + deltaTime * 0.5f);
        }
        
        if (!initialized) {
            return;
        }
        
        // Update rotation angle
        rotationAngle += ROTATION_SPEED * animationSpeed * deltaTime;
        if (rotationAngle > Math.PI * 2) {
            rotationAngle -= (float)(Math.PI * 2);
        }
        
        // Update bob timer
        bobTimer += BOB_SPEED * animationSpeed * deltaTime;
        if (bobTimer > Math.PI * 2) {
            bobTimer -= (float)(Math.PI * 2);
        }
        
        // Calculate camera position on circular path
        float x = (float)(Math.cos(rotationAngle) * CAMERA_DISTANCE);
        float z = (float)(Math.sin(rotationAngle) * CAMERA_DISTANCE);
        float y = CAMERA_HEIGHT + (float)(Math.sin(bobTimer) * BOB_AMPLITUDE);
        
        // Update camera position
        menuCamera.setPosition(x, y, z);
        
        // Make camera look at world center (slightly above ground)
        Vector3f lookTarget = new Vector3f(0, 68, 0);
        menuCamera.lookAt(lookTarget);
    }
    
    /**
     * Renders the menu world background with fade-in.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     */
    public void render(int windowWidth, int windowHeight) {
        if (!initialized || menuWorld == null || menuCamera == null) {
            // Fallback to solid color while loading
            glClearColor(0.1f, 0.05f, 0.15f, 1.0f);
            return;
        }
        
        // Save current OpenGL state
        boolean depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFaceEnabled = glIsEnabled(GL_CULL_FACE);
        boolean blendEnabled = glIsEnabled(GL_BLEND);
        
        // Enable 3D rendering state
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        
        // Get matrices from camera
        float aspect = (float)windowWidth / windowHeight;
        Matrix4f view = menuCamera.getViewMatrix();
        Matrix4f projection = menuCamera.getProjectionMatrix(70.0f, aspect, 0.1f, 1000.0f);
        
        // Render sky
        if (skyRenderer != null && sunLight != null) {
            skyRenderer.render(menuCamera, 70.0f, aspect, sunLight.getDirection());
        }
        
        // Render world chunks (fade-in handled by natural loading)
        if (chunkRenderer != null) {
            Collection<Chunk> chunks = menuWorld.getLoadedChunks();
            chunkRenderer.render(chunks, view, projection);
        }
        
        // Restore OpenGL state
        if (!depthTestEnabled) glDisable(GL_DEPTH_TEST);
        if (!cullFaceEnabled) glDisable(GL_CULL_FACE);
        if (blendEnabled) glEnable(GL_BLEND);
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (menuWorld != null) {
            // Don't cleanup chunk renderer - it's shared with the game
            // Just clear our reference
            menuWorld = null;
        }
        initialized = false;
        System.out.println("[MenuWorldRenderer] Cleaned up");
    }
    
    /**
     * Sets the animation speed multiplier.
     * 
     * @param speed Speed multiplier (1.0 = normal, 0.5 = slow, 2.0 = fast)
     */
    public void setAnimationSpeed(float speed) {
        this.animationSpeed = Math.max(0.1f, Math.min(5.0f, speed));
    }
}
