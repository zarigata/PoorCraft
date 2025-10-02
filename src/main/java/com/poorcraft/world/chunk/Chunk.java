package com.poorcraft.world.chunk;

import com.poorcraft.world.block.BlockType;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Chunk class with block storage and mesh generation.
 * 
 * Chunks are 16x256x16 blocks stored in a flat array for cache efficiency.
 * Mesh generation uses simple face culling - greedy meshing can be added later.
 * 
 * This is where the magic happens. Or where your FPS drops to 2. One of the two.
 */
public class Chunk {
    
    public static final int CHUNK_SIZE = 16;        // Width and depth
    public static final int CHUNK_HEIGHT = 256;     // Height
    public static final int CHUNK_VOLUME = CHUNK_SIZE * CHUNK_HEIGHT * CHUNK_SIZE;
    
    private final ChunkPos position;
    private final BlockType[] blocks;
    private ChunkMesh mesh;
    private boolean meshDirty;
    
    // Neighbor references for face culling across chunk boundaries
    private Chunk northNeighbor;  // -Z
    private Chunk southNeighbor;  // +Z
    private Chunk eastNeighbor;   // +X
    private Chunk westNeighbor;   // -X
    
    /**
     * Creates a new chunk at the specified position.
     * Initializes all blocks to AIR.
     * 
     * @param position Chunk coordinates
     */
    public Chunk(ChunkPos position) {
        this.position = position;
        this.blocks = new BlockType[CHUNK_VOLUME];
        this.meshDirty = true;
        
        // Initialize all blocks to AIR
        // Because uninitialized memory is scary
        for (int i = 0; i < CHUNK_VOLUME; i++) {
            blocks[i] = BlockType.AIR;
        }
    }
    
    /**
     * Converts 3D coordinates to flat array index.
     * Uses Y-major ordering: y * CHUNK_SIZE * CHUNK_SIZE + z * CHUNK_SIZE + x
     * 
     * @param x Local X coordinate (0-15)
     * @param y Local Y coordinate (0-255)
     * @param z Local Z coordinate (0-15)
     * @return Flat array index
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    private int getBlockIndex(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            throw new IllegalArgumentException(
                "Block coordinates out of bounds: (" + x + ", " + y + ", " + z + ")"
            );
        }
        return y * CHUNK_SIZE * CHUNK_SIZE + z * CHUNK_SIZE + x;
    }
    
    /**
     * Gets the block type at the specified local coordinates.
     * 
     * @param x Local X coordinate (0-15)
     * @param y Local Y coordinate (0-255)
     * @param z Local Z coordinate (0-15)
     * @return Block type at that position
     */
    public BlockType getBlock(int x, int y, int z) {
        return blocks[getBlockIndex(x, y, z)];
    }
    
    /**
     * Sets the block type at the specified local coordinates.
     * Marks the mesh as dirty for regeneration.
     * 
     * @param x Local X coordinate (0-15)
     * @param y Local Y coordinate (0-255)
     * @param z Local Z coordinate (0-15)
     * @param type Block type to set
     */
    public void setBlock(int x, int y, int z, BlockType type) {
        blocks[getBlockIndex(x, y, z)] = type;
        meshDirty = true;
    }
    
    /**
     * Checks if the block at the specified coordinates is air.
     * 
     * @param x Local X coordinate
     * @param y Local Y coordinate
     * @param z Local Z coordinate
     * @return true if air
     */
    public boolean isBlockAir(int x, int y, int z) {
        return getBlock(x, y, z) == BlockType.AIR;
    }
    
    /**
     * Sets a neighbor chunk reference for face culling.
     * 
     * @param direction Direction (0=north/-Z, 1=south/+Z, 2=east/+X, 3=west/-X)
     * @param neighbor Neighbor chunk
     */
    public void setNeighbor(int direction, Chunk neighbor) {
        switch (direction) {
            case 0 -> northNeighbor = neighbor;
            case 1 -> southNeighbor = neighbor;
            case 2 -> eastNeighbor = neighbor;
            case 3 -> westNeighbor = neighbor;
        }
        meshDirty = true;
    }
    
