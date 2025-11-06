package com.poorcraft.world.generation;

/**
 * Biome generator that determines biome type at any world coordinate using multiple
 * layered noise fields (temperature, humidity, continentalness, weirdness, erosion).
 *
 * The additional noise layers create more organic, less circular biome shapes and
 * provide extra metadata (warp offsets, noise samples) that terrain generation can
 * use to shape heights consistently with biome selection.
 */
public class BiomeGenerator {

    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    private final SimplexNoise continentalNoise;
    private final SimplexNoise weirdnessNoise;
    private final SimplexNoise erosionNoise;
    private final SimplexNoise warpNoise;

    private static final double BIOME_SCALE = 0.0008;
    private static final double CONTINENT_SCALE = 0.00035;
    private static final double WEIRD_SCALE = 0.0012;
    private static final double EROSION_SCALE = 0.001;
    private static final double WARP_SCALE = 0.0011;
    private static final double WARP_STRENGTH = 120.0;

    /**
     * Creates a new biome generator with the given seed.
     *
     * @param seed World seed
     */
    public BiomeGenerator(long seed) {
        this.temperatureNoise = new SimplexNoise(seed);
        this.humidityNoise = new SimplexNoise(seed + 1000);
        this.continentalNoise = new SimplexNoise(seed + 2000);
        this.weirdnessNoise = new SimplexNoise(seed + 3000);
        this.erosionNoise = new SimplexNoise(seed + 4000);
        this.warpNoise = new SimplexNoise(seed + 5000);
    }

    /**
     * Samples biome data for a world coordinate, returning the selected biome and all
     * intermediate noise values used during classification.
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return A {@link BiomeSample} containing biome selection and noise metadata
     */
    public BiomeSample sample(int worldX, int worldZ) {
        double warp = warpNoise.octaveNoise2D(worldX * WARP_SCALE, worldZ * WARP_SCALE, 3, 0.5, 2.1);
        double warpedX = worldX + warp * WARP_STRENGTH;
        double warpedZ = worldZ + warp * WARP_STRENGTH;

        double temperature = temperatureNoise.octaveNoise2D(warpedX * BIOME_SCALE, warpedZ * BIOME_SCALE, 4, 0.55, 2.05);
        double humidity = humidityNoise.octaveNoise2D((warpedX + 1024.0) * BIOME_SCALE, (warpedZ - 2048.0) * BIOME_SCALE, 4, 0.6, 2.1);
        double continent = continentalNoise.octaveNoise2D(worldX * CONTINENT_SCALE, worldZ * CONTINENT_SCALE, 4, 0.45, 2.0);
        double weirdness = weirdnessNoise.octaveNoise2D(worldX * WEIRD_SCALE, worldZ * WEIRD_SCALE, 3, 0.6, 2.25);
        double erosion = erosionNoise.octaveNoise2D(worldX * EROSION_SCALE, worldZ * EROSION_SCALE, 3, 0.7, 2.0);

        BiomeType biome = pickBiome(temperature, humidity, continent, weirdness, erosion);
        return new BiomeSample(biome, temperature, humidity, continent, weirdness, erosion, warpedX, warpedZ);
    }

    /**
     * Determines the biome type at the provided world coordinates.
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Selected biome type
     */
    public BiomeType getBiome(int worldX, int worldZ) {
        return sample(worldX, worldZ).getBiome();
    }

    private BiomeType pickBiome(double temperature, double humidity, double continent, double weirdness, double erosion) {
        double mountainScore = (continent * 0.6) + Math.max(0.0, weirdness) * 0.8;
        if (mountainScore > 0.65) {
            return BiomeType.MOUNTAINS;
        }

        if (temperature < -0.2) {
            return BiomeType.SNOW;
        }

        if (temperature > 0.3 && humidity < 0.0) {
            return BiomeType.DESERT;
        }

        if (humidity > 0.4 && temperature > -0.2 && erosion < 0.2) {
            return BiomeType.SWAMP;
        }

        if (temperature > 0.15 && humidity > 0.15) {
            return BiomeType.JUNGLE;
        }

        if (humidity > 0.1 && temperature > -0.3 && temperature < 0.5) {
            return BiomeType.FOREST;
        }

        if (humidity < -0.2 && temperature > 0.1) {
            return BiomeType.DESERT;
        }

        if (continent < -0.5 && temperature < -0.1) {
            return BiomeType.SNOW;
        }

        return BiomeType.PLAINS;
    }

    /**
     * Result of biome sampling, exposing the selected biome and raw noise values
     * to downstream generation steps.
     */
    public static final class BiomeSample {
        private final BiomeType biome;
        private final double temperature;
        private final double humidity;
        private final double continent;
        private final double weirdness;
        private final double erosion;
        private final double warpedX;
        private final double warpedZ;

        private BiomeSample(BiomeType biome, double temperature, double humidity,
                            double continent, double weirdness, double erosion,
                            double warpedX, double warpedZ) {
            this.biome = biome;
            this.temperature = temperature;
            this.humidity = humidity;
            this.continent = continent;
            this.weirdness = weirdness;
            this.erosion = erosion;
            this.warpedX = warpedX;
            this.warpedZ = warpedZ;
        }

        public BiomeType getBiome() {
            return biome;
        }

        public double getTemperature() {
            return temperature;
        }

        public double getHumidity() {
            return humidity;
        }

        public double getContinent() {
            return continent;
        }

        public double getWeirdness() {
            return weirdness;
        }

        public double getErosion() {
            return erosion;
        }

        public double getWarpedX() {
            return warpedX;
        }

        public double getWarpedZ() {
            return warpedZ;
        }
    }
}
