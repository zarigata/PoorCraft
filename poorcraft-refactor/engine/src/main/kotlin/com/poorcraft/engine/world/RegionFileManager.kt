package com.poorcraft.engine.world

import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Manages region files for chunk persistence
 * Simple format: one file per chunk with GZIP compression
 */
class RegionFileManager(private val worldDirectory: Path) {
    private val logger = LoggerFactory.getLogger(RegionFileManager::class.java)
    
    init {
        Files.createDirectories(worldDirectory)
    }
    
    fun loadChunk(chunkX: Int, chunkZ: Int): Chunk? {
        val file = getChunkFile(chunkX, chunkZ)
        if (!Files.exists(file)) return null
        
        return try {
            DataInputStream(GZIPInputStream(Files.newInputStream(file))).use { input ->
                val x = input.readInt()
                val z = input.readInt()
                val sizeX = input.readInt()
                val sizeY = input.readInt()
                val sizeZ = input.readInt()
                
                val chunk = Chunk(x, z, sizeX, sizeY, sizeZ)
                val blocks = IntArray(sizeX * sizeY * sizeZ)
                
                for (i in blocks.indices) {
                    blocks[i] = input.readInt()
                }
                
                chunk.setBlocks(blocks)
                chunk.generated = true
                chunk.dirty.set(false)
                chunk
            }
        } catch (e: Exception) {
            logger.error("Failed to load chunk $chunkX, $chunkZ", e)
            null
        }
    }
    
    fun saveChunk(chunk: Chunk) {
        val file = getChunkFile(chunk.x, chunk.z)
        Files.createDirectories(file.parent)
        
        try {
            DataOutputStream(GZIPOutputStream(Files.newOutputStream(file))).use { output ->
                output.writeInt(chunk.x)
                output.writeInt(chunk.z)
                output.writeInt(chunk.sizeX)
                output.writeInt(chunk.sizeY)
                output.writeInt(chunk.sizeZ)
                
                val blocks = chunk.getBlocks()
                for (blockId in blocks) {
                    output.writeInt(blockId)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to save chunk ${chunk.x}, ${chunk.z}", e)
        }
    }
    
    private fun getChunkFile(chunkX: Int, chunkZ: Int): Path {
        val regionX = Math.floorDiv(chunkX, 32)
        val regionZ = Math.floorDiv(chunkZ, 32)
        val regionDir = worldDirectory.resolve("r.$regionX.$regionZ")
        return regionDir.resolve("c.$chunkX.$chunkZ.dat")
    }
    
    fun close() {
        // Nothing to close in this simple implementation
    }
}
