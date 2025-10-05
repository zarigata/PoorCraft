package com.poorcraft.render;

import org.joml.Vector2f;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Helper class for rendering blur effects using framebuffer objects.
 * Implements a two-pass Gaussian blur (horizontal + vertical).
 */
public class BlurRenderer {
    
    private Shader blurShader;
    private int vao;
    private int vbo;
    private boolean initialized;
    
    /**
     * Initializes the blur renderer.
     * Creates shader and full-screen quad geometry.
     */
    public void init() {
        try {
            String vertSource = Files.readString(Paths.get("src/main/resources/shaders/blur.vert"));
            String fragSource = Files.readString(Paths.get("src/main/resources/shaders/blur.frag"));
            
            blurShader = new Shader(vertSource, fragSource);
            System.out.println("[BlurRenderer] Blur shaders compiled successfully");
        } catch (Exception e) {
            System.err.println("[BlurRenderer] Failed to load blur shaders: " + e.getMessage());
            throw new RuntimeException("Blur shader initialization failed", e);
        }
        
        // Create full-screen quad in NDC space (-1 to 1)
        float[] quadVertices = {
            // Position (x, y)  TexCoord (u, v)
            -1.0f,  1.0f,      0.0f, 1.0f,  // Top-left
             1.0f,  1.0f,      1.0f, 1.0f,  // Top-right
             1.0f, -1.0f,      1.0f, 0.0f,  // Bottom-right
            
             1.0f, -1.0f,      1.0f, 0.0f,  // Bottom-right
            -1.0f, -1.0f,      0.0f, 0.0f,  // Bottom-left
            -1.0f,  1.0f,      0.0f, 1.0f   // Top-left
        };
        
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        
        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // TexCoord attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
        
        initialized = true;
        System.out.println("[BlurRenderer] Initialized");
    }
    
    /**
     * Renders a blur pass.
     * 
     * @param sourceTexture Source texture to blur
     * @param targetFbo Target framebuffer (0 for default framebuffer)
     * @param horizontal True for horizontal pass, false for vertical
     * @param texWidth Texture width
     * @param texHeight Texture height
     */
    public void renderBlurPass(int sourceTexture, int targetFbo, boolean horizontal, int texWidth, int texHeight) {
        if (!initialized) {
            System.err.println("[BlurRenderer] Not initialized!");
            return;
        }
        
        // Bind target framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, targetFbo);
        
        // Clear and set viewport
        glClear(GL_COLOR_BUFFER_BIT);
        glViewport(0, 0, texWidth, texHeight);
        
        // Disable depth test and enable blending
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        
        // Bind shader and set uniforms
        blurShader.bind();
        
        // Set blur direction
        Vector2f direction = horizontal ? new Vector2f(1.0f, 0.0f) : new Vector2f(0.0f, 1.0f);
        blurShader.setUniform("uDirection", direction);
        
        // Set texel size
        Vector2f texelSize = new Vector2f(1.0f / texWidth, 1.0f / texHeight);
        blurShader.setUniform("uTexelSize", texelSize);
        
        // Bind source texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sourceTexture);
        blurShader.setUniform("uTexture", 0);
        
        // Draw full-screen quad
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        blurShader.unbind();
        
        // Unbind framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Renders a texture to the screen without blur.
     * Used for fallback or final composite.
     * 
     * @param texture Texture to render
     * @param targetFbo Target framebuffer (0 for default)
     * @param width Viewport width
     * @param height Viewport height
     */
    public void renderTexture(int texture, int targetFbo, int width, int height) {
        if (!initialized) {
            return;
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, targetFbo);
        glViewport(0, 0, width, height);
        
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        blurShader.bind();
        blurShader.setUniform("uDirection", new Vector2f(0.0f, 0.0f)); // No blur
        blurShader.setUniform("uTexelSize", new Vector2f(0.0f, 0.0f));
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        blurShader.setUniform("uTexture", 0);
        
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        blurShader.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Checks if the blur renderer is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (blurShader != null) {
            blurShader.cleanup();
        }
        initialized = false;
        System.out.println("[BlurRenderer] Cleaned up");
    }
}
