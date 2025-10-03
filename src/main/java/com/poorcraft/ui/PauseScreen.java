package com.poorcraft.ui;

import com.poorcraft.config.Settings;
import com.poorcraft.modding.ModContainer;
import com.poorcraft.modding.ModLoader;
import com.poorcraft.core.Game;

import java.util.List;

/**
 * In-game pause menu screen with COMPREHENSIVE settings.
 * 
 * Shown when ESC is pressed during gameplay.
 * This isn't just a pause menu anymore - it's a full in-game configuration panel!
 * 
 * Features:
 * - Semi-transparent overlay that doesn't hide the game world
 * - Quick settings adjustment (FOV, volume, render distance)
 * - Live mod list display (shows what's running)
 * - Chunk loader configuration
 * - Direct access to full settings
 * - Save & exit options
 * 
 * Now you can tweak everything without leaving the game. Just like the good old days
 * when you'd spend more time in the settings than actually playing. Good times.
 */
public class PauseScreen extends UIScreen {
    
    private UIManager uiManager;
    private Settings settings;
    private Game game;
    private float animationTime = 0.0f;
    
    // Quick settings sliders - for live adjustments
    private Slider fovSlider;
    private Slider masterVolumeSlider;
    private Slider renderDistanceSlider;
    private Slider mouseSensitivitySlider;
    
