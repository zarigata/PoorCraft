package com.poorcraft.world.generation;

import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;

import java.util.Random;

/**
 * Feature generator for biome-specific structures.
 * 
 * Generates trees, cacti, snow layers, and other decorative features.
 * Uses deterministic random generation based on world coordinates,
 * so the same features always generate at the same locations.
 * 
 * Like Minecraft's feature generation but simpler. And poorer.
 */
public class FeatureGenerator {
    
    private final long seed;
    private final BiomeGenerator biomeGenerator;
    private final TerrainGenerator terrainGenerator;
    private final Random random;
    
    /**
     * Creates a new feature generator.
     * 
     * @param seed World seed
     * @param biomeGenerator Biome generator for biome lookup
     * @param terrainGenerator Terrain generator for height lookup
     */
    public FeatureGenerator(long seed, BiomeGenerator biomeGenerator, TerrainGenerator terrainGenerator) {
        this.seed = seed;
        this.biomeGenerator = biomeGenerator;
        this.terrainGenerator = terrainGenerator;
        this.random = new Random();
    }
    
    /**
     * Generates features for the given chunk.
     * Called after terrain generation as a post-processing step.
     * 
     * @param chunk Chunk to generate features for
     */
    public void generateFeatures(Chunk chunk) {
        int chunkWorldX = chunk.getPosition().x * Chunk.CHUNK_SIZE;
        int chunkWorldZ = chunk.getPosition().z * Chunk.CHUNK_SIZE;
        
        // Generate features column by column
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkWorldX + x;
                int worldZ = chunkWorldZ + z;
                
                // Get biome for this column
                BiomeType biome = biomeGenerator.getBiome(worldX, worldZ);
                
                // Seed random with deterministic value based on world coordinates
                // This ensures same features generate at same coordinates every time
                // The magic numbers are large primes to avoid patterns
                random.setSeed(seed + worldX * 341873128712L + worldZ * 132897987541L);
                
                // Generate biome-specific features
                switch (biome) {
                    case DESERT -> {
                        // 2% chance for cactus
                        if (random.nextFloat() < 0.02f) {
                            placeCactus(chunk, x, z, worldX, worldZ);
                        }
                    }
                    case SNOW -> {
                        // 60% chance for snow layer on surface
                        if (random.nextFloat() < 0.6f) {
                            placeSnowLayer(chunk, x, z, worldX, worldZ);
                        }
                    }
                    case JUNGLE -> {
                        // 8% chance for tree (dense jungle)
                        if (random.nextFloat() < 0.08f) {
                            placeTree(chunk, x, z, worldX, worldZ, biome);
                        }
                    }
                    case PLAINS -> {
                        // 3% chance for tree (sparse)
                        if (random.nextFloat() < 0.03f) {
                            placeTree(chunk, x, z, worldX, worldZ, biome);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Places a cactus feature at the given location.
     * Cacti are 2-4 blocks tall and only grow on sand.
     * 
     * @param chunk Chunk to place in
     * @param x Local X coordinate
     * @param z Local Z coordinate
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     */
    private void placeCactus(Chunk chunk, int x, int z, int worldX, int worldZ) {
        int height = terrainGenerator.getHeightAt(worldX, worldZ);
        
        // Check if height is within chunk bounds
        if (height < 0 || height >= Chunk.CHUNK_HEIGHT) {
            return;
        }
        
        // Check if surface block is sand
        BlockType surfaceBlock = chunk.getBlock(x, height, z);
        if (surfaceBlock != BlockType.SAND) {
            return;
        }
        
        // Generate cactus height (2-4 blocks)
        int cactusHeight = 2 + random.nextInt(3);
        
        // Place cactus blocks
        for (int i = 1; i <= cactusHeight; i++) {
            int y = height + i;
            if (y < Chunk.CHUNK_HEIGHT) {
                chunk.setBlock(x, y, z, BlockType.CACTUS);
            }
        }
    }
    
    /**
     * Places a tree feature at the given location.
     * Trees have a wood trunk and leaf canopy.
     * 
     * @param chunk Chunk to place in
     * @param x Local X coordinate
     * @param z Local Z coordinate
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @param biome Biome type (affects tree appearance)
     */
    private void placeTree(Chunk chunk, int x, int z, int worldX, int worldZ, BiomeType biome) {
        int height = terrainGenerator.getHeightAt(worldX, worldZ);
        
        // Check if height is within chunk bounds
        if (height < 0 || height >= Chunk.CHUNK_HEIGHT - 8) {
            return;  // Need space for tree
        }
        
        // Check if surface block is grass or jungle grass
        BlockType surfaceBlock = chunk.getBlock(x, height, z);
        if (surfaceBlock != BlockType.GRASS && surfaceBlock != BlockType.JUNGLE_GRASS) {
            return;
        }
        
        // Generate tree dimensions
        int trunkHeight = 4 + random.nextInt(3);  // 4-6 blocks tall
        int leafRadius = 2;
        
        // Place trunk
        for (int i = 1; i <= trunkHeight; i++) {
            int y = height + i;
            if (y < Chunk.CHUNK_HEIGHT) {
                chunk.setBlock(x, y, z, BlockType.WOOD);
            }
        }
        
        // Place leaves in a sphere/cube around top of trunk
        int leafStartY = height + trunkHeight - 1;
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -leafRadius; dx <= leafRadius; dx++) {
                for (int dz = -leafRadius; dz <= leafRadius; dz++) {
                    // Skip corners for more natural shape
                    if (Math.abs(dx) == leafRadius && Math.abs(dz) == leafRadius) {
                        continue;
                    }
                    
                    int leafX = x + dx;
                    int leafY = leafStartY + dy;
                    int leafZ = z + dz;
                    
                    // Check bounds
                    if (leafX >= 0 && leafX < Chunk.CHUNK_SIZE &&
                        leafZ >= 0 && leafZ < Chunk.CHUNK_SIZE &&
                        leafY >= 0 && leafY < Chunk.CHUNK_HEIGHT) {
                        
                        // Don't replace trunk blocks
                        if (dx == 0 && dz == 0 && dy < 2) {
                            continue;
                        }
                        
                        // Place leaf block
                        BlockType currentBlock = chunk.getBlock(leafX, leafY, leafZ);
                        if (currentBlock == BlockType.AIR) {
                            chunk.setBlock(leafX, leafY, leafZ, BlockType.LEAVES);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Places a snow layer on top of the surface block.
     * Snow layers are decorative and non-solid.
     * 
     * @param chunk Chunk to place in
     * @param x Local X coordinate
     * @param z Local Z coordinate
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     */
    private void placeSnowLayer(Chunk chunk, int x, int z, int worldX, int worldZ) {
        int height = terrainGenerator.getHeightAt(worldX, worldZ);
        
        // Check if height is within chunk bounds
        if (height < 0 || height >= Chunk.CHUNK_HEIGHT - 1) {
            return;
        }
        
        // Check if surface block is solid
        BlockType surfaceBlock = chunk.getBlock(x, height, z);
        if (!surfaceBlock.isSolid()) {
            return;
        }
        
        // Place snow layer on top
        int snowY = height + 1;
        if (snowY < Chunk.CHUNK_HEIGHT) {
            chunk.setBlock(x, snowY, z, BlockType.SNOW_LAYER);
        }
    }
}
