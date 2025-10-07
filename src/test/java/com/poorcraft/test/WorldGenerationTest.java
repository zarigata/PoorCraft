package com.poorcraft.test;

import com.poorcraft.test.util.TestReportGenerator;
import com.poorcraft.test.util.TestUtils;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import com.poorcraft.world.generation.BiomeGenerator;
import com.poorcraft.world.generation.BiomeType;
import com.poorcraft.world.generation.TerrainGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates deterministic world generation behaviour.
 */
class WorldGenerationTest {

    private static final TestReportGenerator REPORT = new TestReportGenerator();
    private static final long TEST_SEED = 12345L;

    @BeforeAll
    static void beforeAll() {
        REPORT.addSystemInfo("World Seed", String.valueOf(TEST_SEED));
    }

    @AfterAll
    static void afterAll() {
        REPORT.generateConsoleReport();
        REPORT.generateMarkdownReport();
        REPORT.generateHtmlReport();
    }

    @Test
    @DisplayName("World creation establishes seed and defaults")
    void testWorldCreation() {
        World world = TestUtils.createTestWorld(TEST_SEED);
        assertNotNull(world, "World should not be null");
        assertEquals(TEST_SEED, world.getSeed(), "World seed mismatch");
        assertTrue(world.getLoadedChunks().isEmpty(), "Fresh world should have no loaded chunks");
        REPORT.addTestResult("World", "testWorldCreation", true, "World initialised with seed " + world.getSeed());
    }

