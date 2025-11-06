package com.poorcraft.ui;

import com.poorcraft.render.Shader;
import org.joml.Matrix4f;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * 2D renderer for UI elements using orthographic projection.
 * 
 * This is a simple immediate-mode renderer that draws colored and textured quads.
 * Uses orthographic projection to map screen coordinates directly to pixels.
 * 
 * The coordinate system has origin at top-left (0,0) with Y increasing downward,
 * matching standard UI conventions. This makes positioning UI elements intuitive.
 * 
 * I spent way too long getting the projection matrix right. Turns out flipping Y
 * is important for UI stuff. Who knew? (Everyone. Everyone knew.)
 */
public class UIRenderer {
    
    private Shader uiShader;
    private int vao;
    private int vbo;
    private Matrix4f projectionMatrix;
    private Matrix4f modelMatrix;
    private int windowWidth;
    private int windowHeight;
    private UIScaleManager scaleManager;
    
    /**
     * Creates a new UI renderer.
     * Call init() before using.
     */
    public UIRenderer() {
        this.projectionMatrix = new Matrix4f();
        this.modelMatrix = new Matrix4f();
    }
    
    /**
     * Initializes the UI renderer with window dimensions.
     * Creates shaders, VAO/VBO for quad rendering, and sets up projection.
     * 
     * @param windowWidth Window width in pixels
     * @param windowHeight Window height in pixels
     */
    public void init(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;

        try {
            // Load shader sources from resources
            String vertSource = Files.readString(Paths.get("src/main/resources/shaders/ui.vert"));
            String fragSource = Files.readString(Paths.get("src/main/resources/shaders/ui.frag"));
            
            uiShader = new Shader(vertSource, fragSource);
            
            System.out.println("[UIRenderer] Shaders compiled successfully");
        } catch (Exception e) {
            System.err.println("[UIRenderer] Failed to load UI shaders: " + e.getMessage());
            throw new RuntimeException("UI shader initialization failed", e);
        }
        
        // Create VAO and VBO for a unit quad (0,0 to 1,1)
        // We'll scale and translate this quad to draw UI elements
        float[] quadVertices = {
            // Position (x, y)  TexCoord (u, v)
            0.0f, 0.0f,         0.0f, 0.0f,  // Top-left
            1.0f, 0.0f,         1.0f, 0.0f,  // Top-right
            1.0f, 1.0f,         1.0f, 1.0f,  // Bottom-right
            
            1.0f, 1.0f,         1.0f, 1.0f,  // Bottom-right
            0.0f, 1.0f,         0.0f, 1.0f,  // Bottom-left
            0.0f, 0.0f,         0.0f, 0.0f   // Top-left
        };
        
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        
        // Position attribute (location = 0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // TexCoord attribute (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
        
        // Set up orthographic projection
        updateProjection(windowWidth, windowHeight);
        
        System.out.println("[UIRenderer] Initialized with window size: " + windowWidth + "x" + windowHeight);
    }
    
    /**
     * Updates the orthographic projection matrix for new window dimensions.
     * 
     * Projection maps screen coordinates to clip space:
     * - Left = 0, Right = width (X increases right)
     * - Top = 0, Bottom = height (Y increases down)
     * 
     * @param windowWidth New window width
     * @param windowHeight New window height
     */
    public void updateProjection(int windowWidth, int windowHeight) {
        // Orthographic projection: left, right, bottom, top, near, far
        // Note: bottom > top to flip Y axis (top-left origin)
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        projectionMatrix.identity().ortho(0, windowWidth, windowHeight, 0, -1, 1);
    }
    
