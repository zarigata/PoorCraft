package com.poorcraft.render;

import com.poorcraft.world.chunk.ChunkMesh;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Manages OpenGL buffers (VAO/VBO/EBO) for a single chunk's mesh data.
 * 
 * This class handles uploading mesh data to the GPU and rendering it.
 * When a chunk's mesh changes (blocks placed/destroyed), the buffers are updated.
 * 
 * The vertex format matches the shader expectations:
 * - Position: 3 floats (location 0)
 * - TexCoord: 2 floats (location 1)
 * - Normal: 3 floats (location 2)
 * Total: 8 floats per vertex
 * 
 * @author PoorCraft Team
 */
public class ChunkRenderData {
    
    private int vao;
    private int vbo;
    private int ebo;
    private int indexCount;
    private boolean initialized;
    
    /**
     * Creates uninitialized chunk render data.
     * OpenGL resources are created lazily when mesh data is first uploaded.
     */
    public ChunkRenderData() {
        this.vao = 0;
        this.vbo = 0;
        this.ebo = 0;
        this.indexCount = 0;
        this.initialized = false;
    }
    
    /**
     * Uploads mesh data to the GPU.
     * Creates OpenGL resources if they don't exist yet.
     * 
     * @param mesh Chunk mesh data to upload
     */
    public void uploadMesh(ChunkMesh mesh) {
        // If mesh is empty, cleanup and return
        if (mesh == null || mesh.getVertexCount() == 0) {
            cleanup();
            return;
        }
        
        // Create OpenGL resources if not initialized
        if (!initialized) {
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            ebo = glGenBuffers();
            initialized = true;
        }
        
        // Bind VAO
        glBindVertexArray(vao);
        
        // Upload vertex data to VBO
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(mesh.getVertices().length);
        vertexBuffer.put(mesh.getVertices());
        vertexBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        // Upload index data to EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(mesh.getIndices().length);
        indexBuffer.put(mesh.getIndices());
        indexBuffer.flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        
        // Set up vertex attributes
        // These must match the shader layout locations
        
        // Attribute 0: Position (3 floats)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Attribute 1: TexCoord (2 floats)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // Attribute 2: Normal (3 floats)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 8 * Float.BYTES, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        // Unbind VAO
        glBindVertexArray(0);
        
        // Store index count for rendering
        indexCount = mesh.getIndexCount();
    }
    
    /**
     * Renders this chunk's mesh.
     * Does nothing if no mesh data has been uploaded.
     */
    public void render() {
        if (!initialized || indexCount == 0) {
            return; // Nothing to render
        }
        
        // Bind VAO and draw
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Cleans up OpenGL resources.
     * Should be called when the chunk is unloaded.
     */
    public void cleanup() {
        if (initialized) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
            vao = 0;
            vbo = 0;
            ebo = 0;
            indexCount = 0;
            initialized = false;
        }
    }
    
    /**
     * Checks if OpenGL resources have been initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets the number of indices to render.
     * 
     * @return Index count
     */
    public int getIndexCount() {
        return indexCount;
    }
}