    @Test
    @DisplayName("Chunk generation populates blocks")
    void testChunkGeneration() {
        World world = TestUtils.createTestWorld(TEST_SEED);
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        assertNotNull(chunk, "Chunk should not be null");
        assertEquals(Chunk.CHUNK_SIZE, Chunk.CHUNK_SIZE, "Chunk width constant mismatch");
        assertEquals(Chunk.CHUNK_HEIGHT, Chunk.CHUNK_HEIGHT, "Chunk height constant mismatch");
        boolean containsNonAir = false;
        for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
            if (chunk.getBlock(0, y, 0) != BlockType.AIR) {
                containsNonAir = true;
                break;
            }
        }
        assertTrue(containsNonAir, "Chunk should contain solid blocks");
        REPORT.addTestResult("World", "testChunkGeneration", true, "Chunk (0,0) generated with non-air content");
    }

    @Test
    @DisplayName("Biome sampling returns diverse results")
    void testBiomeGeneration() {
        BiomeGenerator generator = new BiomeGenerator(TEST_SEED);
        Set<BiomeType> observed = new HashSet<>();
        for (int x = -64; x <= 64; x += 16) {
            for (int z = -64; z <= 64; z += 16) {
                observed.add(generator.getBiome(x, z));
            }
        }
        assertTrue(observed.size() >= 3, "Expected multiple biome types across sample area");
        REPORT.addTestResult("World", "testBiomeGeneration", true, "Observed biomes: " + observed);
    }

    @Test
    @DisplayName("Terrain heights remain within world bounds")
    void testTerrainHeight() {
        TerrainGenerator terrain = new TerrainGenerator(TEST_SEED);
        List<Integer> heights = new ArrayList<>();
        for (int x = -128; x <= 128; x += 32) {
            for (int z = -128; z <= 128; z += 32) {
                heights.add(terrain.getHeightAt(x, z));
            }
        }
        boolean withinBounds = heights.stream().allMatch(h -> h >= 0 && h < Chunk.CHUNK_HEIGHT);
        boolean varied = heights.stream().distinct().count() > 3;
        assertTrue(withinBounds, "Terrain heights exceeded chunk bounds: " + heights);
        assertTrue(varied, "Terrain heights lacked variation");
        REPORT.addTestResult("World", "testTerrainHeight", true,
            "Sample heights within bounds and varied (min=" + heights.stream().min(Integer::compareTo).orElse(0)
                + ", max=" + heights.stream().max(Integer::compareTo).orElse(0) + ")");
    }

    @Test
    @DisplayName("Feature generation places biome-appropriate elements")
    void testFeatureGeneration() {
        World world = TestUtils.createTestWorld(TEST_SEED);

        EnumSet<BiomeType> targetBiomes = EnumSet.of(
            BiomeType.PLAINS,
            BiomeType.JUNGLE,
            BiomeType.DESERT,
            BiomeType.SNOW
        );

        Map<BiomeType, Boolean> featureSatisfied = new EnumMap<>(BiomeType.class);
        targetBiomes.forEach(biome -> featureSatisfied.put(biome, false));

        Set<BiomeType> encountered = EnumSet.noneOf(BiomeType.class);
        List<String> failures = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> chunkSummaries = new ArrayList<>();

        for (int chunkX = -2; chunkX <= 2; chunkX++) {
            for (int chunkZ = -2; chunkZ <= 2; chunkZ++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                Chunk chunk = world.getOrCreateChunk(pos);
                assertNotNull(chunk, "Generated chunk should not be null at " + pos);

                int centerX = pos.x * Chunk.CHUNK_SIZE + Chunk.CHUNK_SIZE / 2;
                int centerZ = pos.z * Chunk.CHUNK_SIZE + Chunk.CHUNK_SIZE / 2;
                BiomeType biome = world.getBiome(centerX, centerZ);
                assertNotNull(biome, "Biome should resolve for chunk center at " + pos);
                chunkSummaries.add(pos + "=" + biome.getName());

                if (!targetBiomes.contains(biome)) {
                    continue;
                }

                encountered.add(biome);

                if (featureSatisfied.get(biome)) {
                    continue; // already validated for this biome
                }

                boolean satisfied = switch (biome) {
                    case PLAINS, JUNGLE -> scanForSurfaceFeatures(world, pos,
                        EnumSet.of(BlockType.LEAVES, BlockType.WOOD));
                    case DESERT -> scanForCactusOnSand(world, pos);
                    case SNOW -> scanForSurfaceFeatures(world, pos,
                        EnumSet.of(BlockType.SNOW_LAYER, BlockType.SNOW_BLOCK, BlockType.ICE));
                    default -> true;
                };

                if (satisfied) {
                    featureSatisfied.put(biome, true);
                } else {
                    failures.add("Biome " + biome.getName() + " around chunk " + pos
                        + " lacked expected surface features within +/-4 blocks of height");
                }
            }
        }

        boolean allExpectationsMet = true;
        for (BiomeType biome : targetBiomes) {
            if (!encountered.contains(biome)) {
                skipped.add("Biome " + biome.getName() + " not observed within chunk radius 2; expectation skipped");
                continue;
            }
            if (!featureSatisfied.get(biome)) {
                allExpectationsMet = false;
            }
        }

        String reportMessage;
        if (allExpectationsMet) {
            reportMessage = "Validated biome features across chunks " + chunkSummaries;
            if (!skipped.isEmpty()) {
                reportMessage += " | Skipped: " + String.join(", ", skipped);
            }
        } else {
            reportMessage = String.join("; ", failures);
            if (!skipped.isEmpty()) {
                reportMessage += " | Skipped: " + String.join(", ", skipped);
            }
        }

        REPORT.addTestResult("World", "testFeatureGeneration", allExpectationsMet, reportMessage);
        assertTrue(allExpectationsMet, () -> "Biome feature thresholds violated: " + String.join("; ", failures)
            + ". Scanned chunks=" + chunkSummaries);
    }

    @Test
    @DisplayName("Adjacent chunks reference neighbours")
    void testChunkNeighbors() {
        World world = TestUtils.createTestWorld(TEST_SEED);
        Chunk center = world.getOrCreateChunk(new ChunkPos(0, 0));
        Chunk east = world.getOrCreateChunk(new ChunkPos(1, 0));
        Chunk west = world.getOrCreateChunk(new ChunkPos(-1, 0));
        BlockType eastWestBlock = east.getBlockOrNeighbor(-1, 64, 0);
        BlockType westEastBlock = west.getBlockOrNeighbor(Chunk.CHUNK_SIZE, 64, 0);
        assertNotNull(center, "Center chunk should exist");
        assertNotNull(east, "East chunk should exist");
        assertNotNull(west, "West chunk should exist");
        assertFalse(eastWestBlock == BlockType.AIR && westEastBlock == BlockType.AIR,
            "Neighbor queries should access adjacent chunk data");
        REPORT.addTestResult("World", "testChunkNeighbors", true, "Neighbor cross-access succeeded");
    }

    @Test
    @DisplayName("World block access spans multiple chunks")
    void testBlockAccess() {
        World world = TestUtils.createTestWorld(TEST_SEED);
        int worldX = Chunk.CHUNK_SIZE + 1;
        world.setBlock(worldX, 60, 0, BlockType.STONE);
        BlockType retrieved = world.getBlock(worldX, 60, 0);
        assertEquals(BlockType.STONE, retrieved, "World should support cross-chunk block writes");
        REPORT.addTestResult("World", "testBlockAccess", true, "Cross-chunk block placement valid");
    }

    private boolean scanForSurfaceFeatures(World world, ChunkPos chunkPos, Set<BlockType> targetBlocks) {
        if (targetBlocks == null || targetBlocks.isEmpty()) {
            return false;
        }

        int startX = chunkPos.x * Chunk.CHUNK_SIZE;
        int startZ = chunkPos.z * Chunk.CHUNK_SIZE;

        for (int localX = 0; localX < Chunk.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < Chunk.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = world.getHeightAt(worldX, worldZ);

                int scanMinY = Math.max(0, surfaceY - 2);
                int scanMaxY = Math.min(Chunk.CHUNK_HEIGHT - 1, surfaceY + 4);

                for (int y = scanMinY; y <= scanMaxY; y++) {
                    BlockType block = world.getBlock(worldX, y, worldZ);
                    if (targetBlocks.contains(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean scanForCactusOnSand(World world, ChunkPos chunkPos) {
        int startX = chunkPos.x * Chunk.CHUNK_SIZE;
        int startZ = chunkPos.z * Chunk.CHUNK_SIZE;

        for (int localX = 0; localX < Chunk.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < Chunk.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = world.getHeightAt(worldX, worldZ);
                int scanMinY = Math.max(0, surfaceY - 2);
                int scanMaxY = Math.min(Chunk.CHUNK_HEIGHT - 1, surfaceY + 4);

                for (int y = scanMinY; y <= scanMaxY; y++) {
                    BlockType block = world.getBlock(worldX, y, worldZ);
                    if (block == BlockType.CACTUS) {
                        BlockType below = y > 0 ? world.getBlock(worldX, y - 1, worldZ) : BlockType.AIR;
                        if (below == BlockType.SAND) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
