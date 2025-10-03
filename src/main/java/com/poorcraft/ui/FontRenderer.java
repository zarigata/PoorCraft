package com.poorcraft.ui;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
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
            
            // Upload bitmap (single channel, alpha only)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, ATLAS_WIDTH, ATLAS_HEIGHT, 
                0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
            
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
            
            // Draw character quad
            // Note: We need to draw with custom UVs, but UIRenderer doesn't support that yet
            // For now, we'll use a simple approach with the textured rect method
            // This is a bit hacky but works for our purposes
            drawCharQuad(charX, charY, charW, charH, u0, v0, u1, v1, r, g, b, a);
            
            // Advance cursor
            currentX += charInfo.xadvance();
        }
        
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Draws a single character quad with custom UV coordinates.
     * This is a helper method since UIRenderer doesn't support custom UVs.
     */
    private void drawCharQuad(float x, float y, float w, float h, 
                             float u0, float v0, float u1, float v1,
                             float r, float g, float b, float a) {
        // For now, use a simple immediate mode approach
        // In a production system, we'd batch these into a single draw call
        glBegin(GL_QUADS);
        glColor4f(r, g, b, a);
        
        glTexCoord2f(u0, v0);
        glVertex2f(x, y);
        
        glTexCoord2f(u1, v0);
        glVertex2f(x + w, y);
        
        glTexCoord2f(u1, v1);
        glVertex2f(x + w, y + h);
        
        glTexCoord2f(u0, v1);
        glVertex2f(x, y + h);
        
        glEnd();
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
