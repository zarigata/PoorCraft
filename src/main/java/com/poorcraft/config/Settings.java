package com.poorcraft.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings class that mirrors the JSON configuration structure.
 * Contains all game settings organized into nested categories.
 * 
 * All fields are public for Gson serialization/deserialization.
 * I think this is how Notch did it back in the alpha days... or was it? idk but it works
 */
public class Settings {
    
    public WindowSettings window;
    public GraphicsSettings graphics;
    public AudioSettings audio;
    public ControlsSettings controls;
    public CameraSettings camera;
    public AISettings ai;
    public WorldSettings world;
    public MultiplayerSettings multiplayer;
    
    /**
     * Default constructor required by Gson.
     * Don't ask me why Gson needs this, it just does. Magic? Probably.
     */
    public Settings() {
        // Gson will populate fields from JSON
    }
    
    /**
     * Returns a Settings instance with hardcoded defaults.
     * This is the ultimate fallback if JSON loading fails completely.
     * Like when you forget to include the config file in the jar... been there.
     * 
     * @return Settings with default values matching default_settings.json
     */
    public static Settings getDefault() {
        Settings settings = new Settings();
        
        settings.window = new WindowSettings();
        settings.window.width = 1280;
        settings.window.height = 720;
        settings.window.title = "PoorCraft";
        settings.window.vsync = true;
        settings.window.fullscreen = false;
        
        settings.graphics = new GraphicsSettings();
        settings.graphics.fov = 70.0f;
        settings.graphics.renderDistance = 8;
        settings.graphics.maxFps = 144;
        
        settings.audio = new AudioSettings();
        settings.audio.masterVolume = 1.0f;
        settings.audio.musicVolume = 0.7f;
        settings.audio.sfxVolume = 0.8f;
        
        settings.controls = new ControlsSettings();
        settings.controls.mouseSensitivity = 0.1f;
        settings.controls.invertY = false;
        settings.controls.keybinds = new HashMap<>();
        settings.controls.keybinds.put("forward", 87);    // W
        settings.controls.keybinds.put("backward", 83);   // S
        settings.controls.keybinds.put("left", 65);       // A
        settings.controls.keybinds.put("right", 68);      // D
        settings.controls.keybinds.put("jump", 32);       // Space
        settings.controls.keybinds.put("sneak", 341);     // Left Control
        settings.controls.keybinds.put("sprint", 340);    // Left Shift
        settings.controls.keybinds.put("inventory", 69);  // E
        settings.controls.keybinds.put("debug", 290);     // F3
        
        settings.camera = new CameraSettings();
        settings.camera.moveSpeed = 4.317f;  // Matches Minecraft walking speed, very scientific
        settings.camera.sprintMultiplier = 1.3f;
        settings.camera.sneakMultiplier = 0.3f;
        
        settings.ai = new AISettings();
        settings.ai.aiEnabled = false;
        settings.ai.aiProvider = "ollama";
        
        settings.world = new WorldSettings();
        settings.world.seed = 0;  // 0 means random seed
        settings.world.chunkLoadDistance = 8;
        settings.world.chunkUnloadDistance = 10;
        settings.world.generateStructures = true;
        
        settings.multiplayer = new MultiplayerSettings();
        settings.multiplayer.username = "Player";
        settings.multiplayer.lastServer = "localhost:25565";
        settings.multiplayer.serverPort = 25565;
        settings.multiplayer.autoConnect = false;
        settings.multiplayer.maxPlayers = 10;
        
        return settings;
    }
    
    /**
     * Window configuration settings.
     * Controls the game window appearance and behavior.
     */
    public static class WindowSettings {
        public int width;
        public int height;
        public String title;
        public boolean vsync;
        public boolean fullscreen;
    }
    
    /**
     * Graphics rendering settings.
     * FOV, render distance, fps caps, etc.
     */
    public static class GraphicsSettings {
        public float fov;           // Field of view in degrees
        public int renderDistance;  // Render distance in chunks
        public int maxFps;          // Maximum FPS (0 for unlimited)
    }
    
    /**
     * Audio volume settings.
     * All volumes are 0.0 to 1.0 range.
     */
    public static class AudioSettings {
        public float masterVolume;
        public float musicVolume;
        public float sfxVolume;
    }
    
    /**
     * Input control settings.
     * Mouse sensitivity, key bindings, etc.
     */
    public static class ControlsSettings {
        public float mouseSensitivity;
        public boolean invertY;
        public Map<String, Integer> keybinds;  // Action name -> GLFW key code
        
        /**
         * Safely retrieves a keybind, returning a default value if missing.
         * Prevents NullPointerException when config omits a key.
         * 
         * This saved me from a crash when I deleted half my config testing stuff.
         * I don't know what I'm doing half the time but at least it's safe now!
         * 
         * @param action The action name (e.g., "forward", "jump")
         * @param defaultKey Default GLFW key code if action is not bound
         * @return The bound key code, or defaultKey if not found
         */
        public int getKeybind(String action, int defaultKey) {
            if (keybinds == null) {
                return defaultKey;
            }
            Integer key = keybinds.get(action);
            return key != null ? key : defaultKey;
        }
    }
    
    /**
     * Camera movement settings.
     * Speed multipliers for different movement modes.
     */
    public static class CameraSettings {
        public float moveSpeed;          // Base movement speed in blocks/second
        public float sprintMultiplier;   // Speed multiplier when sprinting
        public float sneakMultiplier;    // Speed multiplier when sneaking
    }
    
    /**
     * AI mod integration settings.
     * For future Python mod integration with Ollama/other AI providers.
     */
    public static class AISettings {
        public boolean aiEnabled;
        public String aiProvider;
    }
    
    /**
     * World generation and chunk management settings.
     * Controls terrain generation, chunk loading behavior, and world features.
     */
    public static class WorldSettings {
        public long seed;                    // World seed (0 for random)
        public int chunkLoadDistance;        // How many chunks to load around player
        public int chunkUnloadDistance;      // Distance before unloading chunks
        public boolean generateStructures;   // Enable/disable feature generation (trees, cacti, etc.)
    }
    
    /**
     * Multiplayer settings.
     * Controls networking, server hosting, and player identification.
     */
    public static class MultiplayerSettings {
        public String username;          // Player username
        public String lastServer;        // Last connected server address
        public int serverPort;           // Default server port when hosting
        public boolean autoConnect;      // Auto-connect to last server on startup
        public int maxPlayers;           // Max players when hosting
    }
}
