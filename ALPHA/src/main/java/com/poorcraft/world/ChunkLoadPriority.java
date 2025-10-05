package com.poorcraft.world;

import com.poorcraft.render.Frustum;
import com.poorcraft.render.PerformanceMonitor;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Maintains a priority queue of chunk load requests based on camera context.
 * <p>
 * Priority factors (higher is better):
 * <ul>
 *     <li>Distance to camera (closer chunks first)</li>
 *     <li>Alignment with camera view direction</li>
 *     <li>Frustum inclusion weight</li>
 *     <li>Vertical alignment with eye level</li>
 *     <li>Connectivity with already loaded neighbours</li>
 * </ul>
 * The queue is rebuilt when the camera moves or rotates significantly.
 */
public class ChunkLoadPriority {
    private final PriorityQueue<ChunkCandidate> queue =
        new PriorityQueue<>(Comparator.comparingDouble(ChunkCandidate::priority).reversed());

    private float averagePriority;
    private int lastBudget;
    private int lastCandidateCount;
    private final Vector3f lastCamera = new Vector3f(Float.NaN);
    private final Vector3f lastView = new Vector3f(Float.NaN);

    public record ChunkCandidate(ChunkPos pos, double priority, boolean inFrustum) {}

    public void rebuild(Vector3f cameraPosition,
                        Vector3f viewDirection,
                        Frustum frustum,
                        int radiusChunks,
                        World world,
                        Set<ChunkPos> loadedChunks,
                        Set<ChunkPos> pendingChunks) {
        Objects.requireNonNull(cameraPosition, "cameraPosition");
        Objects.requireNonNull(viewDirection, "viewDirection");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(loadedChunks, "loadedChunks");
        Objects.requireNonNull(pendingChunks, "pendingChunks");

        queue.clear();
        averagePriority = 0f;

        ChunkPos centerChunk = ChunkPos.fromWorldPos(cameraPosition);
        Vector3f normalizedView = new Vector3f(viewDirection).normalize();

        int maxRadius = Math.max(1, radiusChunks);
        int totalCandidates = 0;
        double prioritySum = 0.0;
        Set<ChunkPos> visited = new HashSet<>();

        for (int dz = -maxRadius; dz <= maxRadius; dz++) {
            for (int dx = -maxRadius; dx <= maxRadius; dx++) {
                int taxicab = Math.max(Math.abs(dx), Math.abs(dz));
                if (taxicab > maxRadius) {
                    continue;
                }

                ChunkPos pos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                if (!visited.add(pos)) {
                    continue;
                }
                if (loadedChunks.contains(pos) || pendingChunks.contains(pos)) {
                    continue;
                }

                float chunkCenterX = (pos.x + 0.5f) * Chunk.CHUNK_SIZE;
                float chunkCenterZ = (pos.z + 0.5f) * Chunk.CHUNK_SIZE;
                Vector3f toChunk = new Vector3f(chunkCenterX, cameraPosition.y, chunkCenterZ)
                    .sub(cameraPosition).normalize();

                double viewScore = Math.max(0.0f, toChunk.dot(normalizedView));

                double distanceXZ = Math.hypot(dx, dz);
                double distanceScore = 1.0 / (1.0 + distanceXZ);

                double neighbourScore = computeNeighbourScore(pos, loadedChunks);

                boolean inFrustum = isInFrustum(frustum, pos);
                double frustumScore = inFrustum ? 1.0 : 0.25;

                double heightScore = computeHeightScore(world, cameraPosition.y, chunkCenterX, chunkCenterZ);

                double priority = distanceScore * 0.35
                    + viewScore * 0.30
                    + frustumScore * 0.20
                    + neighbourScore * 0.10
                    + heightScore * 0.05;

                ChunkCandidate candidate = new ChunkCandidate(pos, priority, inFrustum);
                queue.add(candidate);
                prioritySum += priority;
                totalCandidates++;
            }
        }

        if (totalCandidates > 0) {
            averagePriority = (float) (prioritySum / totalCandidates);
        } else {
            averagePriority = 0f;
        }
        lastCandidateCount = totalCandidates;
        lastCamera.set(cameraPosition);
        lastView.set(normalizedView);
    }

    public int computeBudget(float deltaTime, PerformanceMonitor performanceMonitor) {
        float frameMs = deltaTime * 1000f;
        if (performanceMonitor != null) {
            frameMs = Math.max(performanceMonitor.getFrameTimeMs(), frameMs);
        }

        int budget;
        if (frameMs < 14f) {
            budget = 8;
        } else if (frameMs < 20f) {
            budget = 6;
        } else if (frameMs < 26f) {
            budget = 4;
        } else if (frameMs < 33f) {
            budget = 2;
        } else {
            budget = 1;
        }

        lastBudget = Math.max(1, budget);
        return Math.min(lastBudget, queue.size());
    }

    public Collection<ChunkCandidate> pollCandidates(int maxCount) {
        int count = Math.max(0, maxCount);
        List<ChunkCandidate> result = new ArrayList<>(Math.min(count, queue.size()));
        while (count-- > 0 && !queue.isEmpty()) {
            result.add(queue.poll());
        }
        return result;
    }

    public float getAveragePriority() {
        return averagePriority;
    }

    public int getQueuedCount() {
        return queue.size();
    }

    public int getLastBudget() {
        return lastBudget;
    }

    public int getLastCandidateCount() {
        return lastCandidateCount;
    }

    private static double computeHeightScore(World world, float eyeY, float chunkCenterX, float chunkCenterZ) {
        int sampleX = Math.round(chunkCenterX);
        int sampleZ = Math.round(chunkCenterZ);
        int terrainHeight = world.getHeightAt(sampleX, sampleZ);
        double delta = Math.abs(eyeY - terrainHeight);
        return 1.0 / (1.0 + delta / 16.0);
    }

    private static double computeNeighbourScore(ChunkPos pos, Set<ChunkPos> loadedChunks) {
        int neighbours = 0;
        if (loadedChunks.contains(new ChunkPos(pos.x + 1, pos.z))) neighbours++;
        if (loadedChunks.contains(new ChunkPos(pos.x - 1, pos.z))) neighbours++;
        if (loadedChunks.contains(new ChunkPos(pos.x, pos.z + 1))) neighbours++;
        if (loadedChunks.contains(new ChunkPos(pos.x, pos.z - 1))) neighbours++;
        return neighbours / 4.0;
    }

    private static boolean isInFrustum(Frustum frustum, ChunkPos pos) {
        if (frustum == null) {
            return true;
        }
        float minX = pos.x * Chunk.CHUNK_SIZE;
        float minZ = pos.z * Chunk.CHUNK_SIZE;
        float maxX = minX + Chunk.CHUNK_SIZE;
        float maxZ = minZ + Chunk.CHUNK_SIZE;
        return frustum.testAABB(minX, 0, minZ, maxX, Chunk.CHUNK_HEIGHT, maxZ);
    }

    public Vector3f getLastCamera() {
        return new Vector3f(lastCamera);
    }

    public Vector3f getLastView() {
        return new Vector3f(lastView);
    }
}
