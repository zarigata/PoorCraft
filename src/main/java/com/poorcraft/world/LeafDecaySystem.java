package com.poorcraft.world;

import com.poorcraft.config.Settings;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import com.poorcraft.world.entity.DropManager;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * Handles gradual decay of leaf blocks that are no longer supported by logs.
 */
public class LeafDecaySystem {

    private static final float DEFAULT_DECAY_CHECK_INTERVAL = 0.5f;
    private static final int DEFAULT_MAX_LEAF_DISTANCE = 5;
    private static final float DEFAULT_DECAY_CHANCE = 0.2f;
    private static final float DEFAULT_DROP_CHANCE = 0.2f;
    private static final int DEFAULT_MAX_BATCH_SIZE = 50;
    private static final int DEFAULT_BORDER_SCAN_RADIUS = 6;
    private static final int DEFAULT_BORDER_SCAN_Y_RANGE = 8;

    private final World world;
    private final DropManager dropManager;
    private final Queue<BlockPos> pendingQueue;
    private final Set<BlockPos> pendingSet;
    private final Map<BlockPos, Set<BlockPos>> decayGroups;
    private Random random;
    private final boolean debugLogging;

    private final float decayCheckInterval;
    private final int maxLeafDistance;
    private final float decayChance;
    private final float dropChance;
    private final int maxBatchSize;
    private final int borderScanRadius;
    private final int borderScanYBaseRange;
    private final boolean borderScanAdaptive;

    private float timeSinceLastCheck;

    public LeafDecaySystem(World world, DropManager dropManager) {
        this(world, dropManager, null, new Random());
    }

    public LeafDecaySystem(World world, DropManager dropManager, Random random) {
        this(world, dropManager, null, random);
    }

    public LeafDecaySystem(World world, DropManager dropManager, Settings.LeafDecaySettings settings) {
        this(world, dropManager, settings, new Random());
    }

    public LeafDecaySystem(World world, DropManager dropManager, Settings.LeafDecaySettings settings, Random random) {
        this.world = world;
        this.dropManager = dropManager;
        this.pendingQueue = new ArrayDeque<>();
        this.pendingSet = new HashSet<>();
        this.decayGroups = new HashMap<>();
        this.random = random != null ? random : new Random();
        Settings.LeafDecaySettings applied = settings;
        if (applied == null) {
            applied = new Settings.LeafDecaySettings();
        }
        this.decayCheckInterval = applied.decayCheckInterval > 0.0f ? applied.decayCheckInterval : DEFAULT_DECAY_CHECK_INTERVAL;
        this.maxLeafDistance = applied.maxLeafDistance > 0 ? applied.maxLeafDistance : DEFAULT_MAX_LEAF_DISTANCE;
        this.decayChance = clamp01(applied.decayChance, DEFAULT_DECAY_CHANCE);
        this.dropChance = clamp01(applied.dropChance, DEFAULT_DROP_CHANCE);
        this.maxBatchSize = applied.maxBatchSize > 0 ? applied.maxBatchSize : DEFAULT_MAX_BATCH_SIZE;
        int configuredBorderRadius = applied.borderScanRadius;
        if (configuredBorderRadius <= 0) {
            configuredBorderRadius = DEFAULT_BORDER_SCAN_RADIUS;
        }
        this.borderScanRadius = Math.max(0, configuredBorderRadius);
        int configuredBorderYRange = applied.borderScanYRange;
        if (configuredBorderYRange <= 0) {
            configuredBorderYRange = DEFAULT_BORDER_SCAN_Y_RANGE;
        }
        this.borderScanYBaseRange = Math.max(0, configuredBorderYRange);
        this.borderScanAdaptive = applied.borderScanAdaptive;
        this.timeSinceLastCheck = 0.0f;
        this.debugLogging = applied.debugLogging;
    }

