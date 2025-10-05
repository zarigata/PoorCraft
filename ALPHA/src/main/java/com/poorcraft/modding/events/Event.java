package com.poorcraft.modding.events;

/**
 * Base class for all game events.
 * 
 * <p>Events are fired by the game when certain actions occur (block placement,
 * player join, chunk generation, etc.). Python mods can register callbacks to
 * handle these events.
 * 
 * <p>Some events are cancellable, meaning mods can prevent the action from
 * occurring by calling {@link #setCancelled(boolean)}. For example, a mod can
 * prevent a block from being placed by cancelling the BlockPlaceEvent.
 * 
 * <p>All events have a timestamp indicating when they were created, useful for
 * event ordering and debugging.
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public abstract class Event {
    
    private boolean cancelled;
    private final long timestamp;
    
    /**
     * Creates a new event with the current timestamp.
     */
    public Event() {
        this.cancelled = false;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Checks if this event has been cancelled.
     * 
     * @return true if event is cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Sets the cancelled state of this event.
     * 
     * <p>Only cancellable events should allow this. Non-cancellable events
     * will ignore this call (though it won't throw an exception).
     * 
     * @param cancelled true to cancel the event, false otherwise
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * Cancels this event.
     * Convenience method for setCancelled(true).
     */
    public void cancel() {
        setCancelled(true);
    }
    
    /**
     * Gets the timestamp when this event was created.
     * 
     * @return Timestamp in milliseconds (System.currentTimeMillis())
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the name of this event.
     * Used for routing to Python callbacks.
     * 
     * @return Event name (e.g., "block_place", "player_join")
     */
    public abstract String getEventName();
}
