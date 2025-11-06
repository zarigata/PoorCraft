package com.poorcraft.player;

import com.poorcraft.render.Texture;
import com.poorcraft.ui.FontRenderer;
import com.poorcraft.ui.UIRenderer;
import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages OpenGL textures for player skins.
 */
public class SkinAtlas {

    private final Map<String, Texture> textures;
    private final Set<String> loggedMissing;

    public SkinAtlas() {
        this.textures = new HashMap<>();
        this.loggedMissing = new HashSet<>();
    }

    public synchronized void uploadSkin(PlayerSkin skin) {
        Objects.requireNonNull(skin, "skin");
        if (!skin.isValid()) {
            return;
        }

        Texture previous = textures.remove(skin.getName());
        if (previous != null) {
            previous.cleanup();
        }

        BufferedImage image = skin.getImage();
        if (image == null) {
            return;
        }

        ByteBuffer buffer = toBuffer(image);
        Texture texture = Texture.createFromData(buffer, image.getWidth(), image.getHeight());
        textures.put(skin.getName(), texture);
        skin.setTextureId(texture.getId());
    }

    public synchronized int getTextureId(String name) {
        Texture texture = textures.get(name);
        return texture != null ? texture.getId() : -1;
    }

    public synchronized void renderOrPlaceholder(UIRenderer renderer, float x, float y, float width, float height, String skinId) {
        renderOrPlaceholder(renderer, x, y, width, height, skinId, false);
    }
    
    public synchronized void renderOrPlaceholder(UIRenderer renderer, float x, float y, float width, float height, String skinId, boolean withBackdrop) {
        int textureId = getTextureId(skinId);
        if (textureId <= 0) {
            // Log missing texture once
            if (!loggedMissing.contains(skinId)) {
                System.out.println("[SkinAtlas] Missing texture for skin: " + skinId);
                loggedMissing.add(skinId);
            }
            
            // Draw enhanced placeholder with better visibility
            // Outer background - brighter and more visible
            renderer.drawRect(x, y, width, height, 0.35f, 0.38f, 0.45f, 0.85f);
            
            // Inner indicator with gradient effect (using two rectangles for simple gradient)
            float inset = Math.min(width, height) * 0.12f;
            float innerX = x + inset;
            float innerY = y + inset;
            float innerWidth = width - inset * 2f;
            float innerHeight = height - inset * 2f;
            
            // Top half - lighter
            renderer.drawRect(innerX, innerY, innerWidth, innerHeight / 2f, 
                0.5f, 0.55f, 0.65f, 0.9f);
            // Bottom half - darker
            renderer.drawRect(innerX, innerY + innerHeight / 2f, innerWidth, innerHeight / 2f, 
                0.4f, 0.45f, 0.55f, 0.9f);
            
            // Draw border frame for definition
            float borderWidth = Math.max(2f, Math.min(width, height) * 0.02f);
            float[] bgColor = {0.35f, 0.38f, 0.45f, 0.85f};
            float[] borderColor = {0.6f, 0.65f, 0.75f, 0.95f};
            renderer.drawBorderedRect(x, y, width, height, borderWidth, bgColor, borderColor);
            
            return;
        }
        
        // Draw light background behind texture if requested
        if (withBackdrop) {
            renderer.drawRect(x, y, width, height, 0.92f, 0.92f, 0.95f, 1.0f);
        }
        
        // Render the texture on top
        renderer.drawTexturedRect(x, y, width, height, textureId);
    }
    
    /**
     * Renders a skin texture or placeholder with optional FontRenderer for text.
     * 
     * @param renderer UI renderer
     * @param fontRenderer Font renderer for drawing "?" text (can be null)
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param skinId Skin identifier
     */
    public synchronized void renderOrPlaceholder(UIRenderer renderer, FontRenderer fontRenderer, 
                                                  float x, float y, float width, float height, String skinId) {
        renderOrPlaceholder(renderer, fontRenderer, x, y, width, height, skinId, true);
    }
    
    /**
     * Renders a skin texture or placeholder with optional FontRenderer for text.
     * 
     * @param renderer UI renderer
     * @param fontRenderer Font renderer for drawing "?" text (can be null)
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param skinId Skin identifier
     * @param withBackdrop Whether to draw light background behind texture
     */
    public synchronized void renderOrPlaceholder(UIRenderer renderer, FontRenderer fontRenderer, 
                                                  float x, float y, float width, float height, String skinId, boolean withBackdrop) {
        int textureId = getTextureId(skinId);
        if (textureId <= 0) {
            // Log missing texture once
            if (!loggedMissing.contains(skinId)) {
                System.out.println("[SkinAtlas] Missing texture for skin: " + skinId);
                loggedMissing.add(skinId);
            }
            
            // Draw enhanced placeholder with better visibility
            // Outer background - brighter and more visible
            renderer.drawRect(x, y, width, height, 0.35f, 0.38f, 0.45f, 0.85f);
            
            // Inner indicator with gradient effect
            float inset = Math.min(width, height) * 0.12f;
            float innerX = x + inset;
            float innerY = y + inset;
            float innerWidth = width - inset * 2f;
            float innerHeight = height - inset * 2f;
            
            // Top half - lighter
            renderer.drawRect(innerX, innerY, innerWidth, innerHeight / 2f, 
                0.5f, 0.55f, 0.65f, 0.9f);
            // Bottom half - darker
            renderer.drawRect(innerX, innerY + innerHeight / 2f, innerWidth, innerHeight / 2f, 
                0.4f, 0.45f, 0.55f, 0.9f);
            
            // Draw border frame for definition
            float borderWidth = Math.max(2f, Math.min(width, height) * 0.02f);
            float[] bgColor = {0.35f, 0.38f, 0.45f, 0.85f};
            float[] borderColor = {0.6f, 0.65f, 0.75f, 0.95f};
            renderer.drawBorderedRect(x, y, width, height, borderWidth, bgColor, borderColor);
            
            // Draw "?" text indicator if FontRenderer is available
            if (fontRenderer != null) {
                float textScale = Math.min(width, height) / 40f;
                float textWidth = fontRenderer.getTextWidth("?") * textScale;
                float textHeight = fontRenderer.getTextHeight() * textScale;
                float centerX = x + (width - textWidth) / 2f;
                float centerY = y + (height - textHeight) / 2f;
                
                fontRenderer.drawTextWithShadow("?", centerX, centerY, textScale, 
                    0.7f, 0.75f, 0.85f, 0.9f, 2.0f, 0.7f);
            }
            
            return;
        }
        
        // Draw light background behind texture if requested
        if (withBackdrop) {
            renderer.drawRect(x, y, width, height, 0.92f, 0.92f, 0.95f, 1.0f);
        }
        
        // Render the texture on top
        renderer.drawTexturedRect(x, y, width, height, textureId);
    }

    public synchronized void removeSkin(String name) {
        Texture texture = textures.remove(name);
        if (texture != null) {
            texture.cleanup();
        }
    }

    public synchronized void cleanup() {
        textures.values().forEach(Texture::cleanup);
        textures.clear();
    }

    private ByteBuffer toBuffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }
}
