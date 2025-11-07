package com.poorcraft.test;

import com.poorcraft.test.util.TestReportGenerator;
import com.poorcraft.world.LeafDecaySystem;
import com.poorcraft.world.LeafDecaySystemTestAccessor;
import com.poorcraft.world.TreeFellingSystem;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import com.poorcraft.world.entity.DropManager;
import com.poorcraft.core.MiningSystem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("tree_mechanics")
class TreeMechanicsTest {

    private static final TestReportGenerator REPORT = new TestReportGenerator();
    private static final long TEST_SEED = 424242L;
    private static final int BASE_Y = 70;

    @BeforeAll
    static void beforeAll() {
        REPORT.addSystemInfo("Tree Mechanics Seed", String.valueOf(TEST_SEED));
    }

    @AfterAll
    static void afterAll() {
        REPORT.generateConsoleReport();
        REPORT.generateMarkdownReport();
        REPORT.generateHtmlReport();
    }

    @Test
    @DisplayName("Tall trees trigger felling and leaf marking")
    void testTreeFellingTriggersOnTallTree() throws Exception {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = createLeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int baseX = 8;
        int baseZ = 8;
        int trunkHeight = 5;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 2);

        world.setBlock(baseX, BASE_Y, baseZ, BlockType.AIR);

        boolean trunkCleared = true;
        for (int i = 0; i < trunkHeight; i++) {
            if (world.getBlock(baseX, BASE_Y + i, baseZ) == BlockType.WOOD) {
                trunkCleared = false;
                break;
            }
        }

        int leafPending = getPendingLeafCount(leafDecaySystem);
        int dropCount = dropManager.getDrops().size();

        REPORT.addTestResult("TreeMechanics", "testTreeFellingTriggersOnTallTree", trunkCleared && leafPending > 0,
            "Drops=" + dropCount + ", pendingLeaves=" + leafPending);

