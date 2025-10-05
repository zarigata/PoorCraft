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
        6.0,
        3,
        0.55,
        0.08,
        0.35,
        0.55
    ),
    
    /**
     * Snow biome - cold, icy terrain with snow layers.
     */
    SNOW(
        "Snow",
        BlockType.SNOW_BLOCK,
        BlockType.SNOW_BLOCK,
        72,
        14.0,
        4,
        0.6,
        0.65,
        0.75,
        0.85
    ),
    
    /**
     * Jungle biome - hot, wet, dense vegetation with tall trees.
     */
    JUNGLE(
        "Jungle",
        BlockType.JUNGLE_GRASS,
        BlockType.JUNGLE_DIRT,
        69,
        11.0,
        4,
        0.9,
        0.35,
        0.45,
        0.75
    ),
    
    /**
     * Plains biome - temperate, grassy, rolling hills with occasional trees.
     */
    PLAINS(
        "Plains",
        BlockType.GRASS,
        BlockType.DIRT,
        64,
        8.0,
        4,
        0.7,
        0.18,
        0.35,
        0.65
    ),

    FOREST(
        "Forest",
        BlockType.GRASS,
        BlockType.DIRT,
        66,
        8.5,
        4,
        0.85,
        0.22,
        0.4,
        0.7
    ),

    MOUNTAINS(
        "Mountains",
        BlockType.STONE,
        BlockType.STONE,
        78,
        18.0,
        2,
        0.5,
        0.95,
        0.9,
        0.95
    ),

    SWAMP(
        "Swamp",
        BlockType.GRASS,
        BlockType.DIRT,
        61,
        5.0,
        5,
        0.9,
        0.05,
        0.3,
        0.5
    );
    
    private final String name;
    private final BlockType surfaceBlock;
    private final BlockType subsurfaceBlock;
    private final int baseHeight;
    private final double heightVariation;
    private final int surfaceDepth;
    private final double detailStrength;
    private final double ridgeStrength;
    private final double continentInfluence;
    private final double shapeSharpness;
    
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
              int baseHeight, double heightVariation, int surfaceDepth,
              double detailStrength, double ridgeStrength,
              double continentInfluence, double shapeSharpness) {
        this.name = name;
        this.surfaceBlock = surfaceBlock;
        this.subsurfaceBlock = subsurfaceBlock;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
        this.surfaceDepth = Math.max(1, surfaceDepth);
        this.detailStrength = detailStrength;
        this.ridgeStrength = ridgeStrength;
        this.continentInfluence = continentInfluence;
        this.shapeSharpness = shapeSharpness;
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

    public int getSurfaceDepth() {
        return surfaceDepth;
    }

    public double getDetailStrength() {
        return detailStrength;
    }

    public double getRidgeStrength() {
        return ridgeStrength;
    }

    public double getContinentInfluence() {
        return continentInfluence;
    }

    public double getShapeSharpness() {
        return shapeSharpness;
    }
}