    /**
     * Begins UI rendering.
     * Call this before drawing any UI elements.
     * 
     * Disables depth testing and enables alpha blending for UI transparency.
     */
    public void begin() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        uiShader.bind();
        uiShader.setUniform("uProjection", projectionMatrix);
    }
    
    /**
     * Draws a solid color rectangle.
     *
     * @param x      X position (pixels from left)
     * @param y      Y position (pixels from top)
     * @param width  rectangle width in pixels
     * @param height rectangle height in pixels
     * @param r      red component (0.0 to 1.0)
     * @param g      green component (0.0 to 1.0)
     * @param b      blue component (0.0 to 1.0)
     * @param a      alpha component (0.0 to 1.0)
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        // Build model matrix: translate to position, scale to size
        modelMatrix.identity()
            .translate(x, y, 0)
            .scale(width, height, 1);

        uiShader.setUniform("uModel", modelMatrix);
        uiShader.setUniform("uColor", r, g, b, a);
        uiShader.setUniform("uUseTexture", false);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    /**
     * Draws a semi-transparent drop shadow behind a rectangular element.
     *
     * @param x            source element X coordinate
     * @param y            source element Y coordinate
     * @param width        element width
     * @param height       element height
     * @param shadowOffset offset applied to both axes (pixels)
     * @param shadowAlpha  alpha value for the shadow (0.0-1.0)
     */
    public void drawDropShadow(float x, float y, float width, float height,
                               float shadowOffset, float shadowAlpha) {
        float shadowX = x + shadowOffset;
        float shadowY = y + shadowOffset;
        drawRect(shadowX, shadowY, width, height, 0f, 0f, 0f, shadowAlpha);
    }

    /**
     * Draws a recessed (inset) panel with darker top/left borders and
     * lighter bottom/right borders to simulate depth.
     */
    public void drawInsetPanel(float x, float y, float width, float height,
                               float r, float g, float b, float a) {
        drawRect(x, y, width, height, r, g, b, a);

        float border = Math.max(2f, Math.min(width, height) * 0.015f);

        float darkR = r * 0.6f;
        float darkG = g * 0.6f;
        float darkB = b * 0.6f;

        float lightR = Math.min(1f, r * 1.15f);
        float lightG = Math.min(1f, g * 1.15f);
        float lightB = Math.min(1f, b * 1.15f);

        // Top shadow
        drawRect(x, y, width, border, darkR, darkG, darkB, a);
        // Left shadow
        drawRect(x, y, border, height, darkR, darkG, darkB, a);
        // Bottom highlight
        drawRect(x, y + height - border, width, border, lightR, lightG, lightB, a);
        // Right highlight
        drawRect(x + width - border, y, border, height, lightR, lightG, lightB, a);
    }

    /**
     * Draws a raised (outset) panel with lighter top/left borders and
     * darker bottom/right borders to simulate elevation.
     */
    public void drawOutsetPanel(float x, float y, float width, float height,
                                float r, float g, float b, float a) {
        drawRect(x, y, width, height, r, g, b, a);

        float border = Math.max(2f, Math.min(width, height) * 0.015f);

        float darkR = r * 0.65f;
        float darkG = g * 0.65f;
        float darkB = b * 0.65f;

        float lightR = Math.min(1f, r * 1.2f);
        float lightG = Math.min(1f, g * 1.2f);
        float lightB = Math.min(1f, b * 1.2f);

        // Top highlight
        drawRect(x, y, width, border, lightR, lightG, lightB, a);
        // Left highlight
        drawRect(x, y, border, height, lightR, lightG, lightB, a);
        // Bottom shadow
        drawRect(x, y + height - border, width, border, darkR, darkG, darkB, a);
        // Right shadow
        drawRect(x + width - border, y, border, height, darkR, darkG, darkB, a);
    }

    /**
     * Draws a rectangle with a configurable border.
     */
    public void drawBorderedRect(float x, float y, float width, float height,
                                 float borderWidth, float[] bgColor, float[] borderColor) {
        if (borderWidth > 0f) {
            drawRect(x, y, width, height,
                    borderColor[0], borderColor[1], borderColor[2], borderColor[3]);

            float innerX = x + borderWidth;
            float innerY = y + borderWidth;
            float innerWidth = Math.max(0f, width - borderWidth * 2f);
            float innerHeight = Math.max(0f, height - borderWidth * 2f);

            drawRect(innerX, innerY, innerWidth, innerHeight,
                    bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
        } else {
            drawRect(x, y, width, height,
                    bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
        }
    }

    public void drawTexturedRect(float x, float y, float width, float height, int textureId) {
        drawTexturedRect(x, y, width, height, textureId, 1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    /**
     * Draws a textured rectangle with color tint.
     * 
     * @param x X position
     * @param y Y position
     * @param width Rectangle width
     * @param height Rectangle height
     * @param textureId OpenGL texture ID
     * @param r Red tint (0.0 to 1.0)
     * @param g Green tint (0.0 to 1.0)
     * @param b Blue tint (0.0 to 1.0)
     * @param a Alpha (0.0 to 1.0)
     */
    public void drawTexturedRect(float x, float y, float width, float height, int textureId, 
                                  float r, float g, float b, float a) {
        modelMatrix.identity()
            .translate(x, y, 0)
            .scale(width, height, 1);
        
        uiShader.setUniform("uModel", modelMatrix);
        uiShader.setUniform("uColor", r, g, b, a);
        uiShader.setUniform("uUseTexture", true);
        
        glBindTexture(GL_TEXTURE_2D, textureId);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }
    
    /**
     * Gets the VAO for direct vertex data manipulation.
     * Used by FontRenderer for batched text rendering with custom UVs.
     * 
     * @return VAO handle
     */
    public int getVAO() {
        return vao;
    }
    
    /**
     * Gets the VBO for direct vertex data manipulation.
     * Used by FontRenderer for batched text rendering with custom UVs.
     * 
     * @return VBO handle
     */
    public int getVBO() {
        return vbo;
    }
    
    /**
     * Gets the UI shader.
     * Used by FontRenderer to set uniforms for text rendering.
     * 
     * @return UI shader
     */
    public Shader getShader() {
        return uiShader;
    }
    
    /**
     * Ends UI rendering.
     * Call this after drawing all UI elements.
     * 
     * Re-enables depth testing for 3D rendering.
     */
    public void end() {
        uiShader.unbind();
        glEnable(GL_DEPTH_TEST);
    }
    
    /**
     * Sets the scale manager for percentage-based layout helpers.
     * 
     * @param manager UI scale manager instance
     */
    public void setScaleManager(UIScaleManager manager) {
        this.scaleManager = manager;
    }
    
    /**
     * Draws a rectangle using percentages of window size.
     * Requires scaleManager to be set.
     * 
     * @param xPercent X position as percentage of window width (0.0 to 1.0)
     * @param yPercent Y position as percentage of window height (0.0 to 1.0)
     * @param widthPercent Width as percentage of window width (0.0 to 1.0)
     * @param heightPercent Height as percentage of window height (0.0 to 1.0)
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     */
    public void drawRectPercent(float xPercent, float yPercent, float widthPercent, float heightPercent,
                                float r, float g, float b, float a) {
        if (scaleManager == null) {
            // Fallback to direct pixel calculations if no scale manager
            return;
        }
        
        float x = scaleManager.scaleWidth(xPercent);
        float y = scaleManager.scaleHeight(yPercent);
        float width = scaleManager.scaleWidth(widthPercent);
        float height = scaleManager.scaleHeight(heightPercent);
        
        drawRect(x, y, width, height, r, g, b, a);
    }
    
    /**
     * Draws a rectangle with dimensions scaled by scaleManager.
     * Requires scaleManager to be set.
     * 
     * @param x X position (pixels)
     * @param y Y position (pixels)
     * @param width Width (pixels at reference resolution)
     * @param height Height (pixels at reference resolution)
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     */
    public void drawScaledRect(float x, float y, float width, float height,
                               float r, float g, float b, float a) {
        if (scaleManager == null) {
            // Fallback to unscaled drawing
            drawRect(x, y, width, height, r, g, b, a);
            return;
        }
        
        float scaledWidth = scaleManager.scaleDimension(width);
        float scaledHeight = scaleManager.scaleDimension(height);
        
        drawRect(x, y, scaledWidth, scaledHeight, r, g, b, a);
    }
    
    /**
     * Converts X percentage to pixels using scaleManager.
     * 
     * @param percent X percentage (0.0 to 1.0)
     * @return X position in pixels
     */
    public float toPixelsX(float percent) {
        return scaleManager != null ? scaleManager.scaleWidth(percent) : 0;
    }
    
    /**
     * Converts Y percentage to pixels using scaleManager.
     * 
     * @param percent Y percentage (0.0 to 1.0)
     * @return Y position in pixels
     */
    public float toPixelsY(float percent) {
        return scaleManager != null ? scaleManager.scaleHeight(percent) : 0;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }
    
    /**
     * Converts width percentage to pixels using scaleManager.
     * 
     * @param percent Width percentage (0.0 to 1.0)
     * @return Width in pixels
     */
    public float toPixelsWidth(float percent) {
        return scaleManager != null ? scaleManager.scaleWidth(percent) : 0;
    }
    
    /**
     * Converts height percentage to pixels using scaleManager.
     * 
     * @param percent Height percentage (0.0 to 1.0)
     * @return Height in pixels
     */
    public float toPixelsHeight(float percent) {
        return scaleManager != null ? scaleManager.scaleHeight(percent) : 0;
    }
    
    /**
     * Draws a vertical gradient rectangle from top color to bottom color.
     * 
     * @param x X position
     * @param y Y position
     * @param width Rectangle width
     * @param height Rectangle height
     * @param topColor Top color [r, g, b, a]
     * @param bottomColor Bottom color [r, g, b, a]
     */
    public void drawGradientRect(float x, float y, float width, float height, 
                                  float[] topColor, float[] bottomColor) {
        // Draw gradient using multiple horizontal strips for smooth transition
        int stripCount = 8;
        float stripHeight = height / stripCount;
        
        for (int i = 0; i < stripCount; i++) {
            float t = i / (float) stripCount;
            float stripY = y + i * stripHeight;
            
            // Linear interpolation between top and bottom colors
            float r = topColor[0] * (1 - t) + bottomColor[0] * t;
            float g = topColor[1] * (1 - t) + bottomColor[1] * t;
            float b = topColor[2] * (1 - t) + bottomColor[2] * t;
            float a = topColor[3] * (1 - t) + bottomColor[3] * t;
            
            drawRect(x, stripY, width, stripHeight, r, g, b, a);
        }
    }
    
    /**
     * Draws a glowing border effect around a rectangle.
     * Creates multiple concentric borders with decreasing alpha for a smooth glow.
     * 
     * @param x X position
     * @param y Y position
     * @param width Rectangle width
     * @param height Rectangle height
     * @param borderWidth Thickness of the glow effect
     * @param glowIntensity Multiplier for alpha (0.0 to 1.0)
     * @param color Base color [r, g, b, a] for the glow
     */
    public void drawGlowBorder(float x, float y, float width, float height, 
                               float borderWidth, float glowIntensity, float[] color) {
        int layers = 4;
        
        for (int i = 0; i < layers; i++) {
            float layerProgress = i / (float) layers;
            float alpha = color[3] * glowIntensity * (1.0f - layerProgress);
            float offset = borderWidth * layerProgress;
            
            float layerX = x - offset;
            float layerY = y - offset;
            float layerWidth = width + offset * 2f;
            float layerHeight = height + offset * 2f;
            float thickness = borderWidth / layers;
            
            // Top edge
            drawRect(layerX, layerY, layerWidth, thickness, 
                color[0], color[1], color[2], alpha);
            // Bottom edge
            drawRect(layerX, layerY + layerHeight - thickness, layerWidth, thickness, 
                color[0], color[1], color[2], alpha);
            // Left edge
            drawRect(layerX, layerY, thickness, layerHeight, 
                color[0], color[1], color[2], alpha);
            // Right edge
            drawRect(layerX + layerWidth - thickness, layerY, thickness, layerHeight, 
                color[0], color[1], color[2], alpha);
        }
    }
    
    /**
     * Draws a prominent frame/border with rounded corners effect.
     * Includes inner highlight line for depth.
     * 
     * @param x X position
     * @param y Y position
     * @param width Rectangle width
     * @param height Rectangle height
     * @param thickness Border thickness
     * @param color Border color [r, g, b, a]
     */
    public void drawHighlightFrame(float x, float y, float width, float height, 
                                    float thickness, float[] color) {
        // Draw outer border
        // Top
        drawRect(x, y, width, thickness, color[0], color[1], color[2], color[3]);
        // Bottom
        drawRect(x, y + height - thickness, width, thickness, 
            color[0], color[1], color[2], color[3]);
        // Left
        drawRect(x, y, thickness, height, color[0], color[1], color[2], color[3]);
        // Right
        drawRect(x + width - thickness, y, thickness, height, 
            color[0], color[1], color[2], color[3]);
        
        // Draw inner highlight (brighter)
        float highlightR = Math.min(1.0f, color[0] * 1.3f);
        float highlightG = Math.min(1.0f, color[1] * 1.3f);
        float highlightB = Math.min(1.0f, color[2] * 1.3f);
        float highlightThickness = Math.max(1f, thickness * 0.3f);
        
        float innerOffset = thickness;
        // Top highlight
        drawRect(x + innerOffset, y + innerOffset, width - innerOffset * 2f, highlightThickness, 
            highlightR, highlightG, highlightB, color[3]);
        // Left highlight
        drawRect(x + innerOffset, y + innerOffset, highlightThickness, height - innerOffset * 2f, 
            highlightR, highlightG, highlightB, color[3]);
        
        // Draw corner emphasis squares
        float cornerSize = thickness * 1.5f;
        // Top-left
        drawRect(x, y, cornerSize, cornerSize, color[0], color[1], color[2], color[3]);
        // Top-right
        drawRect(x + width - cornerSize, y, cornerSize, cornerSize, 
            color[0], color[1], color[2], color[3]);
        // Bottom-left
        drawRect(x, y + height - cornerSize, cornerSize, cornerSize, 
            color[0], color[1], color[2], color[3]);
        // Bottom-right
        drawRect(x + width - cornerSize, y + height - cornerSize, cornerSize, cornerSize, 
            color[0], color[1], color[2], color[3]);
    }
    
    /**
     * Draws a panel background suitable for content display.
     * 
     * @param x X position
     * @param y Y position
     * @param width Rectangle width
     * @param height Rectangle height
     * @param baseColor Base color [r, g, b, a]
     * @param withGradient Whether to use gradient or solid color
     */
    public void drawBackdropPanel(float x, float y, float width, float height, 
                                   float[] baseColor, boolean withGradient) {
        if (withGradient) {
            // Create slightly darker bottom color
            float[] bottomColor = {
                baseColor[0] * 0.85f,
                baseColor[1] * 0.85f,
                baseColor[2] * 0.85f,
                baseColor[3]
            };
            drawGradientRect(x, y, width, height, baseColor, bottomColor);
        } else {
            drawRect(x, y, width, height, baseColor[0], baseColor[1], baseColor[2], baseColor[3]);
        }
        
        // Add subtle border
        float[] borderColor = {
            baseColor[0] * 0.7f,
            baseColor[1] * 0.7f,
            baseColor[2] * 0.7f,
            baseColor[3]
        };
        drawBorderedRect(x, y, width, height, 2f, baseColor, borderColor);
        
        // Add drop shadow for depth
        drawDropShadow(x, y, width, height, 3f, 0.3f);
    }
    
    /**
     * Cleans up OpenGL resources.
     * Call when shutting down the game.
     */
    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        if (uiShader != null) {
            uiShader.cleanup();
        }
        System.out.println("[UIRenderer] Cleaned up");
    }
}
