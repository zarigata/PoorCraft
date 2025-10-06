package com.poorcraft.modding;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.poorcraft.core.Game;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Lua mod loader that discovers, loads, and manages Lua mods.
 * 
 * <p>The LuaModLoader replaces the Python-based modding system with Lua,
 * making the game more portable and easier to distribute as a single executable.
 * 
 * <p><b>Lifecycle:</b>
 * <ol>
 *   <li>init() - Create Lua environment, discover and load mods</li>
 *   <li>Mods are initialized (init() called)</li>
 *   <li>Mods are enabled (enable() called)</li>
 *   <li>Game runs with mods active</li>
 *   <li>shutdown() - Disable mods, cleanup Lua environment</li>
 * </ol>
 * 
 * @author PoorCraft Team
 * @version 2.0
 */
public class LuaModLoader {
    
    private final File modsDirectory;
    private final List<LuaModContainer> loadedMods;
    private final ModAPI modAPI;
    private final EventBus eventBus;
    private final Game game;
    private final Globals luaGlobals;
    
    /**
     * Creates a new Lua mod loader.
     * 
     * @param game Reference to the game instance
     */
    public LuaModLoader(Game game) {
        this.game = game;
        this.modsDirectory = new File("gamedata/mods");
        this.loadedMods = new ArrayList<>();
        this.eventBus = new EventBus();
        this.modAPI = new ModAPI(game, eventBus);
        this.luaGlobals = JsePlatform.standardGlobals();
        
        // Set up Lua environment
        setupLuaEnvironment();
    }
    
    /**
     * Sets up the Lua environment with the mod API.
     */
    private void setupLuaEnvironment() {
        // Create Lua API bridge
        LuaModAPI luaAPI = new LuaModAPI(modAPI);
        
        // Expose API to Lua
        luaGlobals.set("api", luaAPI.toLuaValue(luaGlobals));
        
        System.out.println("[LuaModLoader] Lua environment initialized");
    }
    
    /**
     * Initializes the mod loader.
     */
    public void init() {
        System.out.println("[LuaModLoader] Initializing Lua mod loader...");
        
        // Create mods directory if it doesn't exist
        if (!modsDirectory.exists()) {
            modsDirectory.mkdirs();
            System.out.println("[LuaModLoader] Created mods directory at " + modsDirectory.getAbsolutePath());
        }
        
        // Discover mods
        discoverMods();
        
        // Load mods
        loadMods();
        
        // Initialize mods
        initializeMods();
        
        // Enable mods
        enableMods();
        
        System.out.println("[LuaModLoader] Loaded " + loadedMods.size() + " Lua mods");
    }
    
    /**
     * Discovers mods by scanning the gamedata/mods/ directory.
     */
    private void discoverMods() {
        System.out.println("[LuaModLoader] Discovering mods in " + modsDirectory.getAbsolutePath());
        
        File[] modDirs = modsDirectory.listFiles(File::isDirectory);
        if (modDirs == null || modDirs.length == 0) {
            System.out.println("[LuaModLoader] No mod directories found");
            return;
        }
        
        Gson gson = new Gson();
        
        for (File modDir : modDirs) {
            File modJsonFile = new File(modDir, "mod.json");
            
            if (!modJsonFile.exists()) {
                System.out.println("[LuaModLoader] Skipping " + modDir.getName() + " (no mod.json)");
                continue;
            }
            
            try {
                // Parse mod.json
                JsonObject modJson = gson.fromJson(new FileReader(modJsonFile), JsonObject.class);
                
                // Create mod container
                LuaModContainer container = new LuaModContainer(modDir, modJson, luaGlobals);
                loadedMods.add(container);
                
                System.out.println("[LuaModLoader] Discovered mod: " + container.getName() + " v" + container.getVersion());
                
            } catch (Exception e) {
                System.err.println("[LuaModLoader] Failed to load mod.json from " + modDir.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("[LuaModLoader] Discovered " + loadedMods.size() + " mods");
    }
    
    /**
     * Loads Lua scripts for all discovered mods.
     */
    private void loadMods() {
        System.out.println("[LuaModLoader] Loading Lua scripts...");
        
        for (LuaModContainer container : loadedMods) {
            try {
                if (!container.shouldLoad()) {
                    System.out.println("[LuaModLoader] Skipping mod " + container.getName() + " (disabled)");
                    continue;
                }
                
                container.load();
                System.out.println("[LuaModLoader] Loaded Lua script for: " + container.getName());
                
            } catch (Exception e) {
                System.err.println("[LuaModLoader] Failed to load mod " + container.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Initializes all loaded mods by calling their init() function.
     */
    private void initializeMods() {
        System.out.println("[LuaModLoader] Initializing mods...");
        
        for (LuaModContainer container : loadedMods) {
            try {
                if (container.getState() == LuaModContainer.ModState.LOADED) {
                    container.init();
                }
            } catch (Exception e) {
                System.err.println("[LuaModLoader] Failed to initialize mod " + container.getName());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Enables all initialized mods by calling their enable() function.
     */
    private void enableMods() {
        System.out.println("[LuaModLoader] Enabling mods...");
        
        for (LuaModContainer container : loadedMods) {
            try {
                if (container.getState() == LuaModContainer.ModState.INITIALIZED) {
                    container.enable();
                }
            } catch (Exception e) {
                System.err.println("[LuaModLoader] Failed to enable mod " + container.getName());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Shuts down the mod loader.
     */
    public void shutdown() {
        System.out.println("[LuaModLoader] Shutting down mod loader...");
        
        // Disable all mods
        for (LuaModContainer container : loadedMods) {
            try {
                if (container.getState() == LuaModContainer.ModState.ENABLED) {
                    container.disable();
                }
            } catch (Exception e) {
                System.err.println("[LuaModLoader] Error disabling mod " + container.getName());
            }
        }
        
        // Shutdown event bus
        eventBus.shutdown();
        
        // Clear loaded mods
        loadedMods.clear();
        
        System.out.println("[LuaModLoader] Mod loader shut down");
    }
    
    /**
     * Reloads all mods.
     */
    public void reloadMods() {
        System.out.println("[LuaModLoader] Reloading mods...");
        shutdown();
        init();
    }
    
    /**
     * Gets a mod by its ID.
     * 
     * @param id Mod ID
     * @return LuaModContainer, or null if not found
     */
    public LuaModContainer getModById(String id) {
        for (LuaModContainer container : loadedMods) {
            if (container.getId().equals(id)) {
                return container;
            }
        }
        return null;
    }
    
    /**
     * Gets the list of loaded mods.
     * 
     * @return List of mod containers
     */
    public List<LuaModContainer> getLoadedMods() {
        return new ArrayList<>(loadedMods);
    }
    
    /**
     * Gets the event bus.
     * 
     * @return EventBus instance
     */
    public EventBus getEventBus() {
        return eventBus;
    }
    
    /**
     * Gets the mod API.
     * 
     * @return ModAPI instance
     */
    public ModAPI getModAPI() {
        return modAPI;
    }
}
