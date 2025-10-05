package com.poorcraft.modding.events;

import com.poorcraft.world.chunk.Chunk;

/**
 * Event fired when a chunk is generated.
 * 
 * <p>This event is fired AFTER terrain generation but BEFORE features (trees, cacti, etc.)
 * are added. This allows mods to:
 * <ul>
 *   <li>Modify terrain (add custom ores, structures, etc.)</li>
 *   <li>Access the chunk and modify blocks directly</li>
 *   <li>Implement custom world generation features</li>
 *   <li>Add dungeons, caves, or other structures</li>
 * </ul>
 * 
 * <p><b>Cancellable:</b> No - the chunk has already been generated.
 * 
 * <p><b>Example usage in Python:</b>
 * <pre>
 * {@literal @}on_chunk_generate
 * def add_ore(event):
 *     chunk = event.chunk
 *     # Modify chunk blocks here
 * </pre>
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class ChunkGenerateEvent extends Event {
    
    private final int chunkX;
    private final int chunkZ;
    private final Chunk chunk;
    
    /**
     * Creates a new chunk generate event.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @param chunk The generated chunk (can be modified)
     */
    public ChunkGenerateEvent(int chunkX, int chunkZ, Chunk chunk) {
        super();
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunk = chunk;
    }
    
    @Override
    public String getEventName() {
        return "chunk_generate";
    }
    
    /**
     * Gets the chunk X coordinate.
     * @return Chunk X coordinate
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Gets the chunk Z coordinate.
     * @return Chunk Z coordinate
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Gets the chunk object.
     * Mods can modify blocks in this chunk.
     * 
     * @return The generated chunk
     */
    public Chunk getChunk() {
        return chunk;
    }
}
