package com.poorcraft.modding;

import com.google.gson.JsonObject;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Container class that holds metadata and state for a loaded mod.
 * 
 * <p>Each mod has a ModContainer that tracks:
 * <ul>
 *   <li>Metadata (ID, name, version, author, etc.)</li>
 *   <li>Configuration from mod.json</li>
 *   <li>Python module reference (Py4J proxy)</li>
 *   <li>Lifecycle state (unloaded, loaded, initialized, enabled, etc.)</li>
 * </ul>
 * 
 * <p><b>Lifecycle states:</b>
 * <ol>
 *   <li>UNLOADED - Mod discovered but not loaded</li>
 *   <li>LOADED - Python module imported</li>
 *   <li>INITIALIZED - init() called</li>
 *   <li>ENABLED - enable() called, mod is active</li>
 *   <li>DISABLED - disable() called, mod is inactive</li>
 *   <li>ERROR - Mod failed to load or threw exception</li>
 * </ol>
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class ModContainer {
    
    /**
     * Mod lifecycle states.
     */
    public enum ModState {
        UNLOADED,
        LOADED,
        INITIALIZED,
        ENABLED,
        DISABLED,
        ERROR
    }
    
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String author;
    private final String mainModule;
    private final boolean enabled;
    private final boolean serverOnly;
    private final JsonObject config;
    private final File modDirectory;
    
    private Object pythonModule;
    private ModState state;
    
    /**
     * Creates a new mod container from mod.json data.
     * 
     * @param modDirectory Directory containing the mod files
     * @param modJson Parsed mod.json object
     */
    public ModContainer(File modDirectory, JsonObject modJson) {
        this.modDirectory = modDirectory;
        this.id = modJson.get("id").getAsString();
        this.name = modJson.get("name").getAsString();
        this.version = modJson.get("version").getAsString();
        this.description = modJson.has("description") ? modJson.get("description").getAsString() : "";
        this.author = modJson.has("author") ? modJson.get("author").getAsString() : "Unknown";
        this.mainModule = modJson.get("main").getAsString();
        this.enabled = modJson.has("enabled") ? modJson.get("enabled").getAsBoolean() : true;
        this.serverOnly = modJson.has("server_only") ? modJson.get("server_only").getAsBoolean() : false;
        this.config = modJson.has("config") ? modJson.getAsJsonObject("config") : new JsonObject();
        
        this.pythonModule = null;
        this.state = ModState.UNLOADED;
    }
    
    /**
     * Loads the Python module.
     * 
     * @param pythonModule Python module reference (Py4J proxy)
     */
    public void load(Object pythonModule) {
        this.pythonModule = pythonModule;
        this.state = ModState.LOADED;
        System.out.println("[ModContainer] Loaded mod: " + name + " v" + version);
    }
    
    /**
     * Initializes the mod by calling its init() function.
     * 
     * @throws Exception if initialization fails
     */
    public void init() throws Exception {
        try {
            System.out.println("[ModContainer] Initializing mod: " + name);
            
            // Call Python module's init() function
            if (pythonModule != null) {
                callPythonFunction("init");
            }
            
            this.state = ModState.INITIALIZED;
            System.out.println("[ModContainer] Initialized mod: " + name);
            
        } catch (Exception e) {
            this.state = ModState.ERROR;
            System.err.println("[ModContainer] Failed to initialize mod " + name + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Enables the mod by calling its enable() function (if it exists).
     */
    public void enable() {
        try {
            System.out.println("[ModContainer] Enabling mod: " + name);
            
            // Call Python module's enable() function if it exists
            if (pythonModule != null) {
                try {
                    callPythonFunction("enable");
                } catch (NoSuchMethodException e) {
                    // enable() is optional, so this is fine
                    System.out.println("[ModContainer] Mod " + name + " has no enable() function (optional)");
                }
            }
            
            this.state = ModState.ENABLED;
            System.out.println("[ModContainer] Enabled mod: " + name);
            
        } catch (Exception e) {
            this.state = ModState.ERROR;
            System.err.println("[ModContainer] Failed to enable mod " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Disables the mod by calling its disable() function (if it exists).
     */
    public void disable() {
        try {
            System.out.println("[ModContainer] Disabling mod: " + name);
            
            // Call Python module's disable() function if it exists
            if (pythonModule != null) {
                try {
                    callPythonFunction("disable");
                } catch (NoSuchMethodException e) {
                    // disable() is optional, so this is fine
                    System.out.println("[ModContainer] Mod " + name + " has no disable() function (optional)");
                }
            }
            
            this.state = ModState.DISABLED;
            System.out.println("[ModContainer] Disabled mod: " + name);
            
        } catch (Exception e) {
            System.err.println("[ModContainer] Failed to disable mod " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calls a Python function on the module.
     * 
     * @param functionName Name of the function to call
     * @throws Exception if function call fails
     */
    private void callPythonFunction(String functionName) throws Exception {
        if (pythonModule == null) {
            throw new IllegalStateException("Python module not loaded");
        }
        
        try {
            // Use reflection to call the function on the Python module
            Method method = pythonModule.getClass().getMethod(functionName);
            method.invoke(pythonModule);
        } catch (NoSuchMethodException e) {
            // Function doesn't exist
            throw e;
        } catch (Exception e) {
            throw new Exception("Error calling " + functionName + "(): " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if this mod should load on the server.
     * 
     * @return true if mod should load on server
     */
    public boolean shouldLoadOnServer() {
        return enabled;  // Server-only mods always load on server if enabled
    }
    
    /**
     * Checks if this mod should load on the client.
     * 
     * @return true if mod should load on client
     */
    public boolean shouldLoadOnClient() {
        return enabled && !serverOnly;  // Server-only mods don't load on client
    }
    
    // ========== Getters ==========
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public String getMainModule() {
        return mainModule;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isServerOnly() {
        return serverOnly;
    }
    
    public JsonObject getConfig() {
        return config;
    }
    
    public File getModDirectory() {
        return modDirectory;
    }
    
    public Object getPythonModule() {
        return pythonModule;
    }
    
    public ModState getState() {
        return state;
    }
}
