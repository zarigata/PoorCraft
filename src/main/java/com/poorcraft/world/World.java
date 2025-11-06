package com.poorcraft.world;

import com.poorcraft.modding.EventBus;
import com.poorcraft.modding.events.BlockBreakEvent;
import com.poorcraft.modding.events.BlockPlaceEvent;
import com.poorcraft.modding.events.WorldLoadEvent;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import com.poorcraft.world.generation.BiomeGenerator;
import com.poorcraft.world.generation.BiomeType;
import com.poorcraft.world.generation.FeatureGenerator;
import com.poorcraft.world.generation.TerrainGenerator;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Main World class that manages chunks and world state.
 * 
 * The World is the container for all chunks and provides world-space block access.
 * Chunks are generated on-demand and must be explicitly unloaded.
 * This is the big boss class that holds everything together.
 */
public class World {
    
    private final long seed;
    private final Map<ChunkPos, Chunk> chunks;
    private final TerrainGenerator terrainGenerator;
    private final FeatureGenerator featureGenerator;
    private final BiomeGenerator biomeGenerator;
    private final boolean generateStructures;
    private Consumer<ChunkPos> chunkUnloadCallback;
    private EventBus eventBus;
    private boolean worldLoadEventFired;  // Track if WorldLoadEvent has been fired
    
    /**
     * Creates a new world with the given seed.
     * If seed is 0, generates a random seed.
     * 
     * @param seed World seed (0 for random)
     * @param generateStructures Whether to generate biome features (trees, cacti, etc.)
     */
    public World(long seed, boolean generateStructures) {
        // Generate random seed if 0
        if (seed == 0) {
            this.seed = new Random().nextLong();
        } else {
            this.seed = seed;
        }
        
        this.generateStructures = generateStructures;
        this.chunks = new ConcurrentHashMap<>();
        this.biomeGenerator = new BiomeGenerator(this.seed);
        this.terrainGenerator = new TerrainGenerator(this.seed);
        this.featureGenerator = new FeatureGenerator(this.seed, biomeGenerator, terrainGenerator);
        this.chunkUnloadCallback = null;
        this.eventBus = null;
        this.worldLoadEventFired = false;
        
        System.out.println("[World] Created world with seed: " + this.seed);
        System.out.println("[World] Structure generation: " + (generateStructures ? "enabled" : "disabled"));
        
        // WorldLoadEvent is now fired in setEventBus() after the bus is assigned
        // Because firing it here when eventBus is null would be like shouting into the void
        // And nobody wants that. Not even the void.
    }
    
    /**
     * Sets a callback to be invoked when chunks are unloaded.
     * Used by the renderer to cleanup GPU resources.
     * 
     * @param callback Callback function that receives the unloaded chunk position
     */
    public void setChunkUnloadCallback(Consumer<ChunkPos> callback) {
        this.chunkUnloadCallback = callback;
    }
    
    /**
     * Sets the event bus for firing mod events.
     * Fires WorldLoadEvent once when the bus is first assigned.
     * 
     * @param eventBus Event bus instance
     */
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
        
        // Fire WorldLoadEvent now that we have a bus to fire it on
        // This ensures mods actually receive the event (unlike before when we fired into the void)
        if (eventBus != null && !worldLoadEventFired) {
            eventBus.fire(new WorldLoadEvent(this.seed, generateStructures));
            worldLoadEventFired = true;
            System.out.println("[World] WorldLoadEvent fired to mods");
        }
        
