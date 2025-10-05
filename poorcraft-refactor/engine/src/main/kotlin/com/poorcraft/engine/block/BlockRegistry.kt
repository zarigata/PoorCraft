package com.poorcraft.engine.block

import com.poorcraft.engine.rendering.TextureAtlas
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for all block types
 */
class BlockRegistry {
    private val logger = LoggerFactory.getLogger(BlockRegistry::class.java)
    private val blocks = ConcurrentHashMap<Int, Block>()
    private val blocksByName = ConcurrentHashMap<String, Block>()
    private var nextId = 1
    
    var textureAtlas: TextureAtlas? = null
        private set
    
    fun initialize(skinsDirectory: Path) {
        logger.info("Initializing block registry")
        
        // Register default blocks
        register(Block.AIR)
        registerDefault("grass", "grass.png")
        registerDefault("dirt", "dirt.png")
        registerDefault("stone", "stone.png")
        registerDefault("wood", "wood.png")
        registerDefault("leaves", "leaves.png", opaque = false)
        registerDefault("sand", "sand.png")
        registerDefault("water", "water.png", opaque = false, collidable = false)
        registerDefault("glass", "glass.png", opaque = false)
        registerDefault("brick", "brick.png")
        registerDefault("planks", "planks.png")
        
        // Load texture atlas
        textureAtlas = TextureAtlas.load(skinsDirectory)
        
        logger.info("Registered ${blocks.size} blocks")
    }
    
    private fun registerDefault(name: String, texture: String, opaque: Boolean = true, collidable: Boolean = true) {
        register(Block(nextId++, name, texture, opaque, collidable))
    }
    
    fun register(block: Block) {
        if (block.id == 0 && block != Block.AIR) {
            throw IllegalArgumentException("Block ID 0 is reserved for AIR")
        }
        
        blocks[block.id] = block
        blocksByName[block.name] = block
        logger.debug("Registered block: ${block.name} (ID: ${block.id})")
    }
    
    fun getBlock(id: Int): Block = blocks[id] ?: Block.AIR
    
    fun getBlock(name: String): Block? = blocksByName[name]
    
    fun getBlockId(name: String): Int = blocksByName[name]?.id ?: 0
    
    fun getAllBlocks(): Collection<Block> = blocks.values
    
    fun getTextureUV(block: Block): FloatArray {
        return textureAtlas?.getUV(block.textureName) ?: floatArrayOf(0f, 0f, 1f, 1f)
    }
}
