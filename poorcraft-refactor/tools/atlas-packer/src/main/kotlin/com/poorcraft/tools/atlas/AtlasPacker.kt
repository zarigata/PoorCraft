package com.poorcraft.tools.atlas

import com.google.gson.GsonBuilder
import org.lwjgl.stb.STBImage.*
import org.lwjgl.stb.STBImageWrite.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Texture atlas packer - packs multiple PNG files into a single atlas
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: atlas-packer <input-directory> [output-directory]")
        println("  Packs all PNG files from input directory into atlas.png and atlas.json")
        return
    }
    
    val inputDir = Paths.get(args[0])
    val outputDir = if (args.size > 1) Paths.get(args[1]) else inputDir
    
    val packer = AtlasPacker()
    packer.pack(inputDir, outputDir)
}

class AtlasPacker {
    data class TextureInfo(
        val name: String,
        val path: Path,
        val width: Int,
        val height: Int,
        val data: ByteBuffer
    )
    
    data class UVCoords(
        val u1: Float,
        val v1: Float,
        val u2: Float,
        val v2: Float
    )
    
    data class PackedTexture(
        val name: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
    
    fun pack(inputDir: Path, outputDir: Path) {
        println("Atlas Packer - PoorCraft Refactor")
        println("Input: $inputDir")
        println("Output: $outputDir")
        
        // Load all textures
        val textures = loadTextures(inputDir)
        if (textures.isEmpty()) {
            println("No textures found!")
            return
        }
        
        println("Loaded ${textures.size} textures")
        
        // Calculate atlas size
        val textureSize = textures.first().width
        val texturesPerRow = ceil(sqrt(textures.size.toDouble())).toInt()
        val atlasSize = texturesPerRow * textureSize
        
        println("Atlas size: ${atlasSize}x$atlasSize ($texturesPerRow textures per row)")
        
        // Create atlas
        val atlas = MemoryUtil.memAlloc(atlasSize * atlasSize * 4)
        val packedTextures = mutableListOf<PackedTexture>()
        
        var currentX = 0
        var currentY = 0
        
        for (texture in textures) {
            // Copy texture data to atlas
            copyTexture(atlas, atlasSize, texture, currentX, currentY)
            
            packedTextures.add(
                PackedTexture(
                    texture.name,
                    currentX,
                    currentY,
                    texture.width,
                    texture.height
                )
            )
            
            currentX += textureSize
            if (currentX >= atlasSize) {
                currentX = 0
                currentY += textureSize
            }
        }
        
        // Save atlas image
        Files.createDirectories(outputDir)
        val atlasPath = outputDir.resolve("atlas.png")
        
        atlas.position(0)
        if (stbi_write_png(atlasPath.toString(), atlasSize, atlasSize, 4, atlas, atlasSize * 4) == 0) {
            println("Failed to write atlas image!")
            MemoryUtil.memFree(atlas)
            return
        }
        
        println("Wrote atlas image: $atlasPath")
        
        // Generate UV coordinates
        val uvMap = mutableMapOf<String, UVCoords>()
        for (packed in packedTextures) {
            val u1 = packed.x.toFloat() / atlasSize
            val v1 = packed.y.toFloat() / atlasSize
            val u2 = (packed.x + packed.width).toFloat() / atlasSize
            val v2 = (packed.y + packed.height).toFloat() / atlasSize
            
            uvMap[packed.name] = UVCoords(u1, v1, u2, v2)
        }
        
        // Save UV map
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonPath = outputDir.resolve("atlas.json")
        Files.writeString(jsonPath, gson.toJson(uvMap))
        
        println("Wrote UV map: $jsonPath")
        
        // Cleanup
        MemoryUtil.memFree(atlas)
        textures.forEach { MemoryUtil.memFree(it.data) }
        
        println("Atlas packing complete!")
    }
    
    private fun loadTextures(directory: Path): List<TextureInfo> {
        val textures = mutableListOf<TextureInfo>()
        
        Files.walk(directory, 1).use { stream ->
            stream.filter { it.extension == "png" }.forEach { path ->
                try {
                    val texture = loadTexture(path)
                    textures.add(texture)
                } catch (e: Exception) {
                    println("Failed to load texture: $path - ${e.message}")
                }
            }
        }
        
        return textures
    }
    
    private fun loadTexture(path: Path): TextureInfo {
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            
            stbi_set_flip_vertically_on_load(false)
            val data = stbi_load(path.toString(), w, h, channels, 4)
                ?: throw RuntimeException("Failed to load texture: ${stbi_failure_reason()}")
            
            val width = w.get(0)
            val height = h.get(0)
            
            return TextureInfo(
                path.nameWithoutExtension,
                path,
                width,
                height,
                data
            )
        }
    }
    
    private fun copyTexture(
        atlas: ByteBuffer,
        atlasSize: Int,
        texture: TextureInfo,
        destX: Int,
        destY: Int
    ) {
        for (y in 0 until texture.height) {
            for (x in 0 until texture.width) {
                val srcIndex = (y * texture.width + x) * 4
                val destIndex = ((destY + y) * atlasSize + (destX + x)) * 4
                
                atlas.put(destIndex, texture.data.get(srcIndex))         // R
                atlas.put(destIndex + 1, texture.data.get(srcIndex + 1)) // G
                atlas.put(destIndex + 2, texture.data.get(srcIndex + 2)) // B
                atlas.put(destIndex + 3, texture.data.get(srcIndex + 3)) // A
            }
        }
    }
}
