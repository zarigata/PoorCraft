package com.poorcraft.world;

import com.poorcraft.config.Settings;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.entity.DropManager;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Handles automatic tree felling when the base log of a tall tree is broken.
 */
public class TreeFellingSystem {

    private static final int DEFAULT_MIN_TRUNK_HEIGHT = 3;
    private static final int DEFAULT_MAX_LEAF_FLOOD_FILL = 1024;
    private static final int DEFAULT_PASSIVE_LEAF_SCAN_RADIUS = 4;
    private static final int DEFAULT_PASSIVE_LEAF_SCAN_HEIGHT = 3;
    private static final int DEFAULT_CANOPY_HORIZONTAL_RADIUS = 5;
    private static final int DEFAULT_CANOPY_VERTICAL_RADIUS = 3;
    private static final int DEFAULT_CANOPY_DISTANCE_LIMIT = 6;

    private static final int[][] NEIGHBOR_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private final World world;
    private final DropManager dropManager;
    private final LeafDecaySystem leafDecaySystem;
    private final int minTrunkHeight;
    private final int maxLeafFloodFill;
    private final int passiveLeafScanRadius;
    private final int passiveLeafScanHeight;
    private final int canopyHorizontalRadius;
    private final int canopyVerticalRadius;
    private final int canopyDistanceLimit;
    private final int foreignLogCheckRadius;
    private final boolean debugLogging;

    private boolean fellingInProgress;

    public TreeFellingSystem(World world, DropManager dropManager, LeafDecaySystem leafDecaySystem) {
        this(world, dropManager, leafDecaySystem, null);
    }

    public TreeFellingSystem(World world, DropManager dropManager, LeafDecaySystem leafDecaySystem,
        Settings.TreeFellingSettings settings) {
        this.world = world;
        this.dropManager = dropManager;
        this.leafDecaySystem = leafDecaySystem;
        Settings.TreeFellingSettings applied = settings;
        if (applied == null) {
            applied = new Settings.TreeFellingSettings();
        }
        this.minTrunkHeight = applied.minTrunkHeight > 0 ? applied.minTrunkHeight : DEFAULT_MIN_TRUNK_HEIGHT;
        this.maxLeafFloodFill = applied.maxLeafFloodFill > 0 ? applied.maxLeafFloodFill : DEFAULT_MAX_LEAF_FLOOD_FILL;
        this.passiveLeafScanRadius = applied.passiveLeafScanRadius > 0 ? applied.passiveLeafScanRadius
            : DEFAULT_PASSIVE_LEAF_SCAN_RADIUS;
        this.passiveLeafScanHeight = applied.passiveLeafScanHeight > 0 ? applied.passiveLeafScanHeight
            : DEFAULT_PASSIVE_LEAF_SCAN_HEIGHT;
        this.canopyHorizontalRadius = applied.canopyHorizontalRadius > 0 ? applied.canopyHorizontalRadius
            : DEFAULT_CANOPY_HORIZONTAL_RADIUS;
        this.canopyVerticalRadius = applied.canopyVerticalRadius > 0 ? applied.canopyVerticalRadius
            : DEFAULT_CANOPY_VERTICAL_RADIUS;
        this.canopyDistanceLimit = applied.canopyDistanceLimit > 0 ? applied.canopyDistanceLimit
            : DEFAULT_CANOPY_DISTANCE_LIMIT;
        this.debugLogging = applied.debugLogging;
        this.foreignLogCheckRadius = applied.foreignLogCheckRadius > 0 ? applied.foreignLogCheckRadius : 2;
        this.fellingInProgress = false;
    }

    public void onWoodBlockBroken(int x, int y, int z) {
        if (!world.isAuthoritative()) {
            return;
        }
        if (fellingInProgress) {
            return;
        }

        if (debugLogging) {
            System.out.println("[TreeFelling][Debug] Checking wood break at " + x + "," + y + "," + z);
        }

        int logsAbove = countLogsAbove(x, y, z);
        int logsBelow = countLogsBelow(x, y, z);
        int totalTrunkHeight = logsAbove + logsBelow + 1;
        if (debugLogging) {
            System.out.println("[TreeFelling][Debug] Logs above: " + logsAbove);
            System.out.println("[TreeFelling][Debug] Logs below: " + logsBelow);
            System.out.println("[TreeFelling][Debug] Total trunk height: " + totalTrunkHeight
                + " (above=" + logsAbove + ", below=" + logsBelow + ")");
        }
        if (totalTrunkHeight >= minTrunkHeight + 1) {
            if (debugLogging) {
                System.out.println("[TreeFelling][Debug] Initiating felling at " + x + "," + y + "," + z);
            }
            fellTree(x, y, z);
        } else {
            markNearbyLeavesForDecay(x, y, z);
        }
    }

