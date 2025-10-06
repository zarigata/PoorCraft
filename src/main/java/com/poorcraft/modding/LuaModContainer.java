package com.poorcraft.modding;

import com.google.gson.JsonObject;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Container for a Lua mod, managing its lifecycle and state.
 * 
 * @author PoorCraft Team
 * @version 2.0
 */
public class LuaModContainer {
    
    /**
     * Mod lifecycle states.
     */
    public enum ModState {
        DISCOVERED,   // mod.json found
        LOADED,       // Lua script loaded
        INITIALIZED,  // init() called
        ENABLED,      // enable() called
        DISABLED,     // disable() called
        ERROR         // Error occurred
    }
    
    private final File modDirectory;
    private final JsonObject modJson;
    private final Globals luaGlobals;
    private LuaValue modTable;
    private LuaValue updateFunc;
    private ModState state;
    
    /**
     * Creates a new Lua mod container.
     * 
     * @param modDirectory Directory containing the mod
     * @param modJson Parsed mod.json
     * @param luaGlobals Shared Lua globals
     */
    public LuaModContainer(File modDirectory, JsonObject modJson, Globals luaGlobals) {
        this.modDirectory = modDirectory;
        this.modJson = modJson;
        this.luaGlobals = luaGlobals;
        this.state = ModState.DISCOVERED;
        this.updateFunc = null;
    }
    
    /**
     * Loads the Lua script.
     */
    public void load() throws IOException, LuaError {
        String mainScript = getMainScript();
        File scriptFile = new File(modDirectory, mainScript);
        
        if (!scriptFile.exists()) {
            throw new IOException("Main script not found: " + mainScript);
        }
        
        try (FileInputStream fis = new FileInputStream(scriptFile)) {
            // Load and execute the Lua script
            modTable = luaGlobals.load(fis, scriptFile.getName(), "t", luaGlobals).call();
            
            // If the script returns a table, use that as the mod interface
            // Otherwise, use the global table
            if (modTable.isnil()) {
                modTable = luaGlobals;
            }
            
            // Cache update function if present
            cacheUpdateFunction();
            
            state = ModState.LOADED;
        }
    }
    
    /**
     * Calls the mod's init() function.
     */
    public void init() {
        try {
            LuaValue initFunc = modTable.get("init");
            if (!initFunc.isnil() && initFunc.isfunction()) {
                initFunc.call();
                state = ModState.INITIALIZED;
                System.out.println("[LuaModContainer] Initialized mod: " + getName());
            } else {
                System.out.println("[LuaModContainer] No init() function in mod: " + getName());
                state = ModState.INITIALIZED; // Still mark as initialized
            }
        } catch (LuaError e) {
            System.err.println("[LuaModContainer] Error initializing mod " + getName() + ": " + e.getMessage());
            state = ModState.ERROR;
            throw e;
        }
    }
    
    /**
     * Calls the mod's enable() function.
     */
    public void enable() {
        try {
            LuaValue enableFunc = modTable.get("enable");
            if (!enableFunc.isnil() && enableFunc.isfunction()) {
                enableFunc.call();
            }
            state = ModState.ENABLED;
            System.out.println("[LuaModContainer] Enabled mod: " + getName());
        } catch (LuaError e) {
            System.err.println("[LuaModContainer] Error enabling mod " + getName() + ": " + e.getMessage());
            state = ModState.ERROR;
            throw e;
        }
    }
    
    /**
     * Calls the mod's disable() function.
     */
    public void disable() {
        try {
            LuaValue disableFunc = modTable.get("disable");
            if (!disableFunc.isnil() && disableFunc.isfunction()) {
                disableFunc.call();
            }
            state = ModState.DISABLED;
            System.out.println("[LuaModContainer] Disabled mod: " + getName());
        } catch (LuaError e) {
            System.err.println("[LuaModContainer] Error disabling mod " + getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Caches the update function reference if present.
     */
    private void cacheUpdateFunction() {
        if (modTable != null && !modTable.isnil()) {
            LuaValue func = modTable.get("update");
            if (!func.isnil() && func.isfunction()) {
                updateFunc = func;
            } else {
                updateFunc = null;
            }
        } else {
            updateFunc = null;
        }
    }
    
    /**
     * Checks if the mod has an update() function.
     * 
     * @return true if the mod table contains an update function
     */
    public boolean hasUpdateFunction() {
        return updateFunc != null && updateFunc.isfunction();
    }
    
    /**
     * Calls the mod's update(deltaTime) function if it exists and mod is enabled.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public void update(float deltaTime) {
        // Only update if mod is enabled
        if (state != ModState.ENABLED) {
            return;
        }
        
        // Check if mod has an update function (using cached reference)
        if (updateFunc == null || !updateFunc.isfunction()) {
            return;
        }
        
        try {
            // Call cached update function without table lookup
            updateFunc.call(LuaValue.valueOf(deltaTime));
        } catch (LuaError e) {
            System.err.println("[LuaModContainer] Error updating mod " + getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if the mod should load based on its enabled flag.
     */
    public boolean shouldLoad() {
        return modJson.has("enabled") ? modJson.get("enabled").getAsBoolean() : true;
    }
    
    // Getters
    
    public String getId() {
        return modJson.get("id").getAsString();
    }
    
    public String getName() {
        return modJson.get("name").getAsString();
    }
    
    public String getVersion() {
        return modJson.get("version").getAsString();
    }
    
    public String getMainScript() {
        return modJson.has("main") ? modJson.get("main").getAsString() : "main.lua";
    }
    
    public File getModDirectory() {
        return modDirectory;
    }
    
    public JsonObject getModJson() {
        return modJson;
    }
    
    public JsonObject getConfig() {
        return modJson.has("config") ? modJson.getAsJsonObject("config") : new JsonObject();
    }
    
    public ModState getState() {
        return state;
    }
    
    public LuaValue getModTable() {
        return modTable;
    }
}
