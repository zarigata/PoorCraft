package com.poorcraft.engine.block

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BlockRegistryTest {
    
    @Test
    fun `should register and retrieve blocks`() {
        val registry = BlockRegistry()
        
        val block = Block(1, "test_block", "test.png")
        registry.register(block)
        
        assertEquals(block, registry.getBlock(1))
        assertEquals(block, registry.getBlock("test_block"))
    }
    
    @Test
    fun `should return AIR for unknown block ID`() {
        val registry = BlockRegistry()
        
        val block = registry.getBlock(999)
        assertEquals(Block.AIR, block)
    }
    
    @Test
    fun `should return null for unknown block name`() {
        val registry = BlockRegistry()
        
        val block = registry.getBlock("unknown")
        assertNull(block)
    }
    
    @Test
    fun `should get block ID by name`() {
        val registry = BlockRegistry()
        
        val block = Block(5, "stone", "stone.png")
        registry.register(block)
        
        assertEquals(5, registry.getBlockId("stone"))
        assertEquals(0, registry.getBlockId("unknown"))
    }
}
