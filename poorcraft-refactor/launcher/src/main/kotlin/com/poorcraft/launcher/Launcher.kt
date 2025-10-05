package com.poorcraft.launcher

import com.poorcraft.engine.config.EngineConfig
import com.poorcraft.engine.core.Engine
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * Launcher entry point - handles first-run bootstrap and engine initialization
 */
fun main(args: Array<String>) {
    val launcher = Launcher(args)
    launcher.launch()
}

class Launcher(private val args: Array<String>) {
    private val logger = LoggerFactory.getLogger(Launcher::class.java)
    
    private var portable = false
    private var devMode = false
    private var headless = false
    
    fun launch() {
        logger.info("PoorCraft Refactor v0.1.2 - Launcher")
        
        // Parse arguments
        parseArguments()
        
        // Determine data directory
        val dataDirectory = getDataDirectory()
        logger.info("Data directory: $dataDirectory")
        
        // First-run bootstrap
        if (!isInitialized(dataDirectory)) {
            logger.info("First run detected - bootstrapping")
            bootstrap(dataDirectory)
        }
        
        // Load configuration
        val config = EngineConfig.load(dataDirectory.resolve("config.json"))
        
        // Initialize and run engine
        val engine = Engine(config, dataDirectory)
        engine.devMode = devMode
        engine.headless = headless
        
        if (!engine.initialize()) {
            logger.error("Failed to initialize engine")
            exitProcess(1)
        }
        
        try {
            engine.run()
        } catch (e: Exception) {
            logger.error("Engine crashed", e)
            exitProcess(1)
        }
        
        logger.info("Launcher exiting")
    }
    
    private fun parseArguments() {
        for (arg in args) {
            when (arg) {
                "--portable" -> {
                    portable = true
                    logger.info("Portable mode enabled")
                }
                "--dev-mode" -> {
                    devMode = true
                    logger.info("Development mode enabled")
                }
                "--headless" -> {
                    headless = true
                    logger.info("Headless mode enabled")
                }
            }
        }
    }
    
    private fun getDataDirectory(): Path {
        return if (portable) {
            // Portable mode: create directory next to executable
            val exeDir = Paths.get(System.getProperty("user.dir"))
            exeDir.resolve("PoorCraftData")
        } else {
            // Default: use APPDATA on Windows
            val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
            Paths.get(appData).resolve("PoorCraftRefactor")
        }
    }
    
    private fun isInitialized(dataDirectory: Path): Boolean {
        return dataDirectory.resolve(".initialized").exists()
    }
    
    private fun bootstrap(dataDirectory: Path) {
        logger.info("Creating data directory structure")
        
        try {
            // Create directories
            Files.createDirectories(dataDirectory.resolve("skins"))
            Files.createDirectories(dataDirectory.resolve("mods"))
            Files.createDirectories(dataDirectory.resolve("saves"))
            Files.createDirectories(dataDirectory.resolve("logs"))
            
            // Extract default assets
            extractDefaultSkins(dataDirectory.resolve("skins"))
            extractDefaultMods(dataDirectory.resolve("mods"))
            
            // Create default config
            val config = EngineConfig()
            config.save(dataDirectory.resolve("config.json"))
            
            // Create marker file
            Files.writeString(dataDirectory.resolve(".initialized"), "v0.1.2")
            
            logger.info("Bootstrap complete")
        } catch (e: Exception) {
            logger.error("Bootstrap failed", e)
            throw e
        }
    }
    
    private fun extractDefaultSkins(skinsDirectory: Path) {
        logger.info("Extracting default skins")
        
        val defaultDir = skinsDirectory.resolve("default")
        Files.createDirectories(defaultDir)
        
        // Extract embedded skin resources
        val skinNames = listOf(
            "grass.png", "dirt.png", "stone.png", "wood.png", "leaves.png",
            "sand.png", "water.png", "glass.png", "brick.png", "planks.png"
        )
        
        for (skinName in skinNames) {
            val resourcePath = "/assets/skins/default/$skinName"
            val targetPath = defaultDir.resolve(skinName)
            
            try {
                val inputStream = javaClass.getResourceAsStream(resourcePath)
                if (inputStream != null) {
                    Files.copy(inputStream, targetPath)
                    logger.debug("Extracted: $skinName")
                } else {
                    logger.warn("Resource not found: $resourcePath - creating placeholder")
                    createPlaceholderTexture(targetPath)
                }
            } catch (e: Exception) {
                logger.warn("Failed to extract $skinName, creating placeholder", e)
                createPlaceholderTexture(targetPath)
            }
        }
        
        // Create README
        val readme = """
            # PoorCraft Skins
            
            This directory contains block textures for PoorCraft.
            
            ## Format
            - Each block should have a PNG file named after the block ID
            - Default texture size: 16x16 pixels (configurable to 32x32)
            - Textures are automatically packed into an atlas at runtime
            
            ## Naming Convention
            - grass.png - Grass block
            - dirt.png - Dirt block
            - stone.png - Stone block
            - etc.
            
            ## Custom Skins
            To add custom skins:
            1. Create a new directory under skins/
            2. Add your PNG files
            3. Restart the game or use the rebuildAtlas command
            
            ## Atlas Rebuilding
            After editing textures, the atlas will be automatically rebuilt on next launch.
        """.trimIndent()
        
        Files.writeString(skinsDirectory.resolve("README.md"), readme)
    }
    
    private fun createPlaceholderTexture(path: Path) {
        // Create a simple 16x16 magenta texture as placeholder
        // This is a minimal PNG file
        val pngData = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x10,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90.toByte(), 0x91.toByte(), 0x68,
            0x36, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x28, 0x15, 0x63, 0xF8.toByte(), 0x0F, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x18.toByte(), 0xDD.toByte(), 0x8D.toByte(), 0xB4.toByte(),
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
        
        Files.write(path, pngData)
    }
    
    private fun extractDefaultMods(modsDirectory: Path) {
        logger.info("Extracting default mods")
        
        val exampleModDir = modsDirectory.resolve("example_mod")
        Files.createDirectories(exampleModDir)
        
        // Create mod.json
        val modJson = """
            {
              "id": "example_mod",
              "name": "Example Mod",
              "version": "0.1.0",
              "author": "PoorCraft Team",
              "main": "main.lua",
              "entry": "onEnable"
            }
        """.trimIndent()
        
        Files.writeString(exampleModDir.resolve("mod.json"), modJson)
        
        // Create main.lua
        val mainLua = """
            -- Example Mod for PoorCraft Refactor
            local engine = Engine
            local log = Logger
            
            function onLoad()
              log:info("Example mod: onLoad")
            end
            
            function onEnable()
              log:info("Example mod: onEnable")
              
              -- Register event listener
              engine:registerEvent("onPlayerJoin", function(player)
                log:info("Player joined!")
                engine:sendChat(player, "Welcome to PoorCraft Refactor!")
              end)
              
              -- Register tick listener
              engine:registerEvent("onTick", function(tick)
                if tick % 100 == 0 then
                  log:info("Tick: " .. tick)
                end
              end)
              
              -- Schedule a delayed action
              engine:schedule(60, function()
                log:info("Scheduled action executed after 60 ticks!")
              end)
            end
            
            function onDisable()
              log:info("Example mod: onDisable")
            end
            
            function onTick()
              -- Called every tick if the mod is enabled
            end
        """.trimIndent()
        
        Files.writeString(exampleModDir.resolve("main.lua"), mainLua)
        
        logger.info("Default mods extracted")
    }
}
