package com.poorcraft.engine.world

import com.poorcraft.engine.block.Block
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A chunk of blocks (16x256x16 by default)
 */
class Chunk(
    val x: Int,
    val z: Int,
    val sizeX: Int = 16,
    val sizeY: Int = 256,
    val sizeZ: Int = 16
) {
    private val blocks = IntArray(sizeX * sizeY * sizeZ)
    val dirty = AtomicBoolean(true)
    var generated = false
    
    fun getBlock(x: Int, y: Int, z: Int): Int {
        if (!isValidPosition(x, y, z)) return 0
        return blocks[getIndex(x, y, z)]
    }
    
    fun setBlock(x: Int, y: Int, z: Int, blockId: Int) {
        if (!isValidPosition(x, y, z)) return
        blocks[getIndex(x, y, z)] = blockId
        dirty.set(true)
    }
    
    fun fill(blockId: Int) {
        blocks.fill(blockId)
        dirty.set(true)
    }
    
    private fun isValidPosition(x: Int, y: Int, z: Int): Boolean {
        return x in 0 until sizeX && y in 0 until sizeY && z in 0 until sizeZ
    }
    
    private fun getIndex(x: Int, y: Int, z: Int): Int {
        return (y * sizeZ + z) * sizeX + x
    }
    
    fun getBlocks(): IntArray = blocks
    
    fun setBlocks(data: IntArray) {
        if (data.size == blocks.size) {
            System.arraycopy(data, 0, blocks, 0, blocks.size)
            dirty.set(true)
        }
    }
}
