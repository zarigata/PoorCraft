package com.poorcraft.modding;

import com.google.gson.JsonObject;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

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
    private final LuaModLoader loader;
    private Globals modGlobals;
    private LuaValue modTable;
    private LuaValue updateFunc;
    private ModState state;
    private LuaError lastError;
    private int failureCount;
    private boolean updateFailureLogged;
    private boolean treatFunctionsAsMethods;
    
    /**
     * Creates a new Lua mod container.
     * 
     * @param modDirectory Directory containing the mod
     * @param modJson Parsed mod.json
     * @param loader Mod loader reference
     */
    public LuaModContainer(File modDirectory, JsonObject modJson, LuaModLoader loader) {
        this.modDirectory = modDirectory;
        this.modJson = modJson;
        this.loader = loader;
        this.modGlobals = null;
        this.state = ModState.DISCOVERED;
        this.updateFunc = null;
        this.lastError = null;
        this.failureCount = 0;
        this.updateFailureLogged = false;
        this.treatFunctionsAsMethods = false;
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
        
        modGlobals = loader != null ? loader.createSandboxedGlobals() : JsePlatform.standardGlobals();

        LuaModAPI luaAPI = loader != null ? loader.createModAPI() : null;
        if (luaAPI != null) {
            modGlobals.set("api", luaAPI.toLuaValue(modGlobals));
        }

        try (FileInputStream fis = new FileInputStream(scriptFile)) {
            // Load and execute the Lua script
            modTable = modGlobals.load(fis, scriptFile.getName(), "t", modGlobals).call();
            // If the script returns a table, use that as the mod interface
            // Otherwise, use the global table
            if (modTable.isnil()) {
                modTable = modGlobals;
            }

            treatFunctionsAsMethods = modTable != null && modTable.istable();

            // Cache update function if present
            cacheUpdateFunction();
            
            state = ModState.LOADED;
            failureCount = 0;
            updateFailureLogged = false;
            lastError = null;
        }
    }
    
    /**
     * Calls the mod's init() function.
     */
    public void init() {
        try {
            LuaValue initFunc = modTable.get("init");
            if (!initFunc.isnil() && initFunc.isfunction()) {
                invokeFunction(initFunc);
                state = ModState.INITIALIZED;
                System.out.println("[LuaModContainer] Initialized mod: " + getName());
            } else {
                System.out.println("[LuaModContainer] No init() function in mod: " + getName());
                state = ModState.INITIALIZED; // Still mark as initialized
            }
        } catch (LuaError e) {
            System.err.println("[LuaModContainer] Error initializing mod " + getName() + ": " + e.getMessage());
            state = ModState.ERROR;
            lastError = e;
        }
    }
    
    /**
     * Calls the mod's enable() function.
     */
    public void enable() {
        try {
            LuaValue enableFunc = modTable.get("enable");
            if (!enableFunc.isnil() && enableFunc.isfunction()) {
                invokeFunction(enableFunc);
            }
            state = ModState.ENABLED;
            failureCount = 0;
            updateFailureLogged = false;
            lastError = null;
            System.out.println("[LuaModContainer] Enabled mod: " + getName());
        } catch (LuaError e) {
            System.err.println("[LuaModContainer] Error enabling mod " + getName() + ": " + e.getMessage());
            state = ModState.ERROR;
            lastError = e;
        }
    }
    
    /**
     * Calls the mod's disable() function.
     */
    public void disable() {
        try {
            LuaValue disableFunc = modTable.get("disable");
            if (!disableFunc.isnil() && disableFunc.isfunction()) {
                invokeFunction(disableFunc);
            }
            lastError = null;
            System.out.println("[LuaModContainer] Disabled mod: " + getName());
        } catch (LuaError e) {
            lastError = e;
            System.err.println("[LuaModContainer] Error disabling mod " + getName() + ": " + e.getMessage());
        } finally {
            state = ModState.DISABLED;
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

    private void invokeFunction(LuaValue function, LuaValue... args) {
        if (function == null || function.isnil() || !function.isfunction()) {
            return;
        }

        if (treatFunctionsAsMethods && modTable != null && modTable.istable()) {
            LuaValue[] callArgs = new LuaValue[args.length + 1];
            callArgs[0] = modTable;
            System.arraycopy(args, 0, callArgs, 1, args.length);
            try {
                function.invoke(LuaValue.varargsOf(callArgs));
                return;
            } catch (LuaError colonError) {
                if (args.length == 0) {
                    throw colonError;
                }
                try {
                    invokeWithoutSelf(function, args);
                    return;
                } catch (LuaError dotError) {
                    throw colonError;
                }
            }
        }

        invokeWithoutSelf(function, args);
    }

    private void invokeWithoutSelf(LuaValue function, LuaValue... args) {
        if (args.length == 0) {
            function.call();
        } else if (args.length == 1) {
            function.call(args[0]);
        } else {
            function.invoke(LuaValue.varargsOf(args));
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
            // Call cached update function without table lookup, supporting colon syntax
            invokeFunction(updateFunc, LuaValue.valueOf(deltaTime));
        } catch (LuaError e) {
            lastError = e;
            failureCount++;
            if (failureCount >= 1) {
                state = ModState.ERROR;
                if (!updateFailureLogged) {
                    System.err.println("[LuaModContainer] Error updating mod " + getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    updateFailureLogged = true;
                }
            }
        }
    }

    public void markError(String phase, Exception cause) {
        state = ModState.ERROR;
        failureCount++;
        updateFailureLogged = true;
        if (cause instanceof LuaError luaError) {
            lastError = luaError;
        } else {
            String message = (cause != null && cause.getMessage() != null)
                ? cause.getMessage()
                : ("Mod error during " + phase);
            lastError = new LuaError(message);
        }
        String phaseLabel = (phase != null && !phase.isBlank()) ? phase : "unknown";
        System.err.println("[LuaModContainer] Marking mod '" + getName()
            + "' as ERROR during phase '" + phaseLabel + "'.");
        if (cause != null) {
            cause.printStackTrace();
        }
    }
    
    public String getId() {
        return modJson.has("id") ? modJson.get("id").getAsString() : modDirectory.getName();
    }

    public String getName() {
        return modJson.has("name") ? modJson.get("name").getAsString() : getId();
    }

    public String getVersion() {
        return modJson.has("version") ? modJson.get("version").getAsString() : "0.0.0";
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

    public int getFailureCount() {
        return failureCount;
    }

    public LuaError getLastError() {
        return lastError;
    }

    public boolean isHealthy() {
        return state != ModState.ERROR && state != ModState.DISABLED;
    }

    public boolean shouldLoad() {
        return modJson.has("enabled") ? modJson.get("enabled").getAsBoolean() : true;
    }
}
