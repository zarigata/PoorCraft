package com.poorcraft.discord;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.poorcraft.world.generation.BiomeType;

/**
 * Manages Discord Rich Presence integration for PoorCraft.
 * 
 * Shows what you're doing in-game to all your Discord friends.
 * Because everyone needs to know you're grinding in a Minecraft clone.
 * This is basically flex culture but for poor people.
 */
public class DiscordRichPresenceManager {
    
    private static final String APPLICATION_ID = "1234567890123456789";  // TODO: Replace with actual Discord App ID
    private static final String GAME_VERSION = "0.1.0-SNAPSHOT";
    private static final String DOWNLOAD_URL = "https://github.com/yourproject/poorcraft";  // TODO: Update with actual URL
    
    private final DiscordRPC lib;
    private DiscordRichPresence presence;
    private boolean initialized;
    private long startTimestamp;
    
    /**
     * Creates a new Discord Rich Presence manager.
     * Doesn't initialize connection yet - call init() for that.
     */
    public DiscordRichPresenceManager() {
        this.lib = DiscordRPC.INSTANCE;
        this.initialized = false;
        this.startTimestamp = System.currentTimeMillis() / 1000; // Discord wants seconds, not milliseconds
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
            
            // Set up event handlers
            // Most of these are just for logging because we're curious
            // But mostly they just sit there doing nothing. Like me on a Sunday.
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            handlers.ready = (user) -> {
                System.out.println("[Discord] Ready! Connected as: " + user.username + "#" + user.discriminator);
            };
            handlers.disconnected = (errorCode, message) -> {
                System.out.println("[Discord] Disconnected: " + errorCode + " - " + message);
            };
            handlers.errored = (errorCode, message) -> {
                System.err.println("[Discord] Error: " + errorCode + " - " + message);
            };
            
            // Initialize Discord RPC
            lib.Discord_Initialize(APPLICATION_ID, handlers, true, null);
            
            // Create initial presence
            presence = new DiscordRichPresence();
            presence.startTimestamp = startTimestamp;
            presence.largeImageKey = "poorcraft_logo";  // Must match image key in Discord Developer Portal
            presence.largeImageText = "PoorCraft v" + GAME_VERSION;
            presence.details = "In Main Menu";
            presence.state = "Starting up...";
            
            // Set buttons for download/website
            // NOTE: Buttons require Discord RPC API v2 which this library supports
            presence.partyId = "poorcraft";
            presence.partySize = 1;
            presence.partyMax = 1;
            
            // Update presence
            lib.Discord_UpdatePresence(presence);
            
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
     * Updates the rich presence with main menu state.
     */
    public void updateMainMenu() {
        if (!initialized) return;
        
        presence.details = "In Main Menu";
        presence.state = "Deciding what to do";
        presence.smallImageKey = "";
        presence.smallImageText = "";
        
        lib.Discord_UpdatePresence(presence);
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
        if (!initialized) return;
        
        // Set details based on biome
        // This is where we flex what biome we're in
        // Because apparently that matters to people
        String biomeName = biome != null ? biome.getName() : "Unknown";
        presence.details = "Exploring " + biomeName;
        
        // Set state based on mode
        if (multiplayer) {
            presence.state = "Playing Multiplayer";
        } else {
            presence.state = "Playing Singleplayer";
        }
        
        // Set biome-specific image if available
        // We'll use small image for biome icon
        if (biome != null) {
            presence.smallImageKey = biome.getName().toLowerCase() + "_biome";
            presence.smallImageText = biomeName + " Biome";
        }
        
        lib.Discord_UpdatePresence(presence);
    }
    
    /**
     * Updates the rich presence with paused state.
     */
    public void updatePaused() {
        if (!initialized) return;
        
        presence.details = "Game Paused";
        presence.state = "Taking a break";
        
        lib.Discord_UpdatePresence(presence);
    }
    
    /**
     * Updates the rich presence with world creation state.
     */
    public void updateCreatingWorld() {
        if (!initialized) return;
        
        presence.details = "Creating New World";
        presence.state = "Generating terrain...";
        
        lib.Discord_UpdatePresence(presence);
    }
    
    /**
     * Updates the multiplayer rich presence.
     * 
     * @param serverAddress Server address being connected to
     */
    public void updateConnectingMultiplayer(String serverAddress) {
        if (!initialized) return;
        
        presence.details = "Joining Server";
        presence.state = "Connecting to " + serverAddress;
        
        lib.Discord_UpdatePresence(presence);
    }
    
    /**
     * Shuts down Discord Rich Presence.
     * Call this when the game is closing.
     * 
     * Don't forget to call this or Discord will think you're still playing!
     * I learned this the hard way when my friends kept asking why I was
     * playing PoorCraft for 48 hours straight. I wasn't. The game crashed.
     */
    public void shutdown() {
        if (!initialized) return;
        
        System.out.println("[Discord] Shutting down Rich Presence...");
        lib.Discord_Shutdown();
        initialized = false;
        System.out.println("[Discord] Rich Presence shut down");
    }
    
    /**
     * Updates Discord callbacks.
     * Should be called periodically (e.g., in game loop).
     * 
     * This is important! Discord needs to process events.
     * Call this every frame or so, otherwise Discord will think we froze.
     */
    public void runCallbacks() {
        if (!initialized) return;
        lib.Discord_RunCallbacks();
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
