package com.poorcraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration manager responsible for loading and saving game settings.
 * 
 * Loading priority:
 * 1. User config file (filesystem) - allows player customization
 * 2. Default config (classpath) - bundled with the game
 * 3. Hardcoded defaults - ultimate fallback
 * 
 * This is basically how every game since the 90s has done it, but hey it works!
 */
public class ConfigManager {
    
    private static final String DEFAULT_CONFIG_PATH = "/config/default_settings.json";
    private static final String USER_CONFIG_PATH = "config/settings.json";
    
    private final Gson gson;
    private Settings settings;
    
    /**
     * Creates a new ConfigManager with pretty-printed JSON output.
     * Because nobody likes minified JSON when debugging at 3 AM.
     */
    public ConfigManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    /**
     * Loads settings with fallback chain: user file -> default resource -> hardcoded.
     * 
     * @return Loaded settings (never null)
     */
    public Settings loadSettings() {
        // Try user config first (filesystem)
        Path userConfigPath = Paths.get(USER_CONFIG_PATH);
        if (Files.exists(userConfigPath)) {
            try {
                System.out.println("[ConfigManager] Loading settings from user config: " + USER_CONFIG_PATH);
                String json = Files.readString(userConfigPath, StandardCharsets.UTF_8);
                settings = gson.fromJson(json, Settings.class);
                System.out.println("[ConfigManager] Successfully loaded user config");
                return settings;
            } catch (IOException e) {
                System.err.println("[ConfigManager] Failed to load user config: " + e.getMessage());
                System.err.println("[ConfigManager] Falling back to default config...");
            } catch (JsonSyntaxException e) {
                // Handles malformed JSON - like when you accidentally save with a trailing comma
                // or forget a closing brace at 2 AM. We've all been there.
                System.err.println("[ConfigManager] User config has invalid JSON syntax: " + e.getMessage());
                System.err.println("[ConfigManager] Falling back to default config...");
            }
        }
        
        // Try default config from classpath
        try (InputStream stream = getClass().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (stream != null) {
                System.out.println("[ConfigManager] Loading settings from classpath: " + DEFAULT_CONFIG_PATH);
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                settings = gson.fromJson(reader, Settings.class);
                System.out.println("[ConfigManager] Successfully loaded default config");
                return settings;
            }
        } catch (IOException e) {
            System.err.println("[ConfigManager] Failed to load default config: " + e.getMessage());
            System.err.println("[ConfigManager] Falling back to hardcoded defaults...");
        }
        
        // Ultimate fallback: hardcoded defaults
        System.out.println("[ConfigManager] Using hardcoded default settings");
        settings = Settings.getDefault();
        return settings;
    }
    
    /**
     * Saves settings to the user config file.
     * Creates parent directories if they don't exist.
     * 
     * @param settings Settings to save
     */
    public void saveSettings(Settings settings) {
        try {
            Path userConfigPath = Paths.get(USER_CONFIG_PATH);
            
            // Ensure parent directory exists
            // I spent 2 hours debugging this once because I forgot this line. Don't be like me.
            if (userConfigPath.getParent() != null) {
                Files.createDirectories(userConfigPath.getParent());
            }
            
            // Serialize and write
            String json = gson.toJson(settings);
            Files.writeString(userConfigPath, json, StandardCharsets.UTF_8);
            
            System.out.println("[ConfigManager] Settings saved to: " + USER_CONFIG_PATH);
        } catch (IOException e) {
            System.err.println("[ConfigManager] Failed to save settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Returns the currently loaded settings.
     * 
     * @return Current settings (may be null if loadSettings() hasn't been called)
     */
    public Settings getSettings() {
        return settings;
    }
    
    /**
     * Reloads settings from disk.
     * Useful for hot-reloading config changes without restarting the game.
     */
    public void reloadSettings() {
        loadSettings();
    }
}
