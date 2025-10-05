package com.poorcraft.engine.world

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkTest {
    
    @Test
    fun `should create chunk with correct dimensions`() {
        val chunk = Chunk(0, 0, 16, 256, 16)
        
        assertEquals(0, chunk.x)
        assertEquals(0, chunk.z)
        assertEquals(16, chunk.sizeX)
        assertEquals(256, chunk.sizeY)
        assertEquals(16, chunk.sizeZ)
    }
    
    @Test
    fun `should set and get blocks`() {
        val chunk = Chunk(0, 0)
        
        chunk.setBlock(5, 64, 5, 1)
        assertEquals(1, chunk.getBlock(5, 64, 5))
    }
    
    @Test
    fun `should return 0 for out of bounds coordinates`() {
        val chunk = Chunk(0, 0)
        
        assertEquals(0, chunk.getBlock(-1, 0, 0))
        assertEquals(0, chunk.getBlock(0, -1, 0))
        assertEquals(0, chunk.getBlock(0, 0, -1))
        assertEquals(0, chunk.getBlock(16, 0, 0))
        assertEquals(0, chunk.getBlock(0, 256, 0))
        assertEquals(0, chunk.getBlock(0, 0, 16))
    }
    
    @Test
    fun `should mark chunk as dirty when modified`() {
        val chunk = Chunk(0, 0)
        chunk.dirty.set(false)
        
        chunk.setBlock(0, 0, 0, 1)
        assertTrue(chunk.dirty.get())
    }
    
    @Test
    fun `should fill chunk with block`() {
        val chunk = Chunk(0, 0, 4, 4, 4)
        chunk.fill(5)
        
        for (x in 0 until 4) {
            for (y in 0 until 4) {
                for (z in 0 until 4) {
                    assertEquals(5, chunk.getBlock(x, y, z))
                }
            }
        }
    }
}
