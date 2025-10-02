package com.poorcraft.world.block;

/**
 * Enum defining all block types in the game.
 * 
 * Each block has an ID, transparency flag, and solidity flag.
 * This is basically the heart of the voxel system. No blocks = no game.
 * Like Minecraft but poorer. Hence the name.
 */
public enum BlockType {
    
    // Air - the most important block (it's literally nothing but we need it)
    AIR(0, true, false),
    
    // Basic terrain blocks
    DIRT(1, false, true),
    STONE(2, false, true),
    
    // Plains biome blocks
    GRASS(3, false, true),
    
    // Desert biome blocks
    SAND(4, false, true),
    SANDSTONE(5, false, true),
    
    // Snow biome blocks
    SNOW_BLOCK(6, false, true),
    ICE(7, true, true),  // Semi-transparent like in Minecraft
    
    // Jungle biome blocks
    JUNGLE_GRASS(8, false, true),
    JUNGLE_DIRT(9, false, true),
    
    // Tree blocks (for all biomes)
    WOOD(10, false, true),
    LEAVES(11, true, true),  // Semi-transparent
    
    // Desert features
    CACTUS(12, false, true),
    
    // Snow features
    SNOW_LAYER(13, true, false),  // Decorative, non-solid
    
    // Bedrock - the unbreakable bottom layer
    // Because falling into the void is a feature, not a bug
    BEDROCK(14, false, true);
    
    private final int id;
    private final boolean transparent;
    private final boolean solid;
    
    /**
     * Constructor for BlockType enum.
     * 
     * @param id Unique numeric ID for this block type
     * @param transparent Whether light passes through (affects face culling)
     * @param solid Whether player collides with it (for future physics)
     */
    BlockType(int id, boolean transparent, boolean solid) {
        this.id = id;
        this.transparent = transparent;
        this.solid = solid;
    }
    
    /**
     * Gets the unique ID of this block type.
     * 
     * @return Block ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Checks if this block is transparent.
     * Transparent blocks don't cull adjacent faces.
     * 
     * @return true if transparent
     */
    public boolean isTransparent() {
        return transparent;
    }
    
    /**
     * Checks if this block is solid.
     * Solid blocks have collision (for future physics implementation).
     * 
     * @return true if solid
     */
    public boolean isSolid() {
        return solid;
    }
    
    /**
     * Looks up a BlockType by its numeric ID.
     * 
     * @param id The block ID to look up
     * @return The BlockType with that ID, or AIR if not found
     */
    public static BlockType fromId(int id) {
        for (BlockType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        // Default to AIR if ID is invalid
        // Better than crashing when someone mods in a weird block ID
        return AIR;
    }
}
