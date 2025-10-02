package com.poorcraft.world.generation;

/**
 * Biome generator that determines biome type at any world coordinate using noise.
 * 
 * Uses two noise layers (temperature and humidity) to create natural biome distributions.
 * Temperature determines hot vs cold, humidity determines wet vs dry.
 * This creates smooth biome transitions instead of random patches.
 */
public class BiomeGenerator {
    
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    
    private static final double BIOME_SCALE = 0.0008;  // Scale factor for biome noise
    
    /**
     * Creates a new biome generator with the given seed.
     * 
     * @param seed World seed
     */
    public BiomeGenerator(long seed) {
        // Use different seeds for temperature and humidity to avoid correlation
        // Otherwise all hot areas would also be wet, which doesn't make sense
        this.temperatureNoise = new SimplexNoise(seed);
        this.humidityNoise = new SimplexNoise(seed + 1000);
    }
    
    /**
     * Determines the biome at the given world coordinates.
     * 
     * Uses a 2D decision grid based on temperature and humidity:
     * - Cold = Snow biome
     * - Hot + Dry = Desert biome
     * - Hot + Wet = Jungle biome
     * - Temperate = Plains biome
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Biome type at that location
     */
    public BiomeType getBiome(int worldX, int worldZ) {
        double temp = getTemperature(worldX, worldZ);
        double humid = getHumidity(worldX, worldZ);
        
        // Cold regions = snow biome
        if (temp < -0.3) {
            return BiomeType.SNOW;
        }
        
        // Hot and dry = desert
        if (temp > 0.3 && humid < -0.2) {
            return BiomeType.DESERT;
        }
        
        // Hot and wet = jungle
        if (temp > 0.2 && humid > 0.3) {
            return BiomeType.JUNGLE;
        }
        
        // Everything else = plains (the default biome)
        return BiomeType.PLAINS;
    }
    
    /**
     * Gets the raw temperature value at the given coordinates.
     * Useful for biome blending in the future.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Temperature value in range [-1, 1]
     */
    public double getTemperature(int worldX, int worldZ) {
        return temperatureNoise.noise2D(worldX * BIOME_SCALE, worldZ * BIOME_SCALE);
    }
    
    /**
     * Gets the raw humidity value at the given coordinates.
     * Useful for biome blending in the future.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Humidity value in range [-1, 1]
     */
    public double getHumidity(int worldX, int worldZ) {
        return humidityNoise.noise2D(worldX * BIOME_SCALE, worldZ * BIOME_SCALE);
    }
}
