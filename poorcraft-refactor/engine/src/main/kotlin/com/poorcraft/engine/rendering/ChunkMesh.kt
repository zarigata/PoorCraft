package com.poorcraft.engine.rendering

import com.poorcraft.engine.block.BlockRegistry
import com.poorcraft.engine.world.Chunk
import org.joml.Matrix4f
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil

/**
 * Mesh for a chunk - builds vertex data from blocks
 */
class ChunkMesh {
    private var vao = 0
    private var vbo = 0
    private var vertexCount = 0
    private val modelMatrix = Matrix4f()
    
    fun build(chunk: Chunk, blockRegistry: BlockRegistry) {
        val vertices = mutableListOf<Float>()
        
        // Build mesh for visible faces
        for (x in 0 until chunk.sizeX) {
            for (y in 0 until chunk.sizeY) {
                for (z in 0 until chunk.sizeZ) {
                    val blockId = chunk.getBlock(x, y, z)
                    if (blockId == 0) continue // Skip air
                    
                    val block = blockRegistry.getBlock(blockId)
                    val uv = blockRegistry.getTextureUV(block)
                    
                    // Check each face
                    if (shouldRenderFace(chunk, x, y + 1, z)) {
                        addTopFace(vertices, x.toFloat(), y.toFloat(), z.toFloat(), uv, 1.0f)
                    }
                    if (shouldRenderFace(chunk, x, y - 1, z)) {
                        addBottomFace(vertices, x.toFloat(), y.toFloat(), z.toFloat(), uv, 0.5f)
                    }
                    if (shouldRenderFace(chunk, x, y, z + 1)) {
                        addFrontFace(vertices, x.toFloat(), y.toFloat(), z.toFloat(), uv, 0.8f)
                    }
                    if (shouldRenderFace(chunk, x, y, z - 1)) {
                        addBackFace(vertices, x.toFloat(), y.toFloat(), z.toFloat(), uv, 0.8f)
                    }
                    if (shouldRenderFace(chunk, x + 1, y, z)) {
                        addRightFace(vertices, x.toFloat(), y.toFloat(), z.toFloat(), uv, 0.6f)
                    }
                    if (shouldRenderFace(chunk, x - 1, y, z)) {
                        addLeftFace(vertices, x.toFloat(), y.toFloat(), z.toFloat(), uv, 0.6f)
                    }
                }
            }
        }
        
        uploadMesh(vertices)
    }
    
    private fun shouldRenderFace(chunk: Chunk, x: Int, y: Int, z: Int): Boolean {
        if (x < 0 || x >= chunk.sizeX || y < 0 || y >= chunk.sizeY || z < 0 || z >= chunk.sizeZ) {
            return true // Render faces at chunk boundaries
        }
        return chunk.getBlock(x, y, z) == 0 // Render if adjacent block is air
    }
    
    private fun addTopFace(vertices: MutableList<Float>, x: Float, y: Float, z: Float, uv: FloatArray, shade: Float) {
        val y1 = y + 1
        vertices.addAll(listOf(
            x, y1, z, uv[0], uv[1], shade,
            x + 1, y1, z, uv[2], uv[1], shade,
            x + 1, y1, z + 1, uv[2], uv[3], shade,
            x, y1, z, uv[0], uv[1], shade,
            x + 1, y1, z + 1, uv[2], uv[3], shade,
            x, y1, z + 1, uv[0], uv[3], shade
        ))
    }
    
    private fun addBottomFace(vertices: MutableList<Float>, x: Float, y: Float, z: Float, uv: FloatArray, shade: Float) {
        vertices.addAll(listOf(
            x, y, z, uv[0], uv[1], shade,
            x + 1, y, z + 1, uv[2], uv[3], shade,
            x + 1, y, z, uv[2], uv[1], shade,
            x, y, z, uv[0], uv[1], shade,
            x, y, z + 1, uv[0], uv[3], shade,
            x + 1, y, z + 1, uv[2], uv[3], shade
        ))
    }
    
    private fun addFrontFace(vertices: MutableList<Float>, x: Float, y: Float, z: Float, uv: FloatArray, shade: Float) {
        val z1 = z + 1
        val y1 = y + 1
        vertices.addAll(listOf(
            x, y, z1, uv[0], uv[3], shade,
            x + 1, y, z1, uv[2], uv[3], shade,
            x + 1, y1, z1, uv[2], uv[1], shade,
            x, y, z1, uv[0], uv[3], shade,
            x + 1, y1, z1, uv[2], uv[1], shade,
            x, y1, z1, uv[0], uv[1], shade
        ))
    }
    
    private fun addBackFace(vertices: MutableList<Float>, x: Float, y: Float, z: Float, uv: FloatArray, shade: Float) {
        val y1 = y + 1
        vertices.addAll(listOf(
            x, y, z, uv[0], uv[3], shade,
            x + 1, y1, z, uv[2], uv[1], shade,
            x + 1, y, z, uv[2], uv[3], shade,
            x, y, z, uv[0], uv[3], shade,
            x, y1, z, uv[0], uv[1], shade,
            x + 1, y1, z, uv[2], uv[1], shade
        ))
    }
    
    private fun addRightFace(vertices: MutableList<Float>, x: Float, y: Float, z: Float, uv: FloatArray, shade: Float) {
        val x1 = x + 1
        val y1 = y + 1
        vertices.addAll(listOf(
            x1, y, z, uv[0], uv[3], shade,
            x1, y1, z, uv[0], uv[1], shade,
            x1, y1, z + 1, uv[2], uv[1], shade,
            x1, y, z, uv[0], uv[3], shade,
            x1, y1, z + 1, uv[2], uv[1], shade,
            x1, y, z + 1, uv[2], uv[3], shade
        ))
    }
    
    private fun addLeftFace(vertices: MutableList<Float>, x: Float, y: Float, z: Float, uv: FloatArray, shade: Float) {
        val y1 = y + 1
        vertices.addAll(listOf(
            x, y, z, uv[0], uv[3], shade,
            x, y1, z + 1, uv[2], uv[1], shade,
            x, y1, z, uv[0], uv[1], shade,
            x, y, z, uv[0], uv[3], shade,
            x, y, z + 1, uv[2], uv[3], shade,
            x, y1, z + 1, uv[2], uv[1], shade
        ))
    }
    
    private fun uploadMesh(vertices: List<Float>) {
        if (vao != 0) {
            cleanup()
        }
        
        vertexCount = vertices.size / 6 // 6 floats per vertex (pos + uv + shade)
        
        if (vertexCount == 0) return
        
        vao = glGenVertexArrays()
        vbo = glGenBuffers()
        
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        
        val buffer = MemoryUtil.memAllocFloat(vertices.size)
        buffer.put(vertices.toFloatArray()).flip()
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(buffer)
        
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * 4, 0)
        glEnableVertexAttribArray(0)
        
        // Texture coordinate attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 6 * 4, 3 * 4L)
        glEnableVertexAttribArray(1)
        
        // Shade attribute
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 6 * 4, 5 * 4L)
        glEnableVertexAttribArray(2)
        
        glBindVertexArray(0)
    }
    
    fun render(shader: ShaderProgram, x: Float, y: Float, z: Float) {
        if (vertexCount == 0) return
        
        modelMatrix.identity().translate(x, y, z)
        shader.setUniform("model", modelMatrix)
        
        glBindVertexArray(vao)
        glDrawArrays(GL_TRIANGLES, 0, vertexCount)
        glBindVertexArray(0)
    }
    
    fun cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao)
            vao = 0
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo)
            vbo = 0
        }
    }
}
