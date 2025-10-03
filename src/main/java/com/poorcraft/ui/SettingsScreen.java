package com.poorcraft.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;

import java.util.Arrays;

/**
 * Settings menu screen with tabbed interface.
 * 
 * Allows configuring graphics, audio, controls, and AI settings.
 * Changes are previewed in real-time but not saved until Apply is clicked.
 * 
 * This is probably the most complex screen in the game. Lots of widgets,
 * lots of settings, lots of potential for bugs. I tested it though. Mostly.
 */
public class SettingsScreen extends UIScreen {
    
    private static final int TAB_GRAPHICS = 0;
    private static final int TAB_AUDIO = 1;
    private static final int TAB_CONTROLS = 2;
    private static final int TAB_AI = 3;
    
    private UIManager uiManager;
    private Settings workingSettings;
    private Settings originalSettings;
    private ConfigManager configManager;
    private Gson gson;
    
    private int currentTab;
    
    /**
     * Creates the settings screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     * @param settings Current settings
     * @param configManager Config manager
     */
    public SettingsScreen(int windowWidth, int windowHeight, UIManager uiManager, 
                          Settings settings, ConfigManager configManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
        this.originalSettings = settings;
        this.configManager = configManager;
        this.gson = new GsonBuilder().create();
        this.currentTab = TAB_GRAPHICS;
    }
    