    public void setRandom(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void markLeafForDecay(int x, int y, int z) {
        markLeafForDecay(x, y, z, null);
    }

    public void markLeafForDecay(int x, int y, int z, Set<BlockPos> allowedLogs) {
        BlockPos position = new BlockPos(x, y, z);

        if (allowedLogs != null && !allowedLogs.isEmpty()) {
            decayGroups.merge(position, new HashSet<>(allowedLogs), (existing, incoming) -> {
                if (existing == null) {
                    return incoming;
                }
                existing.addAll(incoming);
                return existing;
            });
        } else {
            decayGroups.remove(position);
        }

        if (pendingSet.add(position)) {
            pendingQueue.offer(position);
            if (debugLogging) {
                System.out.println("[LeafDecay][Debug] Marked leaf at (" + x + ", " + y + ", " + z + ") for decay check");
            }
        }
    }

    public void update(float deltaTime) {
        timeSinceLastCheck += deltaTime;
        if (timeSinceLastCheck < decayCheckInterval) {
            return;
        }

        timeSinceLastCheck = 0.0f;

        int processed = 0;
        while (!pendingQueue.isEmpty() && processed < maxBatchSize) {
            BlockPos position = pendingQueue.poll();
            if (position == null) {
                break;
            }

            pendingSet.remove(position);

            if (world.getBlock(position.x, position.y, position.z) != BlockType.LEAVES) {
                decayGroups.remove(position);
                processed++;
                continue;
            }

            checkAndDecayLeaf(position);
            processed++;
        }
    }

    private void checkAndDecayLeaf(BlockPos position) {
        Set<BlockPos> allowedLogs = decayGroups.get(position);
        LogSearchResult result = findNearestLogDistance(position, allowedLogs);
        if (result.blockedByUnloadedChunk) {
            requeue(position);
            return;
        }

        if (result.distance <= maxLeafDistance) {
            decayGroups.remove(position);
            return;
        }

        if (random.nextFloat() < decayChance) {
            world.setBlock(position.x, position.y, position.z, BlockType.AIR);
            if (random.nextFloat() < dropChance) {
                dropManager.spawn(BlockType.LEAVES, position.x + 0.5f, position.y + 0.1f, position.z + 0.5f, 1);
            }
            if (debugLogging) {
                System.out.println("[LeafDecay][Debug] Decayed leaf at (" + position.x + ", " + position.y + ", " + position.z + ")");
            }
            decayGroups.remove(position);
        } else {
            requeue(position);
        }
    }

    private void requeue(BlockPos position) {
        if (pendingSet.add(position)) {
            pendingQueue.offer(position);
        }
    }

    public void onChunkLoaded(Chunk chunk) {
        if (chunk == null) {
            return;
        }

        chunk.ensureDecompressed();

        scanChunkBorderForLeaves(chunk);
    }

    private void scanChunkBorderForLeaves(Chunk chunk) {
        ChunkPos pos = chunk.getPosition();
        int startX = pos.x * Chunk.CHUNK_SIZE;
        int startZ = pos.z * Chunk.CHUNK_SIZE;

        for (int localX = 0; localX < Chunk.CHUNK_SIZE; localX++) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < Chunk.CHUNK_SIZE; localZ++) {
                if (!isWithinBorder(localX, localZ)) {
                    continue;
                }

                int worldZ = startZ + localZ;
                int terrainY = Math.max(0, world.getHeightAt(worldX, worldZ));
                int scanRange = borderScanAdaptive ? Math.max(borderScanYBaseRange, terrainY) : borderScanYBaseRange;
                int minY = Math.max(0, terrainY - scanRange);
                int maxY = Math.min(Chunk.CHUNK_HEIGHT - 1, terrainY + scanRange);

                for (int y = minY; y <= maxY; y++) {
                    if (chunk.getBlock(localX, y, localZ) != BlockType.LEAVES) {
                        continue;
                    }
                    if (!world.areChunksLoadedForBlock(worldX, worldZ)) {
                        continue;
                    }
                    markLeafForDecay(worldX, y, worldZ);
                }
            }
        }
    }

    private boolean isWithinBorder(int localX, int localZ) {
        int radius = Math.min(borderScanRadius, Chunk.CHUNK_SIZE - 1);
        if (radius <= 0) {
            return false;
        }
        return localX < radius || localX >= Chunk.CHUNK_SIZE - radius
            || localZ < radius || localZ >= Chunk.CHUNK_SIZE - radius;
    }

    /**
     * Returns the Chebyshev distance to the nearest supporting log for the given position.
     * Intended solely for verification tests.
     */
    int debugGetNearestLogDistance(int x, int y, int z) {
        LogSearchResult result = findNearestLogDistance(new BlockPos(x, y, z));
        return result.distance;
    }

    private LogSearchResult findNearestLogDistance(BlockPos start) {
        return findNearestLogDistance(start, null);
    }

    private LogSearchResult findNearestLogDistance(BlockPos start, Set<BlockPos> allowedLogs) {
        if (allowedLogs != null && !allowedLogs.isEmpty()) {
            int minDistance = Integer.MAX_VALUE;
            for (BlockPos logPos : allowedLogs) {
                if (world.getBlock(logPos.x, logPos.y, logPos.z) != BlockType.WOOD) {
                    continue;
                }

                int dx = Math.abs(logPos.x - start.x);
                int dy = Math.abs(logPos.y - start.y);
                int dz = Math.abs(logPos.z - start.z);
                int distance = Math.max(Math.max(dx, dy), dz);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }

            if (minDistance == Integer.MAX_VALUE) {
                return new LogSearchResult(maxLeafDistance + 1, false);
            }

            return new LogSearchResult(minDistance, false);
        }

        int maxDistance = maxLeafDistance;
        int minDistance = maxDistance + 1;
        boolean blockedByUnloadedChunk = false;

        for (int dx = -maxDistance; dx <= maxDistance; dx++) {
            for (int dy = -maxDistance; dy <= maxDistance; dy++) {
                int targetY = start.y + dy;
                if (targetY < 0 || targetY >= Chunk.CHUNK_HEIGHT) {
                    continue;
                }

                for (int dz = -maxDistance; dz <= maxDistance; dz++) {
                    int chebyshevDistance = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
                    if (chebyshevDistance == 0 || chebyshevDistance > maxDistance || chebyshevDistance >= minDistance) {
                        continue;
                    }

                    int checkX = start.x + dx;
                    int checkZ = start.z + dz;

                    if (!world.areChunksLoadedForBlock(checkX, checkZ)) {
                        blockedByUnloadedChunk = true;
                        continue;
                    }

                    if (world.getBlock(checkX, targetY, checkZ) == BlockType.WOOD) {
                        minDistance = chebyshevDistance;
                        if (minDistance == 1) {
                            break;
                        }
                    }
                }
            }
        }

        int distance = minDistance <= maxDistance ? minDistance : maxDistance + 1;
        return new LogSearchResult(distance, blockedByUnloadedChunk);
    }

    public void clear() {
        pendingQueue.clear();
        pendingSet.clear();
        decayGroups.clear();
        timeSinceLastCheck = 0.0f;
    }

    private float clamp01(float value, float fallback) {
        if (Float.isNaN(value)) {
            return fallback;
        }
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    public static final class BlockPos {
        public final int x;
        public final int y;
        public final int z;

        public BlockPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            BlockPos other = (BlockPos) obj;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    private static final class LogSearchResult {
        final int distance;
        final boolean blockedByUnloadedChunk;

        LogSearchResult(int distance, boolean blockedByUnloadedChunk) {
            this.distance = distance;
            this.blockedByUnloadedChunk = blockedByUnloadedChunk;
        }
    }
}
