package com.poorcraft.modding;

import com.google.gson.Gson;
import com.poorcraft.core.Game;
import com.poorcraft.modding.events.ChatMessageEvent;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.entity.NPCEntity;
import com.poorcraft.world.entity.NPCManager;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Primary scripting interface exposed to Lua mods (and other supported runtimes).
 *
{{ ... }}
 *   <li>Operate without assuming a specific scripting bridge implementation</li>
 * </ul>
 */
public class ModAPI {
    
    private final Game game;
    private final EventBus eventBus;
    private final Map<String, Object> sharedData;
    private final Map<String, ByteBuffer> proceduralTextures;
    private final List<Consumer<ChatMessageData>> chatListeners;
    private final HttpClient httpClient;
    
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
        this.chatListeners = new ArrayList<>();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
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
    
    /**
     * Gets the player's position in the world.
     * 
     * @return double array [x, y, z] with player position, or null if player not available
     */
    public double[] getPlayerPosition() {
        try {
            if (game == null || game.getPlayerController() == null) {
                return null;
            }
            
            Vector3f position = game.getPlayerPosition();
            if (position == null) {
                return null;
            }
            
            return new double[] { position.x, position.y, position.z };
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error getting player position: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the current game time of day.
     * 
     * @return Time of day (0.0-1.0), or -1.0f if game not available
     */
    public float getGameTime() {
        try {
            if (game == null) {
                return -1.0f;
            }
            
            return game.getTimeOfDay();
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error getting game time: " + e.getMessage());
            return -1.0f;
        }
    }
    
    /**
     * Sets the game time of day.
     * 
     * @param time Time of day (0.0-1.0)
     */
    public void setGameTime(float time) {
        try {
            if (game == null) {
                System.err.println("[ModAPI] Cannot set game time: game not initialized");
                return;
            }
            
            if (time < 0.0f || time > 1.0f) {
                System.err.println("[ModAPI] Invalid game time: " + time + " (must be 0.0-1.0)");
                return;
            }
            
            game.setTimeOfDay(time);
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error setting game time: " + e.getMessage());
        }
    }
    
    /**
     * Gets the current real-world system time.
     * 
     * @return Milliseconds since Unix epoch
     */
    public long getRealTime() {
        return System.currentTimeMillis();
    }
    
    /**
     * Gets the current weather status.
     * 
     * @return Weather status string, currently always "clear" (placeholder)
     */
    public String getWeather() {
        // TODO: Implement weather system
        // For now, always return clear since no weather system exists
        return "clear";
    }

    // ========== Event Registration Methods ==========

    /**
     * Registers a mod callback for the specified event.
     *
     * <p>The callback object is provided by the scripting runtime (Lua function, Java listener, etc.).
     *
     * @param eventName Name of the event (e.g., "block_place", "player_join")
     * @param callbackHandle Callback handle supplied by the mod runtime
     */
    public void registerEvent(String eventName, Object callbackHandle) {
        try {
            eventBus.registerCallback(eventName, callbackHandle);
            System.out.println("[ModAPI] Registered callback for event: " + eventName);
        } catch (Exception e) {
            System.err.println("[ModAPI] Error registering event callback: " + e.getMessage());
        }
    }

    /**
     * Unregisters a mod callback for the specified event.
     *
     * @param eventName Name of the event
     * @param callbackHandle Callback handle to remove
     */
    public void unregisterEvent(String eventName, Object callbackHandle) {
        try {
            eventBus.unregisterCallback(eventName, callbackHandle);
            System.out.println("[ModAPI] Unregistered callback for event: " + eventName);
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
     * @deprecated Legacy entrypoint kept for backward compatibility with the retired Python bridge.
     * Prefer Lua mod registration paths; this method will be removed once old integrations are migrated.
     */
    @Deprecated(forRemoval = true)
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
     * Spawns an NPC in the world.
     *
     * @param npcId       Unique NPC identifier (ignored, auto-generated)
     * @param name        NPC display name
     * @param x           Spawn X coordinate
     * @param y           Spawn Y coordinate
     * @param z           Spawn Z coordinate
     * @param personality Personality descriptor for AI systems
     * @return The actual NPC ID assigned by the manager
     */
    public int spawnNPC(int npcId, String name, float x, float y, float z, String personality) {
        return spawnNPC(npcId, name, x, y, z, personality, "steve");
    }
    
    /**
     * Spawns an NPC in the world with a specific skin.
     *
     * @param npcId       Unique NPC identifier (ignored, auto-generated)
     * @param name        NPC display name
     * @param x           Spawn X coordinate
     * @param y           Spawn Y coordinate
     * @param z           Spawn Z coordinate
     * @param personality Personality descriptor for AI systems
     * @param skinName    Player skin to use for rendering
     * @return The actual NPC ID assigned by the manager
     */
    public int spawnNPC(int npcId, String name, float x, float y, float z, String personality, String skinName) {
        try {
            NPCManager npcManager = game.getNPCManager();
            if (npcManager == null) {
                System.err.println("[ModAPI] Cannot spawn NPC: NPCManager not initialized");
                return -1;
            }
            
            int actualId = npcManager.spawnNPC(name, x, y, z, personality, skinName);
            
            // Store in shared data for backward compatibility
            Map<String, Object> npcData = new HashMap<>();
            npcData.put("id", actualId);
            npcData.put("name", name);
            npcData.put("position", new float[]{x, y, z});
            npcData.put("personality", personality);
            npcData.put("skinName", skinName);
            sharedData.put("npc_" + actualId, npcData);
            
            System.out.println("[ModAPI] Spawned NPC (#" + actualId + ") " + name + " at (" + x + ", " + y + ", " + z + ")");
            return actualId;
        } catch (Exception e) {
            System.err.println("[ModAPI] Error spawning NPC: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Removes an NPC from the world.
     *
     * @param npcId Unique NPC identifier
     */
    public void despawnNPC(int npcId) {
        try {
            NPCManager npcManager = game.getNPCManager();
            if (npcManager != null) {
                npcManager.despawnNPC(npcId);
            }
            sharedData.remove("npc_" + npcId);
            System.out.println("[ModAPI] Despawned NPC (#" + npcId + ")");
        } catch (Exception e) {
            System.err.println("[ModAPI] Error despawning NPC: " + e.getMessage());
        }
    }

    /**
     * Makes an NPC say something in chat.
     * Uses the NPC's name as the sender instead of a generic name.
     *
     * @param npcId   Unique NPC identifier
     * @param message Dialogue text spoken by the NPC
     */
    public void npcSay(int npcId, String message) {
        try {
            NPCManager npcManager = game.getNPCManager();
            if (npcManager != null) {
                NPCEntity npc = npcManager.getNPC(npcId);
                if (npc != null) {
                    // Use NPC name in chat instead of generic sender
                    var uiManager = game.getUIManager();
                    if (uiManager != null) {
                        var chatOverlay = uiManager.getChatOverlay();
                        if (chatOverlay != null) {
                            chatOverlay.enqueueMessage(npcId, npc.getName(), message, System.currentTimeMillis(), false);
                        }
                    }
                    System.out.println("[NPC " + npc.getName() + "] " + message);
                    return;
                }
            }
            System.out.println("[NPC " + npcId + "] " + message);
        } catch (Exception e) {
            System.err.println("[ModAPI] Error in npcSay: " + e.getMessage());
        }
    }
    
    /**
     * Sets the follow distance for an NPC.
     *
     * @param npcId NPC identifier
     * @param distance Follow distance in blocks
     */
    public void setNPCFollowDistance(int npcId, float distance) {
        try {
            NPCManager npcManager = game.getNPCManager();
            if (npcManager != null) {
                NPCEntity npc = npcManager.getNPC(npcId);
                if (npc != null) {
                    npc.setFollowDistance(distance);
                    System.out.println("[ModAPI] Set NPC #" + npcId + " follow distance to " + distance);
                } else {
                    System.err.println("[ModAPI] NPC #" + npcId + " not found");
                }
            }
        } catch (Exception e) {
            System.err.println("[ModAPI] Error setting NPC follow distance: " + e.getMessage());
        }
    }

    public boolean npcGatherResource(int npcId, String resourceType, int quantity) {
        try {
            NPCManager npcManager = game.getNPCManager();
            if (npcManager == null) {
                System.err.println("[ModAPI] Cannot gather resource: NPC manager unavailable");
                return false;
            }

            NPCEntity npc = npcManager.getNPC(npcId);
            if (npc == null) {
                System.err.println("[ModAPI] Cannot gather resource: NPC #" + npcId + " not found");
                return false;
            }

            int searchRadius = 16;
            if (game != null && game.getSettings() != null && game.getSettings().ai != null
                && game.getSettings().ai.maxGatherDistance > 0) {
                searchRadius = game.getSettings().ai.maxGatherDistance;
            }

            int safeQuantity = quantity > 0 ? quantity : 1;
            npc.setCurrentTask(new NPCEntity.GatherTask(resourceType, safeQuantity, searchRadius));
            return true;
        } catch (Exception e) {
            System.err.println("[ModAPI] Error gathering resource for NPC #" + npcId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean npcMoveToPosition(int npcId, float x, float y, float z) {
        try {
            NPCManager npcManager = game.getNPCManager();
            if (npcManager == null) {
                System.err.println("[ModAPI] Cannot move NPC: NPC manager unavailable");
                return false;
            }

            NPCEntity npc = npcManager.getNPC(npcId);
            if (npc == null) {
                System.err.println("[ModAPI] Cannot move NPC: NPC #" + npcId + " not found");
                return false;
            }

            final Vector3f destination = new Vector3f(x, y, z);
            npc.setCurrentTask(new NPCEntity.NPCTask(NPCEntity.TaskType.MOVE_TO) {
                private final Vector3f targetPosition = new Vector3f(destination);
                private boolean reached;

                @Override
                public boolean execute(com.poorcraft.world.World world, NPCEntity entity, float deltaTime) {
                    entity.setTargetPosition(targetPosition);
                    float distance = entity.getPosition().distance(targetPosition);
                    if (distance <= 1.2f) {
                        reached = true;
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean isComplete() {
                    return reached;
                }
            });
            return true;
        } catch (Exception e) {
            System.err.println("[ModAPI] Error moving NPC #" + npcId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean npcBreakBlock(int npcId, int x, int y, int z) {
        try {
            NPCManager npcManager = game.getNPCManager();
            if (npcManager == null) {
                System.err.println("[ModAPI] Cannot break block: NPC manager unavailable");
                return false;
            }

            NPCEntity npc = npcManager.getNPC(npcId);
            if (npc == null) {
                System.err.println("[ModAPI] Cannot break block: NPC #" + npcId + " not found");
                return false;
            }

            var world = game.getWorld();
            if (world == null) {
                System.err.println("[ModAPI] Cannot break block: world not loaded");
                return false;
            }

            return npc.breakBlockAt(world, x, y, z);
        } catch (Exception e) {
            System.err.println("[ModAPI] Error breaking block for NPC #" + npcId + ": " + e.getMessage());
            return false;
        }
    }

    public double[] getNPCPosition(int npcId) {
        try {
            NPCManager npcManager = game.getNPCManager();
            if (npcManager == null) {
                System.err.println("[ModAPI] Cannot get NPC position: NPC manager unavailable");
                return null;
            }

            NPCEntity npc = npcManager.getNPC(npcId);
            if (npc == null) {
                System.err.println("[ModAPI] Cannot get NPC position: NPC #" + npcId + " not found");
                return null;
            }

            Vector3f pos = npc.getPosition();
            return new double[] {pos.x, pos.y, pos.z};
        } catch (Exception e) {
            System.err.println("[ModAPI] Error getting position for NPC #" + npcId + ": " + e.getMessage());
            return null;
        }
    }

    public int[] findNearestBlock(int x, int y, int z, int blockTypeId, int searchRadius) {
        try {
            var world = game.getWorld();
            if (world == null) {
                System.err.println("[ModAPI] Cannot find block: world not loaded");
                return null;
            }

            BlockType targetType = BlockType.fromId(blockTypeId);
            if (targetType == null) {
                System.err.println("[ModAPI] Cannot find block: invalid block type " + blockTypeId);
                return null;
            }

            double bestDistance = Double.MAX_VALUE;
            int[] best = null;

            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                    for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                        int checkX = x + dx;
                        int checkY = y + dy;
                        int checkZ = z + dz;

                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (distance > searchRadius || distance >= bestDistance) {
                            continue;
                        }

                        if (world.getBlock(checkX, checkY, checkZ) == targetType) {
                            bestDistance = distance;
                            best = new int[] {checkX, checkY, checkZ};
                        }
                    }
                }
            }

            return best;
        } catch (Exception e) {
            System.err.println("[ModAPI] Error finding nearest block: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Sets whether external time control is enabled.
     * When enabled, the game will not advance time automatically,
     * allowing mods to manage time progression.
     * 
     * @param enabled true to disable automatic time progression
     */
    public void setTimeControlEnabled(boolean enabled) {
        try {
            if (game == null) {
                System.err.println("[ModAPI] Cannot set time control: game not initialized");
                return;
            }
            
            game.setExternalTimeControlEnabled(enabled);
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error setting time control: " + e.getMessage());
        }
    }
    
    /**
     * Gets a mod's configuration as a Map for easy access.
     * Converts the JSON configuration to a Java Map structure.
     * 
     * @param modId Target mod identifier
     * @return Map containing configuration, or empty map if unavailable
     */
    public Map<String, Object> getModConfigTable(String modId) {
        if (modId == null || modId.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            LuaModLoader modLoader = game.getModLoader();
            if (modLoader == null) {
                return new HashMap<>();
            }
            
            LuaModContainer container = modLoader.getModById(modId);
            if (container == null) {
                return new HashMap<>();
            }
            
            return jsonObjectToMap(container.getConfig());
        } catch (Exception e) {
            System.err.println("[ModAPI] Error retrieving config table for mod '" + modId + "': " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Recursively converts a JsonObject to a Map.
     */
    private Map<String, Object> jsonObjectToMap(com.google.gson.JsonObject json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null) {
            return map;
        }
        
        for (String key : json.keySet()) {
            map.put(key, jsonElementToJava(json.get(key)));
        }
        return map;
    }
    
    /**
     * Converts a JsonElement to a Java object.
     */
    private Object jsonElementToJava(com.google.gson.JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                // Return as double for simplicity (Lua handles all numbers as doubles)
                return primitive.getAsDouble();
            } else if (primitive.isString()) {
                return primitive.getAsString();
            }
        } else if (element.isJsonObject()) {
            return jsonObjectToMap(element.getAsJsonObject());
        } else if (element.isJsonArray()) {
            com.google.gson.JsonArray array = element.getAsJsonArray();
            Object[] result = new Object[array.size()];
            for (int i = 0; i < array.size(); i++) {
                result[i] = jsonElementToJava(array.get(i));
            }
            return result;
        }
        return null;
    }
    
    // ========== Chat API Methods ==========
    
    /**
     * Sends a chat message.
     * In single-player, adds to local chat.
     * In multiplayer, sends to server for broadcasting.
     * 
     * @param message Message to send
     */
    public void sendChatMessage(String message) {
        try {
            if (game == null) {
                System.err.println("[ModAPI] Cannot send chat message: game not initialized");
                return;
            }
            
            // Trim message to max length for safety
            if (message != null && message.length() > 256) {
                message = message.substring(0, 256);
            }
            
            // Get UIManager
            var uiManager = game.getUIManager();
            if (uiManager == null) {
                System.err.println("[ModAPI] Cannot send chat message: UIManager not available");
                return;
            }
            
            // Check if in multiplayer mode
            if (game.isMultiplayerMode() && uiManager.getGameClient() != null && uiManager.getGameClient().isConnected()) {
                // Send to server for broadcasting
                uiManager.getGameClient().sendChatMessage(message);
            } else {
                // Single-player: add locally
                var chatOverlay = uiManager.getChatOverlay();
                if (chatOverlay != null) {
                    chatOverlay.enqueueMessage(-1, "AI Companion", message, System.currentTimeMillis(), false);
                }
            }
        } catch (Exception e) {
            System.err.println("[ModAPI] Error sending chat message: " + e.getMessage());
        }
    }

    /**
     * Registers a callback to be invoked when chat messages are received.
     * 
     * @param listener Callback function
     */
    public void registerChatListener(Consumer<ChatMessageData> listener) {
        chatListeners.add(listener);
        System.out.println("[ModAPI] Registered chat listener");
    }
    
    /**
     * Fires a chat message event to all registered listeners.
     * Called internally when chat messages are received.
     * 
     * @param senderId Sender player ID
     * @param senderName Sender name
     * @param message Message content
     * @param timestamp Message timestamp
     * @param isSystemMessage Whether this is a system message
     */
    public void fireChatMessage(int senderId, String senderName, String message, long timestamp, boolean isSystemMessage) {
        ChatMessageData data = new ChatMessageData(senderId, senderName, message, timestamp, isSystemMessage);
        
        for (Consumer<ChatMessageData> listener : chatListeners) {
            try {
                listener.accept(data);
            } catch (Exception e) {
                System.err.println("[ModAPI] Error in chat listener: " + e.getMessage());
            }
        }
        
        // Also fire through EventBus
        if (eventBus != null) {
            eventBus.fire(new ChatMessageEvent(senderId, senderName, message, timestamp, isSystemMessage));
        }
    }
    
    /**
     * Gets the biome at the player's current position.
     * 
     * @return Biome name, or null if not available
     */
    public String getCurrentBiome() {
        try {
            if (game == null || game.getWorld() == null || game.getPlayerPosition() == null) {
                return null;
            }
            
            Vector3f pos = game.getPlayerPosition();
            return game.getWorld().getBiome((int)pos.x, (int)pos.z).toString();
        } catch (Exception e) {
            System.err.println("[ModAPI] Error getting current biome: " + e.getMessage());
            return null;
        }
    }
    
    // ========== HTTP API Methods ==========
    
    /**
     * Makes an asynchronous HTTP request.
     * The callback will be invoked on the main game thread when the response is received.
     * 
     * @param url URL to request
     * @param method HTTP method ("GET" or "POST")
     * @param jsonBody JSON body for POST requests (can be null for GET)
     * @param callback Callback function to receive response body (null on error)
     */
    public void makeHttpRequest(String url, String method, String jsonBody, Consumer<String> callback) {
        makeHttpRequest(url, method, jsonBody, null, callback);
    }
    
    /**
     * Makes an asynchronous HTTP request with custom headers.
     * The callback will be invoked on the main game thread when the response is received.
     * 
     * @param url URL to request
     * @param method HTTP method ("GET" or "POST")
     * @param jsonBody JSON body for POST requests (can be null for GET)
     * @param headers Custom HTTP headers (can be null)
     * @param callback Callback function to receive response body (null on error)
     */
    public void makeHttpRequest(String url, String method, String jsonBody, Map<String, String> headers, Consumer<String> callback) {
        if (url == null || method == null || callback == null) {
            System.err.println("[ModAPI] Invalid HTTP request parameters");
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));
            
            // Add custom headers
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.header(entry.getKey(), entry.getValue());
                }
            }
            
            if ("POST".equalsIgnoreCase(method) && jsonBody != null) {
                requestBuilder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                requestBuilder.GET();
            }
            
            HttpRequest request = requestBuilder.build();
            
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    // Post callback to main thread for thread safety
                    game.postToMainThread(() -> callback.accept(responseBody));
                })
                .exceptionally(throwable -> {
                    System.err.println("[ModAPI] HTTP request failed: " + throwable.getMessage());
                    game.postToMainThread(() -> callback.accept(null));
                    return null;
                });
                
        } catch (Exception e) {
            System.err.println("[ModAPI] Error making HTTP request: " + e.getMessage());
            callback.accept(null);
        }
    }
    
    /**
     * Makes an asynchronous HTTP POST request with JSON body from a Map.
     * Safely encodes the body using Gson to prevent injection issues.
     * The callback will be invoked on the main game thread when the response is received.
     * 
     * @param url URL to request
     * @param body Map to encode as JSON body
     * @param headers Custom HTTP headers (can be null)
     * @param callback Callback function to receive response body (null on error)
     */
    public void httpPostJson(String url, Map<String, Object> body, Map<String, String> headers, Consumer<String> callback) {
        if (body == null) {
            System.err.println("[ModAPI] httpPostJson requires a non-null body");
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        
        try {
            // Use Gson to safely encode JSON
            String jsonBody = new Gson().toJson(body);
            makeHttpRequest(url, "POST", jsonBody, headers, callback);
        } catch (Exception e) {
            System.err.println("[ModAPI] Error encoding JSON body: " + e.getMessage());
            if (callback != null) {
                callback.accept(null);
            }
        }
    }
    
    /**
     * Parses a JSON string into a Map for Lua consumption.
     * 
     * @param json JSON string to parse
     * @return Map representation of JSON, or empty map on error
     */
    public Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = new Gson().fromJson(json, Map.class);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            System.err.println("[ModAPI] Error parsing JSON: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Makes a synchronous HTTP request.
     * WARNING: This blocks the game thread until the request completes!
     * 
     * @param url URL to request
     * @param method HTTP method ("GET" or "POST")
     * @param jsonBody JSON body for POST requests (can be null for GET)
     * @return Response body, or null on error
     */
    public String makeHttpRequestSync(String url, String method, String jsonBody) {
        if (url == null || method == null) {
            System.err.println("[ModAPI] Invalid HTTP request parameters");
            return null;
        }
        
        System.out.println("[ModAPI] WARNING: Synchronous HTTP request will block the game thread!");
        
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));
            
            if ("POST".equalsIgnoreCase(method) && jsonBody != null) {
                requestBuilder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                requestBuilder.GET();
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return response.body();
            
        } catch (Exception e) {
            System.err.println("[ModAPI] Error making synchronous HTTP request: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Data structure for chat messages passed to listeners.
     */
    public static class ChatMessageData {
        public final int senderId;
        public final String senderName;
        public final String message;
        public final long timestamp;
        public final boolean isSystemMessage;
        
        public ChatMessageData(int senderId, String senderName, String message, long timestamp, boolean isSystemMessage) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message;
            this.timestamp = timestamp;
            this.isSystemMessage = isSystemMessage;
        }
    }
}
