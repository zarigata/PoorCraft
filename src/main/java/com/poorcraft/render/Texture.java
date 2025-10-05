package com.poorcraft.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * Wraps an OpenGL texture for loading and managing 2D textures.
 * 
 * This class uses STB Image to load textures from files or resources,
 * uploads them to the GPU, and manages their lifecycle.
 * 
 * Textures are configured with GL_NEAREST filtering for a pixelated voxel aesthetic.
 * 
 * @author PoorCraft Team
 */
public class Texture {
    
    private int textureId;
    private int width;
    private int height;
    
    /**
     * Private constructor. Use static factory methods to create textures.
     * 
     * @param textureId OpenGL texture ID
     * @param width Texture width in pixels
     * @param height Texture height in pixels
     */
    private Texture(int textureId, int width, int height) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Loads a texture from a file on the filesystem.
     * 
     * @param path File path to the texture image
     * @return Loaded Texture instance
     * @throws RuntimeException if loading fails
     */
    public static Texture loadFromFile(String path) {
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer channelsBuffer = BufferUtils.createIntBuffer(1);
        
        // Load image data - force RGBA format (4 channels)
        ByteBuffer imageBuffer = stbi_load(path, widthBuffer, heightBuffer, channelsBuffer, 4);
        
        if (imageBuffer == null) {
            throw new RuntimeException("Failed to load texture: " + path + " - " + stbi_failure_reason());
        }
        
        int width = widthBuffer.get(0);
        int height = heightBuffer.get(0);
        
        // Create and configure OpenGL texture
        int textureId = createTexture(imageBuffer, width, height);
        
        // Free STB image data - OpenGL has a copy now
        stbi_image_free(imageBuffer);
        
        return new Texture(textureId, width, height);
    }
    
    /**
     * Loads a texture from a classpath resource.
     * 
     * @param resourcePath Path to the texture resource (e.g., "/textures/blocks/stone.png")
     * @return Loaded Texture instance
     * @throws RuntimeException if loading fails
     */
    public static Texture loadFromResource(String resourcePath) {
        try {
            InputStream inputStream = com.poorcraft.resources.ResourceManager.getInstance()
                    .loadResourceStream(resourcePath);
            
            // Read all bytes into a ByteBuffer
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            
            IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer channelsBuffer = BufferUtils.createIntBuffer(1);
            
            // Load from memory instead of file
            ByteBuffer imageBuffer = stbi_load_from_memory(buffer, widthBuffer, heightBuffer, channelsBuffer, 4);
            
            if (imageBuffer == null) {
                throw new RuntimeException("Failed to load texture from resource: " + resourcePath + 
                        " - " + stbi_failure_reason());
            }
            
            int width = widthBuffer.get(0);
            int height = heightBuffer.get(0);
            
            int textureId = createTexture(imageBuffer, width, height);
            
            stbi_image_free(imageBuffer);
            
            return new Texture(textureId, width, height);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture resource: " + resourcePath, e);
        }
    }
    
    /**
     * Creates a texture from raw RGBA data.
     * Useful for procedurally generated textures or texture atlases.
     * 
     * @param data Raw RGBA pixel data (4 bytes per pixel)
     * @param width Texture width in pixels
     * @param height Texture height in pixels
     * @return Created Texture instance
     */
    public static Texture createFromData(ByteBuffer data, int width, int height) {
        int textureId = createTexture(data, width, height);
        return new Texture(textureId, width, height);
    }
    
    /**
     * Creates an OpenGL texture from image data.
     * 
     * @param data RGBA pixel data
     * @param width Texture width
     * @param height Texture height
     * @return OpenGL texture ID
     */
    private static int createTexture(ByteBuffer data, int width, int height) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        // Set texture parameters - GL_NEAREST for that crispy pixel art look
        // Remember the good old days when everything was pixelated? Yeah, we're bringing that back
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        
        // Upload texture data to GPU
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        
        // Generate mipmaps for better quality at distance
        glGenerateMipmap(GL_TEXTURE_2D);
        
        glBindTexture(GL_TEXTURE_2D, 0);
        
        return textureId;
    }
    
    /**
     * Binds this texture to GL_TEXTURE_2D.
     */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Binds this texture to a specific texture unit.
     * 
     * @param unit Texture unit (0-31)
     */
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Unbinds the current texture.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Gets the OpenGL texture ID.
     * 
     * @return Texture ID
     */
    public int getId() {
        return textureId;
    }
    
    /**
     * Gets the texture width in pixels.
     * 
     * @return Width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the texture height in pixels.
     * 
     * @return Height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Deletes the texture and frees GPU resources.
     */
    public void cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
    }
}
