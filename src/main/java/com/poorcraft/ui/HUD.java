package com.poorcraft.ui;

/**
 * In-game HUD overlay.
 * 
 * Displays crosshair, hotbar, and optional F3 debug information.
 * Always visible during gameplay (IN_GAME and PAUSED states).
 * 
 * The HUD is where you see all the important info. FPS, position, what you're looking at.
 * F3 debug overlay is a Minecraft tradition. Gotta have it.
 */
public class HUD extends UIScreen {
    
    private Object game;  // Reference to game instance
    private boolean debugVisible;
    
    /**
     * Creates the HUD.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param game Game instance for accessing stats
     */
    public HUD(int windowWidth, int windowHeight, Object game) {
        super(windowWidth, windowHeight);
        this.game = game;
        this.debugVisible = false;
    }
    
    @Override
    public void init() {
        // HUD doesn't use components, it renders directly
        // This keeps it simple and efficient
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw crosshair
        drawCrosshair(renderer);
        
        // Draw hotbar
        drawHotbar(renderer, fontRenderer);
        
        // Draw debug info if visible
        if (debugVisible) {
            drawDebugInfo(renderer, fontRenderer);
        }
    }
    
    /**
     * Draws the crosshair at screen center.
     */
    private void drawCrosshair(UIRenderer renderer) {
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        float size = 10;
        float thickness = 2;
        
        // Horizontal line
        renderer.drawRect(centerX - size, centerY - thickness / 2, 
            size * 2, thickness, 
            1.0f, 1.0f, 1.0f, 0.8f);
        
        // Vertical line
        renderer.drawRect(centerX - thickness / 2, centerY - size, 
            thickness, size * 2, 
            1.0f, 1.0f, 1.0f, 0.8f);
    }
    
    /**
     * Draws the hotbar at bottom center.
     */
    private void drawHotbar(UIRenderer renderer, FontRenderer fontRenderer) {
        float slotSize = 40;
        float slotSpacing = 2;
        int slotCount = 9;
        float totalWidth = slotCount * slotSize + (slotCount - 1) * slotSpacing;
        float startX = windowWidth / 2.0f - totalWidth / 2;
        float startY = windowHeight - slotSize - 20;
        
        for (int i = 0; i < slotCount; i++) {
            float slotX = startX + i * (slotSize + slotSpacing);
            
            // Draw slot background
            if (i == 0) {
                // Highlight selected slot (slot 0)
                renderer.drawRect(slotX, startY, slotSize, slotSize, 
                    0.4f, 0.4f, 0.4f, 0.9f);
            } else {
                renderer.drawRect(slotX, startY, slotSize, slotSize, 
                    0.2f, 0.2f, 0.2f, 0.8f);
            }
            
            // Draw slot border
            float borderWidth = 2;
            renderer.drawRect(slotX, startY, slotSize, borderWidth, 
                0.5f, 0.5f, 0.5f, 1.0f);
            renderer.drawRect(slotX, startY + slotSize - borderWidth, slotSize, borderWidth, 
                0.5f, 0.5f, 0.5f, 1.0f);
            renderer.drawRect(slotX, startY, borderWidth, slotSize, 
                0.5f, 0.5f, 0.5f, 1.0f);
            renderer.drawRect(slotX + slotSize - borderWidth, startY, borderWidth, slotSize, 
                0.5f, 0.5f, 0.5f, 1.0f);
        }
    }
    
    /**
     * Draws F3 debug information.
     */
    private void drawDebugInfo(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw semi-transparent background
        float bgWidth = 300;
        float bgHeight = 200;
        renderer.drawRect(10, 10, bgWidth, bgHeight, 0.0f, 0.0f, 0.0f, 0.6f);
        
        // Draw debug text
        float textX = 20;
        float textY = 25;
        float lineHeight = fontRenderer.getTextHeight() + 5;
        
        // Try to get game stats via reflection
        try {
            var gameClass = game.getClass();
            
            // FPS (approximate from delta time)
            String fpsText = "FPS: ~60"; // Placeholder
            fontRenderer.drawText(fpsText, textX, textY, 1.0f, 1.0f, 1.0f, 1.0f);
            textY += lineHeight;
            
            // Position
            try {
                var cameraField = gameClass.getDeclaredField("camera");
                cameraField.setAccessible(true);
                var camera = cameraField.get(game);
                var posMethod = camera.getClass().getMethod("getPosition");
                var pos = posMethod.invoke(camera);
                
                // Position is a Vector3f
                var xMethod = pos.getClass().getMethod("x");
                var yMethod = pos.getClass().getMethod("y");
                var zMethod = pos.getClass().getMethod("z");
                
                float x = (float) xMethod.invoke(pos);
                float y = (float) yMethod.invoke(pos);
                float z = (float) zMethod.invoke(pos);
                
                String posText = String.format("Position: %.2f, %.2f, %.2f", x, y, z);
                fontRenderer.drawText(posText, textX, textY, 1.0f, 1.0f, 1.0f, 1.0f);
                textY += lineHeight;
                
                // Chunk coordinates
                int chunkX = (int) Math.floor(x / 16);
                int chunkZ = (int) Math.floor(z / 16);
                String chunkText = String.format("Chunk: %d, %d", chunkX, chunkZ);
                fontRenderer.drawText(chunkText, textX, textY, 1.0f, 1.0f, 1.0f, 1.0f);
                textY += lineHeight;
                
            } catch (Exception e) {
                // Couldn't get position
                fontRenderer.drawText("Position: N/A", textX, textY, 1.0f, 1.0f, 1.0f, 1.0f);
                textY += lineHeight;
            }
            
            // Loaded chunks
            try {
                var chunkManagerField = gameClass.getDeclaredField("chunkManager");
                chunkManagerField.setAccessible(true);
                var chunkManager = chunkManagerField.get(game);
                
                if (chunkManager != null) {
                    var loadedChunksMethod = chunkManager.getClass().getMethod("getLoadedChunkCount");
                    int loadedChunks = (int) loadedChunksMethod.invoke(chunkManager);
                    
                    String chunksText = "Loaded chunks: " + loadedChunks;
                    fontRenderer.drawText(chunksText, textX, textY, 1.0f, 1.0f, 1.0f, 1.0f);
                    textY += lineHeight;
                }
            } catch (Exception e) {
                // Couldn't get chunk count
            }
            
            // Facing direction
            fontRenderer.drawText("Facing: N/A", textX, textY, 1.0f, 1.0f, 1.0f, 1.0f);
            textY += lineHeight;
            
        } catch (Exception e) {
            // Failed to get debug info
            fontRenderer.drawText("Debug info unavailable", textX, textY, 1.0f, 0.5f, 0.5f, 1.0f);
        }
        
        // Instructions
        textY += lineHeight;
        fontRenderer.drawText("F3: Toggle debug", textX, textY, 0.7f, 0.7f, 0.7f, 1.0f);
    }
    
    /**
     * Toggles debug info visibility.
     */
    public void toggleDebug() {
        debugVisible = !debugVisible;
        System.out.println("[HUD] Debug info " + (debugVisible ? "enabled" : "disabled"));
    }
    
    /**
     * Gets debug visibility state.
     * 
     * @return True if debug info is visible
     */
    public boolean isDebugVisible() {
        return debugVisible;
    }
}
