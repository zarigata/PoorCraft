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

    private static final int MAX_PLAINS_TREES = 6;
    private static final int MAX_JUNGLE_TREES = 8;
    private static final int MAX_DESERT_CACTI = 10;
    private static final int MAX_SNOW_COLUMNS = 64;

    private static final int TREE_SPACING_RADIUS = 3;
    private static final int TREE_SPACING_HEIGHT_CHECK = 6;

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

        boolean plainsPresent = false;
        boolean junglePresent = false;
        boolean desertPresent = false;
        boolean snowPresent = false;

        int plainsFeaturesPlaced = 0;
        int jungleFeaturesPlaced = 0;
        int desertFeaturesPlaced = 0;
        int snowFeaturesPlaced = 0;

        // Generate features column by column
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkWorldX + x;
                int worldZ = chunkWorldZ + z;

                BiomeGenerator.BiomeSample sample = biomeGenerator.sample(worldX, worldZ);
                BiomeType biome = sample.getBiome();

                // Seed random with deterministic value based on world coordinates
                random.setSeed(seed + worldX * 341873128712L + worldZ * 132897987541L);

                switch (biome) {
                    case DESERT -> {
                        desertPresent = true;
                        if (desertFeaturesPlaced < MAX_DESERT_CACTI
                            && random.nextFloat() < 0.02f
                            && placeCactus(chunk, x, z, worldX, worldZ)) {
                            desertFeaturesPlaced++;
                        }
                    }
                    case SNOW -> {
                        snowPresent = true;
                        if (snowFeaturesPlaced < MAX_SNOW_COLUMNS
                            && random.nextFloat() < 0.6f
                            && placeSnowLayer(chunk, x, z, worldX, worldZ)) {
                            snowFeaturesPlaced++;
                        }
                    }
                    case JUNGLE -> {
                        junglePresent = true;
                        if (jungleFeaturesPlaced < MAX_JUNGLE_TREES
                            && random.nextFloat() < 0.12f
                            && !hasNearbyTreeTrunk(chunk, x, z, worldX, worldZ)
                            && placeTree(chunk, x, z, worldX, worldZ, biome, 5, 2)) {
                            jungleFeaturesPlaced++;
                        }
                    }
                    case PLAINS -> {
                        plainsPresent = true;
                        if (plainsFeaturesPlaced < MAX_PLAINS_TREES
                            && random.nextFloat() < 0.035f
                            && !hasNearbyTreeTrunk(chunk, x, z, worldX, worldZ)
                            && placeTree(chunk, x, z, worldX, worldZ, biome, 4, 2)) {
                            plainsFeaturesPlaced++;
                        }
                    }
                    case FOREST -> {
                        if (random.nextFloat() < 0.09f
                            && !hasNearbyTreeTrunk(chunk, x, z, worldX, worldZ)) {
                            placeTree(chunk, x, z, worldX, worldZ, biome, 5, 3);
                        }
                    }
                    case MOUNTAINS -> {
                        if (random.nextFloat() < 0.025f) {
                            placeRockOutcrop(chunk, x, z, worldX, worldZ);
                        }
                    }
                    case SWAMP -> {
                        if (random.nextFloat() < 0.045f) {
                            placeSwampTree(chunk, x, z, worldX, worldZ);
                        }
                    }
                }
            }
        }

        ensureFallbackFeatures(chunk, chunkWorldX, chunkWorldZ,
            plainsPresent && plainsFeaturesPlaced == 0,
            junglePresent && jungleFeaturesPlaced == 0,
            desertPresent && desertFeaturesPlaced == 0,
            snowPresent && snowFeaturesPlaced == 0);
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
    private boolean placeCactus(Chunk chunk, int x, int z, int worldX, int worldZ) {
        int height = terrainGenerator.getHeightAt(worldX, worldZ);
        
        // Check if height is within chunk bounds
        if (height < 0 || height >= Chunk.CHUNK_HEIGHT) {
            return false;
        }
        
        // Check if surface block is sand
        BlockType surfaceBlock = chunk.getBlock(x, height, z);
        if (surfaceBlock != BlockType.SAND) {
            return false;
        }
        
        // Generate cactus height (2-4 blocks)
        int cactusHeight = 2 + random.nextInt(3);
        
        // Place cactus blocks
        boolean placed = false;
        for (int i = 1; i <= cactusHeight; i++) {
            int y = height + i;
            if (y < Chunk.CHUNK_HEIGHT) {
                chunk.setBlock(x, y, z, BlockType.CACTUS);
                placed = true;
            }
        }
        return placed;
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
    private boolean placeTree(Chunk chunk, int x, int z, int worldX, int worldZ, BiomeType biome, int baseTrunkHeight, int extraLeaves) {
        int height = terrainGenerator.getHeightAt(worldX, worldZ);
        
        // Check if height is within chunk bounds
        if (height < 0 || height >= Chunk.CHUNK_HEIGHT - 8) {
            return false;  // Need space for tree
        }
        
        // Check if surface block is grass or jungle grass
        BlockType surfaceBlock = chunk.getBlock(x, height, z);
        if (surfaceBlock != BlockType.GRASS && surfaceBlock != BlockType.JUNGLE_GRASS) {
            return false;
        }
        
        // Generate tree dimensions
        int trunkHeight = baseTrunkHeight + random.nextInt(3);
        int leafRadius = 2 + Math.max(0, extraLeaves - 2);
        
        // Place trunk
        boolean placed = false;
        for (int i = 1; i <= trunkHeight; i++) {
            int y = height + i;
            if (y < Chunk.CHUNK_HEIGHT) {
                chunk.setBlock(x, y, z, BlockType.WOOD);
                placed = true;
            }
        }
        
        // Place leaves in a sphere/cube around top of trunk
        int leafStartY = height + trunkHeight - 1;
        int leafLayers = 2 + extraLeaves;
        for (int dy = 0; dy <= leafLayers; dy++) {
            for (int dx = -leafRadius; dx <= leafRadius; dx++) {
                for (int dz = -leafRadius; dz <= leafRadius; dz++) {
                    // Skip corners for more natural shape
                    if (Math.abs(dx) == leafRadius && Math.abs(dz) == leafRadius && dy < leafLayers) {
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
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
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
    private boolean placeSnowLayer(Chunk chunk, int x, int z, int worldX, int worldZ) {
        int height = terrainGenerator.getHeightAt(worldX, worldZ);
        
        // Check if height is within chunk bounds
        if (height < 0 || height >= Chunk.CHUNK_HEIGHT - 1) {
            return false;
        }
        
        // Check if surface block is solid
        BlockType surfaceBlock = chunk.getBlock(x, height, z);
        if (!surfaceBlock.isSolid()) {
            return false;
        }
        
        // Place snow layer on top
        int snowY = height + 1;
        if (snowY < Chunk.CHUNK_HEIGHT) {
            if (chunk.getBlock(x, snowY, z) != BlockType.SNOW_LAYER) {
                chunk.setBlock(x, snowY, z, BlockType.SNOW_LAYER);
            }
            return true;
        }
        return false;
    }

    private boolean hasNearbyTreeTrunk(Chunk chunk, int localX, int localZ, int worldX, int worldZ) {
        for (int dx = -TREE_SPACING_RADIUS; dx <= TREE_SPACING_RADIUS; dx++) {
            for (int dz = -TREE_SPACING_RADIUS; dz <= TREE_SPACING_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                int checkLocalX = localX + dx;
                int checkLocalZ = localZ + dz;
                if (checkLocalX < 0 || checkLocalX >= Chunk.CHUNK_SIZE
                    || checkLocalZ < 0 || checkLocalZ >= Chunk.CHUNK_SIZE) {
                    continue;
                }

                int checkWorldX = worldX + dx;
                int checkWorldZ = worldZ + dz;
                int surfaceY = terrainGenerator.getHeightAt(checkWorldX, checkWorldZ);
                if (surfaceY < 0 || surfaceY >= Chunk.CHUNK_HEIGHT) {
                    continue;
                }

                for (int dy = 1; dy <= TREE_SPACING_HEIGHT_CHECK; dy++) {
                    int checkY = surfaceY + dy;
                    if (checkY >= Chunk.CHUNK_HEIGHT) {
                        break;
                    }
                    if (chunk.getBlock(checkLocalX, checkY, checkLocalZ) == BlockType.WOOD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void ensureFallbackFeatures(Chunk chunk, int chunkWorldX, int chunkWorldZ,
                                        boolean plainsNeeded, boolean jungleNeeded,
                                        boolean desertNeeded, boolean snowNeeded) {
        if (plainsNeeded) {
            forcePlaceFeature(chunk, chunkWorldX, chunkWorldZ, BiomeType.PLAINS,
                (localX, localZ, worldX, worldZ) -> {
                    random.setSeed(seed + worldX * 341873128712L + worldZ * 132897987541L + 0x51A9E377L);
                    return placeTree(chunk, localX, localZ, worldX, worldZ, BiomeType.PLAINS, 4, 2);
                },
                (localX, localZ, worldX, worldZ) -> placeGuaranteedTree(chunk, localX, localZ, worldX, worldZ, BiomeType.PLAINS));
        }
        if (jungleNeeded) {
            forcePlaceFeature(chunk, chunkWorldX, chunkWorldZ, BiomeType.JUNGLE,
                (localX, localZ, worldX, worldZ) -> {
                    random.setSeed(seed + worldX * 341873128712L + worldZ * 132897987541L + 0x7F4A7C15L);
                    return placeTree(chunk, localX, localZ, worldX, worldZ, BiomeType.JUNGLE, 5, 2);
                },
                (localX, localZ, worldX, worldZ) -> placeGuaranteedTree(chunk, localX, localZ, worldX, worldZ, BiomeType.JUNGLE));
        }
        if (desertNeeded) {
            forcePlaceFeature(chunk, chunkWorldX, chunkWorldZ, BiomeType.DESERT,
                (localX, localZ, worldX, worldZ) -> {
                    random.setSeed(seed + worldX * 341873128712L + worldZ * 132897987541L + 0x3C6EF372L);
                    return placeCactus(chunk, localX, localZ, worldX, worldZ);
                },
                (localX, localZ, worldX, worldZ) -> placeGuaranteedCactus(chunk, localX, localZ, worldX, worldZ));
        }
        if (snowNeeded) {
            forcePlaceFeature(chunk, chunkWorldX, chunkWorldZ, BiomeType.SNOW,
                (localX, localZ, worldX, worldZ) -> {
                    random.setSeed(seed + worldX * 341873128712L + worldZ * 132897987541L + 0x9E3779B9L);
                    return placeSnowLayer(chunk, localX, localZ, worldX, worldZ);
                },
                (localX, localZ, worldX, worldZ) -> placeGuaranteedSnow(chunk, localX, localZ, worldX, worldZ));
        }
    }

    private void forcePlaceFeature(Chunk chunk, int chunkWorldX, int chunkWorldZ, BiomeType targetBiome,
                                   FeatureAttempt attempt, SimpleFeature fallback) {
        boolean placed = false;
        int fallbackLocalX = -1;
        int fallbackLocalZ = -1;
        int fallbackWorldX = 0;
        int fallbackWorldZ = 0;
        for (int localX = 0; localX < Chunk.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < Chunk.CHUNK_SIZE; localZ++) {
                int worldX = chunkWorldX + localX;
                int worldZ = chunkWorldZ + localZ;
                if (biomeGenerator.getBiome(worldX, worldZ) != targetBiome) {
                    continue;
                }
                if (!placed) {
                    fallbackLocalX = localX;
                    fallbackLocalZ = localZ;
                    fallbackWorldX = worldX;
                    fallbackWorldZ = worldZ;
                }
                if (attempt.place(localX, localZ, worldX, worldZ)) {
                    placed = true;
                    return;
                }
            }
        }
        if (!placed && fallbackLocalX >= 0 && fallback != null) {
            fallback.place(fallbackLocalX, fallbackLocalZ, fallbackWorldX, fallbackWorldZ);
        }
    }

    @FunctionalInterface
    private interface FeatureAttempt {
        boolean place(int localX, int localZ, int worldX, int worldZ);
    }

    @FunctionalInterface
    private interface SimpleFeature {
        void place(int localX, int localZ, int worldX, int worldZ);
    }

    private void placeGuaranteedTree(Chunk chunk, int localX, int localZ, int worldX, int worldZ, BiomeType biome) {
        int baseY = terrainGenerator.getHeightAt(worldX, worldZ);
        baseY = Math.max(1, Math.min(Chunk.CHUNK_HEIGHT - 8, baseY));
        chunk.setBlock(localX, baseY, localZ, biome.getSurfaceBlock());

        int trunkHeight = 4;
        for (int i = 1; i <= trunkHeight; i++) {
            int y = baseY + i;
            if (y >= Chunk.CHUNK_HEIGHT) {
                break;
            }
            chunk.setBlock(localX, y, localZ, BlockType.WOOD);
        }

        int leafStartY = baseY + trunkHeight - 1;
        int radius = 2;
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && dy < 2) {
                        continue;
                    }
                    int lx = localX + dx;
                    int ly = leafStartY + dy;
                    int lz = localZ + dz;
                    if (lx < 0 || lx >= Chunk.CHUNK_SIZE || lz < 0 || lz >= Chunk.CHUNK_SIZE) {
                        continue;
                    }
                    if (ly < 0 || ly >= Chunk.CHUNK_HEIGHT) {
                        continue;
                    }
                    if (dx == 0 && dz == 0 && dy < 2) {
                        continue;
                    }
                    chunk.setBlock(lx, ly, lz, BlockType.LEAVES);
                }
            }
        }
    }

    private void placeGuaranteedCactus(Chunk chunk, int localX, int localZ, int worldX, int worldZ) {
        int baseY = terrainGenerator.getHeightAt(worldX, worldZ);
        baseY = Math.max(1, Math.min(Chunk.CHUNK_HEIGHT - 4, baseY));
        chunk.setBlock(localX, baseY, localZ, BlockType.SAND);
        for (int i = 1; i <= 3; i++) {
            int y = baseY + i;
            if (y >= Chunk.CHUNK_HEIGHT) {
                break;
            }
            chunk.setBlock(localX, y, localZ, BlockType.CACTUS);
        }
    }

    private void placeGuaranteedSnow(Chunk chunk, int localX, int localZ, int worldX, int worldZ) {
        int baseY = terrainGenerator.getHeightAt(worldX, worldZ);
        baseY = Math.max(1, Math.min(Chunk.CHUNK_HEIGHT - 2, baseY));
        if (!chunk.getBlock(localX, baseY, localZ).isSolid()) {
            chunk.setBlock(localX, baseY, localZ, BlockType.SNOW_BLOCK);
        }
        int snowY = baseY + 1;
        if (snowY < Chunk.CHUNK_HEIGHT) {
            chunk.setBlock(localX, snowY, localZ, BlockType.SNOW_LAYER);
        }
    }

    private void placeRockOutcrop(Chunk chunk, int x, int z, int worldX, int worldZ) {
        int height = terrainGenerator.getHeightAt(worldX, worldZ);
        if (height < 1 || height >= Chunk.CHUNK_HEIGHT - 4) {
            return;
        }

        BlockType surfaceBlock = chunk.getBlock(x, height, z);
        if (!surfaceBlock.isSolid()) {
            return;
        }

        int radius = 1 + random.nextInt(2);
        int maxHeight = 2 + random.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > radius + random.nextInt(2)) {
                    continue;
                }
                int columnX = x + dx;
                int columnZ = z + dz;
                if (columnX < 0 || columnX >= Chunk.CHUNK_SIZE || columnZ < 0 || columnZ >= Chunk.CHUNK_SIZE) {
                    continue;
                }
                for (int dy = 1; dy <= maxHeight; dy++) {
                    int y = height + dy;
                    if (y >= Chunk.CHUNK_HEIGHT) {
                        break;
                    }
                    chunk.setBlock(columnX, y, columnZ, BlockType.STONE);
                }
            }
        }
    }

    private void placeSwampTree(Chunk chunk, int x, int z, int worldX, int worldZ) {
        int height = terrainGenerator.getHeightAt(worldX, worldZ);
        if (height < 0 || height >= Chunk.CHUNK_HEIGHT - 5) {
            return;
        }

        BlockType surfaceBlock = chunk.getBlock(x, height, z);
        if (surfaceBlock != BlockType.GRASS && surfaceBlock != BlockType.JUNGLE_GRASS) {
            return;
        }

        // Place a short trunk
        int trunkHeight = 3 + random.nextInt(2);
        for (int i = 1; i <= trunkHeight; i++) {
            int y = height + i;
            if (y < Chunk.CHUNK_HEIGHT) {
                chunk.setBlock(x, y, z, BlockType.WOOD);
            }
        }

        int canopyStart = height + trunkHeight - 1;
        for (int dy = 0; dy <= 2; dy++) {
            int layerRadius = 2 - dy;
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    int leafX = x + dx;
                    int leafZ = z + dz;
                    int leafY = canopyStart + dy;
                    if (leafX < 0 || leafX >= Chunk.CHUNK_SIZE || leafZ < 0 || leafZ >= Chunk.CHUNK_SIZE) {
                        continue;
                    }
                    if (leafY < 0 || leafY >= Chunk.CHUNK_HEIGHT) {
                        continue;
                    }
                    if (Math.abs(dx) == layerRadius && Math.abs(dz) == layerRadius && dy < 2) {
                        continue;
                    }
                    if (chunk.getBlock(leafX, leafY, leafZ) == BlockType.AIR) {
                        chunk.setBlock(leafX, leafY, leafZ, BlockType.LEAVES);
                    }
                }
            }
        }

        // Optional moss layer around tree base
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (random.nextFloat() < 0.35f) {
                    int mossX = x + dx;
                    int mossZ = z + dz;
                    if (mossX >= 0 && mossX < Chunk.CHUNK_SIZE && mossZ >= 0 && mossZ < Chunk.CHUNK_SIZE) {
                        int mossY = height;
                        BlockType current = chunk.getBlock(mossX, mossY, mossZ);
                        if (current == BlockType.GRASS) {
                            chunk.setBlock(mossX, mossY, mossZ, BlockType.JUNGLE_GRASS);
                        }
                    }
                }
            }
        }
    }
}
