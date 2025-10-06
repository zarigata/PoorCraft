package com.poorcraft.modding;

import com.google.gson.Gson;
import com.poorcraft.core.Game;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main API class exposed to Python mods via Py4J.
 * 
 * <p>This class is the entry point for Python mods to interact with the game.
 * It provides methods for:
 * <ul>
 *   <li>World access (getting/setting blocks, biomes, height)</li>
 *   <li>Event registration (registering Python callbacks)</li>
 *   <li>Utility functions (logging, server check, shared data)</li>
 * </ul>
 * 
 * <p>All methods are designed to be safe when called from Python:
 * <ul>
 *   <li>Return safe defaults (null, -1) instead of throwing exceptions</li>
 *   <li>Handle null checks internally</li>
 *   <li>Log errors for debugging</li>
 * </ul>
 * 
 * <p><b>Python access:</b>
 * <pre>
 * from py4j.java_gateway import JavaGateway
 * gateway = JavaGateway()
 * mod_api = gateway.entry_point  # This ModAPI instance
 * block_id = mod_api.getBlock(100, 64, 200)
 * </pre>
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class ModAPI {
    
    private final Game game;
    private final EventBus eventBus;
    private final Map<String, Object> sharedData;
    private final Map<String, ByteBuffer> proceduralTextures;
    
    /**
     * Creates a new ModAPI instance.
     * 
     * @param game Reference to the game instance
     * @param eventBus Event system for registering callbacks
     */
    public ModAPI(Game game, EventBus eventBus) {
        this.game = game;
        this.eventBus = eventBus;
        this.sharedData = new HashMap<>();
        this.proceduralTextures = new LinkedHashMap<>();
    }
    
    // ========== World Access Methods ==========
    
    /**
     * Gets the block type at the specified world coordinates.
     * 
     * @param x X coordinate
     * @param y Y coordinate (0-255)
     * @param z Z coordinate
     * @return Block type ID (0-255), or -1 if world not loaded or coordinates invalid
     */
    public int getBlock(int x, int y, int z) {
        try {
            World world = game.getWorld();
            if (world == null) {
                return -1;  // World not loaded
            }
            
            BlockType blockType = world.getBlock(x, y, z);
            return blockType != null ? blockType.getId() : 0;  // AIR if null
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error getting block at (" + x + ", " + y + ", " + z + "): " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Sets the block type at the specified world coordinates.
     * 
     * @param x X coordinate
     * @param y Y coordinate (0-255)
     * @param z Z coordinate
     * @param blockTypeId Block type ID to set (0-255)
     */
    public void setBlock(int x, int y, int z, int blockTypeId) {
        try {
            World world = game.getWorld();
            if (world == null) {
                System.err.println("[ModAPI] Cannot set block: world not loaded");
                return;
            }
            
            BlockType blockType = BlockType.fromId(blockTypeId);
            if (blockType == null) {
                System.err.println("[ModAPI] Invalid block type ID: " + blockTypeId);
                return;
            }
            
            world.setBlock(x, y, z, blockType);
            // Debug logging (can be verbose, maybe make configurable later)
            // System.out.println("[ModAPI] Set block at (" + x + ", " + y + ", " + z + ") to " + blockType);
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error setting block at (" + x + ", " + y + ", " + z + "): " + e.getMessage());
        }
    }
    
    /**
     * Gets the biome at the specified coordinates.
     * 
     * @param x X coordinate
     * @param z Z coordinate
     * @return Biome name ("Desert", "Snow", "Jungle", "Plains"), or null if world not loaded
     */
    public String getBiome(int x, int z) {
        try {
            World world = game.getWorld();
            if (world == null) {
                return null;
            }
            
            return world.getBiome(x, z).toString();
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error getting biome at (" + x + ", " + z + "): " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the terrain height at the specified coordinates.
     * 
     * @param x X coordinate
     * @param z Z coordinate
     * @return Y coordinate of the surface, or -1 if world not loaded
     */
    public int getHeightAt(int x, int z) {
        try {
            World world = game.getWorld();
            if (world == null) {
                return -1;
            }
            
            return world.getHeightAt(x, z);
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error getting height at (" + x + ", " + z + "): " + e.getMessage());
            return -1;
        }
    }
    
    // ========== Event Registration Methods ==========
    
    /**
     * Registers a Python callback for the specified event.
     * 
     * <p>The callback should be a Python callable that accepts one argument (the event object).
     * 
     * @param eventName Name of the event (e.g., "block_place", "player_join")
     * @param callback Python callback object (Py4J proxy)
     */
    public void registerEvent(String eventName, Object callback) {
        try {
            eventBus.registerPythonCallback(eventName, callback);
            System.out.println("[ModAPI] Registered Python callback for event: " + eventName);
        } catch (Exception e) {
            System.err.println("[ModAPI] Error registering event callback: " + e.getMessage());
        }
    }
    
    /**
     * Unregisters a Python callback for the specified event.
     * 
     * @param eventName Name of the event
     * @param callback Python callback object to remove
     */
    public void unregisterEvent(String eventName, Object callback) {
        try {
            eventBus.unregisterPythonCallback(eventName, callback);
            System.out.println("[ModAPI] Unregistered Python callback for event: " + eventName);
        } catch (Exception e) {
            System.err.println("[ModAPI] Error unregistering event callback: " + e.getMessage());
        }
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Logs a message to the console with [MOD] prefix.
     * Useful for mod debugging.
     * 
     * @param message Message to log
     */
    public void log(String message) {
        System.out.println("[MOD] " + message);
    }
    
    /**
     * Checks if the game is running on the server side.
     * 
     * <p>In single-player, the game acts as both client and server.
     * In multiplayer, this returns true only on the dedicated server.
     * 
     * @return true if running on server side, false otherwise
     */
    public boolean isServer() {
        // For now, we're always server in single-player (world exists)
        // In multiplayer, check if we're the host/server
        // TODO: Implement proper server/client distinction when multiplayer is added
        return game.getWorld() != null;
    }
    
    /**
     * Stores data in the shared data map.
     * Allows mods to communicate with each other.
     * 
     * @param key Data key
     * @param value Data value (any object)
     */
    public void setSharedData(String key, Object value) {
        sharedData.put(key, value);
    }
    
    /**
     * Retrieves data from the shared data map.
     * 
     * @param key Data key
     * @return Stored value, or null if key doesn't exist
     */
    public Object getSharedData(String key) {
        return sharedData.get(key);
    }
    
    /**
     * Gets the game instance.
     * For internal use by the modding system.
     * 
     * @return Game instance
     */
    public Game getGame() {
        return game;
    }
    
    /**
     * Gets the event bus.
     * For internal use by the modding system.
     * 
     * @return EventBus instance
     */
    public EventBus getEventBus() {
        return eventBus;
    }
    
    /**
     * Imports a Python module by path.
     * This is called from Java to dynamically load Python mod modules.
     * 
     * <p>The Python side should implement this by using importlib or __import__.
     * This method is designed to be called via Py4J callback.
     * 
     * @param modulePath Python module path (e.g., "mods.ai_npc.main")
     * @return Python module object (Py4J proxy), or null if import fails
     */
    public Object importPythonModule(String modulePath) {
        try {
            // This method is a placeholder that will be overridden by Python
            // The actual implementation happens on the Python side via callback
            // We can't directly import Python modules from Java, so we need
            // the Python gateway to provide this functionality
            log("Java cannot directly import Python modules - this should be called from Python side");
            return null;
        } catch (Exception e) {
            System.err.println("[ModAPI] Error importing Python module: " + e.getMessage());
            return null;
        }
    }

    /**
     * Registers a procedurally generated texture provided by a Python mod.
     * Texture data must be a 16x16 image encoded as 1024 RGBA bytes.
     *
     * @param name     Unique texture name used by the renderer
     * @param rgbaData Raw RGBA bytes (length must be 1024)
     */
    public void addProceduralTexture(String name, byte[] rgbaData) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Texture name cannot be null or empty");
        }

        if (rgbaData == null || rgbaData.length != 16 * 16 * 4) {
            throw new IllegalArgumentException("Procedural textures must be 16x16 RGBA (1024 bytes)");
        }

        ByteBuffer buffer = BufferUtils.createByteBuffer(rgbaData.length);
        buffer.put(rgbaData);
        buffer.flip();

        proceduralTextures.put(name, buffer);
        System.out.println("[ModAPI] Registered procedural texture: " + name);

        if (proceduralTextures.size() > 256) {
            System.err.println("[ModAPI] Warning: Procedural texture count exceeds atlas capacity (256)");
        }
    }

    /**
     * Provides read-only access to all registered procedural textures.
     *
     * @return Map of texture names to RGBA byte buffers
     */
    public Map<String, ByteBuffer> getProceduralTextures() {
        return proceduralTextures;
    }

    /**
     * Indicates whether any procedural textures have been registered.
     *
     * @return true if at least one procedural texture exists
     */
    public boolean hasProceduralTextures() {
        return !proceduralTextures.isEmpty();
    }

    /**
     * Gets the number of registered procedural textures.
     *
     * @return Count of textures registered by mods
     */
    public int getProceduralTextureCount() {
        return proceduralTextures.size();
    }

    /**
     * Retrieves a mod's configuration as a JSON string for Python consumption.
     *
     * @param modId Target mod identifier
     * @return JSON string of the configuration, or null if unavailable
     */
    public String getModConfig(String modId) {
        if (modId == null || modId.isEmpty()) {
            return null;
        }

        try {
            LuaModLoader modLoader = game.getModLoader();
            if (modLoader == null) {
                return null;
            }

            LuaModContainer container = modLoader.getModById(modId);
            if (container == null) {
                return null;
            }

            return new Gson().toJson(container.getConfig());
        } catch (Exception e) {
            System.err.println("[ModAPI] Error retrieving config for mod '" + modId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Stores NPC metadata in shared data for mods that manage conversational NPCs.
     * The entity system will pick this up once NPC spawning is implemented in Java.
     *
     * @param npcId       Unique NPC identifier
     * @param name        NPC display name
     * @param x           Spawn X coordinate
     * @param y           Spawn Y coordinate
     * @param z           Spawn Z coordinate
     * @param personality Personality descriptor for AI systems
     */
    public void spawnNPC(int npcId, String name, float x, float y, float z, String personality) {
        Map<String, Object> npcData = new HashMap<>();
        npcData.put("id", npcId);
        npcData.put("name", name);
        npcData.put("position", new float[]{x, y, z});
        npcData.put("personality", personality);

        sharedData.put("npc_" + npcId, npcData);
        System.out.println("[ModAPI] Spawned NPC (#" + npcId + ") " + name + " at (" + x + ", " + y + ", " + z + ")");
    }

    /**
     * Removes NPC metadata from the shared data map.
     *
     * @param npcId Unique NPC identifier
     */
    public void despawnNPC(int npcId) {
        sharedData.remove("npc_" + npcId);
        System.out.println("[ModAPI] Despawned NPC (#" + npcId + ")");
    }

    /**
     * Logs NPC dialogue so mods can provide conversational feedback.
     *
     * @param npcId   Unique NPC identifier
     * @param message Dialogue text spoken by the NPC
     */
    public void npcSay(int npcId, String message) {
        System.out.println("[NPC " + npcId + "] " + message);
    }
}