        // Also set on terrain generator so it can fire chunk generation events
        if (terrainGenerator != null) {
            terrainGenerator.setEventBus(eventBus);
        }
    }
    
    /**
     * Gets a chunk at the specified position.
     * Returns null if chunk is not loaded.
     * 
     * @param pos Chunk position
     * @return Chunk at that position, or null if not loaded
     */
    public Chunk getChunk(ChunkPos pos) {
        return chunks.get(pos);
    }
    
    /**
     * Gets a chunk at the specified coordinates.
     * Convenience overload.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return Chunk at that position, or null if not loaded
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        return getChunk(new ChunkPos(chunkX, chunkZ));
    }
    
    /**
     * Gets an existing chunk or generates a new one if it doesn't exist.
     * This is the main method for chunk loading.
     * 
     * @param pos Chunk position
     * @return Chunk at that position (never null)
     */
    public Chunk getOrCreateChunk(ChunkPos pos) {
        Chunk chunk = chunks.get(pos);
        if (chunk == null) {
            chunk = generateChunk(pos);
            chunks.put(pos, chunk);
        }
        return chunk;
    }
    
    /**
     * Generates a new chunk at the specified position.
     * Fills it with terrain and features, and sets up neighbor references.
     * 
     * @param pos Chunk position
     * @return Generated chunk
     */
    private Chunk generateChunk(ChunkPos pos) {
        // Create new chunk
        Chunk chunk = new Chunk(pos);
        
        // Generate terrain
        terrainGenerator.generateTerrain(chunk);
        
        // Generate features only if enabled
        // I spent way too long debugging why my flat world had trees... this flag is important!
        if (generateStructures) {
            featureGenerator.generateFeatures(chunk);
        }
        
        // Set up neighbor references for face culling
        setupNeighbors(chunk);
        
        return chunk;
    }
    
    /**
     * Sets up neighbor references for a chunk.
     * This enables proper face culling across chunk boundaries.
     * 
     * @param chunk Chunk to set up neighbors for
     */
    private void setupNeighbors(Chunk chunk) {
        ChunkPos pos = chunk.getPosition();
        
        // North neighbor (-Z)
        Chunk north = chunks.get(new ChunkPos(pos.x, pos.z - 1));
        if (north != null) {
            chunk.setNeighbor(0, north);
            north.setNeighbor(1, chunk);  // Set reverse reference
        }
        
        // South neighbor (+Z)
        Chunk south = chunks.get(new ChunkPos(pos.x, pos.z + 1));
        if (south != null) {
            chunk.setNeighbor(1, south);
            south.setNeighbor(0, chunk);  // Set reverse reference
        }
        
        // East neighbor (+X)
        Chunk east = chunks.get(new ChunkPos(pos.x + 1, pos.z));
        if (east != null) {
            chunk.setNeighbor(2, east);
            east.setNeighbor(3, chunk);  // Set reverse reference
        }
        
        // West neighbor (-X)
        Chunk west = chunks.get(new ChunkPos(pos.x - 1, pos.z));
        if (west != null) {
            chunk.setNeighbor(3, west);
            west.setNeighbor(2, chunk);  // Set reverse reference
        }
    }
    
    /**
     * Unloads a chunk at the specified position.
     * Removes it from memory and clears neighbor references.
     * 
     * @param pos Chunk position
     */
    public void unloadChunk(ChunkPos pos) {
        Chunk chunk = chunks.remove(pos);
        if (chunk != null) {
            // Notify callback before clearing references
            if (chunkUnloadCallback != null) {
                chunkUnloadCallback.accept(pos);
            }
            
            // Clear neighbor references in adjacent chunks
            clearNeighborReferences(chunk);
        }
    }
    
    /**
     * Clears neighbor references in adjacent chunks.
     * Called when unloading a chunk.
     * 
     * @param chunk Chunk being unloaded
     */
    private void clearNeighborReferences(Chunk chunk) {
        ChunkPos pos = chunk.getPosition();
        
        // Clear references in neighbors
        Chunk north = chunks.get(new ChunkPos(pos.x, pos.z - 1));
        if (north != null) {
            north.setNeighbor(1, null);
        }
        
        Chunk south = chunks.get(new ChunkPos(pos.x, pos.z + 1));
        if (south != null) {
            south.setNeighbor(0, null);
        }
        
        Chunk east = chunks.get(new ChunkPos(pos.x + 1, pos.z));
        if (east != null) {
            east.setNeighbor(3, null);
        }
        
        Chunk west = chunks.get(new ChunkPos(pos.x - 1, pos.z));
        if (west != null) {
            west.setNeighbor(2, null);
        }
    }
    
    /**
     * Gets all currently loaded chunks.
     * 
     * @return Collection of loaded chunks
     */
    public Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }

    public void cleanup() {
        if (chunkUnloadCallback != null && !chunks.isEmpty()) {
            chunks.keySet().forEach(chunkUnloadCallback);
        }
        chunks.clear();
        chunkUnloadCallback = null;
        eventBus = null;
        worldLoadEventFired = false;
    }
    
    /**
     * Gets the block type at the specified world coordinates.
     * Returns AIR if the chunk is not loaded.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Block type at that position
     */
    public BlockType getBlock(int worldX, int worldY, int worldZ) {
        // Convert to chunk coordinates
        ChunkPos chunkPos = ChunkPos.fromWorldPos(worldX, worldZ);
        Chunk chunk = getChunk(chunkPos);
        
        if (chunk == null) {
            return BlockType.AIR;
        }
        
        // Convert to local coordinates
        int localX = Math.floorMod(worldX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.CHUNK_SIZE);
        
        // Check Y bounds
        if (worldY < 0 || worldY >= Chunk.CHUNK_HEIGHT) {
            return BlockType.AIR;
        }
        
        return chunk.getBlock(localX, worldY, localZ);
    }
    
    /**
     * Sets the block type at the specified world coordinates.
     * Creates the chunk if it doesn't exist.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @param type Block type to set
     */
    public void setBlock(int worldX, int worldY, int worldZ, BlockType type) {
        // Fire mod event for block change (cancellable)
        if (eventBus != null) {
            BlockType oldBlock = getBlock(worldX, worldY, worldZ);
            
            // Determine if placing or breaking
            if (oldBlock == BlockType.AIR && type != BlockType.AIR) {
                // Placing a block
                BlockPlaceEvent event = new BlockPlaceEvent(worldX, worldY, worldZ, type.getId(), -1);
                eventBus.fire(event);
                if (event.isCancelled()) {
                    return;  // Event cancelled, abort block placement
                }
            } else if (oldBlock != BlockType.AIR && type == BlockType.AIR) {
                // Breaking a block
                BlockBreakEvent event = new BlockBreakEvent(worldX, worldY, worldZ, oldBlock.getId(), -1);
                eventBus.fire(event);
                if (event.isCancelled()) {
                    return;  // Event cancelled, abort block break
                }
            }
        }
        
        // Convert to chunk coordinates
        ChunkPos chunkPos = ChunkPos.fromWorldPos(worldX, worldZ);
        Chunk chunk = getOrCreateChunk(chunkPos);
        
        // Convert to local coordinates
        int localX = Math.floorMod(worldX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.CHUNK_SIZE);
        
        // Check Y bounds
        if (worldY < 0 || worldY >= Chunk.CHUNK_HEIGHT) {
            return;
        }
        
        chunk.setBlock(localX, worldY, localZ, type);

        if (isEdgeBlock(localX, localZ)) {
            if (localZ == 0) {
                markNeighborChunkDirty(chunkPos, 0);
            }
            if (localZ == Chunk.CHUNK_SIZE - 1) {
                markNeighborChunkDirty(chunkPos, 1);
            }
            if (localX == Chunk.CHUNK_SIZE - 1) {
                markNeighborChunkDirty(chunkPos, 2);
            }
            if (localX == 0) {
                markNeighborChunkDirty(chunkPos, 3);
            }
        }
    }

    private boolean isEdgeBlock(int localX, int localZ) {
        return localX == 0 || localX == Chunk.CHUNK_SIZE - 1 || localZ == 0 || localZ == Chunk.CHUNK_SIZE - 1;
    }

    private void markNeighborChunkDirty(ChunkPos pos, int direction) {
        ChunkPos neighborPos;
        switch (direction) {
            case 0 -> neighborPos = new ChunkPos(pos.x, pos.z - 1);
            case 1 -> neighborPos = new ChunkPos(pos.x, pos.z + 1);
            case 2 -> neighborPos = new ChunkPos(pos.x + 1, pos.z);
            case 3 -> neighborPos = new ChunkPos(pos.x - 1, pos.z);
            default -> {
                return;
            }
        }

        Chunk neighbor = getChunk(neighborPos);
        if (neighbor != null) {
            neighbor.markMeshDirty();
            System.out.println("[World] Marked neighbor chunk dirty at " + neighborPos);
        }
    }
    
    /**
     * Gets the world seed.
     * 
     * @return World seed
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Gets the biome at the specified world coordinates.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Biome type at that location
     */
    public BiomeType getBiome(int worldX, int worldZ) {
        return biomeGenerator.getBiome(worldX, worldZ);
    }
    
    /**
     * Gets the terrain height at the specified world coordinates.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Terrain height at that location
     */
    public int getHeightAt(int worldX, int worldZ) {
        return terrainGenerator.getHeightAt(worldX, worldZ);
    }
}
