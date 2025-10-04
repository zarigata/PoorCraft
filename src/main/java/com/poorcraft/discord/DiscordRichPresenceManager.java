package com.poorcraft.discord;

import com.poorcraft.world.generation.BiomeType;

/**
 * Manages Discord Rich Presence integration for PoorCraft.
 * 
 * Shows what you're doing in-game to all your Discord friends.
 * Because everyone needs to know you're grinding in a Minecraft clone.
 * This is basically flex culture but for poor people.
 * 
 * Uses custom IPC implementation - no external dependencies!
 */
public class DiscordRichPresenceManager {
    
    private static final long APPLICATION_ID = 1234567890123456789L;  // TODO: Replace with actual Discord App ID
    private static final String GAME_VERSION = "0.1.0-SNAPSHOT";
    
    private SimpleDiscordIPC ipc;
    private boolean initialized;
    private long startTimestamp;
    
    /**
     * Creates a new Discord Rich Presence manager.
     * Doesn't initialize connection yet - call init() for that.
     */
    public DiscordRichPresenceManager() {
        this.ipc = null;
        this.initialized = false;
        this.startTimestamp = System.currentTimeMillis() / 1000;
    }
    
    /**
     * Initializes Discord Rich Presence connection.
     * Call this after creating the manager.
     * 
     * @return true if initialized successfully, false otherwise
     */
    public boolean init() {
        if (initialized) {
            System.out.println("[Discord] Already initialized, skipping");
            return true;
        }
        
        try {
            System.out.println("[Discord] Initializing Rich Presence...");
            System.out.println("[Discord] NOTE: Discord Rich Presence requires Java 16+ for Unix sockets");
            System.out.println("[Discord] Windows support requires external library or Java 16+");
            
            // Check Java version
            int javaVersion = getJavaVersion();
            if (javaVersion < 16) {
                System.err.println("[Discord] Java " + javaVersion + " detected. Java 16+ required for native Discord IPC");
                System.err.println("[Discord] Rich Presence disabled. Please upgrade to Java 16+ or use external library.");
                return false;
            }
            
            // Create IPC client
            ipc = new SimpleDiscordIPC(APPLICATION_ID);
            
            try {
                ipc.connect();
            } catch (Exception e) {
                System.err.println("[Discord] Could not connect to Discord: " + e.getMessage());
                System.err.println("[Discord] Make sure Discord is running!");
                return false;
            }
            
            // Set initial presence
            updateMainMenu();
            
            initialized = true;
            System.out.println("[Discord] Rich Presence initialized successfully!");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("[Discord] Failed to initialize Rich Presence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets Java major version.
     */
    private int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 8; // Default to 8 if we can't parse
        }
    }
    
    /**
     * Updates the rich presence with main menu state.
     */
    public void updateMainMenu() {
        if (!initialized || ipc == null) return;
        
        try {
            SimpleDiscordIPC.RichPresenceData presence = new SimpleDiscordIPC.RichPresenceData();
            presence.state = "Deciding what to do";
            presence.details = "In Main Menu";
            presence.startTimestamp = startTimestamp;
            presence.largeImageKey = "poorcraft_logo";
            presence.largeImageText = "PoorCraft v" + GAME_VERSION;
            
            ipc.updatePresence(presence);
        } catch (Exception e) {
            System.err.println("[Discord] Failed to update presence: " + e.getMessage());
        }
    }
    
    /**
     * Updates the rich presence with in-game state.
     * Shows the current biome and game mode.
     * 
     * @param biome Current biome the player is in
     * @param multiplayer Whether in multiplayer mode
     * @param seed World seed (optional, can be 0)
     */
    public void updateInGame(BiomeType biome, boolean multiplayer, long seed) {
        if (!initialized || ipc == null) return;
        
        try {
            SimpleDiscordIPC.RichPresenceData presence = new SimpleDiscordIPC.RichPresenceData();
            
            String biomeName = biome != null ? biome.getName() : "Unknown";
            presence.details = "Exploring " + biomeName;
            presence.state = multiplayer ? "Playing Multiplayer" : "Playing Singleplayer";
            presence.startTimestamp = startTimestamp;
            presence.largeImageKey = "poorcraft_logo";
            presence.largeImageText = "PoorCraft v" + GAME_VERSION;
            
            if (biome != null) {
                presence.smallImageKey = biome.getName().toLowerCase() + "_biome";
                presence.smallImageText = biomeName + " Biome";
            }
            
            ipc.updatePresence(presence);
        } catch (Exception e) {
            System.err.println("[Discord] Failed to update presence: " + e.getMessage());
        }
    }
    
    /**
     * Updates the rich presence with paused state.
     */
    public void updatePaused() {
        if (!initialized || ipc == null) return;
        
        try {
            SimpleDiscordIPC.RichPresenceData presence = new SimpleDiscordIPC.RichPresenceData();
            presence.state = "Taking a break";
            presence.details = "Game Paused";
            presence.startTimestamp = startTimestamp;
            presence.largeImageKey = "poorcraft_logo";
            presence.largeImageText = "PoorCraft v" + GAME_VERSION;
            
            ipc.updatePresence(presence);
        } catch (Exception e) {
            System.err.println("[Discord] Failed to update presence: " + e.getMessage());
        }
    }
    
    /**
     * Updates the rich presence with world creation state.
     */
    public void updateCreatingWorld() {
        if (!initialized || ipc == null) return;
        
        try {
            SimpleDiscordIPC.RichPresenceData presence = new SimpleDiscordIPC.RichPresenceData();
            presence.state = "Generating terrain...";
            presence.details = "Creating New World";
            presence.startTimestamp = startTimestamp;
            presence.largeImageKey = "poorcraft_logo";
            presence.largeImageText = "PoorCraft v" + GAME_VERSION;
            
            ipc.updatePresence(presence);
        } catch (Exception e) {
            System.err.println("[Discord] Failed to update presence: " + e.getMessage());
        }
    }
    
    /**
     * Updates the multiplayer rich presence.
     * 
     * @param serverAddress Server address being connected to
     */
    public void updateConnectingMultiplayer(String serverAddress) {
        if (!initialized || ipc == null) return;
        
        try {
            SimpleDiscordIPC.RichPresenceData presence = new SimpleDiscordIPC.RichPresenceData();
            presence.state = "Connecting to " + serverAddress;
            presence.details = "Joining Server";
            presence.startTimestamp = startTimestamp;
            presence.largeImageKey = "poorcraft_logo";
            presence.largeImageText = "PoorCraft v" + GAME_VERSION;
            
            ipc.updatePresence(presence);
        } catch (Exception e) {
            System.err.println("[Discord] Failed to update presence: " + e.getMessage());
        }
    }
    
    /**
     * Shuts down Discord Rich Presence.
     * Call this when the game is closing.
     */
    public void shutdown() {
        if (!initialized || ipc == null) return;
        
        System.out.println("[Discord] Shutting down Rich Presence...");
        try {
            ipc.close();
        } catch (Exception e) {
            System.err.println("[Discord] Error during shutdown: " + e.getMessage());
        }
        initialized = false;
        System.out.println("[Discord] Rich Presence shut down");
    }
    
    /**
     * Updates Discord callbacks.
     * 
     * With our custom IPC, we don't need periodic callbacks.
     * This method exists for API compatibility.
     */
    public void runCallbacks() {
        // No-op - our IPC doesn't need periodic callbacks
        if (!initialized || ipc == null) return;
        
        // Check if still connected
        if (!ipc.isConnected()) {
            System.out.println("[Discord] Connection lost");
            initialized = false;
        }
    }
    
    /**
     * Checks if Discord Rich Presence is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
}
