package com.poorcraft.engine.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Path

/**
 * Engine configuration
 */
data class EngineConfig(
    val version: String = "0.1.2",
    val windowWidth: Int = 1280,
    val windowHeight: Int = 720,
    val vsync: Boolean = true,
    val renderDistance: Int = 8,
    val fov: Float = 70f,
    val textureSize: Int = 16,
    val chunkSize: Int = 16,
    val chunkHeight: Int = 256,
    val maxLoadedChunks: Int = 256,
    val workerThreads: Int = 4,
    val modMemoryLimitMB: Int = 64,
    val modTickTimeoutMs: Long = 100,
    val enableModSandbox: Boolean = true,
    val logLevel: String = "INFO"
) {
    companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        
        fun load(configPath: Path): EngineConfig {
            return if (Files.exists(configPath)) {
                try {
                    val json = Files.readString(configPath)
                    gson.fromJson(json, EngineConfig::class.java)
                } catch (e: Exception) {
                    EngineConfig().also { it.save(configPath) }
                }
            } else {
                EngineConfig().also { it.save(configPath) }
            }
        }
    }
    
    fun save(configPath: Path) {
        Files.createDirectories(configPath.parent)
        Files.writeString(configPath, gson.toJson(this))
    }
}