    /**
     * Gets a block at the specified coordinates, checking neighbors if out of bounds.
     * Returns AIR if neighbor doesn't exist or Y is out of world bounds.
     * 
     * @param x Local X coordinate (can be outside 0-15)
     * @param y Local Y coordinate (can be outside 0-255)
     * @param z Local Z coordinate (can be outside 0-15)
     * @return Block type at that position, or AIR if unavailable
     */
    private BlockType getBlockOrNeighbor(int x, int y, int z) {
        // Check Y bounds first (no vertical neighbors)
        if (y < 0 || y >= CHUNK_HEIGHT) {
            return BlockType.AIR;
        }
        
        // Check if within this chunk
        if (x >= 0 && x < CHUNK_SIZE && z >= 0 && z < CHUNK_SIZE) {
            return getBlock(x, y, z);
        }
        
        // Check neighbors
        if (x < 0 && westNeighbor != null) {
            return westNeighbor.getBlock(CHUNK_SIZE - 1, y, z);
        }
        if (x >= CHUNK_SIZE && eastNeighbor != null) {
            return eastNeighbor.getBlock(0, y, z);
        }
        if (z < 0 && northNeighbor != null) {
            return northNeighbor.getBlock(x, y, CHUNK_SIZE - 1);
        }
        if (z >= CHUNK_SIZE && southNeighbor != null) {
            return southNeighbor.getBlock(x, y, 0);
        }
        
        // No neighbor available, assume air
        return BlockType.AIR;
    }
    
