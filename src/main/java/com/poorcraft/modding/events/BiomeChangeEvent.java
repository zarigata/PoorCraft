package com.poorcraft.modding.events;

/**
 * Event fired when a player moves from one biome to another.
 * 
 * <p>This event is fired when the game detects the player has entered a different biome,
 * allowing mods to:
 * <ul>
 *   <li>Display biome-specific messages</li>
 *   <li>Trigger biome-specific effects</li>
 *   <li>Track player exploration</li>
 *   <li>Adjust gameplay based on biome</li>
 * </ul>
 * 
 * <p><b>Cancellable:</b> No - this is an informational event.
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class BiomeChangeEvent extends Event {
    
    private final int playerId;
    private final String previousBiome;
    private final String newBiome;
    private final int worldX;
    private final int worldZ;
    
    /**
     * Creates a new biome change event.
     * 
     * @param playerId Player ID who changed biomes
     * @param previousBiome Previous biome name
     * @param newBiome New biome name
     * @param worldX World X coordinate where change occurred
     * @param worldZ World Z coordinate where change occurred
     */
    public BiomeChangeEvent(int playerId, String previousBiome, String newBiome, int worldX, int worldZ) {
        super();
        this.playerId = playerId;
        this.previousBiome = previousBiome;
        this.newBiome = newBiome;
        this.worldX = worldX;
        this.worldZ = worldZ;
    }
    
    @Override
    public String getEventName() {
        return "biome_change";
    }
    
    /**
     * Gets the player ID who changed biomes.
     * @return Player ID
     */
    public int getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets the previous biome name.
     * @return Previous biome name
     */
    public String getPreviousBiome() {
        return previousBiome;
    }
    
    /**
     * Gets the new biome name.
     * @return New biome name
     */
    public String getNewBiome() {
        return newBiome;
    }
    
    /**
     * Gets the world X coordinate where the change occurred.
     * @return World X coordinate
     */
    public int getWorldX() {
        return worldX;
    }
    
    /**
     * Gets the world Z coordinate where the change occurred.
     * @return World Z coordinate
     */
    public int getWorldZ() {
        return worldZ;
    }
}
