package com.poorcraft;

import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.core.Game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main entry point for PoorCraft.
 * 
 * Bootstraps the game by loading configuration and starting the game loop.
 * This is where it all begins... or where it all crashes. Hopefully the former.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("PoorCraft starting...");

        try {
            initializeUserDirectories();
            // Load configuration
            ConfigManager configManager = new ConfigManager();
            Settings settings = configManager.loadSettings();
            System.out.println("Settings loaded successfully");
            
            // Create and run game
            Game game = new Game(settings, configManager);
            game.run();
            
        } catch (Exception e) {
            System.err.println("Fatal error occurred:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            System.out.println("PoorCraft shut down");
        }
    }

    private static void initializeUserDirectories() {
        Map<String, String> directories = new LinkedHashMap<>();
        directories.put("skins", "Player skin directory");
        directories.put("skins/default", "Bundled default skins directory");
        directories.put("resourcepacks", "Resource pack directory");
        directories.put("screenshots", "Screenshot directory");
        directories.put("worlds", "World saves directory");
        directories.put("config", "Configuration directory");

        directories.forEach((relativePath, description) -> {
            Path path = Paths.get(relativePath);
            try {
                if (Files.notExists(path)) {
                    Files.createDirectories(path);
                    System.out.println("[Main] Created " + description + ": " + path.toAbsolutePath());
                } else {
                    System.out.println("[Main] Found " + description + ": " + path.toAbsolutePath());
                }
            } catch (IOException e) {
                System.err.println("[Main] Failed to create " + description + " at " + path.toAbsolutePath());
                e.printStackTrace();
            }
        });
    }
}
