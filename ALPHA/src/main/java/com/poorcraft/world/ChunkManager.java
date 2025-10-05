package com.poorcraft.world;

import com.poorcraft.render.Frustum;
import com.poorcraft.render.PerformanceMonitor;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Chunk manager that handles loading/unloading chunks based on camera position.
 * 
 * Dynamically loads chunks in a square around the player and unloads distant chunks.
 * The unload distance should be larger than load distance to prevent thrashing.
 * 
 * This is what keeps your RAM from exploding when you walk around.
 * You're welcome.
 */
public class ChunkManager {
    
    private final World world;
    private final PerformanceMonitor performanceMonitor;
    private int loadDistance;
    private int unloadDistance;
    private int preloadDistance;
    private ChunkPos lastPlayerChunk;
    private final Set<ChunkPos> loadedChunkPositions;
    private final Set<ChunkPos> pendingChunkLoads;
    private final ConcurrentMap<ChunkPos, Future<?>> inFlightLoads;
    private final ExecutorService chunkLoadExecutor;
    private final ChunkLoadPriority prioritySystem;
    private final Vector3f lastCameraPosition = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
    private final Vector3f lastViewDirection = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
    private float timeSinceLastRebuild;
    private int cancellationsThisFrame;
    
    /**
     * Creates a new chunk manager.
     * 
     * @param world World to manage chunks for
     * @param loadDistance How many chunks to load around player
     * @param unloadDistance Distance before unloading chunks
     */
    public ChunkManager(World world, int loadDistance, int unloadDistance) {
        this(world, loadDistance, unloadDistance, null);
    }

    public ChunkManager(World world, int loadDistance, int unloadDistance, PerformanceMonitor performanceMonitor) {
        this.world = world;
        this.performanceMonitor = performanceMonitor;
        this.loadDistance = loadDistance;
        this.unloadDistance = unloadDistance;
        this.preloadDistance = Math.max(1, loadDistance / 2);
        this.loadedChunkPositions = ConcurrentHashMap.newKeySet();
        this.pendingChunkLoads = ConcurrentHashMap.newKeySet();
        this.inFlightLoads = new ConcurrentHashMap<>();
        this.chunkLoadExecutor = createChunkExecutor();
        this.lastPlayerChunk = null;
        this.prioritySystem = new ChunkLoadPriority();
    }