    private int countLogsAbove(int x, int y, int z) {
        int count = 0;
        for (int currentY = y + 1; currentY < Chunk.CHUNK_HEIGHT; currentY++) {
            BlockType block = world.getBlock(x, currentY, z);
            if (block != BlockType.WOOD) {
                break;
            }
            count++;
        }
        return count;
    }

    private int countLogsBelow(int x, int y, int z) {
        int count = 0;
        for (int currentY = y - 1; currentY >= 0; currentY--) {
            BlockType block = world.getBlock(x, currentY, z);
            if (block != BlockType.WOOD) {
                break;
            }
            count++;
        }
        return count;
    }

    private void fellTree(int x, int y, int z) {
        fellingInProgress = true;

        try {
            int removedLogs = 0;
            Set<IntPos> trunkPositionKeys = new HashSet<>();
            Set<LeafDecaySystem.BlockPos> trunkBlockPositions = new HashSet<>();

            int baseRemoved = removeLogSegment(x, y, z, true, trunkPositionKeys, trunkBlockPositions);
            if (baseRemoved == 0) {
                return;
            }
            removedLogs += baseRemoved;

            int currentY = y + 1;
            while (currentY < Chunk.CHUNK_HEIGHT) {
                BlockType block = world.getBlock(x, currentY, z);
                if (block != BlockType.WOOD) {
                    break;
                }

                removedLogs += removeLogSegment(x, currentY, z, false, trunkPositionKeys, trunkBlockPositions);
                currentY++;
            }

            currentY = y - 1;
            while (currentY >= 0) {
                BlockType block = world.getBlock(x, currentY, z);
                if (block != BlockType.WOOD) {
                    break;
                }

                removedLogs += removeLogSegment(x, currentY, z, false, trunkPositionKeys, trunkBlockPositions);
                currentY--;
            }

            findAndMarkLeavesForDecay(trunkPositionKeys, trunkBlockPositions);
            if (debugLogging) {
                System.out.println("[TreeFelling] Felled tree at (" + x + ", " + y + ", " + z + "), removed "
                    + removedLogs + " logs");
            }
        } finally {
            fellingInProgress = false;
        }
    }

    private int removeLogSegment(int x, int y, int z, boolean registerBaseDrop, Set<IntPos> trunkPositionKeys,
        Set<LeafDecaySystem.BlockPos> trunkBlockPositions) {
        boolean cancelled = false;
        if (!registerBaseDrop) {
            cancelled = world.fireBlockBreakEventWithCancellation(x, y, z, BlockType.WOOD, -1);
            if (cancelled) {
                return 0;
            }
        }

        world.setBlockSilent(x, y, z, BlockType.AIR);
        dropManager.spawn(BlockType.WOOD, x + 0.5f, y + 0.1f, z + 0.5f, 1);
        if (registerBaseDrop) {
            world.registerTreeFellingBaseDrop(x, y, z);
        }
        IntPos key = new IntPos(x, y, z);
        trunkPositionKeys.add(key);
        trunkBlockPositions.add(new LeafDecaySystem.BlockPos(x, y, z));
        return 1;
    }

