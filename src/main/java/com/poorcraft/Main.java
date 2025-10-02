package com.poorcraft;

import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.core.Game;

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
            // Load configuration
            ConfigManager configManager = new ConfigManager();
            Settings settings = configManager.loadSettings();
            System.out.println("Settings loaded successfully");
            
            // Create and run game
            Game game = new Game(settings);
            game.run();
            
        } catch (Exception e) {
            System.err.println("Fatal error occurred:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            System.out.println("PoorCraft shut down");
        }
    }
}