    /**
     * Generates mesh data for this chunk.
     * Iterates through all blocks and adds faces that are adjacent to air/transparent blocks.
     * 
     * @return Generated chunk mesh
     */
    public ChunkMesh generateMesh() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Iterate through all blocks
        for (int y = 0; y < CHUNK_HEIGHT; y++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    BlockType blockType = getBlock(x, y, z);
                    
                    // Skip air blocks
                    if (blockType == BlockType.AIR) {
                        continue;
                    }
                    
                    // Check all 6 faces
                    // Face 0: Top (+Y)
                    if (shouldRenderFace(x, y + 1, z)) {
                        addFaceVertices(vertices, indices, x, y, z, 0, blockType);
                    }
                    
                    // Face 1: Bottom (-Y)
                    if (shouldRenderFace(x, y - 1, z)) {
                        addFaceVertices(vertices, indices, x, y, z, 1, blockType);
                    }
                    
                    // Face 2: North (-Z)
                    if (shouldRenderFace(x, y, z - 1)) {
                        addFaceVertices(vertices, indices, x, y, z, 2, blockType);
                    }
                    
                    // Face 3: South (+Z)
                    if (shouldRenderFace(x, y, z + 1)) {
                        addFaceVertices(vertices, indices, x, y, z, 3, blockType);
                    }
                    
                    // Face 4: West (-X)
                    if (shouldRenderFace(x - 1, y, z)) {
                        addFaceVertices(vertices, indices, x, y, z, 4, blockType);
                    }
                    
                    // Face 5: East (+X)
                    if (shouldRenderFace(x + 1, y, z)) {
                        addFaceVertices(vertices, indices, x, y, z, 5, blockType);
                    }
                }
            }
        }
        
        // Convert lists to arrays
        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }
        
        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }
        
        // Cache mesh and mark as clean
        mesh = new ChunkMesh(vertexArray, indexArray);
        meshDirty = false;
        
        return mesh;
    }
    
    /**
     * Checks if a face should be rendered at the given coordinates.
     * A face should be rendered if the adjacent block is air or transparent.
     * 
     * @param x Local X coordinate
     * @param y Local Y coordinate
     * @param z Local Z coordinate
     * @return true if face should be rendered
     */
    private boolean shouldRenderFace(int x, int y, int z) {
        BlockType adjacentBlock = getBlockOrNeighbor(x, y, z);
        return adjacentBlock == BlockType.AIR || adjacentBlock.isTransparent();
    }
    
    /**
     * Adds vertices and indices for a single block face.
     * Each face has 4 vertices and 6 indices (2 triangles).
     * 
     * Vertex format: position (3 floats) + texture coords (2 floats) + normal (3 floats) = 8 floats per vertex
     * 
     * @param vertices Vertex list to append to
     * @param indices Index list to append to
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param face Face direction (0=top, 1=bottom, 2=north, 3=south, 4=west, 5=east)
     * @param blockType Block type (for future texture mapping)
     */
    private void addFaceVertices(List<Float> vertices, List<Integer> indices, 
                                  int x, int y, int z, int face, BlockType blockType) {
        int baseIndex = vertices.size() / 8;  // Current vertex count (8 floats per vertex)
        
        // Define face vertices based on direction
        // Texture coords are placeholder (0,0 to 1,1) - actual atlas mapping happens in Rendering phase
        switch (face) {
            case 0 -> addTopFace(vertices, x, y, z);
            case 1 -> addBottomFace(vertices, x, y, z);
            case 2 -> addNorthFace(vertices, x, y, z);
            case 3 -> addSouthFace(vertices, x, y, z);
            case 4 -> addWestFace(vertices, x, y, z);
            case 5 -> addEastFace(vertices, x, y, z);
        }
        
        // Add indices for 2 triangles (0,1,2, 0,2,3 pattern)
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }
    
    // Helper methods to add face vertices
    // Format: position (x,y,z), texCoords (u,v), normal (nx,ny,nz)
    
    private void addTopFace(List<Float> v, int x, int y, int z) {
        float x0 = x, x1 = x + 1;
        float y0 = y + 1;
        float z0 = z, z1 = z + 1;
        
        // Vertex 0
        v.add(x0); v.add(y0); v.add(z0);  // Position
        v.add(0.0f); v.add(0.0f);          // TexCoords
        v.add(0.0f); v.add(1.0f); v.add(0.0f);  // Normal (+Y)
        
        // Vertex 1
        v.add(x1); v.add(y0); v.add(z0);
        v.add(1.0f); v.add(0.0f);
        v.add(0.0f); v.add(1.0f); v.add(0.0f);
        
        // Vertex 2
        v.add(x1); v.add(y0); v.add(z1);
        v.add(1.0f); v.add(1.0f);
        v.add(0.0f); v.add(1.0f); v.add(0.0f);
        
        // Vertex 3
        v.add(x0); v.add(y0); v.add(z1);
        v.add(0.0f); v.add(1.0f);
        v.add(0.0f); v.add(1.0f); v.add(0.0f);
    }
    
    private void addBottomFace(List<Float> v, int x, int y, int z) {
        float x0 = x, x1 = x + 1;
        float y0 = y;
        float z0 = z, z1 = z + 1;
        
        v.add(x0); v.add(y0); v.add(z0);
        v.add(0.0f); v.add(0.0f);
        v.add(0.0f); v.add(-1.0f); v.add(0.0f);  // Normal (-Y)
        
        v.add(x0); v.add(y0); v.add(z1);
        v.add(0.0f); v.add(1.0f);
        v.add(0.0f); v.add(-1.0f); v.add(0.0f);
        
        v.add(x1); v.add(y0); v.add(z1);
        v.add(1.0f); v.add(1.0f);
        v.add(0.0f); v.add(-1.0f); v.add(0.0f);
        
        v.add(x1); v.add(y0); v.add(z0);
        v.add(1.0f); v.add(0.0f);
        v.add(0.0f); v.add(-1.0f); v.add(0.0f);
    }
    
    private void addNorthFace(List<Float> v, int x, int y, int z) {
        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z;
        
        v.add(x0); v.add(y0); v.add(z0);
        v.add(0.0f); v.add(0.0f);
        v.add(0.0f); v.add(0.0f); v.add(-1.0f);  // Normal (-Z)
        
        v.add(x0); v.add(y1); v.add(z0);
        v.add(0.0f); v.add(1.0f);
        v.add(0.0f); v.add(0.0f); v.add(-1.0f);
        
        v.add(x1); v.add(y1); v.add(z0);
        v.add(1.0f); v.add(1.0f);
        v.add(0.0f); v.add(0.0f); v.add(-1.0f);
        
        v.add(x1); v.add(y0); v.add(z0);
        v.add(1.0f); v.add(0.0f);
        v.add(0.0f); v.add(0.0f); v.add(-1.0f);
    }
    
    private void addSouthFace(List<Float> v, int x, int y, int z) {
        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z + 1;
        
        v.add(x0); v.add(y0); v.add(z0);
        v.add(0.0f); v.add(0.0f);
        v.add(0.0f); v.add(0.0f); v.add(1.0f);  // Normal (+Z)
        
        v.add(x1); v.add(y0); v.add(z0);
        v.add(1.0f); v.add(0.0f);
        v.add(0.0f); v.add(0.0f); v.add(1.0f);
        
        v.add(x1); v.add(y1); v.add(z0);
        v.add(1.0f); v.add(1.0f);
        v.add(0.0f); v.add(0.0f); v.add(1.0f);
        
        v.add(x0); v.add(y1); v.add(z0);
        v.add(0.0f); v.add(1.0f);
        v.add(0.0f); v.add(0.0f); v.add(1.0f);
    }
    
    private void addWestFace(List<Float> v, int x, int y, int z) {
        float x0 = x;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;
        
        v.add(x0); v.add(y0); v.add(z0);
        v.add(0.0f); v.add(0.0f);
        v.add(-1.0f); v.add(0.0f); v.add(0.0f);  // Normal (-X)
        
        v.add(x0); v.add(y0); v.add(z1);
        v.add(1.0f); v.add(0.0f);
        v.add(-1.0f); v.add(0.0f); v.add(0.0f);
        
        v.add(x0); v.add(y1); v.add(z1);
        v.add(1.0f); v.add(1.0f);
        v.add(-1.0f); v.add(0.0f); v.add(0.0f);
        
        v.add(x0); v.add(y1); v.add(z0);
        v.add(0.0f); v.add(1.0f);
        v.add(-1.0f); v.add(0.0f); v.add(0.0f);
    }
    
    private void addEastFace(List<Float> v, int x, int y, int z) {
        float x0 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;
        
        v.add(x0); v.add(y0); v.add(z0);
        v.add(0.0f); v.add(0.0f);
        v.add(1.0f); v.add(0.0f); v.add(0.0f);  // Normal (+X)
        
        v.add(x0); v.add(y1); v.add(z0);
        v.add(0.0f); v.add(1.0f);
        v.add(1.0f); v.add(0.0f); v.add(0.0f);
        
        v.add(x0); v.add(y1); v.add(z1);
        v.add(1.0f); v.add(1.0f);
        v.add(1.0f); v.add(0.0f); v.add(0.0f);
        
        v.add(x0); v.add(y0); v.add(z1);
        v.add(1.0f); v.add(0.0f);
        v.add(1.0f); v.add(0.0f); v.add(0.0f);
    }
    
    /**
     * Gets the cached mesh, generating it if dirty.
     * 
     * @return Chunk mesh
     */
    public ChunkMesh getMesh() {
        if (meshDirty || mesh == null) {
            return generateMesh();
        }
        return mesh;
    }
    
    /**
     * Marks the mesh as dirty, requiring regeneration.
     */
    public void markMeshDirty() {
        meshDirty = true;
    }
    
    /**
     * Gets the chunk position.
     * 
     * @return Chunk coordinates
     */
    public ChunkPos getPosition() {
        return position;
    }
    
    /**
     * Checks if this chunk is entirely empty (all air blocks).
     * 
     * @return true if all blocks are air
     */
    public boolean isEmpty() {
        for (BlockType block : blocks) {
            if (block != BlockType.AIR) {
                return false;
            }
        }
        return true;
    }
}
