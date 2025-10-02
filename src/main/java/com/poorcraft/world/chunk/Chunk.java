package com.poorcraft.world.chunk;

import com.poorcraft.render.GreedyMeshGenerator;
import com.poorcraft.world.block.BlockType;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Chunk class with block storage and mesh generation.
 * 
 * Chunks are 16x256x16 blocks stored in a flat array for cache efficiency.
 * Mesh generation uses greedy meshing algorithm for optimization.
 * Greedy meshing merges adjacent same-type faces into larger quads, reducing vertex count by 50-90%.
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
     * Generates mesh data for this chunk using greedy meshing algorithm.
     * 
     * @return Generated chunk mesh
     */
    public ChunkMesh generateMesh() {
        GreedyMeshGenerator generator = new GreedyMeshGenerator(this);
        mesh = generator.generateMesh();
        meshDirty = false;
        return mesh;
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
