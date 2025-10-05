package com.poorcraft.engine.world

import kotlin.math.floor
import kotlin.random.Random

/**
 * Simple chunk generator with basic terrain
 */
class ChunkGenerator(private val seed: Long = 0) {
    private val random = Random(seed)
    
    fun generate(chunk: Chunk) {
        // Simple flat terrain with some noise
        for (x in 0 until chunk.sizeX) {
            for (z in 0 until chunk.sizeZ) {
                val worldX = chunk.x * chunk.sizeX + x
                val worldZ = chunk.z * chunk.sizeZ + z
                
                val height = getHeight(worldX, worldZ)
                
                for (y in 0 until chunk.sizeY) {
                    val blockId = when {
                        y > height -> 0 // Air
                        y == height -> 1 // Grass
                        y > height - 3 -> 2 // Dirt
                        else -> 3 // Stone
                    }
                    chunk.setBlock(x, y, z, blockId)
                }
            }
        }
        
        chunk.generated = true
        chunk.dirty.set(true)
    }
    
    private fun getHeight(x: Int, z: Int): Int {
        // Simple noise-like height calculation
        val noise = (Math.sin(x * 0.1) * Math.cos(z * 0.1) * 5.0).toInt()
        return 64 + noise
    }
}
