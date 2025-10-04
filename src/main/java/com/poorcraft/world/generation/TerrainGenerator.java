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
    private final SimplexNoise continentNoise;
    private final SimplexNoise detailNoise;
    private final SimplexNoise ridgeNoise;
    private final SimplexNoise microNoise;
    private final BiomeGenerator biomeGenerator;
    private EventBus eventBus;
    
    private static final double CONTINENT_SCALE = 0.00045;
    private static final double DETAIL_SCALE = 0.0032;
    private static final double RIDGE_SCALE = 0.0016;
    private static final double MICRO_SCALE = 0.01;
    private static final int BEDROCK_LAYER = 0;         // Y level for bedrock
    private static final int STONE_LAYER_START = 1;     // Y level where stone starts
    
    /**
     * Creates a new terrain generator with the given seed.
     * 
     * @param seed World seed
     */
    public TerrainGenerator(long seed) {
        this.seed = seed;
        this.continentNoise = new SimplexNoise(seed);
        this.detailNoise = new SimplexNoise(seed + 500);  // Different seed for detail
        this.ridgeNoise = new SimplexNoise(seed + 850);
        this.microNoise = new SimplexNoise(seed + 1337);
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
                
                var sample = biomeGenerator.sample(worldX, worldZ);
                BiomeType biome = sample.getBiome();
                int height = computeColumnHeight(worldX, worldZ, sample, biome);
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
        int surfaceDepth = Math.max(1, biome.getSurfaceDepth());
        int stoneTop = Math.max(STONE_LAYER_START, height - surfaceDepth);
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
        var sample = biomeGenerator.sample(worldX, worldZ);
        return computeColumnHeight(worldX, worldZ, sample, sample.getBiome());
    }

    private int computeColumnHeight(int worldX, int worldZ, BiomeGenerator.BiomeSample sample, BiomeType biome) {
        double continentNoiseValue = continentNoise.octaveNoise2D(
            worldX * CONTINENT_SCALE,
            worldZ * CONTINENT_SCALE,
            5, 0.48, 2.05
        );

        double continentBlend = (continentNoiseValue + 1.0) * 0.5;
        double continentHeight = lerp(-28.0, 46.0, continentBlend) * biome.getContinentInfluence();

        double ridgeSignalNoise = ridgeNoise.octaveNoise2D(
            sample.getWarpedX() * RIDGE_SCALE,
            sample.getWarpedZ() * RIDGE_SCALE,
            4, 0.6, 2.15
        );
        double ridgeSignal = saturate((ridgeSignalNoise * 0.55) + Math.max(0.0, sample.getWeirdness()) * 0.45 + 0.25);
        double erosionFactor = Math.pow(saturate(1.0 - (sample.getErosion() + 1.0) * 0.5), 1.25);
        double ridgeHeight = Math.pow(ridgeSignal, 1.0 + biome.getShapeSharpness() * 2.0)
            * biome.getHeightVariation() * biome.getRidgeStrength() * erosionFactor;

        double detailSignal = detailNoise.octaveNoise2D(
            sample.getWarpedX() * DETAIL_SCALE,
            sample.getWarpedZ() * DETAIL_SCALE,
            5, 0.58, 2.05
        );
        double detailHeight = detailSignal * biome.getDetailStrength() * 10.0;

        double microSignal = microNoise.octaveNoise2D(
            sample.getWarpedX() * MICRO_SCALE,
            sample.getWarpedZ() * MICRO_SCALE,
            3, 0.6, 2.95
        );
        double microHeight = microSignal * 2.5;

        double finalHeight = biome.getBaseHeight()
            + continentHeight
            + ridgeHeight
            + detailHeight
            + microHeight;

        if (biome == BiomeType.SWAMP) {
            finalHeight = Math.min(finalHeight, biome.getBaseHeight() + 3.0);
        } else if (biome == BiomeType.MOUNTAINS) {
            finalHeight += 6.0 * erosionFactor;
        }

        int height = (int) Math.round(finalHeight);
        height = Math.max(1, Math.min(Chunk.CHUNK_HEIGHT - 2, height));
        return height;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double saturate(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
