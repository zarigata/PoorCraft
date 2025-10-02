package com.poorcraft.world.chunk;

import org.joml.Vector3f;

/**
 * Immutable class representing chunk coordinates.
 * 
 * Chunks are 16x256x16 blocks, so chunk coordinates are world coordinates divided by 16.
 * This is used as a HashMap key, so equals() and hashCode() are critical.
 * I learned this the hard way when chunks kept duplicating... good times.
 */
public class ChunkPos {
    
    public final int x;
    public final int z;
    
    /**
     * Creates a new chunk position.
     * 
     * @param x Chunk X coordinate
     * @param z Chunk Z coordinate
     */
    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    /**
     * Converts world coordinates to chunk coordinates.
     * Uses floor division to handle negative coordinates correctly.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Chunk position containing those world coordinates
     */
    public static ChunkPos fromWorldPos(float worldX, float worldZ) {
        // Math.floorDiv handles negative numbers correctly
        // Regular division would give wrong results for negative coords
        return new ChunkPos(
            Math.floorDiv((int) worldX, 16),
            Math.floorDiv((int) worldZ, 16)
        );
    }
    
    /**
     * Converts a 3D world position to chunk coordinates.
     * Convenience overload that takes a Vector3f.
     * 
     * @param worldPos World position vector
     * @return Chunk position containing that world position
     */
    public static ChunkPos fromWorldPos(Vector3f worldPos) {
        return fromWorldPos(worldPos.x, worldPos.z);
    }
    
    /**
     * Checks equality based on x and z coordinates.
     * Required for HashMap key usage.
     * 
     * @param obj Object to compare
     * @return true if same chunk position
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChunkPos other = (ChunkPos) obj;
        return x == other.x && z == other.z;
    }
    
    /**
     * Generates hash code based on x and z coordinates.
     * Required for HashMap key usage.
     * 
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return 31 * x + z;
    }
    
    /**
     * Returns a string representation of this chunk position.
     * Useful for debugging and logging.
     * 
     * @return String like "ChunkPos[x, z]"
     */
    @Override
    public String toString() {
        return "ChunkPos[" + x + ", " + z + "]";
    }
}
