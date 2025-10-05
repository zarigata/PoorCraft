package com.poorcraft.modding.events;

/**
 * Event fired when a block is about to be broken.
 * 
 * <p>This event is fired BEFORE the block is actually removed from the world,
 * allowing mods to:
 * <ul>
 *   <li>Cancel the break by calling {@link #cancel()}</li>
 *   <li>Log block destruction</li>
 *   <li>Drop custom items</li>
 *   <li>Protect certain blocks</li>
 * </ul>
 * 
 * <p><b>Cancellable:</b> Yes - calling cancel() will prevent the block from being broken.
 * 
 * <p><b>Example usage in Python:</b>
 * <pre>
 * {@literal @}on_block_break
 * def protect_bedrock(event):
 *     if event.block_type_id == 14:  # Bedrock
 *         event.cancel()
 * </pre>
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class BlockBreakEvent extends Event {
    
    private final int x;
    private final int y;
    private final int z;
    private final int blockTypeId;
    private final int playerId;
    
    /**
     * Creates a new block break event.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate (0-255)
     * @param z Block Z coordinate
     * @param blockTypeId Block type being broken (0-255)
     * @param playerId Player breaking the block, or -1 if not player-initiated
     */
    public BlockBreakEvent(int x, int y, int z, int blockTypeId, int playerId) {
        super();
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockTypeId = blockTypeId;
        this.playerId = playerId;
    }
    
    @Override
    public String getEventName() {
        return "block_break";
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
     * Gets the block type ID being broken.
     * @return Block type ID (0-255)
     */
    public int getBlockTypeId() {
        return blockTypeId;
    }
    
    /**
     * Gets the player ID who is breaking the block.
     * @return Player ID, or -1 if not player-initiated
     */
    public int getPlayerId() {
        return playerId;
    }
}
