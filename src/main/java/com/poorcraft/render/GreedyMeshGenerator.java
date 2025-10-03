package com.poorcraft.render;

import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimized greedy meshing algorithm for chunk mesh generation.
 * 
 * This algorithm merges adjacent same-type block faces into larger rectangular quads,
 * dramatically reducing vertex count compared to naive per-block face generation.
 * 
 * The algorithm works by:
 * 1. Scanning through each layer of blocks along each axis
 * 2. Creating a 2D mask of which faces need to be rendered in that layer
 * 3. Greedily merging adjacent faces into larger rectangles
 * 4. Generating quads for the merged rectangles
 * 
 * This typically reduces vertex count by 50-90% depending on terrain complexity.
 * 
 * Based on the greedy meshing algorithm by Mikola Lysenko.
 * 
 * @author PoorCraft Team
 */
public class GreedyMeshGenerator {
    
    private final Chunk chunk;
    private final TextureAtlas textureAtlas;
    
    /**
     * Creates a greedy mesh generator for the specified chunk.
     * 
     * @param chunk Chunk to generate mesh for
     * @param textureAtlas Texture atlas for UV coordinates
     */
    public GreedyMeshGenerator(Chunk chunk, TextureAtlas textureAtlas) {
        this.chunk = chunk;
        this.textureAtlas = textureAtlas;
    }
    
    /**
     * Generates an optimized mesh using the greedy meshing algorithm.
     * 
     * @return Generated chunk mesh with merged faces
     */
    public ChunkMesh generateMesh() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Generate faces for all 6 directions
        // 0=top, 1=bottom, 2=north, 3=south, 4=west, 5=east
        for (int direction = 0; direction < 6; direction++) {
            generateFacesForDirection(direction, vertices, indices);
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
        
        return new ChunkMesh(vertexArray, indexArray);
    }
    
    /**
     * Generates merged faces for a specific direction.
     * 
     * This is where the magic happens - we scan through layers and merge adjacent faces.
     * Or at least we try to. Sometimes it works, sometimes... well, you get a weird mesh.
     * 
     * @param direction Face direction (0-5)
     * @param vertices Vertex list to append to
     * @param indices Index list to append to
     */
    private void generateFacesForDirection(int direction, List<Float> vertices, List<Integer> indices) {
        // Determine axis dimensions based on direction
        // For each direction, we scan through layers perpendicular to that direction
        int width, height, depth;
        
        switch (direction) {
            case 0, 1 -> { // Top/Bottom (scan XZ plane, iterate through Y)
                width = Chunk.CHUNK_SIZE;
                height = Chunk.CHUNK_SIZE;
                depth = Chunk.CHUNK_HEIGHT;
            }
            case 2, 3 -> { // North/South (scan XY plane, iterate through Z)
                width = Chunk.CHUNK_SIZE;
                height = Chunk.CHUNK_HEIGHT;
                depth = Chunk.CHUNK_SIZE;
            }
            default -> { // West/East (scan YZ plane, iterate through X)
                width = Chunk.CHUNK_HEIGHT;
                height = Chunk.CHUNK_SIZE;
                depth = Chunk.CHUNK_SIZE;
            }
        }
        
        // Create mask for face visibility
        // mask[row][col] stores the block type if a face should be rendered, null otherwise
        BlockType[][] mask = new BlockType[height][width];
        
        // Scan through each layer along the depth axis
        for (int d = 0; d < depth; d++) {
            // Clear mask
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    mask[h][w] = null;
                }
            }
            
            // Fill mask for this layer
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    // Convert 2D mask coordinates to 3D world coordinates based on direction
                    int x, y, z;
                    int adjX, adjY, adjZ; // Adjacent block coordinates
                    
                    switch (direction) {
                        case 0 -> { // Top (+Y)
                            x = w; y = d; z = h;
                            adjX = x; adjY = y + 1; adjZ = z;
                        }
                        case 1 -> { // Bottom (-Y)
                            x = w; y = d; z = h;
                            adjX = x; adjY = y - 1; adjZ = z;
                        }
                        case 2 -> { // North (-Z)
                            x = w; y = h; z = d;
                            adjX = x; adjY = y; adjZ = z - 1;
                        }
                        case 3 -> { // South (+Z)
                            x = w; y = h; z = d;
                            adjX = x; adjY = y; adjZ = z + 1;
                        }
                        case 4 -> { // West (-X)
                            x = d; y = w; z = h;
                            adjX = x - 1; adjY = y; adjZ = z;
                        }
                        default -> { // East (+X)
                            x = d; y = w; z = h;
                            adjX = x + 1; adjY = y; adjZ = z;
                        }
                    }
                    