    private void findAndMarkLeavesForDecay(Set<IntPos> trunkPositionKeys,
        Set<LeafDecaySystem.BlockPos> trunkBlockPositions) {
        if (leafDecaySystem == null || trunkBlockPositions == null || trunkBlockPositions.isEmpty()) {
            return;
        }

        SearchBounds bounds = createSearchBounds(trunkBlockPositions);
        CylindricalBounds horizontalBounds = createCylindricalBounds(trunkBlockPositions);
        Queue<LeafDecaySystem.BlockPos> queue = new ArrayDeque<>();
        Set<IntPos> visited = new HashSet<>();
        int discovered = 0;

        for (LeafDecaySystem.BlockPos trunkPos : trunkBlockPositions) {
            for (int[] offset : NEIGHBOR_OFFSETS) {
                int leafX = trunkPos.x + offset[0];
                int leafY = trunkPos.y + offset[1];
                int leafZ = trunkPos.z + offset[2];
                if (!isWithinWorldY(leafY) || !bounds.contains(leafX, leafY, leafZ)) {
                    continue;
                }
                if (horizontalBounds != null && !horizontalBounds.contains(leafX, leafZ)) {
                    continue;
                }
                if (!world.areChunksLoadedForBlock(leafX, leafZ)) {
                    continue;
                }
                BlockType neighborType = world.getBlock(leafX, leafY, leafZ);
                boolean neighborIsTrunk = neighborType == BlockType.WOOD
                    && isTrunkPosition(leafX, leafY, leafZ, trunkPositionKeys);
                if (neighborType == BlockType.LEAVES) {
                    if (!isWithinCanopyDistance(leafX, leafY, leafZ, trunkBlockPositions)) {
                        continue;
                    }
                    if (hasForeignLogNearby(leafX, leafY, leafZ, trunkPositionKeys)) {
                        continue;
                    }
                }
                if (neighborType == BlockType.LEAVES || neighborIsTrunk) {
                    IntPos key = new IntPos(leafX, leafY, leafZ);
                    if (visited.add(key)) {
                        queue.offer(new LeafDecaySystem.BlockPos(leafX, leafY, leafZ));
                    }
                }
            }
        }

        while (!queue.isEmpty() && discovered < maxLeafFloodFill) {
            LeafDecaySystem.BlockPos current = queue.poll();
            if (current == null) {
                continue;
            }

            if (!isWithinWorldY(current.y) || !bounds.contains(current.x, current.y, current.z)
                || !world.areChunksLoadedForBlock(current.x, current.z)) {
                continue;
            }
            if (horizontalBounds != null && !horizontalBounds.contains(current.x, current.z)) {
                continue;
            }

            BlockType blockType = world.getBlock(current.x, current.y, current.z);
            boolean isTrunkWood = blockType == BlockType.WOOD
                && isTrunkPosition(current.x, current.y, current.z, trunkPositionKeys);
            if (blockType != BlockType.LEAVES && !isTrunkWood) {
                continue;
            }

            if (blockType == BlockType.LEAVES) {
                if (hasForeignLogNearby(current.x, current.y, current.z, trunkPositionKeys)) {
                    continue;
                }
                leafDecaySystem.markLeafForDecay(current.x, current.y, current.z, trunkBlockPositions);
                discovered++;

                if (discovered >= maxLeafFloodFill) {
                    break;
                }
            }

            for (int[] offset : NEIGHBOR_OFFSETS) {
                int nextX = current.x + offset[0];
                int nextY = current.y + offset[1];
                int nextZ = current.z + offset[2];
                if (!isWithinWorldY(nextY) || !bounds.contains(nextX, nextY, nextZ)) {
                    continue;
                }
                if (horizontalBounds != null && !horizontalBounds.contains(nextX, nextZ)) {
                    continue;
                }
                if (!world.areChunksLoadedForBlock(nextX, nextZ)) {
                    continue;
                }
                BlockType nextType = world.getBlock(nextX, nextY, nextZ);
                boolean nextIsTrunk = nextType == BlockType.WOOD
                    && isTrunkPosition(nextX, nextY, nextZ, trunkPositionKeys);
                if (nextType == BlockType.LEAVES) {
                    if (!isWithinCanopyDistance(nextX, nextY, nextZ, trunkBlockPositions)) {
                        continue;
                    }
                    if (hasForeignLogNearby(nextX, nextY, nextZ, trunkPositionKeys)) {
                        continue;
                    }
                }
                if (nextType == BlockType.LEAVES || nextIsTrunk) {
                    IntPos nextKey = new IntPos(nextX, nextY, nextZ);
                    if (visited.add(nextKey)) {
                        queue.offer(new LeafDecaySystem.BlockPos(nextX, nextY, nextZ));
                    }
                }
            }
        }
    }

    private CylindricalBounds createCylindricalBounds(Set<LeafDecaySystem.BlockPos> trunkPositions) {
        if (trunkPositions == null || trunkPositions.isEmpty()) {
            return null;
        }

        double sumX = 0.0;
        double sumZ = 0.0;
        for (LeafDecaySystem.BlockPos trunkPos : trunkPositions) {
            sumX += trunkPos.x + 0.5;
            sumZ += trunkPos.z + 0.5;
        }

        double centerX = sumX / trunkPositions.size();
        double centerZ = sumZ / trunkPositions.size();

        double maxOffset = 0.0;
        for (LeafDecaySystem.BlockPos trunkPos : trunkPositions) {
            double dx = (trunkPos.x + 0.5) - centerX;
            double dz = (trunkPos.z + 0.5) - centerZ;
            double distance = Math.hypot(dx, dz);
            if (distance > maxOffset) {
                maxOffset = distance;
            }
        }

        double allowedRadius = canopyHorizontalRadius + maxOffset;
        return new CylindricalBounds(centerX, centerZ, allowedRadius);
    }

