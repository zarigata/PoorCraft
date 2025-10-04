package com.poorcraft.world;

import com.poorcraft.world.chunk.ChunkPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private int loadDistance;
    private int unloadDistance;
    private int preloadDistance;
    private ChunkPos lastPlayerChunk;
    private final Set<ChunkPos> loadedChunkPositions;
    private final Set<ChunkPos> pendingChunkLoads;
    private final ExecutorService chunkLoadExecutor;
    
    /**
     * Creates a new chunk manager.
     * 
     * @param world World to manage chunks for
     * @param loadDistance How many chunks to load around player
     * @param unloadDistance Distance before unloading chunks
     */
    public ChunkManager(World world, int loadDistance, int unloadDistance) {
        this.world = world;
        this.loadDistance = loadDistance;
        this.unloadDistance = unloadDistance;
        this.preloadDistance = Math.max(1, loadDistance / 2);
        this.loadedChunkPositions = ConcurrentHashMap.newKeySet();
        this.pendingChunkLoads = ConcurrentHashMap.newKeySet();
        this.chunkLoadExecutor = createChunkExecutor();
        this.lastPlayerChunk = null;
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
    public void update(Vector3f cameraPosition) {
        // Convert camera position to chunk coordinates
        ChunkPos currentChunk = ChunkPos.fromWorldPos(cameraPosition);
        
        // Early exit if player hasn't moved to a different chunk
        if (currentChunk.equals(lastPlayerChunk)) {
            return;
        }
        
        lastPlayerChunk = currentChunk;
        
        // Load chunks around player
        loadChunksAroundPlayer(currentChunk);
        
        // Unload distant chunks
        unloadDistantChunks(currentChunk);
    }
    
    /**
     * Loads chunks in a square around the player.
     * 
     * @param centerChunk Center chunk position (player's chunk)
     */
    private void loadChunksAroundPlayer(ChunkPos centerChunk) {
        int maxRadius = loadDistance + preloadDistance;
        List<ChunkRequest> requests = new ArrayList<>();

        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                ChunkPos pos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);

                if (loadedChunkPositions.contains(pos) || pendingChunkLoads.contains(pos)) {
                    continue;
                }

                int manhattan = Math.max(Math.abs(dx), Math.abs(dz));
                if (manhattan > maxRadius) {
                    continue;
                }

                boolean withinActiveView = manhattan <= loadDistance;
                int distanceSq = dx * dx + dz * dz;
                requests.add(new ChunkRequest(pos, distanceSq, withinActiveView));
            }
        }

        if (requests.isEmpty()) {
            return;
        }

        requests.sort(Comparator
            .comparingInt((ChunkRequest req) -> req.withinActiveView ? 0 : 1)
            .thenComparingInt(req -> req.distanceSq));

        for (ChunkRequest request : requests) {
            pendingChunkLoads.add(request.pos);
            chunkLoadExecutor.submit(() -> {
                try {
                    world.getOrCreateChunk(request.pos);
                    loadedChunkPositions.add(request.pos);
                } finally {
                    pendingChunkLoads.remove(request.pos);
                }
            });
        }
    }
    
    /**
     * Unloads chunks that are beyond the unload distance.
     * 
     * @param centerChunk Center chunk position (player's chunk)
     */
    private void unloadDistantChunks(ChunkPos centerChunk) {
        // Build list of chunks to unload (avoid concurrent modification)
        List<ChunkPos> toUnload = new ArrayList<>();
        
        for (ChunkPos pos : loadedChunkPositions) {
            int dx = Math.abs(pos.x - centerChunk.x);
            int dz = Math.abs(pos.z - centerChunk.z);
            
            // Unload if beyond unload distance
            if (dx > unloadDistance || dz > unloadDistance) {
                toUnload.add(pos);
            }
        }
        
        // Unload chunks
        for (ChunkPos pos : toUnload) {
            world.unloadChunk(pos);
            loadedChunkPositions.remove(pos);
            pendingChunkLoads.remove(pos);
            
            // Optional: Log chunk unloads (can be verbose)
            // System.out.println("[ChunkManager] Unloaded chunk: " + pos);
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
    }

    private static final class ChunkRequest {
        private final ChunkPos pos;
        private final int distanceSq;
        private final boolean withinActiveView;

        private ChunkRequest(ChunkPos pos, int distanceSq, boolean withinActiveView) {
            this.pos = pos;
            this.distanceSq = distanceSq;
            this.withinActiveView = withinActiveView;
        }
    }
}
