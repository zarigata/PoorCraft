package com.poorcraft.modding.events;

/**
 * Event fired when a player disconnects from the server.
 * 
 * <p>This event is fired when a player leaves the server, allowing mods to:
 * <ul>
 *   <li>Save player data</li>
 *   <li>Despawn player NPCs</li>
 *   <li>Track player activity</li>
 *   <li>Broadcast leave messages</li>
 *   <li>Cleanup player resources</li>
 * </ul>
 * 
 * <p><b>Cancellable:</b> No - the player has already left.
 * 
 * <p><b>Example usage in Python:</b>
 * <pre>
 * {@literal @}on_player_leave
 * def goodbye_player(event):
 *     log(f"{event.username} left: {event.reason}")
 * </pre>
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class PlayerLeaveEvent extends Event {
    
    private final int playerId;
    private final String username;
    private final String reason;
    
    /**
     * Creates a new player leave event.
     * 
     * @param playerId Player ID
     * @param username Player username
     * @param reason Disconnect reason ("Quit", "Timeout", "Kicked", etc.)
     */
    public PlayerLeaveEvent(int playerId, String username, String reason) {
        super();
        this.playerId = playerId;
        this.username = username;
        this.reason = reason;
    }
    
    @Override
    public String getEventName() {
        return "player_leave";
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
     * Gets the disconnect reason.
     * @return Reason string ("Quit", "Timeout", "Kicked", etc.)
     */
    public String getReason() {
        return reason;
    }
}
