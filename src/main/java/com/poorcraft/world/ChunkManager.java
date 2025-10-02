package com.poorcraft.world;

import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private ChunkPos lastPlayerChunk;
    private final Set<ChunkPos> loadedChunkPositions;
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
        this.loadedChunkPositions = new HashSet<>();
        this.chunkLoadExecutor = Executors.newSingleThreadExecutor();  // Single-threaded for simplicity
        this.lastPlayerChunk = null;
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
        for (int x = -loadDistance; x <= loadDistance; x++) {
            for (int z = -loadDistance; z <= loadDistance; z++) {
                ChunkPos pos = new ChunkPos(centerChunk.x + x, centerChunk.z + z);
                
                // Skip if already loaded
                if (loadedChunkPositions.contains(pos)) {
                    continue;
                }
                
                // Load chunk (synchronous for now - async can be added later)
                world.getOrCreateChunk(pos);
                loadedChunkPositions.add(pos);
                
                // Optional: Log chunk loads (can be verbose)
                // System.out.println("[ChunkManager] Loaded chunk: " + pos);
            }
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
     * 
     * @param distance Unload distance in chunks
     */
    public void setUnloadDistance(int distance) {
        this.unloadDistance = distance;
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
}
