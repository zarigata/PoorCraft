package com.poorcraft.player;

import com.poorcraft.render.Texture;
import com.poorcraft.ui.UIRenderer;
import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Manages OpenGL textures for player skins.
 */
public class SkinAtlas {

    private final Map<String, Texture> textures;

    public SkinAtlas() {
        this.textures = new HashMap<>();
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
        int textureId = getTextureId(skinId);
        if (textureId <= 0) {
            renderer.drawRect(x, y, width, height, 0.22f, 0.25f, 0.3f, 0.6f);
            float inset = Math.min(width, height) * 0.18f;
            renderer.drawRect(x + inset, y + inset, width - inset * 2f, height - inset * 2f,
                0.6f, 0.22f, 0.22f, 0.65f);
            return;
        }
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
