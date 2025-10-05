package com.poorcraft.modding.events;

/**
 * Event fired when a block is about to be placed.
 * 
 * <p>This event is fired BEFORE the block is actually placed in the world,
 * allowing mods to:
 * <ul>
 *   <li>Cancel the placement by calling {@link #cancel()}</li>
 *   <li>Log block placements</li>
 *   <li>Trigger custom effects</li>
 *   <li>Track who placed the block</li>
 * </ul>
 * 
 * <p><b>Cancellable:</b> Yes - calling cancel() will prevent the block from being placed.
 * 
 * <p><b>Example usage in Python:</b>
 * <pre>
 * {@literal @}on_block_place
 * def prevent_bedrock(event):
 *     if event.block_type_id == 14:  # Bedrock
 *         event.cancel()
 * </pre>
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class BlockPlaceEvent extends Event {
    
    private final int x;
    private final int y;
    private final int z;
    private final int blockTypeId;
    private final int playerId;
    
    /**
     * Creates a new block place event.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate (0-255)
     * @param z Block Z coordinate
     * @param blockTypeId Block type being placed (0-255)
     * @param playerId Player placing the block, or -1 if not player-initiated
     */
    public BlockPlaceEvent(int x, int y, int z, int blockTypeId, int playerId) {
        super();
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockTypeId = blockTypeId;
        this.playerId = playerId;
    }
    
    @Override
    public String getEventName() {
        return "block_place";
    }
    
    /**
     * Gets the X coordinate of the block.
     * @return X coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Gets the Y coordinate of the block.
     * @return Y coordinate (0-255)
     */
    public int getY() {
        return y;
    }
    
    /**
     * Gets the Z coordinate of the block.
     * @return Z coordinate
     */
    public int getZ() {
        return z;
    }
    
    /**
     * Gets the block type ID being placed.
     * @return Block type ID (0-255)
     */
    public int getBlockTypeId() {
        return blockTypeId;
    }
    
    /**
     * Gets the player ID who is placing the block.
     * @return Player ID, or -1 if not player-initiated (e.g., world generation)
     */
    public int getPlayerId() {
        return playerId;
    }
}
