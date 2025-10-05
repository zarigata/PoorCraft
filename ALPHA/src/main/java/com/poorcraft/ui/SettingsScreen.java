package com.poorcraft.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;

import java.util.Arrays;
import java.util.Locale;

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
        float uiScale = workingSettings.graphics.uiScale;
        
        // Title
        Label titleLabel = new Label(centerX, 30 * uiScale, "Settings", 1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(uiScale);
        addComponent(titleLabel);
        
        // Tab buttons
        float tabY = 70 * uiScale;
        float tabWidth = 120 * uiScale;
        float tabHeight = 35 * uiScale;
        float tabSpacing = 5 * uiScale;
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
        float buttonY = windowHeight - 60 * uiScale;
        float buttonWidth = 100 * uiScale;
        float buttonHeight = 40 * uiScale;
        float buttonSpacing = 10 * uiScale;
        
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
        float uiScale = workingSettings.graphics.uiScale;
        float contentY = 130 * uiScale;
        float contentX = windowWidth / 2.0f - 200 * uiScale;
        float controlWidth = 400 * uiScale;
        float controlHeight = 35 * uiScale;
        float spacing = 50 * uiScale;
        
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

        // Target FPS slider
        Slider targetFpsSlider = new Slider(x, currentY, width, height,
            "Target FPS", 30, 144, workingSettings.graphics.targetFPS,
            value -> workingSettings.graphics.targetFPS = (int) value.floatValue());
        targetFpsSlider.setDecimalPlaces(0);
        addComponent(targetFpsSlider);
        currentY += spacing;

        // Chunk load rate slider
        Slider chunkLoadSlider = new Slider(x, currentY, width, height,
            "Chunk Load Rate", 1, 8, workingSettings.graphics.chunkLoadRate,
            value -> workingSettings.graphics.chunkLoadRate = (int) value.floatValue());
        chunkLoadSlider.setDecimalPlaces(0);
        addComponent(chunkLoadSlider);
        currentY += spacing;

        // Memory budget slider
        Slider memoryBudgetSlider = new Slider(x, currentY, width, height,
            "Memory Budget (MB)", 256, 2048, workingSettings.graphics.memoryBudgetMB,
            value -> workingSettings.graphics.memoryBudgetMB = (int) value.floatValue());
        memoryBudgetSlider.setDecimalPlaces(0);
        addComponent(memoryBudgetSlider);
        currentY += spacing;

        // Render quality dropdown
        Label qualityLabel = new Label(x, currentY, "Terrain Quality:");
        addComponent(qualityLabel);
        currentY += 25;

        java.util.List<String> qualityOptions = java.util.Arrays.asList("Low", "Medium", "High");
        int currentQualityIndex = qualityOptions.indexOf(capitalize(workingSettings.graphics.renderQuality));
        if (currentQualityIndex < 0) {
            currentQualityIndex = 2; // default High
        }
        Dropdown qualityDropdown = new Dropdown(x, currentY, width, height,
            qualityOptions,
            currentQualityIndex,
            index -> {
                String[] keys = {"low", "medium", "high"};
                workingSettings.graphics.renderQuality = keys[Math.max(0, Math.min(index, keys.length - 1))];
            });
        addComponent(qualityDropdown);
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
        currentY += spacing;

        // Chunk compression checkbox
        Checkbox compressionCheckbox = new Checkbox(x, currentY, 20,
            "Chunk Compression", workingSettings.graphics.useChunkCompression,
            checked -> workingSettings.graphics.useChunkCompression = checked);
        addComponent(compressionCheckbox);
        currentY += spacing;

        // Adaptive loading checkbox
        Checkbox adaptiveLoadingCheckbox = new Checkbox(x, currentY, 20,
            "Adaptive Loading", workingSettings.graphics.adaptiveLoading,
            checked -> workingSettings.graphics.adaptiveLoading = checked);
        addComponent(adaptiveLoadingCheckbox);
        currentY += spacing;

        // Use UBOs checkbox
        Checkbox ubosCheckbox = new Checkbox(x, currentY, 20,
            "Use UBOs", workingSettings.graphics.useUBOs,
            checked -> workingSettings.graphics.useUBOs = checked);
        addComponent(ubosCheckbox);
        currentY += spacing;

        // Aggressive culling checkbox
        Checkbox aggressiveCullingCheckbox = new Checkbox(x, currentY, 20,
            "Aggressive Culling", workingSettings.graphics.aggressiveCulling,
            checked -> workingSettings.graphics.aggressiveCulling = checked);
        addComponent(aggressiveCullingCheckbox);
        currentY += spacing;
        
        // UI & Visual Effects section header
        Label uiEffectsLabel = new Label(x, currentY, "UI & Visual Effects", 0.9f, 0.9f, 0.7f, 1.0f);
        addComponent(uiEffectsLabel);
        currentY += 30;
        
        // Animated Menu Background checkbox
        Checkbox animatedBgCheckbox = new Checkbox(x, currentY, 20,
            "Animated Menu Background", workingSettings.graphics.animatedMenuBackground,
            checked -> workingSettings.graphics.animatedMenuBackground = checked);
        addComponent(animatedBgCheckbox);
        currentY += spacing;
        
        // Menu Animation Speed slider
        Slider menuSpeedSlider = new Slider(x, currentY, width, height,
            "Menu Animation Speed", 0.5f, 2.0f, workingSettings.graphics.menuBackgroundSpeed,
            value -> workingSettings.graphics.menuBackgroundSpeed = value);
        menuSpeedSlider.setDecimalPlaces(1);
        menuSpeedSlider.setEnabled(workingSettings.graphics.animatedMenuBackground);
        addComponent(menuSpeedSlider);
        currentY += spacing;
        
        // Head Bobbing checkbox
        Checkbox headBobbingCheckbox = new Checkbox(x, currentY, 20,
            "Head Bobbing", workingSettings.graphics.headBobbing,
            checked -> workingSettings.graphics.headBobbing = checked);
        addComponent(headBobbingCheckbox);
        currentY += spacing;
        
        // Head Bobbing Intensity slider
        Slider bobbingIntensitySlider = new Slider(x, currentY, width, height,
            "Head Bobbing Intensity", 0.0f, 2.0f, workingSettings.graphics.headBobbingIntensity,
            value -> workingSettings.graphics.headBobbingIntensity = value);
        bobbingIntensitySlider.setDecimalPlaces(1);
        bobbingIntensitySlider.setEnabled(workingSettings.graphics.headBobbing);
        addComponent(bobbingIntensitySlider);
        currentY += spacing;
        
        // UI Scale slider
        Slider uiScaleSlider = new Slider(x, currentY, width, height,
            "UI Scale", 0.75f, 1.5f, workingSettings.graphics.uiScale,
            value -> workingSettings.graphics.uiScale = value);
        uiScaleSlider.setDecimalPlaces(2);
        addComponent(uiScaleSlider);
        currentY += spacing;
        
        // Pause Menu Blur checkbox
        Checkbox pauseBlurCheckbox = new Checkbox(x, currentY, 20,
            "Pause Menu Blur", workingSettings.graphics.pauseMenuBlur,
            checked -> workingSettings.graphics.pauseMenuBlur = checked);
        addComponent(pauseBlurCheckbox);
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

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
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