        assertTrue(trunkCleared, "Trunk blocks should all be removed");
        assertEquals(trunkHeight, dropCount, "Each felled log should spawn a drop");
        assertTrue(leafPending > 0, "Leaves around the trunk should be queued for decay");
    }

    @Test
    @DisplayName("Short trees do not trigger felling")
    void testTreeFellingDoesNotTriggerOnShortTree() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int baseX = 20;
        int baseZ = 20;
        int trunkHeight = 2;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 1);

        world.setBlock(baseX, BASE_Y, baseZ, BlockType.AIR);

        boolean topLogIntact = world.getBlock(baseX, BASE_Y + 1, baseZ) == BlockType.WOOD;
        int dropCount = dropManager.getDrops().size();

        REPORT.addTestResult("TreeMechanics", "testTreeFellingDoesNotTriggerOnShortTree", topLogIntact,
            "Drops=" + dropCount);

        assertTrue(topLogIntact, "Top log should remain for short trees");
        assertEquals(0, dropCount, "No additional drops expected for non-felled tree trunk");
    }

    @Test
    @DisplayName("Breaking middle trunk log fells tall tree")
    void testTreeFellingWorksOnMiddleLog() throws Exception {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = createLeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int baseX = 30;
        int baseZ = 30;
        int trunkHeight = 5;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 2);

        int middleY = BASE_Y + (trunkHeight / 2);
        world.setBlock(baseX, middleY, baseZ, BlockType.AIR);

        boolean trunkCleared = true;
        for (int i = 0; i < trunkHeight; i++) {
            if (world.getBlock(baseX, BASE_Y + i, baseZ) == BlockType.WOOD) {
                trunkCleared = false;
                break;
            }
        }

        int dropCount = dropManager.getDrops().size();
        int pendingLeaves = getPendingLeafCount(leafDecaySystem);

        REPORT.addTestResult("TreeMechanics", "testTreeFellingWorksOnMiddleLog",
            trunkCleared && dropCount == trunkHeight && pendingLeaves > 0,
            "Drops=" + dropCount + ", pendingLeaves=" + pendingLeaves);

        assertTrue(trunkCleared, "Breaking middle log should fell the entire trunk");
        assertEquals(trunkHeight, dropCount, "Each felled log should spawn one drop");
        assertTrue(pendingLeaves > 0, "Leaves should be queued for decay after felling");
    }

    @Test
    @DisplayName("Breaking top trunk log still fells tall tree")
    void testTreeFellingWorksOnTopLog() throws Exception {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = createLeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int baseX = 42;
        int baseZ = 42;
        int trunkHeight = 5;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 2);

        int topY = BASE_Y + trunkHeight - 1;
        world.setBlock(baseX, topY, baseZ, BlockType.AIR);

        boolean trunkCleared = true;
        for (int i = 0; i < trunkHeight; i++) {
            if (world.getBlock(baseX, BASE_Y + i, baseZ) == BlockType.WOOD) {
                trunkCleared = false;
                break;
            }
        }

        int dropCount = dropManager.getDrops().size();
        int pendingLeaves = getPendingLeafCount(leafDecaySystem);

        REPORT.addTestResult("TreeMechanics", "testTreeFellingWorksOnTopLog",
            trunkCleared && dropCount == trunkHeight && pendingLeaves > 0,
            "Drops=" + dropCount + ", pendingLeaves=" + pendingLeaves);

        assertTrue(trunkCleared, "Breaking the top log should fell the entire trunk");
        assertEquals(trunkHeight, dropCount, "Each felled log should spawn one drop");
        assertTrue(pendingLeaves > 0, "Leaves should be queued for decay after felling");
    }

    @Test
    @DisplayName("Leaf decay removes unsupported leaves")
    void testLeafDecayRemovesDistantLeaves() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int baseX = 40;
        int baseZ = 40;
        int trunkHeight = 5;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 3);

        world.setBlock(baseX, BASE_Y, baseZ, BlockType.AIR);

        List<int[]> leaves = collectLeaves(world, baseX, BASE_Y + trunkHeight - 1, baseZ, 4);
        leaves.forEach(pos -> leafDecaySystem.markLeafForDecay(pos[0], pos[1], pos[2]));

        for (int i = 0; i < 200; i++) {
            leafDecaySystem.update(0.25f);
        }

        long remainingLeaves = leaves.stream()
            .filter(pos -> world.getBlock(pos[0], pos[1], pos[2]) == BlockType.LEAVES)
            .count();

        int totalDrops = dropManager.getDrops().size();

        REPORT.addTestResult("TreeMechanics", "testLeafDecayRemovesDistantLeaves", remainingLeaves < leaves.size(),
            "RemainingLeaves=" + remainingLeaves + ", drops=" + totalDrops);

        assertTrue(remainingLeaves < leaves.size(), "At least one leaf should decay when unsupported");
        assertTrue(totalDrops >= trunkHeight, "Leaf decay should eventually spawn some drops");
    }

    @Test
    @DisplayName("Nearby leaves remain when supported")
    void testLeafDecayPreservesNearbyLeaves() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);

        int baseX = 55;
        int baseZ = 55;
        int trunkHeight = 4;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 2);

        List<int[]> leaves = collectLeaves(world, baseX, BASE_Y + trunkHeight - 1, baseZ, 3);
        leaves.forEach(pos -> leafDecaySystem.markLeafForDecay(pos[0], pos[1], pos[2]));

        for (int i = 0; i < 120; i++) {
            leafDecaySystem.update(0.25f);
        }

        boolean allLeavesPresent = leaves.stream()
            .allMatch(pos -> world.getBlock(pos[0], pos[1], pos[2]) == BlockType.LEAVES);

        REPORT.addTestResult("TreeMechanics", "testLeafDecayPreservesNearbyLeaves", allLeavesPresent,
            "LeavesChecked=" + leaves.size());

        assertTrue(allLeavesPresent, "Leaves within trunk range should remain");
    }

    @Test
    @DisplayName("Leaf decay BFS finds nearest log distances")
    void testLeafDecayBFSFindsNearestLog() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);

        int baseX = 70;
        int baseZ = 70;
        int trunkHeight = 5;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 2);

        int distance1 = LeafDecaySystemTestAccessor.getNearestLogDistance(leafDecaySystem, baseX, BASE_Y + trunkHeight, baseZ + 1);
        int distance2 = LeafDecaySystemTestAccessor.getNearestLogDistance(leafDecaySystem, baseX + 4, BASE_Y + trunkHeight, baseZ + 4);
        int distance3 = LeafDecaySystemTestAccessor.getNearestLogDistance(leafDecaySystem, baseX + 8, BASE_Y + trunkHeight, baseZ);

        REPORT.addTestResult("TreeMechanics", "testLeafDecayBFSFindsNearestLog",
            distance1 == 1 && distance2 == 4 && distance3 == 6,
            "Distances=" + distance1 + "," + distance2 + "," + distance3);

        assertEquals(1, distance1, "Adjacent leaves should report Chebyshev distance 1");
        assertEquals(4, distance2, "Diagonal leaves four blocks away should report distance 4");
        assertEquals(6, distance3, "Leaves beyond search radius should exceed max distance");
    }

    @Test
    @DisplayName("Decay groups ignore nearby foreign trunks")
    void testLeafDecayGroupIgnoresOtherTree() {
        World world = createWorld();
        DropManager dropManager = new DropManager();

        int firstX = 150;
        int secondX = firstX + 7;
        int z = 150;
        int trunkHeight = 5;

        for (int i = 0; i < trunkHeight; i++) {
            world.setBlock(firstX, BASE_Y + i, z, BlockType.WOOD);
            world.setBlock(secondX, BASE_Y + i, z, BlockType.WOOD);
        }

        int leafY = BASE_Y + trunkHeight;
        world.setBlock(secondX - 1, leafY, z, BlockType.LEAVES);

        LeafDecaySystem globalDecay = new LeafDecaySystem(world, dropManager);
        globalDecay.setRandom(new AlwaysDecayRandom());
        globalDecay.markLeafForDecay(secondX - 1, leafY, z);
        for (int i = 0; i < 10; i++) {
            globalDecay.update(0.5f);
        }

        assertEquals(BlockType.LEAVES, world.getBlock(secondX - 1, leafY, z),
            "Global search should treat nearby foreign logs as support");

        world.setBlock(secondX - 1, leafY, z, BlockType.LEAVES);

        LeafDecaySystem groupedDecay = new LeafDecaySystem(world, dropManager);
        groupedDecay.setRandom(new AlwaysDecayRandom());
        Set<LeafDecaySystem.BlockPos> allowedLogs = new HashSet<>();
        for (int i = 0; i < trunkHeight; i++) {
            allowedLogs.add(new LeafDecaySystem.BlockPos(firstX, BASE_Y + i, z));
        }

        groupedDecay.markLeafForDecay(secondX - 1, leafY, z, allowedLogs);
        for (int i = 0; i < 10; i++) {
            groupedDecay.update(0.5f);
        }

        assertEquals(BlockType.AIR, world.getBlock(secondX - 1, leafY, z),
            "Decay groups should ignore other tree trunks when determining support");
    }

    @Test
    @DisplayName("Trees at chunk boundaries fell correctly")
    void testTreeFellingAcrossChunkBoundary() throws Exception {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int baseX = Chunk.CHUNK_SIZE - 1;
        int baseZ = Chunk.CHUNK_SIZE - 1;
        int trunkHeight = 5;

        world.getOrCreateChunk(ChunkPos.fromWorldPos(baseX, baseZ));
        world.getOrCreateChunk(ChunkPos.fromWorldPos(baseX + 1, baseZ));

        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 3);

        world.setBlock(baseX, BASE_Y, baseZ, BlockType.AIR);

        boolean trunkCleared = true;
        for (int i = 0; i < trunkHeight; i++) {
            if (world.getBlock(baseX, BASE_Y + i, baseZ) == BlockType.WOOD) {
                trunkCleared = false;
                break;
            }
        }

        Chunk primary = world.getOrCreateChunk(ChunkPos.fromWorldPos(baseX, baseZ));
        Chunk neighbor = world.getOrCreateChunk(ChunkPos.fromWorldPos(baseX + 1, baseZ));
        boolean chunksDirty = isChunkDirty(primary) && isChunkDirty(neighbor);

        REPORT.addTestResult("TreeMechanics", "testTreeFellingAcrossChunkBoundary", trunkCleared && chunksDirty,
            "chunksDirty=" + chunksDirty);

        assertTrue(trunkCleared, "Boundary tree trunk should be removed");
        assertTrue(chunksDirty, "Both chunks should be marked dirty for mesh updates");
    }

    @Test
    @DisplayName("Multiple trees fall independently")
    void testMultipleTreesIndependent() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int firstX = 90;
        int secondX = 96;
        int z = 90;
        int trunkHeight = 4;

        buildTree(world, firstX, BASE_Y, z, trunkHeight, 2);
        buildTree(world, secondX, BASE_Y, z, trunkHeight, 2);

        world.setBlock(firstX, BASE_Y, z, BlockType.AIR);

        boolean firstCleared = true;
        boolean secondIntact = true;
        for (int i = 0; i < trunkHeight; i++) {
            if (world.getBlock(firstX, BASE_Y + i, z) == BlockType.WOOD) {
                firstCleared = false;
            }
            if (world.getBlock(secondX, BASE_Y + i, z) != BlockType.WOOD) {
                secondIntact = false;
            }
        }

        REPORT.addTestResult("TreeMechanics", "testMultipleTreesIndependent", firstCleared && secondIntact,
            "firstCleared=" + firstCleared + ", secondIntact=" + secondIntact);

        assertTrue(firstCleared, "First tree should be felled");
        assertTrue(secondIntact, "Second tree should remain untouched");
    }

    @Test
    @DisplayName("Leaf decay proceeds gradually")
    void testLeafDecayGradual() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int baseX = 110;
        int baseZ = 110;
        int trunkHeight = 5;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 3);

        world.setBlock(baseX, BASE_Y, baseZ, BlockType.AIR);
        List<int[]> leaves = collectLeaves(world, baseX, BASE_Y + trunkHeight - 1, baseZ, 4);
        leaves.forEach(pos -> leafDecaySystem.markLeafForDecay(pos[0], pos[1], pos[2]));

        leafDecaySystem.update(0.5f);
        long firstPassLeaves = leaves.stream()
            .filter(pos -> world.getBlock(pos[0], pos[1], pos[2]) == BlockType.LEAVES)
            .count();

        for (int i = 0; i < 200; i++) {
            leafDecaySystem.update(0.5f);
        }

        long remainingLeaves = leaves.stream()
            .filter(pos -> world.getBlock(pos[0], pos[1], pos[2]) == BlockType.LEAVES)
            .count();

        boolean gradual = firstPassLeaves > 0 && remainingLeaves == 0;
        REPORT.addTestResult("TreeMechanics", "testLeafDecayGradual", gradual,
            "afterFirstPass=" + firstPassLeaves + ", afterFull=" + remainingLeaves);

        assertTrue(firstPassLeaves > 0, "Not all leaves should decay immediately");
        assertEquals(0, remainingLeaves, "Leaves should eventually decay fully");
    }

    @Test
    @DisplayName("Player placed trees follow same behaviour")
    void testPlayerPlacedTreeBehavior() throws Exception {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = createLeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int baseX = 125;
        int baseZ = 125;
        int trunkHeight = 6;
        for (int i = 0; i < trunkHeight; i++) {
            world.setBlock(baseX, BASE_Y + i, baseZ, BlockType.WOOD);
        }
        world.setBlock(baseX, BASE_Y + trunkHeight, baseZ, BlockType.LEAVES);
        world.setBlock(baseX + 1, BASE_Y + trunkHeight, baseZ, BlockType.LEAVES);
        world.setBlock(baseX - 1, BASE_Y + trunkHeight, baseZ, BlockType.LEAVES);

        world.setBlock(baseX, BASE_Y, baseZ, BlockType.AIR);

        boolean trunkCleared = true;
        for (int i = 0; i < trunkHeight; i++) {
            if (world.getBlock(baseX, BASE_Y + i, baseZ) == BlockType.WOOD) {
                trunkCleared = false;
                break;
            }
        }

        int leafPending = getPendingLeafCount(leafDecaySystem);
        REPORT.addTestResult("TreeMechanics", "testPlayerPlacedTreeBehavior", trunkCleared && leafPending > 0,
            "pendingLeaves=" + leafPending);

        assertTrue(trunkCleared, "Player placed tree should fell like natural tree");
        assertTrue(leafPending > 0, "Leaves should be queued for decay");
    }

    @Test
    @DisplayName("Leaf distance returns max when no logs present")
    void testLeafDecayDistanceWhenNoLog() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);

        int x = 140;
        int z = 140;
        world.setBlock(x, BASE_Y + 1, z, BlockType.LEAVES);

        int distance = LeafDecaySystemTestAccessor.getNearestLogDistance(leafDecaySystem, x, BASE_Y + 1, z);

        REPORT.addTestResult("TreeMechanics", "testLeafDecayDistanceWhenNoLog", distance > 5,
            "distance=" + distance);

        assertTrue(distance > 5, "Distance should exceed max when no logs are nearby");
    }

    @Test
    @DisplayName("Leaf decay performance remains within budget")
    void testLeafDecayPerformanceBenchmark() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);

        int treeCount = 100;
        int trunkHeight = 4;
        int startX = 0;
        int startZ = 200;

        for (int i = 0; i < treeCount; i++) {
            int offsetX = startX + (i % 10) * 4;
            int offsetZ = startZ + (i / 10) * 4;
            buildTree(world, offsetX, BASE_Y, offsetZ, trunkHeight, 2);
            world.setBlock(offsetX, BASE_Y, offsetZ, BlockType.AIR);
            List<int[]> leaves = collectLeaves(world, offsetX, BASE_Y + trunkHeight - 1, offsetZ, 3);
            leaves.forEach(pos -> leafDecaySystem.markLeafForDecay(pos[0], pos[1], pos[2]));
        }

        long startNs = System.nanoTime();
        leafDecaySystem.update(0.5f);
        long durationNs = System.nanoTime() - startNs;

        REPORT.addTestResult("TreeMechanics", "testLeafDecayPerformanceBenchmark",
            durationNs < Duration.ofMillis(100).toNanos(), "durationNs=" + durationNs);

        assertTrue(durationNs < Duration.ofMillis(100).toNanos(), "Decay update should remain under 100ms");
    }

    @Test
    @DisplayName("Tree felling does not duplicate base log drops")
    void testTreeFellingNoDuplicateBaseLogDrop() {
        World world = createWorld();
        DropManager dropManager = new DropManager();
        LeafDecaySystem leafDecaySystem = new LeafDecaySystem(world, dropManager);
        TreeFellingSystem treeFellingSystem = new TreeFellingSystem(world, dropManager, leafDecaySystem);
        world.setTreeFellingSystem(treeFellingSystem);
        world.setLeafDecaySystem(leafDecaySystem);

        int baseX = 200;
        int baseZ = 200;
        int trunkHeight = 4;
        buildTree(world, baseX, BASE_Y, baseZ, trunkHeight, 2);
        MiningSystem miningSystem = new MiningSystem();
        miningSystem.setDropManager(dropManager);

        // Break the base log through the world to trigger felling
        world.setBlock(baseX, BASE_Y, baseZ, BlockType.AIR);

        long dropsAfterFelling = dropManager.getDrops().stream()
            .filter(drop -> drop.getBlockType() == BlockType.WOOD)
            .count();

        assertEquals(trunkHeight, dropsAfterFelling, "Tree felling should spawn one drop per trunk block");

        try {
            Method spawnDrop = MiningSystem.class.getDeclaredMethod("spawnDrop", World.class, BlockType.class, int.class, int.class, int.class);
            spawnDrop.setAccessible(true);
            spawnDrop.invoke(miningSystem, world, BlockType.WOOD, baseX, BASE_Y, baseZ);
        } catch (Exception e) {
            fail("Failed to invoke MiningSystem.spawnDrop via reflection: " + e.getMessage());
        }

        long dropsAfterMining = dropManager.getDrops().stream()
            .filter(drop -> drop.getBlockType() == BlockType.WOOD)
            .count();

        REPORT.addTestResult("TreeMechanics", "testTreeFellingNoDuplicateBaseLogDrop",
            dropsAfterMining == trunkHeight, "dropsAfterMining=" + dropsAfterMining + ", trunkHeight=" + trunkHeight);

        assertEquals(trunkHeight, dropsAfterMining, "MiningSystem should not duplicate base log drop after felling");
    }

    private static World createWorld() {
        return new World(TEST_SEED, false);
    }

    private static LeafDecaySystem createLeafDecaySystem(World world, DropManager dropManager) {
        return new LeafDecaySystem(world, dropManager, new Random(1337));
    }

    private static final class AlwaysDecayRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        public float nextFloat() {
            return 0.0f;
        }
    }

    private static void buildTree(World world, int baseX, int baseY, int baseZ, int trunkHeight, int leafRadius) {
        for (int i = 0; i < trunkHeight; i++) {
            world.setBlock(baseX, baseY + i, baseZ, BlockType.WOOD);
        }
        int leafBaseY = baseY + trunkHeight - 1;
        for (int dx = -leafRadius; dx <= leafRadius; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -leafRadius; dz <= leafRadius; dz++) {
                    if (dx == 0 && dz == 0 && dy <= 0) {
                        continue;
                    }
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > leafRadius + 0.5) {
                        continue;
                    }
                    int lx = baseX + dx;
                    int ly = leafBaseY + dy;
                    int lz = baseZ + dz;
                    if (ly >= 0 && ly < Chunk.CHUNK_HEIGHT) {
                        world.setBlock(lx, ly, lz, BlockType.LEAVES);
                    }
                }
            }
        }
    }

    private static List<int[]> collectLeaves(World world, int centerX, int centerY, int centerZ, int radius) {
        List<int[]> leaves = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = centerX + dx;
                    int y = centerY + dy;
                    int z = centerZ + dz;
                    if (world.getBlock(x, y, z) == BlockType.LEAVES) {
                        leaves.add(new int[]{x, y, z});
                    }
                }
            }
        }
        return leaves;
    }

    private static int getPendingLeafCount(LeafDecaySystem system) throws Exception {
        Field field = LeafDecaySystem.class.getDeclaredField("pendingSet");
        field.setAccessible(true);
        Set<?> pending = (Set<?>) field.get(system);
        return pending.size();
    }

    private static boolean isChunkDirty(Chunk chunk) throws Exception {
        Field field = Chunk.class.getDeclaredField("meshDirty");
        field.setAccessible(true);
        return field.getBoolean(chunk);
    }

}
