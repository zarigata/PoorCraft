package com.poorcraft.world.generation;

import com.poorcraft.modding.EventBus;
import com.poorcraft.modding.events.ChunkGenerateEvent;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;

/**
 * Main terrain generator that fills chunks with blocks based on height maps and biomes.
 * 
 * Uses multi-octave simplex noise to create natural-looking terrain.
 * Each biome has its own base height and variation, creating distinct landscapes.
 * This is where the world actually gets generated. No pressure.
 */
public class TerrainGenerator {
    
    private final long seed;
    private final SimplexNoise heightNoise;
    private final SimplexNoise detailNoise;
    private final BiomeGenerator biomeGenerator;
    private EventBus eventBus;
    
    private static final double HEIGHT_SCALE = 0.005;   // Scale for primary height noise
    private static final double DETAIL_SCALE = 0.02;    // Scale for detail noise
    private static final int BEDROCK_LAYER = 0;         // Y level for bedrock
    private static final int STONE_LAYER_START = 1;     // Y level where stone starts
    private static final int SURFACE_DEPTH = 4;         // Depth of surface/subsurface layers
    
    /**
     * Creates a new terrain generator with the given seed.
     * 
     * @param seed World seed
     */
    public TerrainGenerator(long seed) {
        this.seed = seed;
        this.heightNoise = new SimplexNoise(seed);
        this.detailNoise = new SimplexNoise(seed + 500);  // Different seed for detail
        this.biomeGenerator = new BiomeGenerator(seed);
        this.eventBus = null;
    }
    
    /**
     * Sets the event bus for firing mod events.
     * 
     * @param eventBus Event bus instance
     */
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * Generates terrain for the given chunk.
     * Fills the chunk with blocks based on height maps and biome data.
     * 
     * @param chunk Chunk to generate terrain for
     */
    public void generateTerrain(Chunk chunk) {
        int chunkWorldX = chunk.getPosition().x * Chunk.CHUNK_SIZE;
        int chunkWorldZ = chunk.getPosition().z * Chunk.CHUNK_SIZE;
        
        // Generate terrain column by column
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkWorldX + x;
                int worldZ = chunkWorldZ + z;
                
                // Get biome for this column
                BiomeType biome = biomeGenerator.getBiome(worldX, worldZ);
                
                // Calculate terrain height using multi-octave noise
                double heightValue = heightNoise.octaveNoise2D(
                    worldX * HEIGHT_SCALE,
                    worldZ * HEIGHT_SCALE,
                    4,    // octaves
                    0.5,  // persistence
                    2.0   // lacunarity
                );
                
                // Add detail noise for small-scale variation
                double detailValue = detailNoise.octaveNoise2D(
                    worldX * DETAIL_SCALE,
                    worldZ * DETAIL_SCALE,
                    3,    // octaves
                    0.5,  // persistence
                    2.0   // lacunarity
                );
                
                // Combine noise with biome characteristics
                double combinedHeight = heightValue * biome.getHeightVariation() + detailValue * 3.0;
                int height = biome.getBaseHeight() + (int) combinedHeight;
                
                // Clamp height to valid range
                height = Math.max(1, Math.min(Chunk.CHUNK_HEIGHT - 1, height));
                
                // Fill column with blocks
                fillColumn(chunk, x, z, height, biome);
            }
        }
        
        // Fire mod event for chunk generation (mods can modify terrain)
        if (eventBus != null) {
            eventBus.fire(new ChunkGenerateEvent(chunk.getPosition().x, chunk.getPosition().z, chunk));
        }
    }
    
    /**
     * Fills a single column with blocks based on height and biome.
     * 
     * Layer structure:
     * - Y=0: Bedrock (indestructible bottom)
     * - Y=1 to height-SURFACE_DEPTH: Stone
     * - Y=height-SURFACE_DEPTH+1 to height-1: Subsurface block (biome-specific)
     * - Y=height: Surface block (biome-specific)
     * - Y=height+1 to top: Air
     * 
     * @param chunk Chunk to fill
     * @param x Local X coordinate
     * @param z Local Z coordinate
     * @param height Terrain height at this column
     * @param biome Biome type
     */
    private void fillColumn(Chunk chunk, int x, int z, int height, BiomeType biome) {
        // Bedrock layer (Y=0)
        chunk.setBlock(x, BEDROCK_LAYER, z, BlockType.BEDROCK);
        
        // Stone core (Y=1 to height-SURFACE_DEPTH)
        int stoneTop = Math.max(STONE_LAYER_START, height - SURFACE_DEPTH);
        for (int y = STONE_LAYER_START; y <= stoneTop; y++) {
            chunk.setBlock(x, y, z, BlockType.STONE);
        }
        
        // Subsurface layers (biome-specific)
        int subsurfaceStart = stoneTop + 1;
        int subsurfaceEnd = height - 1;
        for (int y = subsurfaceStart; y <= subsurfaceEnd; y++) {
            if (y < Chunk.CHUNK_HEIGHT) {
                chunk.setBlock(x, y, z, biome.getSubsurfaceBlock());
            }
        }
        
        // Surface layer (biome-specific)
        if (height < Chunk.CHUNK_HEIGHT) {
            chunk.setBlock(x, height, z, biome.getSurfaceBlock());
        }
        
        // Air above (already initialized to AIR, but explicit for clarity)
        // No need to set AIR blocks explicitly since chunk initializes to AIR
    }
    
    /**
     * Gets the terrain height at the given world coordinates.
     * Utility method for feature placement without generating full chunk.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Terrain height at that location
     */
    public int getHeightAt(int worldX, int worldZ) {
        BiomeType biome = biomeGenerator.getBiome(worldX, worldZ);
        
        double heightValue = heightNoise.octaveNoise2D(
            worldX * HEIGHT_SCALE,
            worldZ * HEIGHT_SCALE,
            4, 0.5, 2.0
        );
        
        double detailValue = detailNoise.octaveNoise2D(
            worldX * DETAIL_SCALE,
            worldZ * DETAIL_SCALE,
            3, 0.5, 2.0
        );
        
        double combinedHeight = heightValue * biome.getHeightVariation() + detailValue * 3.0;
        int height = biome.getBaseHeight() + (int) combinedHeight;
        
        return Math.max(1, Math.min(Chunk.CHUNK_HEIGHT - 1, height));
    }
}