    private ExecutorService createChunkExecutor() {
        int threadCount = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4));
        ThreadFactory factory = r -> {
            Thread thread = new Thread(r, "ChunkLoader-" + Integer.toHexString(r.hashCode()));
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };
        return Executors.newFixedThreadPool(threadCount, factory);
    }
    
    /**
     * Updates chunk loading/unloading based on camera position.
     * Should be called every frame.
     * 
     * @param cameraPosition Current camera position
     */
    public void update(Vector3f cameraPosition, Vector3f viewDirection, Frustum frustum, float deltaTime) {
        Objects.requireNonNull(cameraPosition, "cameraPosition");
        Objects.requireNonNull(viewDirection, "viewDirection");
        ChunkPos currentChunk = ChunkPos.fromWorldPos(cameraPosition);
        boolean moved = !currentChunk.equals(lastPlayerChunk);
        lastPlayerChunk = currentChunk;

        unloadDistantChunks(currentChunk);

        float movementDelta = Float.isNaN(lastCameraPosition.x) ? Float.POSITIVE_INFINITY : cameraPosition.distance(lastCameraPosition);
        float viewDelta = Float.isNaN(lastViewDirection.x) ? Float.POSITIVE_INFINITY : viewDirection.angle(lastViewDirection);
        timeSinceLastRebuild += deltaTime;

        boolean rebuildPriorities = moved
            || movementDelta > Chunk.CHUNK_SIZE
            || viewDelta > 0.15f
            || timeSinceLastRebuild > 0.5f;

        if (rebuildPriorities) {
            prioritySystem.rebuild(
                cameraPosition,
                viewDirection,
                frustum,
                loadDistance + preloadDistance,
                world,
                loadedChunkPositions,
                pendingChunkLoads
            );
            lastCameraPosition.set(cameraPosition);
            lastViewDirection.set(viewDirection);
            timeSinceLastRebuild = 0f;
            if (performanceMonitor != null) {
                performanceMonitor.setChunkLoadAveragePriority(prioritySystem.getAveragePriority());
                performanceMonitor.setChunkLoadCandidateCount(prioritySystem.getLastCandidateCount());
            }
        }

        int maxLoadsThisFrame = prioritySystem.computeBudget(deltaTime, performanceMonitor);
        Collection<ChunkLoadPriority.ChunkCandidate> candidates = prioritySystem.pollCandidates(maxLoadsThisFrame);

        for (ChunkLoadPriority.ChunkCandidate candidate : candidates) {
            ChunkPos pos = candidate.pos();
            if (loadedChunkPositions.contains(pos)) {
                continue;
            }
            if (pendingChunkLoads.add(pos)) {
                Future<?> future = chunkLoadExecutor.submit(() -> {
                    try {
                        world.getOrCreateChunk(pos);
                        loadedChunkPositions.add(pos);
                    } finally {
                        pendingChunkLoads.remove(pos);
                        inFlightLoads.remove(pos);
                    }
                });
                inFlightLoads.put(pos, future);
            }
        }

        cancellationsThisFrame = cancelStaleLoads(cameraPosition);

        if (performanceMonitor != null) {
            performanceMonitor.setPendingChunkLoads(pendingChunkLoads.size());
            performanceMonitor.setChunkLoadCancellations(cancellationsThisFrame);
            performanceMonitor.setChunkLoadQueueSize(prioritySystem.getQueuedCount());
            performanceMonitor.setChunkLoadBudgetLast(prioritySystem.getLastBudget());
        }
    }
    
    /**
     * Unloads chunks that are beyond the unload distance.
     * 
     * @param centerChunk Center chunk position (player's chunk)
     */
    private void unloadDistantChunks(ChunkPos centerChunk) {
        List<ChunkPos> toUnload = new ArrayList<>();

        for (ChunkPos pos : loadedChunkPositions) {
            int dx = Math.abs(pos.x - centerChunk.x);
            int dz = Math.abs(pos.z - centerChunk.z);

            if (dx > unloadDistance || dz > unloadDistance) {
                toUnload.add(pos);
            }
        }

        for (ChunkPos pos : toUnload) {
            world.unloadChunk(pos);
            loadedChunkPositions.remove(pos);
            Future<?> future = inFlightLoads.remove(pos);
            if (future != null) {
                future.cancel(true);
            }
            pendingChunkLoads.remove(pos);
        }
    }
    
    /**
     * Sets the load distance.
     * 
     * @param distance Load distance in chunks
     */
    public void setLoadDistance(int distance) {
        this.loadDistance = distance;
    }
    
    /**
     * Sets the unload distance.
     * @param distance Unload distance in chunks
     */
    public void setUnloadDistance(int distance) {
        this.unloadDistance = distance;
    }

    /**
     * Sets the preload distance.
     * 
     * @param distance Preload distance in chunks
     */
    public void setPreloadDistance(int distance) {
        this.preloadDistance = Math.max(0, distance);
    }
    /**
     * Gets the number of currently loaded chunks.
     * 
     * @return Loaded chunk count
     */
    public int getLoadedChunkCount() {
        return loadedChunkPositions.size();
    }

    /**
     * Gets the number of chunks currently queued for loading.
     * Useful for debugging streaming performance.
     *
     * @return Pending chunk load count
     */
    public int getPendingChunkCount() {
        return pendingChunkLoads.size();
    }

    /**
     * Shuts down the chunk loading thread pool.
     * Should be called when the game exits.
     */
    public void shutdown() {
        chunkLoadExecutor.shutdown();
        try {
            if (!chunkLoadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                chunkLoadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkLoadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        for (Future<?> future : inFlightLoads.values()) {
            future.cancel(true);
        }
        inFlightLoads.clear();
    }

    private int cancelStaleLoads(Vector3f cameraPosition) {
        float maxDistance = (loadDistance + preloadDistance + 1) * Chunk.CHUNK_SIZE;
        List<ChunkPos> toCancel = new ArrayList<>();
        for (ChunkPos pos : pendingChunkLoads) {
            Future<?> future = inFlightLoads.get(pos);
            if (future == null || future.isDone()) {
                continue;
            }
            Vector3f chunkCenter = new Vector3f(
                pos.x * Chunk.CHUNK_SIZE + Chunk.CHUNK_SIZE * 0.5f,
                cameraPosition.y,
                pos.z * Chunk.CHUNK_SIZE + Chunk.CHUNK_SIZE * 0.5f
            );
            if (chunkCenter.distance(cameraPosition) > maxDistance) {
                future.cancel(true);
                toCancel.add(pos);
            }
        }
        for (ChunkPos pos : toCancel) {
            pendingChunkLoads.remove(pos);
            inFlightLoads.remove(pos);
        }
        return toCancel.size();
    }
}
