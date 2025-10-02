package com.poorcraft.world.generation;

import com.poorcraft.world.block.BlockType;

/**
 * Enum defining the 4 biome types in PoorCraft.
 * 
 * Each biome has unique terrain characteristics and block types.
 * Desert = hot and dry, Snow = cold, Jungle = hot and wet, Plains = temperate.
 * Like Minecraft biomes but with only 4 because we're poor.
 */
public enum BiomeType {
    
    /**
     * Desert biome - hot, dry, sandy terrain with cacti.
     */
    DESERT(
        "Desert",
        BlockType.SAND,
        BlockType.SANDSTONE,
        62,
        6.0
    ),
    
    /**
     * Snow biome - cold, icy terrain with snow layers.
     */
    SNOW(
        "Snow",
        BlockType.SNOW_BLOCK,
        BlockType.SNOW_BLOCK,
        70,
        10.0
    ),
    
    /**
     * Jungle biome - hot, wet, dense vegetation with tall trees.
     */
    JUNGLE(
        "Jungle",
        BlockType.JUNGLE_GRASS,
        BlockType.JUNGLE_DIRT,
        68,
        12.0
    ),
    
    /**
     * Plains biome - temperate, grassy, rolling hills with occasional trees.
     */
    PLAINS(
        "Plains",
        BlockType.GRASS,
        BlockType.DIRT,
        64,
        8.0
    );
    
    private final String name;
    private final BlockType surfaceBlock;
    private final BlockType subsurfaceBlock;
    private final int baseHeight;
    private final double heightVariation;
    
    /**
     * Constructor for BiomeType enum.
     * 
     * @param name Display name
     * @param surfaceBlock Top layer block type
     * @param subsurfaceBlock Layer below surface
     * @param baseHeight Base terrain height for this biome
     * @param heightVariation How much terrain height varies
     */
    BiomeType(String name, BlockType surfaceBlock, BlockType subsurfaceBlock, 
              int baseHeight, double heightVariation) {
        this.name = name;
        this.surfaceBlock = surfaceBlock;
        this.subsurfaceBlock = subsurfaceBlock;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
    }
    
    /**
     * Gets the display name of this biome.
     * 
     * @return Biome name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the surface block type (top layer).
     * 
     * @return Surface block type
     */
    public BlockType getSurfaceBlock() {
        return surfaceBlock;
    }
    
    /**
     * Gets the subsurface block type (layers below surface).
     * 
     * @return Subsurface block type
     */
    public BlockType getSubsurfaceBlock() {
        return subsurfaceBlock;
    }
    
    /**
     * Gets the base terrain height for this biome.
     * This is the Y level around which terrain generates.
     * 
     * @return Base height
     */
    public int getBaseHeight() {
        return baseHeight;
    }
    
    /**
     * Gets the height variation for this biome.
     * Higher values = more dramatic terrain changes.
     * 
     * @return Height variation
     */
    public double getHeightVariation() {
        return heightVariation;
    }
}
