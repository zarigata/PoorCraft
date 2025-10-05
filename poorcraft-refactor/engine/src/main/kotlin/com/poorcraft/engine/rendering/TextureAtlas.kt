package com.poorcraft.engine.rendering

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.glGenerateMipmap
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

/**
 * Texture atlas for block textures
 */
class TextureAtlas(
    val textureId: Int,
    val width: Int,
    val height: Int,
    private val uvMap: Map<String, UVCoords>
) {
    data class UVCoords(val u1: Float, val v1: Float, val u2: Float, val v2: Float)
    
    companion object {
        private val logger = LoggerFactory.getLogger(TextureAtlas::class.java)
        private val gson = Gson()
        
        fun load(skinsDirectory: Path): TextureAtlas? {
            val atlasImagePath = skinsDirectory.resolve("atlas.png")
            val atlasJsonPath = skinsDirectory.resolve("atlas.json")
            
            if (!Files.exists(atlasImagePath) || !Files.exists(atlasJsonPath)) {
                logger.warn("Atlas not found, will use default texture")
                return createDefaultAtlas()
            }
            
            return try {
                // Load UV map
                val json = Files.readString(atlasJsonPath)
                val type = object : TypeToken<Map<String, UVCoords>>() {}.type
                val uvMap: Map<String, UVCoords> = gson.fromJson(json, type)
                
                // Load texture
                val textureId = loadTexture(atlasImagePath)
                
                MemoryStack.stackPush().use { stack ->
                    val w = stack.mallocInt(1)
                    val h = stack.mallocInt(1)
                    val channels = stack.mallocInt(1)
                    
                    stbi_set_flip_vertically_on_load(true)
                    val imageBuffer = stbi_load(atlasImagePath.toString(), w, h, channels, 4)
                        ?: throw RuntimeException("Failed to load texture: ${stbi_failure_reason()}")
                    
                    val width = w.get(0)
                    val height = h.get(0)
                    
                    stbi_image_free(imageBuffer)
                    
                    logger.info("Loaded texture atlas: ${width}x${height}, ${uvMap.size} textures")
                    TextureAtlas(textureId, width, height, uvMap)
                }
            } catch (e: Exception) {
                logger.error("Failed to load atlas", e)
                createDefaultAtlas()
            }
        }
        
        private fun loadTexture(path: Path): Int {
            val textureId = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, textureId)
            
            MemoryStack.stackPush().use { stack ->
                val w = stack.mallocInt(1)
                val h = stack.mallocInt(1)
                val channels = stack.mallocInt(1)
                
                stbi_set_flip_vertically_on_load(true)
                val imageBuffer = stbi_load(path.toString(), w, h, channels, 4)
                    ?: throw RuntimeException("Failed to load texture: ${stbi_failure_reason()}")
                
                val width = w.get(0)
                val height = h.get(0)
                
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer)
                glGenerateMipmap(GL_TEXTURE_2D)
                
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
                
                stbi_image_free(imageBuffer)
            }
            
            return textureId
        }
        
        private fun createDefaultAtlas(): TextureAtlas {
            logger.info("Creating default atlas")
            
            // Create a simple 16x16 white texture
            val textureId = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, textureId)
            
            val pixels = ByteArray(16 * 16 * 4) { 255.toByte() }
            val buffer = org.lwjgl.BufferUtils.createByteBuffer(pixels.size)
            buffer.put(pixels).flip()
            
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 16, 16, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            
            val defaultUV = mapOf("default" to UVCoords(0f, 0f, 1f, 1f))
            return TextureAtlas(textureId, 16, 16, defaultUV)
        }
    }
    
    fun getUV(textureName: String): FloatArray {
        val uv = uvMap[textureName] ?: uvMap["default"] ?: UVCoords(0f, 0f, 1f, 1f)
        return floatArrayOf(uv.u1, uv.v1, uv.u2, uv.v2)
    }
    
    fun bind() {
        glBindTexture(GL_TEXTURE_2D, textureId)
    }
    
    fun cleanup() {
        glDeleteTextures(textureId)
    }
}
