package com.poorcraft.ui;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * Bitmap font renderer using STB TrueType.
 * 
 * This renderer bakes a font atlas at initialization containing all ASCII printable
 * characters (32-126). Text is then rendered by drawing textured quads for each character.
 * 
 * For a simple UI, ASCII is sufficient. Could be extended to support Unicode later
 * if we ever need fancy characters like ñ or emoji (probably not for a Minecraft clone).
 * 
 * I tried to use FreeType first but STB is so much simpler. Sometimes simple is better.
 */
public class FontRenderer {
    
    private static final int ATLAS_WIDTH = 512;
    private static final int ATLAS_HEIGHT = 512;
    private static final int FIRST_CHAR = 32;   // Space
    private static final int CHAR_COUNT = 95;   // ASCII 32-126 (printable characters)
    
    private UIRenderer uiRenderer;
    private int fontAtlasTexture;
    private int fontSize;
    private STBTTBakedChar.Buffer charData;
    private float lineHeight;
    
    /**
     * Creates a new font renderer.
     * 
     * @param uiRenderer UI renderer for drawing character quads
     * @param fontSize Font size in pixels
     */
    public FontRenderer(UIRenderer uiRenderer, int fontSize) {
        this.uiRenderer = uiRenderer;
        this.fontSize = fontSize;
    }
    
    /**
     * Initializes the font renderer by baking a font atlas.
     * 
     * @param fontPath Path to TTF font file (can be resource path or filesystem path)
     */
    public void init(String fontPath) {
        try {
            // Load TTF font file
            ByteBuffer fontBuffer;
            try {
                // Try loading from filesystem first
                byte[] fontBytes = Files.readAllBytes(Paths.get(fontPath));
                fontBuffer = BufferUtils.createByteBuffer(fontBytes.length);
                fontBuffer.put(fontBytes);
                fontBuffer.flip();
                System.out.println("[FontRenderer] Loaded font from filesystem: " + fontPath);
            } catch (IOException e) {
                // Try loading from resources
                var stream = getClass().getResourceAsStream(fontPath);
                if (stream == null) {
                    throw new RuntimeException("Font file not found: " + fontPath);
                }
                byte[] fontBytes = stream.readAllBytes();
                fontBuffer = BufferUtils.createByteBuffer(fontBytes.length);
                fontBuffer.put(fontBytes);
                fontBuffer.flip();
                System.out.println("[FontRenderer] Loaded font from resources: " + fontPath);
            }
            
            // Create bitmap for font atlas
            ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_WIDTH * ATLAS_HEIGHT);
            
            // Allocate character data buffer
            charData = STBTTBakedChar.malloc(CHAR_COUNT);
            
            // Bake font bitmap
            // This rasterizes all characters into the bitmap and fills charData with metrics
            int result = stbtt_BakeFontBitmap(fontBuffer, fontSize, bitmap, 
                ATLAS_WIDTH, ATLAS_HEIGHT, FIRST_CHAR, charData);
            
            if (result <= 0) {
                throw new RuntimeException("Failed to bake font bitmap");
            }
            
            // Create OpenGL texture from bitmap
            fontAtlasTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, fontAtlasTexture);
            
            // Upload bitmap (single channel, using GL_RED for OpenGL 3.3 core compatibility)
            // GL_ALPHA is deprecated in core profile, GL_RED works the same for grayscale
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, ATLAS_WIDTH, ATLAS_HEIGHT, 
                0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
            
            // Set texture parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            
            glBindTexture(GL_TEXTURE_2D, 0);
            
            // Calculate line height (approximate)
            lineHeight = fontSize * 1.2f;
            
