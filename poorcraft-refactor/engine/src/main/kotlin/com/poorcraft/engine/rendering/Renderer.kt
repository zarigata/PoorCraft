package com.poorcraft.engine.rendering

import com.poorcraft.engine.block.BlockRegistry
import com.poorcraft.engine.config.EngineConfig
import com.poorcraft.engine.world.WorldManager
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.slf4j.LoggerFactory

/**
 * Main renderer - handles all OpenGL rendering
 */
class Renderer(
    private val blockRegistry: BlockRegistry,
    private val config: EngineConfig
) {
    private val logger = LoggerFactory.getLogger(Renderer::class.java)
    private lateinit var shader: ShaderProgram
    private val camera = Camera()
    private val chunkMeshes = mutableMapOf<Pair<Int, Int>, ChunkMesh>()
    
    fun initialize() {
        logger.info("Initializing renderer")
        
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f) // Sky blue
        
        shader = ShaderProgram.createDefault()
        
        camera.position.set(0f, 80f, 0f)
        camera.updateProjection(config.windowWidth.toFloat() / config.windowHeight.toFloat(), config.fov)
        
        logger.info("Renderer initialized")
    }
    
    fun render(worldManager: WorldManager) {
        // Update camera
        camera.update()
        
        // Bind shader and set uniforms
        shader.use()
        shader.setUniform("projection", camera.projectionMatrix)
        shader.setUniform("view", camera.viewMatrix)
        
        // Bind texture atlas
        blockRegistry.textureAtlas?.bind()
        
        // Render chunks
        for (chunk in worldManager.getLoadedChunks()) {
            val key = chunk.x to chunk.z
            
            // Build mesh if dirty
            if (chunk.dirty.get()) {
                val mesh = chunkMeshes.getOrPut(key) { ChunkMesh() }
                mesh.build(chunk, blockRegistry)
                chunk.dirty.set(false)
            }
            
            // Render mesh
            chunkMeshes[key]?.render(shader, chunk.x * 16f, 0f, chunk.z * 16f)
        }
    }
    
    fun onResize(width: Int, height: Int) {
        camera.updateProjection(width.toFloat() / height.toFloat(), config.fov)
    }
    
    fun cleanup() {
        logger.info("Cleaning up renderer")
        shader.cleanup()
        chunkMeshes.values.forEach { it.cleanup() }
        blockRegistry.textureAtlas?.cleanup()
    }
}
