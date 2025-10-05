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
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float contentPadding;
    
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

        panelWidth = clamp(windowWidth * 0.72f, 720f, Math.max(720f, windowWidth - 90f));
        panelHeight = clamp(windowHeight * 0.72f, 540f, Math.max(540f, windowHeight - 100f));
        panelX = (windowWidth - panelWidth) / 2.0f;
        panelY = (windowHeight - panelHeight) / 2.0f;
        contentPadding = Math.max(26f, panelWidth * 0.038f);

        float titleScale = Math.max(1.7f, panelWidth / 620f);
        float titleBaseline = panelY + contentPadding;
        Label titleLabel = new Label(centerX, titleBaseline, "GAME PAUSED",
            0.02f, 0.96f, 0.96f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        addComponent(titleLabel);

        float topContentY = titleBaseline + Math.max(52f, 34f * titleScale);
        float columnWidth = (panelWidth - contentPadding * 3f) / 2f;
        float leftPanelX = panelX + contentPadding;
        float leftPanelWidth = columnWidth;
        float rightPanelX = panelX + contentPadding * 2f + columnWidth;
        float sliderHeight = Math.max(56f, panelHeight * 0.11f);
        float sliderSpacing = Math.max(20f, sliderHeight * 0.32f);
        float sliderFontScale = Math.max(1.0f, panelWidth / 760f);
        float contentY = topContentY;
        
        Label quickSettingsLabel = new Label(leftPanelX, contentY, "QUICK SETTINGS",
            0.7f, 0.9f, 0.95f, 1.0f);
        quickSettingsLabel.setScale(Math.max(1.1f, sliderFontScale * 0.95f));
        addComponent(quickSettingsLabel);
        contentY += 38f;
        fovSlider = new Slider(leftPanelX, contentY, leftPanelWidth, sliderHeight,
            "Field of View", 60, 110, settings.graphics.fov,
            value -> settings.graphics.fov = value);
        fovSlider.setDecimalPlaces(0);
        fovSlider.setFontScale(sliderFontScale, sliderFontScale);
        addComponent(fovSlider);
        contentY += sliderHeight + sliderSpacing;
        
        masterVolumeSlider = new Slider(leftPanelX, contentY, leftPanelWidth, sliderHeight,
            "Master Volume", 0, 100, settings.audio.masterVolume * 100,
            value -> settings.audio.masterVolume = value / 100.0f);
        masterVolumeSlider.setDecimalPlaces(0);
        masterVolumeSlider.setFontScale(sliderFontScale, sliderFontScale);
        addComponent(masterVolumeSlider);
        contentY += sliderHeight + sliderSpacing;
        
        renderDistanceSlider = new Slider(leftPanelX, contentY, leftPanelWidth, sliderHeight,
            "Render Distance", 4, 16, settings.graphics.renderDistance,
            value -> settings.graphics.renderDistance = (int) value.floatValue());
        renderDistanceSlider.setDecimalPlaces(0);
        renderDistanceSlider.setFontScale(sliderFontScale, sliderFontScale);
        addComponent(renderDistanceSlider);
        contentY += sliderHeight + sliderSpacing;
        
        mouseSensitivitySlider = new Slider(leftPanelX, contentY, leftPanelWidth, sliderHeight,
            "Mouse Sensitivity", 0.01f, 0.5f, settings.controls.mouseSensitivity,
            value -> settings.controls.mouseSensitivity = value);
        mouseSensitivitySlider.setDecimalPlaces(2);
        mouseSensitivitySlider.setFontScale(sliderFontScale, sliderFontScale);
        addComponent(mouseSensitivitySlider);
        
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
        
        contentY = topContentY;
        Label modsLabel = new Label(rightPanelX, contentY, "LOADED MODS",
            0.95f, 0.7f, 0.9f, 1.0f);
        modsLabel.setScale(Math.max(1.1f, sliderFontScale * 0.95f));
        addComponent(modsLabel);
        contentY += 38f;
        
        // Display loaded mods - if any exist
        if (game != null && game.getModLoader() != null) {
            ModLoader modLoader = game.getModLoader();
            List<ModContainer> mods = modLoader.getLoadedMods();
            
            if (mods.isEmpty()) {
                Label noModsLabel = new Label(rightPanelX, contentY, "No mods loaded",
                    0.6f, 0.6f, 0.6f, 0.8f);
                noModsLabel.setScale(sliderFontScale * 0.85f);
                addComponent(noModsLabel);
            } else {
                for (int i = 0; i < Math.min(mods.size(), 8); i++) {
                    ModContainer mod = mods.get(i);
                    String modText = String.format("• %s v%s", mod.getName(), mod.getVersion());
                    Label modLabel = new Label(rightPanelX, contentY, modText,
                        0.8f, 0.95f, 0.8f, 0.9f);
                    modLabel.setScale(sliderFontScale * 0.8f);
                    addComponent(modLabel);
                    contentY += 22f * sliderFontScale;
                }
                if (mods.size() > 8) {
                    Label moreLabel = new Label(rightPanelX, contentY,
                        String.format("... and %d more", mods.size() - 8),
                        0.6f, 0.6f, 0.6f, 0.8f);
                    moreLabel.setScale(sliderFontScale * 0.75f);
                    addComponent(moreLabel);
                }
            }
        } else {
            Label errorLabel = new Label(rightPanelX, contentY, "Mod system unavailable",
                0.8f, 0.5f, 0.5f, 0.8f);
            errorLabel.setScale(sliderFontScale * 0.85f);
            addComponent(errorLabel);
        }
        
        contentY = Math.max(contentY + 36f, topContentY + columnWidth * 0.25f);
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
        
        float buttonY = panelY + panelHeight - 72f;
        float buttonHeight = Math.max(56f, panelHeight * 0.11f);
        float buttonSpacing = 10f;
        float buttonCount = 5f;
        float buttonWidth = (panelWidth - contentPadding * 2f - buttonSpacing * (buttonCount - 1f)) / buttonCount;
        float buttonX = panelX + contentPadding;

        MenuButton resumeButton = new MenuButton(buttonX, buttonY, buttonWidth, buttonHeight,
            "RESUME", () -> uiManager.setState(GameState.IN_GAME));
        addComponent(resumeButton);

        MenuButton settingsButton = new MenuButton(buttonX + (buttonWidth + buttonSpacing) * 1f, buttonY,
            buttonWidth, buttonHeight, "SETTINGS",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                uiManager.setState(GameState.SETTINGS_MENU);
            });
        addComponent(settingsButton);

        MenuButton skinsButton = new MenuButton(buttonX + (buttonWidth + buttonSpacing) * 2f, buttonY,
            buttonWidth, buttonHeight, "SKINS",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                uiManager.setState(GameState.SKIN_MANAGER);
            });
        addComponent(skinsButton);
        skinsButton.setEnabled(uiManager.getCurrentState() != GameState.SKIN_MANAGER);

        MenuButton saveButton = new MenuButton(buttonX + (buttonWidth + buttonSpacing) * 3f, buttonY,
            buttonWidth, buttonHeight, "SAVE",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                System.out.println("[PauseScreen] Settings saved!");
            });
        addComponent(saveButton);

        MenuButton quitButton = new MenuButton(buttonX + (buttonWidth + buttonSpacing) * 4f, buttonY,
            buttonWidth, buttonHeight, "QUIT",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                System.out.println("[PauseScreen] Saving and returning to main menu...");
                uiManager.setState(GameState.MAIN_MENU);
            });
        addComponent(quitButton);
        
        float footerBaseline = panelY + panelHeight - 24f;
        Label hintLabel = new Label(centerX, footerBaseline,
            "Press ESC to resume | Changes are applied instantly",
            0.7f, 0.5f, 0.9f, 0.7f);
        hintLabel.setCentered(true);
        hintLabel.setScale(Math.max(0.9f, panelWidth / 800f));
        addComponent(hintLabel);
        
        System.out.println("[PauseScreen] Layout initialized for " + windowWidth + "x" + windowHeight);
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
        renderer.drawRect(0, 0, windowWidth, windowHeight,
            0.05f, 0.02f, 0.10f, 0.85f);
        
        renderer.drawRect(panelX, panelY, panelWidth, panelHeight,
            0.08f, 0.05f, 0.12f, 0.95f);
        float border = Math.max(2f, panelWidth * 0.003f);
        renderer.drawRect(panelX, panelY, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX, panelY + panelHeight - border, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX + panelWidth - border, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX + panelWidth / 2f, panelY + contentPadding * 1.2f,
            2f, panelHeight - contentPadding * 2.4f, 0.0f, 0.95f, 0.95f, 0.32f);
        
        super.render(renderer, fontRenderer);
    }
}
