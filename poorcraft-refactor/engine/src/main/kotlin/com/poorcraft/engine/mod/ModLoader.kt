package com.poorcraft.engine.mod

import com.google.gson.Gson
import com.poorcraft.engine.core.Engine
import com.poorcraft.engine.event.EventBus
import org.luaj.vm2.*
import org.luaj.vm2.lib.jse.JsePlatform
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * Loads and manages Lua mods with sandboxing
 */
class ModLoader(
    private val modsDirectory: Path,
    private val eventBus: EventBus,
    private val engine: Engine
) {
    private val logger = LoggerFactory.getLogger(ModLoader::class.java)
    private val gson = Gson()
    private val loadedMods = mutableListOf<LoadedMod>()
    
    data class ModManifest(
        val id: String,
        val name: String,
        val version: String,
        val author: String = "Unknown",
        val main: String = "main.lua",
        val entry: String = "onEnable"
    )
    
    data class LoadedMod(
        val manifest: ModManifest,
        val globals: Globals,
        val scriptPath: Path,
        var enabled: Boolean = false,
        var failed: Boolean = false
    )
    
    fun loadMods() {
        logger.info("Loading mods from: $modsDirectory")
        
        if (!Files.exists(modsDirectory)) {
            logger.warn("Mods directory does not exist")
            return
        }
        
        Files.list(modsDirectory).use { stream ->
            stream.filter { Files.isDirectory(it) }.forEach { modDir ->
                try {
                    loadMod(modDir)
                } catch (e: Exception) {
                    logger.error("Failed to load mod from ${modDir.name}", e)
                }
            }
        }
        
        logger.info("Loaded ${loadedMods.size} mods")
    }
    
    private fun loadMod(modDir: Path) {
        val manifestPath = modDir.resolve("mod.json")
        if (!manifestPath.exists()) {
            logger.warn("No mod.json found in ${modDir.name}")
            return
        }
        
        val manifest = try {
            val json = Files.readString(manifestPath)
            gson.fromJson(json, ModManifest::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse mod.json in ${modDir.name}", e)
            return
        }
        
        val scriptPath = modDir.resolve(manifest.main)
        if (!scriptPath.exists()) {
            logger.error("Main script ${manifest.main} not found in ${modDir.name}")
            return
        }
        
        // Create sandboxed Lua environment
        val globals = createSandboxedGlobals(manifest.id, modDir)
        
        val loadedMod = LoadedMod(manifest, globals, scriptPath)
        loadedMods.add(loadedMod)
        
        // Load and execute the script
        try {
            val script = Files.readString(scriptPath)
            val chunk = globals.load(script, manifest.id)
            chunk.call()
            
            // Call onLoad
            callModFunction(loadedMod, "onLoad")
            
            // Call entry point (default: onEnable)
            callModFunction(loadedMod, manifest.entry)
            loadedMod.enabled = true
            
            logger.info("Loaded mod: ${manifest.name} v${manifest.version} by ${manifest.author}")
        } catch (e: Exception) {
            logger.error("Failed to execute mod ${manifest.id}", e)
            loadedMod.failed = true
        }
    }
    
    private fun createSandboxedGlobals(modId: String, modDir: Path): Globals {
        val globals = JsePlatform.standardGlobals()
        
        // Remove dangerous functions
        globals.set("os", LuaValue.NIL)
        globals.set("io", LuaValue.NIL)
        globals.set("package", LuaValue.NIL)
        globals.set("require", LuaValue.NIL)
        globals.set("dofile", LuaValue.NIL)
        globals.set("loadfile", LuaValue.NIL)
        globals.set("module", LuaValue.NIL)
        
        // Add safe API
        globals.set("Engine", createEngineAPI(modId))
        globals.set("Logger", createLoggerAPI(modId))
        globals.set("FileAPI", createFileAPI(modId, modDir))
        
        return globals
    }
    
    private fun createEngineAPI(modId: String): LuaTable {
        val api = LuaTable()
        
        // registerEvent(eventName, function)
        api.set("registerEvent", object : TwoArgFunction() {
            override fun call(eventName: LuaValue, callback: LuaValue): LuaValue {
                if (!callback.isfunction()) {
                    throw LuaError("Second argument must be a function")
                }
                
                eventBus.register(eventName.tojstring()) { args ->
                    try {
                        val luaArgs = args.map { toLuaValue(it) }.toTypedArray()
                        callback.invoke(LuaValue.varargsOf(luaArgs))
                    } catch (e: Exception) {
                        logger.error("Error in mod $modId event handler", e)
                    }
                }
                
                return LuaValue.NIL
            }
        })
        
        // schedule(delayTicks, function)
        api.set("schedule", object : TwoArgFunction() {
            override fun call(delay: LuaValue, callback: LuaValue): LuaValue {
                if (!callback.isfunction()) {
                    throw LuaError("Second argument must be a function")
                }
                
                val targetTick = engine.getTickCount() + delay.tolong()
                eventBus.register("onTick") { args ->
                    val currentTick = args[0] as Long
                    if (currentTick >= targetTick) {
                        try {
                            callback.call()
                        } catch (e: Exception) {
                            logger.error("Error in mod $modId scheduled callback", e)
                        }
                    }
                }
                
                return LuaValue.NIL
            }
        })
        
        // getTime()
        api.set("getTime", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(engine.getTickCount())
            }
        })
        
        // sendChat(player, message) - placeholder
        api.set("sendChat", object : TwoArgFunction() {
            override fun call(player: LuaValue, message: LuaValue): LuaValue {
                logger.info("[CHAT] ${message.tojstring()}")
                return LuaValue.NIL
            }
        })
        
        return api
    }
    
    private fun createLoggerAPI(modId: String): LuaTable {
        val api = LuaTable()
        val modLogger = LoggerFactory.getLogger("Mod:$modId")
        
        api.set("info", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                modLogger.info(message.tojstring())
                return LuaValue.NIL
            }
        })
        
        api.set("warn", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                modLogger.warn(message.tojstring())
                return LuaValue.NIL
            }
        })
        
        api.set("error", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                modLogger.error(message.tojstring())
                return LuaValue.NIL
            }
        })
        
        return api
    }
    
    private fun createFileAPI(modId: String, modDir: Path): LuaTable {
        val api = LuaTable()
        
        // read(path) - restricted to mod directory
        api.set("read", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val filePath = modDir.resolve(path.tojstring()).normalize()
                if (!filePath.startsWith(modDir)) {
                    throw LuaError("Access denied: path outside mod directory")
                }
                
                return try {
                    LuaValue.valueOf(Files.readString(filePath))
                } catch (e: Exception) {
                    logger.error("Mod $modId failed to read file", e)
                    LuaValue.NIL
                }
            }
        })
        
        // write(path, data) - restricted to mod directory
        api.set("write", object : TwoArgFunction() {
            override fun call(path: LuaValue, data: LuaValue): LuaValue {
                val filePath = modDir.resolve(path.tojstring()).normalize()
                if (!filePath.startsWith(modDir)) {
                    throw LuaError("Access denied: path outside mod directory")
                }
                
                return try {
                    Files.writeString(filePath, data.tojstring())
                    LuaValue.TRUE
                } catch (e: Exception) {
                    logger.error("Mod $modId failed to write file", e)
                    LuaValue.FALSE
                }
            }
        })
        
        return api
    }
    
    private fun toLuaValue(obj: Any?): LuaValue {
        return when (obj) {
            null -> LuaValue.NIL
            is String -> LuaValue.valueOf(obj)
            is Int -> LuaValue.valueOf(obj)
            is Long -> LuaValue.valueOf(obj.toDouble())
            is Double -> LuaValue.valueOf(obj)
            is Boolean -> LuaValue.valueOf(obj)
            else -> LuaValue.userdataOf(obj)
        }
    }
    
    private fun callModFunction(mod: LoadedMod, functionName: String) {
        if (mod.failed) return
        
        try {
            val function = mod.globals.get(functionName)
            if (function.isfunction()) {
                function.call()
            }
        } catch (e: Exception) {
            logger.error("Error calling $functionName in mod ${mod.manifest.id}", e)
            mod.failed = true
        }
    }
    
    fun tick() {
        for (mod in loadedMods) {
            if (!mod.enabled || mod.failed) continue
            
            try {
                callModFunction(mod, "onTick")
            } catch (e: Exception) {
                logger.error("Error in mod ${mod.manifest.id} tick", e)
                mod.failed = true
                mod.enabled = false
            }
        }
    }
    
    fun shutdown() {
        logger.info("Shutting down mods")
        
        for (mod in loadedMods) {
            if (!mod.enabled) continue
            
            try {
                callModFunction(mod, "onDisable")
            } catch (e: Exception) {
                logger.error("Error disabling mod ${mod.manifest.id}", e)
            }
        }
        
        loadedMods.clear()
    }
    
    fun getLoadedMods(): List<LoadedMod> = loadedMods.toList()
}
