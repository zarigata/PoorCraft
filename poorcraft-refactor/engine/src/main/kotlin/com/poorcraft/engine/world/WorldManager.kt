package com.poorcraft.engine.world

import com.poorcraft.engine.block.BlockRegistry
import com.poorcraft.engine.event.EventBus
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.*

/**
 * Manages world chunks, loading, and generation
 */
class WorldManager(
    private val savesDirectory: Path,
    private val blockRegistry: BlockRegistry,
    private val eventBus: EventBus
) {
    private val logger = LoggerFactory.getLogger(WorldManager::class.java)
    private val chunks = ConcurrentHashMap<ChunkPos, Chunk>()
    private val chunkGenerator = ChunkGenerator()
    private val regionManager = RegionFileManager(savesDirectory.resolve("world"))
    
    private val executor = Executors.newFixedThreadPool(4) { r ->
        Thread(r, "ChunkWorker").apply { isDaemon = true }
    }
    
    private val loadQueue = ConcurrentLinkedQueue<ChunkPos>()
    private val saveQueue = ConcurrentLinkedQueue<Chunk>()
    
    data class ChunkPos(val x: Int, val z: Int) {
        override fun hashCode(): Int = x * 31 + z
        override fun equals(other: Any?): Boolean {
            if (other !is ChunkPos) return false
            return x == other.x && z == other.z
        }
    }
    
    fun initialize() {
        logger.info("Initializing world manager")
        
        // Load spawn chunks
        for (x in -2..2) {
            for (z in -2..2) {
                loadChunk(x, z)
            }
        }
    }
    
    fun tick() {
        // Process load queue
        repeat(4) {
            val pos = loadQueue.poll() ?: return@repeat
            if (!chunks.containsKey(pos)) {
                executor.submit { loadChunkAsync(pos) }
            }
        }
        
        // Process save queue
        repeat(2) {
            val chunk = saveQueue.poll() ?: return@repeat
            executor.submit { saveChunkAsync(chunk) }
        }
    }
    
    fun loadChunk(x: Int, z: Int) {
        val pos = ChunkPos(x, z)
        if (chunks.containsKey(pos)) return
        
        loadQueue.offer(pos)
    }
    
    private fun loadChunkAsync(pos: ChunkPos) {
        try {
            val chunk = regionManager.loadChunk(pos.x, pos.z) ?: run {
                // Generate new chunk
                val newChunk = Chunk(pos.x, pos.z)
                chunkGenerator.generate(newChunk)
                newChunk
            }
            
            chunks[pos] = chunk
            eventBus.dispatch("onChunkLoad", pos.x, pos.z)
            logger.debug("Loaded chunk at ${pos.x}, ${pos.z}")
        } catch (e: Exception) {
            logger.error("Failed to load chunk at ${pos.x}, ${pos.z}", e)
        }
    }
    
    fun unloadChunk(x: Int, z: Int) {
        val pos = ChunkPos(x, z)
        val chunk = chunks.remove(pos) ?: return
        
        if (chunk.dirty.get()) {
            saveQueue.offer(chunk)
        }
        
        eventBus.dispatch("onChunkUnload", x, z)
        logger.debug("Unloaded chunk at $x, $z")
    }
    
    private fun saveChunkAsync(chunk: Chunk) {
        try {
            regionManager.saveChunk(chunk)
            chunk.dirty.set(false)
            logger.debug("Saved chunk at ${chunk.x}, ${chunk.z}")
        } catch (e: Exception) {
            logger.error("Failed to save chunk at ${chunk.x}, ${chunk.z}", e)
        }
    }
    
    fun getChunk(x: Int, z: Int): Chunk? {
        return chunks[ChunkPos(x, z)]
    }
    
    fun getBlock(worldX: Int, worldY: Int, worldZ: Int): Int {
        val chunkX = Math.floorDiv(worldX, 16)
        val chunkZ = Math.floorDiv(worldZ, 16)
        val chunk = getChunk(chunkX, chunkZ) ?: return 0
        
        val localX = Math.floorMod(worldX, 16)
        val localZ = Math.floorMod(worldZ, 16)
        
        return chunk.getBlock(localX, worldY, localZ)
    }
    
    fun setBlock(worldX: Int, worldY: Int, worldZ: Int, blockId: Int) {
        val chunkX = Math.floorDiv(worldX, 16)
        val chunkZ = Math.floorDiv(worldZ, 16)
        val chunk = getChunk(chunkX, chunkZ) ?: return
        
        val localX = Math.floorMod(worldX, 16)
        val localZ = Math.floorMod(worldZ, 16)
        
        val oldBlockId = chunk.getBlock(localX, worldY, localZ)
        chunk.setBlock(localX, worldY, localZ, blockId)
        
        if (oldBlockId == 0 && blockId != 0) {
            eventBus.dispatch("onBlockPlace", worldX, worldY, worldZ, blockId)
        } else if (oldBlockId != 0 && blockId == 0) {
            eventBus.dispatch("onBlockBreak", worldX, worldY, worldZ, oldBlockId)
        }
    }
    
    fun getLoadedChunks(): Collection<Chunk> = chunks.values
    
    fun shutdown() {
        logger.info("Shutting down world manager")
        
        // Save all dirty chunks
        chunks.values.filter { it.dirty.get() }.forEach { saveChunkAsync(it) }
        
        executor.shutdown()
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
        
        regionManager.close()
        logger.info("World manager shutdown complete")
    }
}