    private boolean hasForeignLogNearby(int x, int y, int z, Set<IntPos> trunkPositions) {
        if (trunkPositions == null || trunkPositions.isEmpty()) {
            return false;
        }

        for (int dx = -foreignLogCheckRadius; dx <= foreignLogCheckRadius; dx++) {
            for (int dy = -foreignLogCheckRadius; dy <= foreignLogCheckRadius; dy++) {
                int checkY = y + dy;
                if (!isWithinWorldY(checkY)) {
                    continue;
                }
                for (int dz = -foreignLogCheckRadius; dz <= foreignLogCheckRadius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    int checkX = x + dx;
                    int checkZ = z + dz;

                    if (!world.areChunksLoadedForBlock(checkX, checkZ)) {
                        continue;
                    }

                    BlockType type = world.getBlock(checkX, checkY, checkZ);
                    if (type == BlockType.WOOD && !isTrunkPosition(checkX, checkY, checkZ, trunkPositions)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void markNearbyLeavesForDecay(int centerX, int centerY, int centerZ) {
        if (leafDecaySystem == null) {
            return;
        }

        for (int dx = -passiveLeafScanRadius; dx <= passiveLeafScanRadius; dx++) {
            for (int dz = -passiveLeafScanRadius; dz <= passiveLeafScanRadius; dz++) {
                for (int dy = -passiveLeafScanHeight; dy <= passiveLeafScanHeight; dy++) {
                    int x = centerX + dx;
                    int y = centerY + dy;
                    int z = centerZ + dz;
                    if (!isWithinWorldY(y)) {
                        continue;
                    }
                    if (!world.areChunksLoadedForBlock(x, z)) {
                        continue;
                    }
                    if (world.getBlock(x, y, z) == BlockType.LEAVES) {
                        leafDecaySystem.markLeafForDecay(x, y, z);
                    }
                }
            }
        }
    }

    private boolean isWithinWorldY(int y) {
        return y >= 0 && y < Chunk.CHUNK_HEIGHT;
    }

    private boolean isTrunkPosition(int x, int y, int z, Set<IntPos> trunkPositions) {
        return trunkPositions.contains(new IntPos(x, y, z));
    }

    private boolean isWithinCanopyDistance(int x, int y, int z, Set<LeafDecaySystem.BlockPos> trunkPositions) {
        int distance = getClosestTrunkDistance(x, y, z, trunkPositions);
        return distance != Integer.MAX_VALUE && distance <= canopyDistanceLimit;
    }

    private int getClosestTrunkDistance(int x, int y, int z, Set<LeafDecaySystem.BlockPos> trunkPositions) {
        int minDistance = Integer.MAX_VALUE;
        for (LeafDecaySystem.BlockPos trunkPos : trunkPositions) {
            int dx = Math.abs(trunkPos.x - x);
            int dy = Math.abs(trunkPos.y - y);
            int dz = Math.abs(trunkPos.z - z);
            int distance = Math.max(Math.max(dx, dy), dz);
            if (distance < minDistance) {
                minDistance = distance;
                if (minDistance == 0) {
                    break;
                }
            }
        }
        return minDistance;
    }

    private SearchBounds createSearchBounds(Set<LeafDecaySystem.BlockPos> trunkPositions) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (LeafDecaySystem.BlockPos trunkPos : trunkPositions) {
            if (trunkPos.x < minX) {
                minX = trunkPos.x;
            }
            if (trunkPos.x > maxX) {
                maxX = trunkPos.x;
            }
            if (trunkPos.y < minY) {
                minY = trunkPos.y;
            }
            if (trunkPos.y > maxY) {
                maxY = trunkPos.y;
            }
            if (trunkPos.z < minZ) {
                minZ = trunkPos.z;
            }
            if (trunkPos.z > maxZ) {
                maxZ = trunkPos.z;
            }
        }

        if (minX == Integer.MAX_VALUE) {
            return new SearchBounds(0, 0, 0, 0, 0, 0);
        }

        return new SearchBounds(
            minX - canopyHorizontalRadius,
            maxX + canopyHorizontalRadius,
            minY - canopyVerticalRadius,
            maxY + canopyVerticalRadius,
            minZ - canopyHorizontalRadius,
            maxZ + canopyHorizontalRadius
        );
    }

    private static final class CylindricalBounds {
        private final double centerX;
        private final double centerZ;
        private final double radiusSquared;

        private CylindricalBounds(double centerX, double centerZ, double radius) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radiusSquared = radius * radius;
        }

        private boolean contains(int x, int z) {
            double dx = (x + 0.5) - centerX;
            double dz = (z + 0.5) - centerZ;
            return (dx * dx + dz * dz) <= radiusSquared;
        }
    }

    private static final class IntPos {
        private final int x;
        private final int y;
        private final int z;

        private IntPos(int x, int y, int z) {
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
            IntPos other = (IntPos) obj;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(x);
            result = 31 * result + Integer.hashCode(y);
            result = 31 * result + Integer.hashCode(z);
            return result;
        }
    }

    private static final class SearchBounds {
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;

        private SearchBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        private boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }
}