                    // Check if face should be rendered
                    BlockType currentBlock = getBlockSafe(x, y, z);
                    BlockType adjacentBlock = getBlockSafe(adjX, adjY, adjZ);
                    
                    if (currentBlock != BlockType.AIR && 
                        (adjacentBlock == BlockType.AIR || adjacentBlock.isTransparent())) {
                        mask[h][w] = currentBlock;
                    }
                }
            }
            
            // Greedy merge: scan through mask and create merged quads
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; ) {
                    if (mask[h][w] != null) {
                        BlockType blockType = mask[h][w];
                        
                        // Determine width of this quad (scan right)
                        int quadWidth = 1;
                        while (w + quadWidth < width && mask[h][w + quadWidth] == blockType) {
                            quadWidth++;
                        }
                        
                        // Determine height of this quad (scan down)
                        int quadHeight = 1;
                        boolean canExtendHeight = true;
                        
                        while (h + quadHeight < height && canExtendHeight) {
                            // Check if entire width can extend down one row
                            for (int checkW = 0; checkW < quadWidth; checkW++) {
                                if (mask[h + quadHeight][w + checkW] != blockType) {
                                    canExtendHeight = false;
                                    break;
                                }
                            }
                            if (canExtendHeight) {
                                quadHeight++;
                            }
                        }
                        
                        // Create quad with dimensions (quadWidth × quadHeight)
                        addMergedQuad(vertices, indices, direction, d, w, h, quadWidth, quadHeight, blockType);
                        
                        // Clear merged area in mask
                        for (int clearH = 0; clearH < quadHeight; clearH++) {
                            for (int clearW = 0; clearW < quadWidth; clearW++) {
                                mask[h + clearH][w + clearW] = null;
                            }
                        }
                        
                        // Move to next unprocessed column
                        w += quadWidth;
                    } else {
                        w++;
                    }
                }
            }
        }
    }
    
    /**
     * Gets a block safely, returning AIR if out of bounds.
     * Now with proper neighbor chunk checking! No more weird border faces.
     * Well, hopefully. I think this works. It should work. Right?
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Block type, or AIR if out of bounds
     */
    private BlockType getBlockSafe(int x, int y, int z) {
        // Delegate to chunk's neighbor-aware accessor
        // This handles checking neighboring chunks for proper border face culling
        return chunk.getBlockOrNeighbor(x, y, z);
    }
    
    /**
     * Adds vertices and indices for a merged quad.
     * 
     * @param vertices Vertex list
     * @param indices Index list
     * @param direction Face direction
     * @param depth Layer depth
     * @param w Mask width coordinate
     * @param h Mask height coordinate
     * @param quadWidth Quad width in blocks
     * @param quadHeight Quad height in blocks
     * @param blockType Block type
     */
    private void addMergedQuad(List<Float> vertices, List<Integer> indices, 
                               int direction, int depth, int w, int h, 
                               int quadWidth, int quadHeight, BlockType blockType) {
        int baseIndex = vertices.size() / 8;
        
        // Convert mask coordinates back to 3D world coordinates
        float x0, y0, z0, x1, y1, z1;
        float nx, ny, nz; // Normal
        
        switch (direction) {
            case 0 -> { // Top (+Y)
                x0 = w; x1 = w + quadWidth;
                y0 = depth + 1; y1 = depth + 1;
                z0 = h; z1 = h + quadHeight;
                nx = 0; ny = 1; nz = 0;
            }
            case 1 -> { // Bottom (-Y)
                x0 = w; x1 = w + quadWidth;
                y0 = depth; y1 = depth;
                z0 = h; z1 = h + quadHeight;
                nx = 0; ny = -1; nz = 0;
            }
            case 2 -> { // North (-Z)
                x0 = w; x1 = w + quadWidth;
                y0 = h; y1 = h + quadHeight;
                z0 = depth; z1 = depth;
                nx = 0; ny = 0; nz = -1;
            }
            case 3 -> { // South (+Z)
                x0 = w; x1 = w + quadWidth;
                y0 = h; y1 = h + quadHeight;
                z0 = depth + 1; z1 = depth + 1;
                nx = 0; ny = 0; nz = 1;
            }
            case 4 -> { // West (-X)
                x0 = depth; x1 = depth;
                y0 = w; y1 = w + quadWidth;
                z0 = h; z1 = h + quadHeight;
                nx = -1; ny = 0; nz = 0;
            }
            default -> { // East (+X)
                x0 = depth + 1; x1 = depth + 1;
                y0 = w; y1 = w + quadWidth;
                z0 = h; z1 = h + quadHeight;
                nx = 1; ny = 0; nz = 0;
            }
        }
        
        // Get proper UV coordinates from texture atlas
        // No more hardcoded values! Each face gets its own texture rectangle.
        // This was the bug causing all textures to look wrong. Classic mistake.
        float[] uvBounds = textureAtlas.getUVsForFace(blockType, direction);
        float atlasU0 = uvBounds[0];
        float atlasV0 = uvBounds[1];
        float atlasU1 = uvBounds[2];
        float atlasV1 = uvBounds[3];
        
        // Tile texture coordinates based on quad size
        // This makes the texture repeat across the merged quad
        float u0 = atlasU0;
        float v0 = atlasV0;
        float u1 = atlasU0 + (atlasU1 - atlasU0) * quadWidth;
        float v1 = atlasV0 + (atlasV1 - atlasV0) * quadHeight;
        
        // Add 4 vertices for the quad
        // Vertex order depends on direction to ensure correct winding
        if (direction == 0) { // Top (+Y) - ensure CCW winding when viewed from above
            float y = depth + 1;

            vertices.add(x0); vertices.add(y); vertices.add(z0);
            vertices.add(u0); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x0); vertices.add(y); vertices.add(z1);
            vertices.add(u0); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x1); vertices.add(y); vertices.add(z1);
            vertices.add(u1); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x1); vertices.add(y); vertices.add(z0);
            vertices.add(u1); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
        } else if (direction == 1) { // Bottom (-Y) - ensure CCW winding when viewed from below
            float y = depth;

            vertices.add(x0); vertices.add(y); vertices.add(z0);
            vertices.add(u0); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x1); vertices.add(y); vertices.add(z0);
            vertices.add(u1); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x1); vertices.add(y); vertices.add(z1);
            vertices.add(u1); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x0); vertices.add(y); vertices.add(z1);
            vertices.add(u0); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
        } else if (direction == 2) { // North
            vertices.add(x0); vertices.add(y0); vertices.add(z0);
            vertices.add(u0); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
            
            vertices.add(x0); vertices.add(y1); vertices.add(z1);
            vertices.add(u0); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
            
            vertices.add(x1); vertices.add(y1); vertices.add(z1);
            vertices.add(u1); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
            
            vertices.add(x1); vertices.add(y0); vertices.add(z0);
            vertices.add(u1); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
        } else if (direction == 3) { // South
            vertices.add(x0); vertices.add(y0); vertices.add(z0);
            vertices.add(u0); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
            
            vertices.add(x1); vertices.add(y0); vertices.add(z1);
            vertices.add(u1); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
            
            vertices.add(x1); vertices.add(y1); vertices.add(z1);
            vertices.add(u1); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
            
            vertices.add(x0); vertices.add(y1); vertices.add(z0);
            vertices.add(u0); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
        } else if (direction == 4) { // West
            vertices.add(x0); vertices.add(y0); vertices.add(z0);
            vertices.add(u0); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x0); vertices.add(y1); vertices.add(z0);
            vertices.add(u0); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x0); vertices.add(y1); vertices.add(z1);
            vertices.add(u1); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x0); vertices.add(y0); vertices.add(z1);
            vertices.add(u1); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
        } else { // East
            vertices.add(x0); vertices.add(y0); vertices.add(z0);
            vertices.add(u0); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x0); vertices.add(y0); vertices.add(z1);
            vertices.add(u0); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x0); vertices.add(y1); vertices.add(z1);
            vertices.add(u1); vertices.add(v1);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);

            vertices.add(x0); vertices.add(y1); vertices.add(z0);
            vertices.add(u1); vertices.add(v0);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
        }
        
        // Add indices for 2 triangles
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }
}
