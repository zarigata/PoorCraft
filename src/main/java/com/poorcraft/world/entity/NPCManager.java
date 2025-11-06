package com.poorcraft.world.entity;

import com.poorcraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages active NPCs in the world.
 */
public class NPCManager {

    private final List<NPCEntity> npcs = new ArrayList<>();
    private final Map<Integer, NPCEntity> npcById = new HashMap<>();
    private final Object npcsLock = new Object();
    private int nextNpcId = 1;

    /**
     * Spawns a new NPC in the world.
     * 
     * @param name NPC display name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param personality AI personality descriptor
     * @param skinName Player skin to use for rendering
     * @return The unique NPC ID
     */
    public int spawnNPC(String name, float x, float y, float z, String personality, String skinName) {
        synchronized (npcsLock) {
            int npcId = nextNpcId++;
            NPCEntity npc = new NPCEntity(npcId, name, x, y, z, personality, skinName);
            npcs.add(npc);
            npcById.put(npcId, npc);
            System.out.println("[NPCManager] Spawned NPC #" + npcId + " (" + name + ") at (" + x + ", " + y + ", " + z + ")");
            return npcId;
        }
    }

    /**
     * Removes an NPC from the world.
     * 
     * @param npcId The NPC ID to remove
     */
    public void despawnNPC(int npcId) {
        synchronized (npcsLock) {
            NPCEntity npc = npcById.remove(npcId);
            if (npc != null) {
                npcs.remove(npc);
                System.out.println("[NPCManager] Despawned NPC #" + npcId + " (" + npc.getName() + ")");
            }
        }
    }

    /**
     * Gets an NPC by its ID.
     * 
     * @param npcId The NPC ID
     * @return The NPC entity, or null if not found
     */
    public NPCEntity getNPC(int npcId) {
        synchronized (npcsLock) {
            return npcById.get(npcId);
        }
    }

    /**
     * Gets all active NPCs.
     * 
     * @return Unmodifiable list of all NPCs
     */
    public List<NPCEntity> getAllNPCs() {
        synchronized (npcsLock) {
            return Collections.unmodifiableList(new ArrayList<>(npcs));
        }
    }

    /**
     * Updates all NPCs.
     * 
     * @param world The world instance
     * @param playerPosition The player's position
     * @param deltaTime Time since last update in seconds
     */
    public void update(World world, Vector3f playerPosition, float deltaTime) {
        synchronized (npcsLock) {
            for (NPCEntity npc : npcs) {
                try {
                    npc.update(world, playerPosition, deltaTime);
                } catch (Exception e) {
                    System.err.println("[NPCManager] Error updating NPC #" + npc.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Removes all NPCs from the world.
     */
    public void clear() {
        synchronized (npcsLock) {
            int count = npcs.size();
            npcs.clear();
            npcById.clear();
            if (count > 0) {
                System.out.println("[NPCManager] Cleared " + count + " NPC(s)");
            }
        }
    }

    /**
     * Gets the number of active NPCs.
     * 
     * @return NPC count
     */
    public int getNPCCount() {
        synchronized (npcsLock) {
            return npcs.size();
        }
    }
}
