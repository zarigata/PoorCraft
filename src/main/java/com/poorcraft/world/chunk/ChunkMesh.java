package com.poorcraft.world.chunk;

/**
 * Data container for chunk mesh data.
 * 
 * This is a DTO (Data Transfer Object) that holds vertex and index data
 * for rendering. The actual OpenGL buffer creation happens in the Rendering phase.
 * 
 * Think of this as the blueprint that the renderer uses to build the actual mesh.
 * Like IKEA instructions but for voxels.
 */
public class ChunkMesh {
    
    private final float[] vertices;
    private final int[] indices;
    private final int vertexCount;
    private final int indexCount;
    private final boolean empty;
    
    /**
     * Creates a new chunk mesh with the given vertex and index data.
     * 
     * @param vertices Vertex data (position + texture coords + normals)
     * @param indices Index data for indexed drawing
     */
    public ChunkMesh(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
        this.vertexCount = vertices.length;
        this.indexCount = indices.length;
        this.empty = vertexCount == 0;
    }
    
    /**
     * Creates an empty mesh with no geometry.
     * Used for chunks that are entirely air or underground.
     * 
     * @return An empty ChunkMesh
     */
    public static ChunkMesh empty() {
        return new ChunkMesh(new float[0], new int[0]);
    }
    
    /**
     * Gets the vertex data array.
     * 
     * @return Vertex data (position + texture coords + normals)
     */
    public float[] getVertices() {
        return vertices;
    }
    
    /**
     * Gets the index data array.
     * 
     * @return Index data for indexed drawing
     */
    public int[] getIndices() {
        return indices;
    }
    
    /**
     * Gets the number of vertices.
     * 
     * @return Vertex count
     */
    public int getVertexCount() {
        return vertexCount;
    }
    
    /**
     * Gets the number of indices.
     * 
     * @return Index count
     */
    public int getIndexCount() {
        return indexCount;
    }
    
    /**
     * Checks if this mesh is empty (no visible faces).
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return empty;
    }
}
