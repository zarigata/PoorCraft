package com.poorcraft.world.block;

/**
 * Simple block data class that wraps a BlockType.
 * 
 * Currently just a lightweight wrapper, but designed to be extensible.
 * In the future, this can hold metadata like rotation, damage, NBT data, etc.
 * For now, it's basically just BlockType with extra steps.
 */
public class Block {
    
    private final BlockType type;
    
    /**
     * Creates a new block of the specified type.
     * 
     * @param type The block type
     */
    public Block(BlockType type) {
        this.type = type;
    }
    
    /**
     * Gets the block type.
     * 
     * @return The block type
     */
    public BlockType getType() {
        return type;
    }
    
    /**
     * Checks if this block is air.
     * Convenience method because we check this A LOT.
     * 
     * @return true if this is an air block
     */
    public boolean isAir() {
        return type == BlockType.AIR;
    }
    
    /**
     * Checks if this block is transparent.
     * 
     * @return true if transparent
     */
    public boolean isTransparent() {
        return type.isTransparent();
    }
    
    /**
     * Checks if this block is solid.
     * 
     * @return true if solid
     */
    public boolean isSolid() {
        return type.isSolid();
    }
    
    /**
     * Creates an air block.
     * Static factory method for convenience.
     * 
     * @return A new air block
     */
    public static Block air() {
        return new Block(BlockType.AIR);
    }
    
    /**
     * Creates a block of the specified type.
     * Static factory method for cleaner code.
     * 
     * @param type The block type
     * @return A new block of that type
     */
    public static Block of(BlockType type) {
        return new Block(type);
    }
}