    /**
     * Creates the pause screen with all the bells and whistles.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public PauseScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
        this.settings = uiManager.getSettings();
        
        // Get game instance via reflection - yeah it's hacky but it works
        // Don't judge me, sometimes you gotta do what you gotta do
        try {
            var field = uiManager.getClass().getDeclaredField("game");
            field.setAccessible(true);
            this.game = (Game) field.get(uiManager);
        } catch (Exception e) {
            System.err.println("[PauseScreen] Couldn't get game instance: " + e.getMessage());
            this.game = null;
        }
    }
    
    @Override
    public void init() {
        clearComponents();
        
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        
        // Layout calculations - everything scales with screen size
        float panelWidth = Math.min(windowWidth * 0.85f, 900f);  // Max 900px wide
        float panelHeight = Math.min(windowHeight * 0.85f, 650f);
        float panelX = centerX - panelWidth / 2;
        float panelY = centerY - panelHeight / 2;
        
        // Title - big and bold
        Label titleLabel = new Label(centerX, panelY + 20, "GAME PAUSED",
            0.0f, 0.95f, 0.95f, 1.0f);  // Cyan like the old CRT monitors
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // ======================
        // LEFT PANEL - Quick Settings
        // ======================
        float leftPanelX = panelX + 20;
        float leftPanelWidth = panelWidth * 0.48f;
        float contentY = panelY + 60;
        float controlHeight = 30;
        float spacing = 45;
        
        Label quickSettingsLabel = new Label(leftPanelX, contentY, "QUICK SETTINGS",
            0.7f, 0.9f, 0.95f, 1.0f);
        addComponent(quickSettingsLabel);
        contentY += 30;
        
        // FOV Slider - because everyone wants to see EVERYTHING at once
        fovSlider = new Slider(leftPanelX, contentY, leftPanelWidth, controlHeight,
            "Field of View", 60, 110, settings.graphics.fov,
            value -> {
                settings.graphics.fov = value;
                // Live update! No need to restart or anything
            });
        fovSlider.setDecimalPlaces(0);
        addComponent(fovSlider);
        contentY += spacing;
        
        // Master Volume - for when your mom walks in
        masterVolumeSlider = new Slider(leftPanelX, contentY, leftPanelWidth, controlHeight,
            "Master Volume", 0, 100, settings.audio.masterVolume * 100,
            value -> settings.audio.masterVolume = value / 100.0f);
        masterVolumeSlider.setDecimalPlaces(0);
        addComponent(masterVolumeSlider);
        contentY += spacing;
        
        // Render Distance - performance vs beauty
        renderDistanceSlider = new Slider(leftPanelX, contentY, leftPanelWidth, controlHeight,
            "Render Distance", 4, 16, settings.graphics.renderDistance,
            value -> {
                settings.graphics.renderDistance = (int) value.floatValue();
                // This might cause some lag as chunks reload but that's the price of beauty
            });
        renderDistanceSlider.setDecimalPlaces(0);
        addComponent(renderDistanceSlider);
        contentY += spacing;
        
        // Mouse Sensitivity - for the twitchy players
        mouseSensitivitySlider = new Slider(leftPanelX, contentY, leftPanelWidth, controlHeight,
            "Mouse Sensitivity", 0.01f, 0.5f, settings.controls.mouseSensitivity,
            value -> settings.controls.mouseSensitivity = value);
        mouseSensitivitySlider.setDecimalPlaces(2);
        addComponent(mouseSensitivitySlider);
        contentY += spacing;
        
        // Chunk Load Distance info
        Label chunkInfoLabel = new Label(leftPanelX, contentY,
            String.format("Chunk Load Distance: %d chunks", settings.world.chunkLoadDistance),
            0.8f, 0.8f, 0.8f, 0.9f);
        addComponent(chunkInfoLabel);
        contentY += 25;
        
        Label chunkUnloadLabel = new Label(leftPanelX, contentY,
            String.format("Chunk Unload Distance: %d chunks", settings.world.chunkUnloadDistance),
            0.8f, 0.8f, 0.8f, 0.9f);
        addComponent(chunkUnloadLabel);
        
        // ======================
        // RIGHT PANEL - Mods & Info
        // ======================
        float rightPanelX = panelX + panelWidth * 0.52f + 20;
        float rightPanelWidth = panelWidth * 0.48f - 40;
        contentY = panelY + 60;
        
        Label modsLabel = new Label(rightPanelX, contentY, "LOADED MODS",
            0.95f, 0.7f, 0.9f, 1.0f);
        addComponent(modsLabel);
        contentY += 30;
        
        // Display loaded mods - if any exist
        if (game != null && game.getModLoader() != null) {
            ModLoader modLoader = game.getModLoader();
            List<ModContainer> mods = modLoader.getLoadedMods();
            
            if (mods.isEmpty()) {
                Label noModsLabel = new Label(rightPanelX, contentY,
                    "No mods loaded",
                    0.6f, 0.6f, 0.6f, 0.8f);
                addComponent(noModsLabel);
                contentY += 25;
            } else {
                // List each mod with name and version
                // I know it's not the fanciest display but it works
                for (int i = 0; i < Math.min(mods.size(), 8); i++) {  // Show max 8 mods
                    ModContainer mod = mods.get(i);
                    String modText = String.format("• %s v%s", 
                        mod.getName(), mod.getVersion());
                    
                    Label modLabel = new Label(rightPanelX, contentY,
                        modText, 0.8f, 0.95f, 0.8f, 0.9f);
                    addComponent(modLabel);
                    contentY += 22;
                }
                
                if (mods.size() > 8) {
                    Label moreLabel = new Label(rightPanelX, contentY,
                        String.format("... and %d more", mods.size() - 8),
                        0.6f, 0.6f, 0.6f, 0.8f);
                    addComponent(moreLabel);
                }
            }
        } else {
            Label errorLabel = new Label(rightPanelX, contentY,
                "Mod system unavailable",
                0.8f, 0.5f, 0.5f, 0.8f);
            addComponent(errorLabel);
        }
        
        contentY = panelY + 320;  // Position for game info
        
        // Game info section - because why not show off some stats?
        Label gameInfoLabel = new Label(rightPanelX, contentY, "GAME INFO",
            0.9f, 0.9f, 0.7f, 1.0f);
        addComponent(gameInfoLabel);
        contentY += 25;
        
        // Max FPS
        Label fpsLabel = new Label(rightPanelX, contentY,
            String.format("Max FPS: %d", settings.graphics.maxFps),
            0.7f, 0.7f, 0.7f, 0.9f);
        addComponent(fpsLabel);
        contentY += 20;
        
        // VSync status
        Label vsyncLabel = new Label(rightPanelX, contentY,
            "VSync: " + (settings.window.vsync ? "ON" : "OFF"),
            0.7f, 0.7f, 0.7f, 0.9f);
        addComponent(vsyncLabel);
        
        // ======================
        // BOTTOM BUTTONS - The important stuff
        // ======================
        float buttonY = panelY + panelHeight - 60;
        float buttonWidth = (panelWidth - 60) / 4;  // 4 buttons with spacing
        float buttonHeight = 45;
        float buttonSpacing = 10;
        
        // Resume button - get back to the action!
        MenuButton resumeButton = new MenuButton(
            panelX + 20, buttonY,
            buttonWidth, buttonHeight,
            "RESUME",
            () -> uiManager.setState(GameState.IN_GAME)
        );
        addComponent(resumeButton);
        
        // Full Settings button - for when you want to tweak EVERYTHING
        MenuButton settingsButton = new MenuButton(
            panelX + 20 + buttonWidth + buttonSpacing, buttonY,
            buttonWidth, buttonHeight,
            "SETTINGS",
            () -> {
                // Save current quick settings before going to full menu
                uiManager.getConfigManager().saveSettings(settings);
                uiManager.setState(GameState.SETTINGS_MENU);
            }
        );
        addComponent(settingsButton);
        
        // Save button - because losing progress sucks
        MenuButton saveButton = new MenuButton(
            panelX + 20 + (buttonWidth + buttonSpacing) * 2, buttonY,
            buttonWidth, buttonHeight,
            "SAVE",
            () -> {
                // Save settings immediately
                uiManager.getConfigManager().saveSettings(settings);
                System.out.println("[PauseScreen] Settings saved!");
                // TODO: Also save world when that's implemented
                // For now just settings which is better than nothing I guess
            }
        );
        addComponent(saveButton);
        
        // Save & Quit button - the coward's way out (just kidding)
        MenuButton quitButton = new MenuButton(
            panelX + 20 + (buttonWidth + buttonSpacing) * 3, buttonY,
            buttonWidth, buttonHeight,
            "QUIT",
            () -> {
                // Save everything before quitting
                uiManager.getConfigManager().saveSettings(settings);
                System.out.println("[PauseScreen] Saving and returning to main menu...");
                // TODO: Save the world here when world saving is implemented
                // The game state is lost but at least settings are saved
                uiManager.setState(GameState.MAIN_MENU);
            }
        );
        addComponent(quitButton);
        
        // Hint label at the very bottom
        Label hintLabel = new Label(centerX, panelY + panelHeight - 20,
            "Press ESC to resume | Changes are applied instantly",
            0.7f, 0.5f, 0.9f, 0.7f);
        hintLabel.setCentered(true);
        addComponent(hintLabel);
        
        System.out.println("[PauseScreen] Enhanced layout initialized for " + windowWidth + "x" + windowHeight);
    }
    
    /**
     * Clamps a value between min and max.
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    @Override
    public void onResize(int width, int height) {
        System.out.println("[PauseScreen] Resize detected: " + width + "x" + height);
        
        // Update dimensions
        this.windowWidth = width;
        this.windowHeight = height;
        
        // Rebuild layout
        init();
        
        System.out.println("[PauseScreen] Resize complete, components rebuilt");
    }
    
    @Override
    public void update(float deltaTime) {
        animationTime += deltaTime;
        super.update(deltaTime);
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        
        // Draw semi-transparent dark overlay over the game world
        // You can still see your beautiful creation through the menu
        renderer.drawRect(0, 0, windowWidth, windowHeight,
            0.05f, 0.02f, 0.10f, 0.85f);  // Dark blue-purple, 85% opacity
        
        // Panel background - slightly lighter so it stands out
        float panelWidth = Math.min(windowWidth * 0.85f, 900f);
        float panelHeight = Math.min(windowHeight * 0.85f, 650f);
        float panelX = centerX - panelWidth / 2;
        float panelY = centerY - panelHeight / 2;
        
        renderer.drawRect(panelX, panelY, panelWidth, panelHeight,
            0.08f, 0.05f, 0.12f, 0.95f);
        
        // Panel border - that retro glow effect
        float borderWidth = 2;
        renderer.drawRect(panelX, panelY, panelWidth, borderWidth,
            0.0f, 0.95f, 0.95f, 0.8f);  // Top
        renderer.drawRect(panelX, panelY + panelHeight - borderWidth, panelWidth, borderWidth,
            0.0f, 0.95f, 0.95f, 0.8f);  // Bottom
        renderer.drawRect(panelX, panelY, borderWidth, panelHeight,
            0.0f, 0.95f, 0.95f, 0.8f);  // Left
        renderer.drawRect(panelX + panelWidth - borderWidth, panelY, borderWidth, panelHeight,
            0.0f, 0.95f, 0.95f, 0.8f);  // Right
        
        // Vertical divider between left and right panels
        float dividerX = panelX + panelWidth * 0.5f;
        renderer.drawRect(dividerX, panelY + 50, 1, panelHeight - 100,
            0.0f, 0.95f, 0.95f, 0.3f);
        
        // Optional: Subtle scanline animation because we can
        // (This is what happens when developers have too much fun)
        float scanlineY = (animationTime * 30) % windowHeight;
        renderer.drawRect(0, scanlineY, windowWidth, 1,
            0.0f, 0.95f, 0.95f, 0.03f);  // Very subtle cyan scanline
        
        // Render all UI components (sliders, buttons, labels, the works)
        super.render(renderer, fontRenderer);
    }
}
