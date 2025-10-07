package com.poorcraft.ui;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    private boolean useFallback = false;
    
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
        useFallback = false;
        STBTTBakedChar.Buffer bakedChars = null;
        int createdTexture = 0;
        try {
            FontLoadResult fontResult = loadFontData(fontPath);
            if (fontResult == null) {
                throw new RuntimeException("Font file not found: " + fontPath);
            }

            ByteBuffer fontBuffer = fontResult.buffer();

            // Create bitmap for font atlas
            ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_WIDTH * ATLAS_HEIGHT);

            // Allocate character data buffer
            bakedChars = STBTTBakedChar.malloc(CHAR_COUNT);

            // Bake font bitmap
            // This rasterizes all characters into the bitmap and fills charData with metrics
            int result = stbtt_BakeFontBitmap(fontBuffer, fontSize, bitmap,
                ATLAS_WIDTH, ATLAS_HEIGHT, FIRST_CHAR, bakedChars);

            if (result <= 0) {
                throw new RuntimeException("Failed to bake font bitmap");
            }

            // Create OpenGL texture from bitmap
            createdTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, createdTexture);

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

            charData = bakedChars;
            fontAtlasTexture = createdTexture;

            System.out.println("[FontRenderer] Font atlas created from " + fontResult.source() + 
                ": " + ATLAS_WIDTH + "x" + ATLAS_HEIGHT + ", " + CHAR_COUNT + " characters at " + fontSize + "px");

        } catch (Exception e) {
            System.err.println("[FontRenderer] Failed to initialize font: " + e.getMessage());
            System.err.println("[FontRenderer] Using fallback rendering (no font atlas)");
            if (bakedChars != null) {
                bakedChars.free();
            }
            if (createdTexture != 0) {
                glDeleteTextures(createdTexture);
            }
            useFallback = true;
            charData = null;
            fontAtlasTexture = 0;
            lineHeight = fontSize;
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
        drawText(text, x, y, 1.0f, r, g, b, a);
    }

    /**
     * Draws text at the specified position with a scale multiplier.
     *
     * @param text Text to draw
     * @param x X position (pixels from left)
     * @param y Y position (pixels from top, baseline)
     * @param scale Scale factor relative to the baked font size
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     */
    public void drawText(String text, float x, float y, float scale, float r, float g, float b, float a) {
        if (useFallback) {
            return;
        }

        if (text == null || text.isEmpty()) {
            return;
        }

        float appliedScale = scale <= 0 ? 1.0f : scale;

        glBindTexture(GL_TEXTURE_2D, fontAtlasTexture);

        float currentX = x;
        float currentY = y;

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(text.length() * 6 * 4);
        int quadCount = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                currentX = x;
                currentY += lineHeight * appliedScale;
                continue;
            }

            if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) {
                continue;
            }

            STBTTBakedChar charInfo = charData.get(c - FIRST_CHAR);

            float charX = currentX + charInfo.xoff() * appliedScale;
            float charY = currentY + charInfo.yoff() * appliedScale;
            float charW = (charInfo.x1() - charInfo.x0()) * appliedScale;
            float charH = (charInfo.y1() - charInfo.y0()) * appliedScale;

            float u0 = charInfo.x0() / (float) ATLAS_WIDTH;
            float v0 = charInfo.y0() / (float) ATLAS_HEIGHT;
            float u1 = charInfo.x1() / (float) ATLAS_WIDTH;
            float v1 = charInfo.y1() / (float) ATLAS_HEIGHT;

            vertexBuffer.put(charX).put(charY).put(u0).put(v0);
            vertexBuffer.put(charX + charW).put(charY).put(u1).put(v0);
            vertexBuffer.put(charX + charW).put(charY + charH).put(u1).put(v1);

            vertexBuffer.put(charX + charW).put(charY + charH).put(u1).put(v1);
            vertexBuffer.put(charX).put(charY + charH).put(u0).put(v1);
            vertexBuffer.put(charX).put(charY).put(u0).put(v0);

            quadCount++;

            currentX += charInfo.xadvance() * appliedScale;
        }

        if (quadCount == 0) {
            glBindTexture(GL_TEXTURE_2D, 0);
            return;
        }

        vertexBuffer.flip();

        int vbo = uiRenderer.getVBO();
        int vao = uiRenderer.getVAO();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);

        uiRenderer.getShader().setUniform("uModel", new org.joml.Matrix4f().identity());
        uiRenderer.getShader().setUniform("uColor", r, g, b, a);
        uiRenderer.getShader().setUniform("uUseTexture", true);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, quadCount * 6);
        glBindVertexArray(0);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Draws text with a shadow offset for improved readability.
     */
    public void drawTextWithShadow(String text, float x, float y, float scale,
                                   float r, float g, float b, float a,
                                   float shadowOffset, float shadowAlpha) {
        if (useFallback) {
            return;
        }

        if (text == null || text.isEmpty()) {
            return;
        }

        float offset = Math.max(0f, shadowOffset);
        float alpha = Math.max(0f, Math.min(1f, shadowAlpha));

        drawText(text, x + offset, y + offset, scale, 0f, 0f, 0f, alpha);
        drawText(text, x, y, scale, r, g, b, a);
    }

    /**
     * Convenience overload using default Minecraft shadow values.
     */
    public void drawTextWithShadow(String text, float x, float y, float scale,
                                   float r, float g, float b, float a) {
        drawTextWithShadow(text, x, y, scale, r, g, b, a, 2.0f, 0.6f);
    }
    
    /**
     * Calculates the width of a text string in pixels.
     * Useful for centering text or sizing buttons.
     * 
     * @param text Text to measure
     * @return Width in pixels
     */
    public float getTextWidth(String text) {
        if (useFallback || text == null || text.isEmpty()) {
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
        return useFallback ? fontSize : lineHeight;
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
        if (!useFallback && fontAtlasTexture != 0) {
            glDeleteTextures(fontAtlasTexture);
        }
        if (!useFallback && charData != null) {
            charData.free();
        }
        System.out.println("[FontRenderer] Cleaned up");
    }

    private FontLoadResult loadFontData(String fontPath) {
        ByteBuffer buffer = tryLoadFont(fontPath);
        if (buffer != null) {
            return new FontLoadResult(buffer, fontPath);
        }

        FontLoadResult fallback = tryLoadFallbackSystemFont();
        if (fallback != null) {
            return fallback;
        }

        return null;
    }

    private ByteBuffer tryLoadFont(String fontPath) {
        if (fontPath == null || fontPath.isEmpty()) {
            return null;
        }

        try {
            var path = Paths.get(fontPath);
            if (Files.exists(path) && !Files.isDirectory(path)) {
                byte[] fontBytes = Files.readAllBytes(path);
                return toByteBuffer(fontBytes);
            }
        } catch (IOException | InvalidPathException ignored) {
            // Ignore and try resource lookup
        }

        try (var stream = getClass().getResourceAsStream(fontPath)) {
            if (stream != null) {
                byte[] fontBytes = stream.readAllBytes();
                return toByteBuffer(fontBytes);
            }
        } catch (IOException ignored) {
            // Ignore and let caller handle failure
        }

        return null;
    }

    private FontLoadResult tryLoadFallbackSystemFont() {
        List<String> candidates = new ArrayList<>();

        // Development resources
        candidates.add("src/main/resources/fonts/default.ttf");
        candidates.add("fonts/default.ttf");

        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            candidates.add("C:\\Windows\\Fonts\\segoeui.ttf");
            candidates.add("C:\\Windows\\Fonts\\arial.ttf");
        } else if (osName.contains("mac")) {
            candidates.add("/System/Library/Fonts/SFNS.ttf");
            candidates.add("/Library/Fonts/Arial.ttf");
        } else {
            candidates.add("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
            candidates.add("/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf");
        }

        for (String candidate : candidates) {
            ByteBuffer buffer = tryLoadFont(candidate);
            if (buffer != null) {
                System.out.println("[FontRenderer] Loaded fallback font: " + candidate);
                return new FontLoadResult(buffer, candidate);
            }
        }

        return null;
    }

    private ByteBuffer toByteBuffer(byte[] fontBytes) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(fontBytes.length);
        buffer.put(fontBytes);
        buffer.flip();
        return buffer;
    }

    private static class FontLoadResult {
        private final ByteBuffer buffer;
        private final String source;

        private FontLoadResult(ByteBuffer buffer, String source) {
            this.buffer = buffer;
            this.source = source;
        }

        public ByteBuffer buffer() {
            return buffer;
        }

        public String source() {
            return source;
        }
    }
}
