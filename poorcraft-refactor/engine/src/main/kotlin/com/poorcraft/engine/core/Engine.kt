package com.poorcraft.engine.core

import com.poorcraft.engine.event.EventBus
import com.poorcraft.engine.rendering.Renderer
import com.poorcraft.engine.world.WorldManager
import com.poorcraft.engine.block.BlockRegistry
import com.poorcraft.engine.mod.ModLoader
import com.poorcraft.engine.config.EngineConfig
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Main engine class - coordinates all subsystems
 */
class Engine(
    private val config: EngineConfig,
    private val dataDirectory: Path
) {
    private val logger = LoggerFactory.getLogger(Engine::class.java)
    
    private var window: Long = 0
    private var running = false
    private var tickCount = 0L
    
    val eventBus = EventBus()
    val blockRegistry = BlockRegistry()
    val worldManager = WorldManager(dataDirectory.resolve("saves"), blockRegistry, eventBus)
    val modLoader = ModLoader(dataDirectory.resolve("mods"), eventBus, this)
    
    private lateinit var renderer: Renderer
    
    var devMode = false
    var headless = false
    
    fun initialize(): Boolean {
        logger.info("Initializing PoorCraft Engine v${config.version}")
        
        if (!headless) {
            if (!initWindow()) {
                logger.error("Failed to initialize window")
                return false
            }
        }
        
        // Initialize subsystems
        blockRegistry.initialize(dataDirectory.resolve("skins"))
        
        if (!headless) {
            renderer = Renderer(blockRegistry, config)
            renderer.initialize()
        }
        
        worldManager.initialize()
        modLoader.loadMods()
        
        logger.info("Engine initialized successfully")
        return true
    }
    
    private fun initWindow(): Boolean {
        GLFWErrorCallback.createPrint(System.err).set()
        
        if (!glfwInit()) {
            logger.error("Failed to initialize GLFW")
            return false
        }
        
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        
        window = glfwCreateWindow(
            config.windowWidth,
            config.windowHeight,
            "PoorCraft Refactor v${config.version}",
            0,
            0
        )
        
        if (window == 0L) {
            logger.error("Failed to create GLFW window")
            return false
        }
        
        glfwSetKeyCallback(window) { _, key, _, action, _ ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true)
            }
            if (key == GLFW_KEY_GRAVE_ACCENT && action == GLFW_RELEASE && devMode) {
                // Toggle console (future implementation)
                logger.info("Console toggle requested")
            }
        }
        
        glfwSetFramebufferSizeCallback(window) { _, width, height ->
            glViewport(0, 0, width, height)
            renderer.onResize(width, height)
        }
        
        glfwMakeContextCurrent(window)
        glfwSwapInterval(if (config.vsync) 1 else 0)
        glfwShowWindow(window)
        
        GL.createCapabilities()
        
        logger.info("OpenGL Version: ${glGetString(GL_VERSION)}")
        logger.info("GLSL Version: ${glGetString(GL_SHADING_LANGUAGE_VERSION)}")
        
        return true
    }
    
    fun run() {
        running = true
        
        val targetTPS = 20.0
        val nsPerTick = 1_000_000_000.0 / targetTPS
        var lastTime = System.nanoTime()
        var delta = 0.0
        
        var lastFpsTime = System.currentTimeMillis()
        var frames = 0
        
        logger.info("Starting main loop")
        
        while (running && (headless || !glfwWindowShouldClose(window))) {
            val now = System.nanoTime()
            delta += (now - lastTime) / nsPerTick
            lastTime = now
            
            // Tick at fixed rate
            while (delta >= 1.0) {
                tick()
                delta--
            }
            
            if (!headless) {
                render()
                frames++
                
                glfwSwapBuffers(window)
                glfwPollEvents()
                
                // FPS counter
                if (System.currentTimeMillis() - lastFpsTime >= 1000) {
                    glfwSetWindowTitle(window, "PoorCraft Refactor v${config.version} - FPS: $frames")
                    frames = 0
                    lastFpsTime = System.currentTimeMillis()
                }
            } else {
                // Headless mode - just sleep a bit
                Thread.sleep(50)
                
                // Auto-stop after 10 seconds in headless mode
                if (tickCount > 200) {
                    logger.info("Headless test complete")
                    stop()
                }
            }
        }
        
        shutdown()
    }
    
    private fun tick() {
        tickCount++
        
        try {
            eventBus.dispatch("onTick", tickCount)
            worldManager.tick()
            modLoader.tick()
        } catch (e: Exception) {
            logger.error("Error during tick $tickCount", e)
        }
    }
    
    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        
        try {
            renderer.render(worldManager)
        } catch (e: Exception) {
            logger.error("Error during render", e)
        }
    }
    
    fun stop() {
        logger.info("Stopping engine")
        running = false
    }
    
    private fun shutdown() {
        logger.info("Shutting down engine")
        
        try {
            modLoader.shutdown()
            worldManager.shutdown()
            
            if (!headless) {
                renderer.cleanup()
                glfwDestroyWindow(window)
                glfwTerminate()
                glfwSetErrorCallback(null)?.free()
            }
        } catch (e: Exception) {
            logger.error("Error during shutdown", e)
        }
        
        logger.info("Engine shutdown complete")
    }
    
    fun getTickCount(): Long = tickCount
}
