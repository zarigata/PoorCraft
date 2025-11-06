package com.poorcraft.ui;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private Map<Integer, FontAtlas> fontAtlases;
    private int currentFontSize;
    private boolean useFallback = false;
    private boolean usingSystemFallbackFont = false;
    private int textVao;
    private int textVbo;
    
    /**
     * Encapsulates a font atlas with its texture, character data, and metrics.
     */
    private static class FontAtlas {
        int texture;
        STBTTBakedChar.Buffer charData;
        float lineHeight;
        
        FontAtlas(int texture, STBTTBakedChar.Buffer charData, float lineHeight) {
            this.texture = texture;
            this.charData = charData;
            this.lineHeight = lineHeight;
        }
        
        void cleanup() {
            if (texture != 0) {
                glDeleteTextures(texture);
            }
            if (charData != null) {
                charData.free();
            }
        }
    }
    
    /**
     * Creates a new font renderer.
     * 
     * @param uiRenderer UI renderer for drawing character quads
     * @param fontSize Default font size in pixels
     */
    public FontRenderer(UIRenderer uiRenderer, int fontSize) {
        this.uiRenderer = uiRenderer;
        this.currentFontSize = fontSize;
        this.fontAtlases = new HashMap<>();
    }
    
    /**
     * Initializes the font renderer by baking multiple font atlases at different sizes.
     * Creates atlases at 16px, 20px, 24px, and 32px for dynamic font size selection.
     * 
     * @param fontPath Path to TTF font file (can be resource path or filesystem path)
     */
    public void init(String fontPath) {
        useFallback = false;
        usingSystemFallbackFont = false;
        int requestedFontSize = currentFontSize;
        fontAtlases.clear();
        
        // Create dedicated VAO/VBO for text rendering
        // Attributes match UIRenderer shader: pos (vec2) + uv (vec2) = 4 floats per vertex
        textVao = glGenVertexArrays();
        textVbo = glGenBuffers();
        
        glBindVertexArray(textVao);
        glBindBuffer(GL_ARRAY_BUFFER, textVbo);
        
        // Position attribute (location = 0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // TexCoord attribute (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        try {
            FontLoadResult fontResult = loadFontData(fontPath);
            if (fontResult == null) {
                throw new RuntimeException("Font file not found: " + fontPath);
            }

            ByteBuffer fontBuffer = fontResult.buffer();
            usingSystemFallbackFont = !Objects.equals(fontResult.source(), fontPath);
            
            // Bake atlases at common sizes: 16px, 20px, 24px, 32px
            int[] sizes = {16, 20, 24, 32};
            int successCount = 0;
            
            for (int size : sizes) {
                try {
                    FontAtlas atlas = bakeAtlas(fontBuffer, size);
                    fontAtlases.put(size, atlas);
                    successCount++;
                } catch (Exception e) {
                    System.err.println("[FontRenderer] Failed to bake " + size + "px atlas: " + e.getMessage());
                }
            }
            
            if (successCount == 0) {
                throw new RuntimeException("Failed to bake any font atlases");
            }
            
            if (usingSystemFallbackFont) {
                currentFontSize = resolveClosestAvailableSize(requestedFontSize);
            } else {
                currentFontSize = resolveClosestAvailableSize(20);
            }

            System.out.println("[FontRenderer] Font atlases created from " + fontResult.source() +
                ": " + successCount + " sizes (" + fontAtlases.keySet() + ")");

        } catch (Exception e) {
            System.err.println("[FontRenderer] Failed to initialize font: " + e.getMessage());
            System.err.println("[FontRenderer] Using fallback rendering (no font atlas)");
            useFallback = true;
            usingSystemFallbackFont = false;
            fontAtlases.clear();
        }
    }
    
    /**
     * Bakes a single font atlas at the specified size.
     * 
     * @param fontBuffer Font data buffer
     * @param fontSize Font size in pixels
     * @return FontAtlas instance
     * @throws RuntimeException if baking fails
     */
    private FontAtlas bakeAtlas(ByteBuffer fontBuffer, int fontSize) {
        // Create bitmap for font atlas
        ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_WIDTH * ATLAS_HEIGHT);

        // Allocate character data buffer
        STBTTBakedChar.Buffer bakedChars = STBTTBakedChar.malloc(CHAR_COUNT);

        // Bake font bitmap
        int result = stbtt_BakeFontBitmap(fontBuffer, fontSize, bitmap,
            ATLAS_WIDTH, ATLAS_HEIGHT, FIRST_CHAR, bakedChars);

        if (result <= 0) {
            bakedChars.free();
            throw new RuntimeException("Failed to bake font bitmap at " + fontSize + "px");
        }

        // Create OpenGL texture from bitmap
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);

        // Upload bitmap (single channel, using GL_RED for OpenGL 3.3 core compatibility)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, ATLAS_WIDTH, ATLAS_HEIGHT,
            0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);

        // Calculate line height
        float lineHeight = fontSize * 1.2f;
        
        return new FontAtlas(texture, bakedChars, lineHeight);
    }
    
    /**
     * Sets the current font size by selecting the closest available atlas.
     * 
     * @param size Desired font size in pixels
     */
    public void setFontSize(int size) {
        if (useFallback || fontAtlases.isEmpty()) {
            return;
        }
        
        currentFontSize = resolveClosestAvailableSize(size);
    }

    private int resolveClosestAvailableSize(int size) {
        if (fontAtlases.containsKey(size)) {
            return size;
        }

        int closestSize = currentFontSize;
        int minDiff = Integer.MAX_VALUE;

        for (int availableSize : fontAtlases.keySet()) {
            int diff = Math.abs(availableSize - size);
            if (diff < minDiff) {
                minDiff = diff;
                closestSize = availableSize;
            }
        }

        return closestSize;
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
        if (useFallback || fontAtlases.isEmpty()) {
            return;
        }

        if (text == null || text.isEmpty()) {
            return;
        }
        
        // Get current atlas
        FontAtlas atlas = fontAtlases.get(currentFontSize);
        if (atlas == null) {
            return;
        }

        float appliedScale = scale <= 0 ? 1.0f : scale;

        glBindTexture(GL_TEXTURE_2D, atlas.texture);

        float currentX = x;
        float currentY = y;

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(text.length() * 6 * 4);
        int quadCount = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                currentX = x;
                currentY += atlas.lineHeight * appliedScale;
                continue;
            }

            if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) {
                continue;
            }

            STBTTBakedChar charInfo = atlas.charData.get(c - FIRST_CHAR);

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

        // Use dedicated text VAO/VBO instead of UIRenderer's buffers
        glBindBuffer(GL_ARRAY_BUFFER, textVbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);

        uiRenderer.getShader().setUniform("uModel", new org.joml.Matrix4f().identity());
        uiRenderer.getShader().setUniform("uColor", r, g, b, a);
        uiRenderer.getShader().setUniform("uUseTexture", true);

        glBindVertexArray(textVao);
        glDrawArrays(GL_TRIANGLES, 0, quadCount * 6);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

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
        if (useFallback || fontAtlases.isEmpty() || text == null || text.isEmpty()) {
            return 0;
        }
        
        FontAtlas atlas = fontAtlases.get(currentFontSize);
        if (atlas == null) {
            return 0;
        }
        
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) {
                continue;
            }
            
            STBTTBakedChar charInfo = atlas.charData.get(c - FIRST_CHAR);
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
        if (useFallback || fontAtlases.isEmpty()) {
            return currentFontSize;
        }

        if (usingSystemFallbackFont) {
            return currentFontSize;
        }

        FontAtlas atlas = fontAtlases.get(currentFontSize);
        return atlas != null ? atlas.lineHeight : currentFontSize;
    }
    
    /**
     * Returns the current font size.
     * 
     * @return Font size in pixels
     */
    public int getFontSize() {
        return currentFontSize;
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        for (FontAtlas atlas : fontAtlases.values()) {
            atlas.cleanup();
        }
        fontAtlases.clear();
        
        // Clean up text VAO/VBO
        if (textVao != 0) {
            glDeleteVertexArrays(textVao);
            textVao = 0;
        }
        if (textVbo != 0) {
            glDeleteBuffers(textVbo);
            textVbo = 0;
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

        String resourcePath = fontPath.startsWith("/") ? fontPath : "/" + fontPath;

        try (var stream = getClass().getResourceAsStream(resourcePath)) {
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
