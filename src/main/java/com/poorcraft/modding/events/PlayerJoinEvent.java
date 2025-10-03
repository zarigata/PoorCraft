package com.poorcraft.modding.events;

/**
 * Event fired when a player successfully joins the server.
 * 
 * <p>This event is fired AFTER the player has successfully logged in and
 * been added to the server. Mods can use this to:
 * <ul>
 *   <li>Send welcome messages</li>
 *   <li>Initialize player data</li>
 *   <li>Spawn NPCs for the player</li>
 *   <li>Track player logins</li>
 *   <li>Apply custom spawn logic</li>
 * </ul>
 * 
 * <p><b>Cancellable:</b> No - the player has already joined.
 * 
 * <p><b>Example usage in Python:</b>
 * <pre>
 * {@literal @}on_player_join
 * def welcome_player(event):
 *     log(f"Welcome {event.username}!")
 * </pre>
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class PlayerJoinEvent extends Event {
    
    private final int playerId;
    private final String username;
    private final float x;
    private final float y;
    private final float z;
    
    /**
     * Creates a new player join event.
     * 
     * @param playerId Unique player ID
     * @param username Player username
     * @param x Spawn X coordinate
     * @param y Spawn Y coordinate
     * @param z Spawn Z coordinate
     */
    public PlayerJoinEvent(int playerId, String username, float x, float y, float z) {
        super();
        this.playerId = playerId;
        this.username = username;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public String getEventName() {
        return "player_join";
    }
    
    /**
     * Gets the player's unique ID.
     * @return Player ID
     */
    public int getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets the player's username.
     * @return Username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Gets the spawn X coordinate.
     * @return X coordinate
     */
    public float getX() {
        return x;
    }
    
    /**
     * Gets the spawn Y coordinate.
     * @return Y coordinate
     */
    public float getY() {
        return y;
    }
    
    /**
     * Gets the spawn Z coordinate.
     * @return Z coordinate
     */
    public float getZ() {
        return z;
    }
}