    @Override
    public void init() {
        // Create working copy of settings
        String json = gson.toJson(originalSettings);
        workingSettings = gson.fromJson(json, Settings.class);
        
        clearComponents();
        
        float centerX = windowWidth / 2.0f;
        
        // Title
        Label titleLabel = new Label(centerX, 30, "Settings", 1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // Tab buttons
        float tabY = 70;
        float tabWidth = 120;
        float tabHeight = 35;
        float tabSpacing = 5;
        float tabStartX = centerX - (tabWidth * 4 + tabSpacing * 3) / 2;
        
        String[] tabNames = {"Graphics", "Audio", "Controls", "AI"};
        for (int i = 0; i < tabNames.length; i++) {
            final int tabIndex = i;
            Button tabButton = new Button(
                tabStartX + i * (tabWidth + tabSpacing), tabY,
                tabWidth, tabHeight,
                tabNames[i],
                () -> switchTab(tabIndex)
            );
            // Highlight current tab
            if (i != currentTab) {
                // Make inactive tabs slightly darker
                tabButton.setEnabled(true);
            }
            addComponent(tabButton);
        }
        
        // Build tab content
        buildTabComponents();
        
        // Bottom buttons
        float buttonY = windowHeight - 60;
        float buttonWidth = 100;
        float buttonHeight = 40;
        float buttonSpacing = 10;
        
        Button applyButton = new Button(
            centerX - buttonWidth - buttonSpacing / 2, buttonY,
            buttonWidth, buttonHeight,
            "Apply",
            this::onApply
        );
        addComponent(applyButton);
        
        Button cancelButton = new Button(
            centerX + buttonSpacing / 2, buttonY,
            buttonWidth, buttonHeight,
            "Cancel",
            this::onCancel
        );
        addComponent(cancelButton);
    }
    
    /**
     * Switches to a different tab.
     */
    private void switchTab(int tabIndex) {
        currentTab = tabIndex;
        init(); // Rebuild screen
    }
    
    /**
     * Builds components for the current tab.
     */
    private void buildTabComponents() {
        float contentY = 130;
        float contentX = windowWidth / 2.0f - 200;
        float controlWidth = 400;
        float controlHeight = 35;
        float spacing = 50;
        
        switch (currentTab) {
            case TAB_GRAPHICS:
                buildGraphicsTab(contentX, contentY, controlWidth, controlHeight, spacing);
                break;
            case TAB_AUDIO:
                buildAudioTab(contentX, contentY, controlWidth, controlHeight, spacing);
                break;
            case TAB_CONTROLS:
                buildControlsTab(contentX, contentY, controlWidth, controlHeight, spacing);
                break;
            case TAB_AI:
                buildAITab(contentX, contentY, controlWidth, controlHeight, spacing);
                break;
        }
    }
    
    /**
     * Builds the Graphics tab.
     */
    private void buildGraphicsTab(float x, float y, float width, float height, float spacing) {
        float currentY = y;
        
        // FOV slider
        Slider fovSlider = new Slider(x, currentY, width, height,
            "Field of View", 60, 110, workingSettings.graphics.fov,
            value -> workingSettings.graphics.fov = value);
        fovSlider.setDecimalPlaces(0);
        addComponent(fovSlider);
        currentY += spacing;
        
        // Render distance slider
        Slider renderDistanceSlider = new Slider(x, currentY, width, height,
            "Render Distance (chunks)", 4, 16, workingSettings.graphics.renderDistance,
            value -> workingSettings.graphics.renderDistance = (int) value.floatValue());
        renderDistanceSlider.setDecimalPlaces(0);
        addComponent(renderDistanceSlider);
        currentY += spacing;
        
        // Max FPS slider
        Slider maxFpsSlider = new Slider(x, currentY, width, height,
            "Max FPS", 30, 240, workingSettings.graphics.maxFps,
            value -> workingSettings.graphics.maxFps = (int) value.floatValue());
        maxFpsSlider.setDecimalPlaces(0);
        addComponent(maxFpsSlider);
        currentY += spacing;
        
        // VSync checkbox
        Checkbox vsyncCheckbox = new Checkbox(x, currentY, 20,
            "VSync", workingSettings.window.vsync,
            checked -> workingSettings.window.vsync = checked);
        addComponent(vsyncCheckbox);
        currentY += spacing;
        
        // Fullscreen checkbox
        Checkbox fullscreenCheckbox = new Checkbox(x, currentY, 20,
            "Fullscreen", workingSettings.window.fullscreen,
            checked -> workingSettings.window.fullscreen = checked);
        addComponent(fullscreenCheckbox);
    }
    
    /**
     * Builds the Audio tab.
     */
    private void buildAudioTab(float x, float y, float width, float height, float spacing) {
        float currentY = y;
        
        // Master volume slider
        Slider masterVolumeSlider = new Slider(x, currentY, width, height,
            "Master Volume", 0, 100, workingSettings.audio.masterVolume * 100,
            value -> workingSettings.audio.masterVolume = value / 100.0f);
        masterVolumeSlider.setDecimalPlaces(0);
        addComponent(masterVolumeSlider);
        currentY += spacing;
        
        // Music volume slider
        Slider musicVolumeSlider = new Slider(x, currentY, width, height,
            "Music Volume", 0, 100, workingSettings.audio.musicVolume * 100,
            value -> workingSettings.audio.musicVolume = value / 100.0f);
        musicVolumeSlider.setDecimalPlaces(0);
        addComponent(musicVolumeSlider);
        currentY += spacing;
        
        // SFX volume slider
        Slider sfxVolumeSlider = new Slider(x, currentY, width, height,
            "SFX Volume", 0, 100, workingSettings.audio.sfxVolume * 100,
            value -> workingSettings.audio.sfxVolume = value / 100.0f);
        sfxVolumeSlider.setDecimalPlaces(0);
        addComponent(sfxVolumeSlider);
    }
    
    /**
     * Builds the Controls tab.
     */
    private void buildControlsTab(float x, float y, float width, float height, float spacing) {
        float currentY = y;
        
        // Mouse sensitivity slider
        Slider sensitivitySlider = new Slider(x, currentY, width, height,
            "Mouse Sensitivity", 0.01f, 0.5f, workingSettings.controls.mouseSensitivity,
            value -> workingSettings.controls.mouseSensitivity = value);
        sensitivitySlider.setDecimalPlaces(2);
        addComponent(sensitivitySlider);
        currentY += spacing;
        
        // Invert Y checkbox
        Checkbox invertYCheckbox = new Checkbox(x, currentY, 20,
            "Invert Y Axis", workingSettings.controls.invertY,
            checked -> workingSettings.controls.invertY = checked);
        addComponent(invertYCheckbox);
        currentY += spacing;
        
        // Note about keybinds
        Label keybindNote = new Label(x, currentY,
            "Keybind customization coming soon!", 
            0.7f, 0.7f, 0.7f, 1.0f);
        addComponent(keybindNote);
    }
    
    /**
     * Builds the AI tab.
     */
    private void buildAITab(float x, float y, float width, float height, float spacing) {
        float currentY = y;
        
        // Enable AI checkbox
        Checkbox aiEnabledCheckbox = new Checkbox(x, currentY, 20,
            "Enable AI NPCs", workingSettings.ai.aiEnabled,
            checked -> workingSettings.ai.aiEnabled = checked);
        addComponent(aiEnabledCheckbox);
        currentY += spacing;
        
        // AI provider dropdown
        Label providerLabel = new Label(x, currentY, "AI Provider:");
        addComponent(providerLabel);
        currentY += 25;
        
        Dropdown providerDropdown = new Dropdown(x, currentY, width, height,
            Arrays.asList("ollama", "gemini", "openrouter", "openai"),
            Arrays.asList("ollama", "gemini", "openrouter", "openai").indexOf(workingSettings.ai.aiProvider),
            index -> {
                String[] providers = {"ollama", "gemini", "openrouter", "openai"};
                workingSettings.ai.aiProvider = providers[index];
            });
        addComponent(providerDropdown);
        currentY += spacing + 10;
        
        // Note
        Label aiNote = new Label(x, currentY,
            "AI features only available in multiplayer", 
            0.7f, 0.7f, 0.7f, 1.0f);
        addComponent(aiNote);
    }
    
    /**
     * Called when Apply button is clicked.
     */
    private void onApply() {
        // Copy working settings to original
        String json = gson.toJson(workingSettings);
        Settings newSettings = gson.fromJson(json, Settings.class);
        
        // Update original settings
        originalSettings.window = newSettings.window;
        originalSettings.graphics = newSettings.graphics;
        originalSettings.audio = newSettings.audio;
        originalSettings.controls = newSettings.controls;
        originalSettings.camera = newSettings.camera;
        originalSettings.ai = newSettings.ai;
        originalSettings.world = newSettings.world;
        
        // Save to disk
        configManager.saveSettings(originalSettings);
        
        System.out.println("[Settings] Settings applied and saved");
        
        // Return to previous screen
        GameState previousState = uiManager.getPreviousState();
        if (previousState != null && previousState != GameState.SETTINGS_MENU) {
            uiManager.setState(previousState);
        } else {
            uiManager.setState(GameState.MAIN_MENU);
        }
    }
    
    /**
     * Called when Cancel button is clicked.
     */
    private void onCancel() {
        // Discard changes
        System.out.println("[Settings] Settings discarded");
        
        // Return to previous screen
        GameState previousState = uiManager.getPreviousState();
        if (previousState != null && previousState != GameState.SETTINGS_MENU) {
            uiManager.setState(previousState);
        } else {
            uiManager.setState(GameState.MAIN_MENU);
        }
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init();
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw background
        renderer.drawRect(0, 0, windowWidth, windowHeight, 0.1f, 0.1f, 0.15f, 1.0f);
        
        // Draw all components
        super.render(renderer, fontRenderer);
    }
}
