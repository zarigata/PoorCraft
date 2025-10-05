package com.poorcraft.modding.events;

/**
 * Event fired when a world is created or loaded.
 * 
 * <p>This event is fired when a new world is created or an existing world is loaded.
 * Mods can use this to:
 * <ul>
 *   <li>Initialize world-specific data</li>
 *   <li>Log world information</li>
 *   <li>Set up custom world features</li>
 *   <li>Prepare mod state for the new world</li>
 * </ul>
 * 
 * <p><b>Cancellable:</b> No - the world has already been created/loaded.
 * 
 * <p><b>Example usage in Python:</b>
 * <pre>
 * {@literal @}on_world_load
 * def init_world(event):
 *     log(f"World loaded with seed: {event.seed}")
 * </pre>
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class WorldLoadEvent extends Event {
    
    private final long seed;
    private final boolean generateStructures;
    
    /**
     * Creates a new world load event.
     * 
     * @param seed World seed
     * @param generateStructures Whether structures (trees, etc.) are enabled
     */
    public WorldLoadEvent(long seed, boolean generateStructures) {
        super();
        this.seed = seed;
        this.generateStructures = generateStructures;
    }
    
    @Override
    public String getEventName() {
        return "world_load";
    }
    
    /**
     * Gets the world seed.
     * @return World seed
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Checks if structure generation is enabled.
     * @return true if structures are enabled, false otherwise
     */
    public boolean isGenerateStructures() {
        return generateStructures;
    }
}