            System.out.println("[FontRenderer] Font atlas created: " + ATLAS_WIDTH + "x" + ATLAS_HEIGHT + 
                ", " + CHAR_COUNT + " characters at " + fontSize + "px");
            
        } catch (Exception e) {
            System.err.println("[FontRenderer] Failed to initialize font: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Font initialization failed", e);
        }
    }
    
    /**
     * Draws text at the specified position.
     * Now uses VBOs via UIRenderer instead of deprecated immediate mode.
     * 
     * @param text Text to draw
     * @param x X position (pixels from left)
     * @param y Y position (pixels from top, baseline)
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     */
    public void drawText(String text, float x, float y, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        glBindTexture(GL_TEXTURE_2D, fontAtlasTexture);
        
        float currentX = x;
        float currentY = y;
        
        // Batch all character quads into a single buffer
        // This is way more efficient than drawing each character separately
        // Old me would've just used glBegin/glEnd, but that's so 2005
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(text.length() * 6 * 4);
        int quadCount = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Handle newlines
            if (c == '\n') {
                currentX = x;
                currentY += lineHeight;
                continue;
            }
            
            // Skip characters outside our range
            if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) {
                continue;
            }
            
            // Get character data
            STBTTBakedChar charInfo = charData.get(c - FIRST_CHAR);
            
            // Calculate quad position and size
            float charX = currentX + charInfo.xoff();
            float charY = currentY + charInfo.yoff();
            float charW = charInfo.x1() - charInfo.x0();
            float charH = charInfo.y1() - charInfo.y0();
            
            // Calculate UV coordinates
            float u0 = charInfo.x0() / (float) ATLAS_WIDTH;
            float v0 = charInfo.y0() / (float) ATLAS_HEIGHT;
            float u1 = charInfo.x1() / (float) ATLAS_WIDTH;
            float v1 = charInfo.y1() / (float) ATLAS_HEIGHT;
            
            // Add character quad to batch (6 vertices, 4 floats each: x, y, u, v)
            // Triangle 1
            vertexBuffer.put(charX).put(charY).put(u0).put(v0);
            vertexBuffer.put(charX + charW).put(charY).put(u1).put(v0);
            vertexBuffer.put(charX + charW).put(charY + charH).put(u1).put(v1);
            
            // Triangle 2
            vertexBuffer.put(charX + charW).put(charY + charH).put(u1).put(v1);
            vertexBuffer.put(charX).put(charY + charH).put(u0).put(v1);
            vertexBuffer.put(charX).put(charY).put(u0).put(v0);
            
            quadCount++;
            
            // Advance cursor
            currentX += charInfo.xadvance();
        }
        
        if (quadCount == 0) {
            glBindTexture(GL_TEXTURE_2D, 0);
            return;
        }
        
        vertexBuffer.flip();
        
        // Upload vertex data to VBO and draw
        // Using UIRenderer's VBO for efficiency (no need to create our own)
        int vbo = uiRenderer.getVBO();
        int vao = uiRenderer.getVAO();
        
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);
        
        // Set shader uniforms
        uiRenderer.getShader().setUniform("uModel", new org.joml.Matrix4f().identity());
        uiRenderer.getShader().setUniform("uColor", r, g, b, a);
        uiRenderer.getShader().setUniform("uUseTexture", true);
        
        // Draw all character quads in one call
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, quadCount * 6);
        glBindVertexArray(0);
        
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Calculates the width of a text string in pixels.
     * Useful for centering text or sizing buttons.
     * 
     * @param text Text to measure
     * @return Width in pixels
     */
    public float getTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) {
                continue;
            }
            
            STBTTBakedChar charInfo = charData.get(c - FIRST_CHAR);
            width += charInfo.xadvance();
        }
        
        return width;
    }
    
    /**
     * Returns the approximate line height for this font.
     * 
     * @return Line height in pixels
     */
    public float getTextHeight() {
        return lineHeight;
    }
    
    /**
     * Returns the font size.
     * 
     * @return Font size in pixels
     */
    public int getFontSize() {
        return fontSize;
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        if (fontAtlasTexture != 0) {
            glDeleteTextures(fontAtlasTexture);
        }
        if (charData != null) {
            charData.free();
        }
        System.out.println("[FontRenderer] Cleaned up");
    }
}
