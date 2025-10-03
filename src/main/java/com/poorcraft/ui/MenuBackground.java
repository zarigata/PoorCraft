package com.poorcraft.ui;

import com.poorcraft.render.Texture;

/**
 * Manages the textured background for menu screens.
 * 
 * Loads block textures and renders them tiled across the screen
 * at low opacity (20%) so they don't overpower the UI elements.
 * 
 * This gives the menu a nice Minecraft vibe without making it hard to read.
 * I tried 50% opacity first and couldn't read anything. 20% is the sweet spot.
 */
public class MenuBackground {
    
    private Texture[] textures;
    private int currentTextureIndex;
    private static final float TILE_SIZE = 64.0f;  // Size of each texture tile in pixels
    private static final float OPACITY = 0.20f;     // 20% opacity - subtle but visible
    
    /**
     * Creates a new menu background.
     * Loads a selection of block textures for variety.
     */
    public MenuBackground() {
        this.currentTextureIndex = 0;
        loadTextures();
    }
    
    /**
     * Loads block textures from resources.
     * We pick nice-looking textures that tile well.
     */
    private void loadTextures() {
        try {
            // Load a variety of block textures
            // These all tile nicely and look good at low opacity
            textures = new Texture[]{
                Texture.loadFromResource("/textures/blocks/stone.png"),
                Texture.loadFromResource("/textures/blocks/grass_top.png"),
                Texture.loadFromResource("/textures/blocks/dirt.png"),
                Texture.loadFromResource("/textures/blocks/wood_top.png"),
                Texture.loadFromResource("/textures/blocks/sand.png")
            };
            
            System.out.println("[MenuBackground] Loaded " + textures.length + " background textures");
            
        } catch (Exception e) {
            System.err.println("[MenuBackground] Failed to load textures: " + e.getMessage());
            textures = new Texture[0];
        }
    }
    
    /**
     * Renders the tiled background across the entire window.
     * 
     * @param renderer UI renderer
     * @param windowWidth Window width in pixels
     * @param windowHeight Window height in pixels
     */
    public void render(UIRenderer renderer, int windowWidth, int windowHeight) {
        if (textures == null || textures.length == 0) {
            // Fallback to solid color if textures failed to load
            renderer.drawRect(0, 0, windowWidth, windowHeight, 
                0.10f, 0.05f, 0.15f, 1.0f);  // Dark purple
            return;
        }
        
        // Draw solid background first
        renderer.drawRect(0, 0, windowWidth, windowHeight, 
            0.10f, 0.05f, 0.15f, 1.0f);  // Dark purple base
        
        // Calculate how many tiles we need
        int tilesX = (int) Math.ceil(windowWidth / TILE_SIZE) + 1;
        int tilesY = (int) Math.ceil(windowHeight / TILE_SIZE) + 1;
        
        // Get current texture
        Texture texture = textures[currentTextureIndex % textures.length];
        int textureId = texture.getId();
        
        // Render tiled texture across the screen
        for (int y = 0; y < tilesY; y++) {
            for (int x = 0; x < tilesX; x++) {
                float tileX = x * TILE_SIZE;
                float tileY = y * TILE_SIZE;
                
                // Draw textured tile with low opacity
                renderer.drawTexturedRect(
                    tileX, tileY, TILE_SIZE, TILE_SIZE, 
                    textureId,
                    1.0f, 1.0f, 1.0f, OPACITY  // White tint with 20% opacity
                );
            }
        }
    }
    
    /**
     * Changes to the next texture in the list.
     * Could be called on a timer or button press for variety.
     */
    public void nextTexture() {
        if (textures != null && textures.length > 0) {
            currentTextureIndex = (currentTextureIndex + 1) % textures.length;
        }
    }
    
    /**
     * Sets a specific texture by index.
     * 
     * @param index Texture index (0 to textures.length-1)
     */
    public void setTexture(int index) {
        if (textures != null && textures.length > 0) {
            currentTextureIndex = index % textures.length;
        }
    }
    
    /**
     * Cleans up texture resources.
     */
    public void cleanup() {
        if (textures != null) {
            for (Texture texture : textures) {
                if (texture != null) {
                    texture.cleanup();
                }
            }
        }
    }
}
