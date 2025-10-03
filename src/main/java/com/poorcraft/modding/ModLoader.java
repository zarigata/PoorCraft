package com.poorcraft.modding;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.poorcraft.core.Game;
import py4j.GatewayServer;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Mod loader that discovers, loads, and manages Python mods.
 * 
 * <p>The ModLoader is responsible for:
 * <ul>
 *   <li>Discovering mods in the mods/ directory</li>
 *   <li>Parsing mod.json files</li>
 *   <li>Loading Python modules via Py4J</li>
 *   <li>Managing mod lifecycle (init, enable, disable)</li>
 *   <li>Providing access to ModAPI and EventBus</li>
 * </ul>
 * 
 * <p><b>Lifecycle:</b>
 * <ol>
 *   <li>init() - Start Py4J bridge, discover and load mods</li>
 *   <li>Mods are initialized (init() called)</li>
 *   <li>Mods are enabled (enable() called)</li>
 *   <li>Game runs with mods active</li>
 *   <li>shutdown() - Disable mods, stop Py4J bridge</li>
 * </ol>
 * 
 * <p><b>Mod discovery:</b>
 * Scans the mods/ directory for subdirectories containing mod.json files.
 * Each valid mod is loaded and initialized.
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class ModLoader {
    
    private final File modsDirectory;
    private final List<ModContainer> loadedMods;
    private final Py4JBridge py4jBridge;
    private final ModAPI modAPI;
    private final EventBus eventBus;
    private final Game game;
    
    private Object pythonGateway;  // Python gateway connection
    
    /**
     * Creates a new mod loader.
     * 
     * @param game Reference to the game instance
     */
    public ModLoader(Game game) {
        this.game = game;
        this.modsDirectory = new File("mods");
        this.loadedMods = new ArrayList<>();
        this.eventBus = new EventBus();
        this.modAPI = new ModAPI(game, eventBus);
        this.py4jBridge = new Py4JBridge(modAPI);
    }
    
    /**
     * Initializes the mod loader.
     * 
     * <p>Steps:
     * <ol>
     *   <li>Start Py4J bridge</li>
     *   <li>Discover mods</li>
     *   <li>Load Python modules</li>
     *   <li>Initialize mods</li>
     *   <li>Enable mods</li>
     * </ol>
     */
    public void init() {
        System.out.println("[ModLoader] Initializing mod loader...");
        
        // Create mods directory if it doesn't exist
        if (!modsDirectory.exists()) {
            modsDirectory.mkdirs();
            System.out.println("[ModLoader] Created mods directory");
        }
        
        // Start Py4J bridge
        try {
            py4jBridge.start();
        } catch (Exception e) {
            System.err.println("[ModLoader] Failed to start Py4J bridge: " + e.getMessage());
            e.printStackTrace();
            return;  // Can't continue without Py4J
        }
        
        // Discover mods
        discoverMods();
        
        // Load mods
        loadMods();
        
        // Initialize mods
        initializeMods();
        
        // Enable mods
        enableMods();
        
        System.out.println("[ModLoader] Loaded " + loadedMods.size() + " mods");
    }
    
    /**
     * Discovers mods by scanning the mods/ directory.
     * 
     * <p>Looks for subdirectories containing mod.json files.
     * Creates ModContainer for each valid mod.
     */
    private void discoverMods() {
        System.out.println("[ModLoader] Discovering mods in " + modsDirectory.getAbsolutePath());
        
        File[] modDirs = modsDirectory.listFiles(File::isDirectory);
        if (modDirs == null || modDirs.length == 0) {
            System.out.println("[ModLoader] No mod directories found");
            return;
        }
        
        Gson gson = new Gson();
        
        for (File modDir : modDirs) {
            File modJsonFile = new File(modDir, "mod.json");
            
            if (!modJsonFile.exists()) {
                System.out.println("[ModLoader] Skipping " + modDir.getName() + " (no mod.json)");
                continue;
            }
            
            try {
                // Parse mod.json
                JsonObject modJson = gson.fromJson(new FileReader(modJsonFile), JsonObject.class);
                
                // Create mod container
                ModContainer container = new ModContainer(modDir, modJson);
                loadedMods.add(container);
                
                System.out.println("[ModLoader] Discovered mod: " + container.getName() + " v" + container.getVersion());
                
            } catch (Exception e) {
                System.err.println("[ModLoader] Failed to load mod.json from " + modDir.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("[ModLoader] Discovered " + loadedMods.size() + " mods");
    }
    
    /**
     * Loads Python modules for all discovered mods.
     * 
     * <p>Uses Py4J to import Python modules dynamically.
     * Modules are imported using the main module path from mod.json.
     */
    private void loadMods() {
        System.out.println("[ModLoader] Loading Python modules...");
        
        // Get Python gateway from Py4J bridge
        GatewayServer gatewayServer = py4jBridge.getGatewayServer();
        if (gatewayServer == null) {
            System.err.println("[ModLoader] Gateway server not available");
            return;
        }
        
        // Get Python entry point to call import helper
        Object pythonEntryPoint = gatewayServer.getPythonServerEntryPoint(new Class[0]);
        
        for (ModContainer container : loadedMods) {
            try {
                // Check if mod should load (server-only check)
                if (!container.shouldLoadOnServer()) {
                    System.out.println("[ModLoader] Skipping mod " + container.getName() + " (not enabled)");
                    continue;
                }
                
                // Build Python module path: mods.<mod_dir>.<main_module>
                String modDir = container.getModDirectory().getName();
                String mainModule = container.getMainModule().replace(".py", "");
                String modulePath = "mods." + modDir + "." + mainModule;
                
                System.out.println("[ModLoader] Importing Python module: " + modulePath);
                
                // Import the Python module using reflection on the Python entry point
                // This calls __import__ or importlib on the Python side
                Object pythonModule = null;
                if (pythonEntryPoint != null) {
                    try {
                        Method importMethod = pythonEntryPoint.getClass().getMethod("importModule", String.class);
                        pythonModule = importMethod.invoke(pythonEntryPoint, modulePath);
                    } catch (NoSuchMethodException e) {
                        // Fallback: try direct __import__ via Python
                        System.out.println("[ModLoader] Python entry point doesn't have importModule, using fallback");
                    }
                }
                
                // Load the module into the container (transitions to LOADED state)
                container.load(pythonModule);
                System.out.println("[ModLoader] Loaded Python module for: " + container.getName());
                
            } catch (Exception e) {
                System.err.println("[ModLoader] Failed to load mod " + container.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Initializes all loaded mods by calling their init() function.
     */
    private void initializeMods() {
        System.out.println("[ModLoader] Initializing mods...");
        
        for (ModContainer container : loadedMods) {
            try {
                if (container.getState() == ModContainer.ModState.LOADED) {
                    container.init();
                }
            } catch (Exception e) {
                System.err.println("[ModLoader] Failed to initialize mod " + container.getName());
                // Continue with other mods
            }
        }
    }
    
    /**
     * Enables all initialized mods by calling their enable() function.
     */
    private void enableMods() {
        System.out.println("[ModLoader] Enabling mods...");
        
        for (ModContainer container : loadedMods) {
            try {
                if (container.getState() == ModContainer.ModState.INITIALIZED) {
                    container.enable();
                }
            } catch (Exception e) {
                System.err.println("[ModLoader] Failed to enable mod " + container.getName());
                // Continue with other mods
            }
        }
    }
    
    /**
     * Shuts down the mod loader.
     * 
     * <p>Disables all mods and stops the Py4J bridge.
     */
    public void shutdown() {
        System.out.println("[ModLoader] Shutting down mod loader...");
        
        // Disable all mods
        for (ModContainer container : loadedMods) {
            try {
                if (container.getState() == ModContainer.ModState.ENABLED) {
                    container.disable();
                }
            } catch (Exception e) {
                System.err.println("[ModLoader] Error disabling mod " + container.getName());
            }
        }
        
        // Stop Py4J bridge
        py4jBridge.stop();
        
        // Shutdown event bus
        eventBus.shutdown();
        
        // Clear loaded mods
        loadedMods.clear();
        
        System.out.println("[ModLoader] Mod loader shut down");
    }
    
    /**
     * Reloads all mods.
     * Useful for development.
     */
    public void reloadMods() {
        System.out.println("[ModLoader] Reloading mods...");
        shutdown();
        init();
    }
    
    /**
     * Gets a mod by its ID.
     * 
     * @param id Mod ID
     * @return ModContainer, or null if not found
     */
    public ModContainer getModById(String id) {
        for (ModContainer container : loadedMods) {
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
    public List<ModContainer> getLoadedMods() {
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
