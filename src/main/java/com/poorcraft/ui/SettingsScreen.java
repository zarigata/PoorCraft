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
    private static final int TAB_COUNT = 4;
    private final float[] tabScrollOffsets;
    private ScrollContainer activeScrollContainer;
    private int activeScrollTabIndex = -1;

    private static final String[] TAB_NAMES = {"Graphics", "Audio", "Controls", "AI"};

    private boolean componentsInitialized;

    private Label titleLabel;
    private Button[] tabButtons;
    private ScrollContainer tabContentContainer;
    private Button applyButton;
    private Button cancelButton;
    
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
                          Settings settings, ConfigManager configManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.originalSettings = settings;
        this.configManager = configManager;
        this.gson = new GsonBuilder().create();
        this.currentTab = TAB_GRAPHICS;
        this.tabScrollOffsets = new float[TAB_COUNT];
    }
    
    @Override
    public void init() {
        ensureWorkingSettings();
        if (!componentsInitialized) {
            clearComponents();
            createBaseComponents();
            componentsInitialized = true;
        }
        rebuildLayout();
    }

    private void rebuildLayout() {
        ensureWorkingSettings();
        storeActiveScrollOffset();
        updateLayout();
        rebuildTabContent();
    }

    private void createBaseComponents() {
        titleLabel = new Label(0f, 0f, "Settings", 1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);

        tabButtons = new Button[TAB_COUNT];
        for (int i = 0; i < TAB_COUNT; i++) {
            final int tabIndex = i;
            Button tabButton = new Button(0f, 0f, 0f, 0f,
                TAB_NAMES[i], () -> switchTab(tabIndex));
            tabButtons[i] = tabButton;
            addComponent(tabButton);
        }

        tabContentContainer = new ScrollContainer(0f, 0f, 0f, 0f);
        addComponent(tabContentContainer);
        activeScrollContainer = tabContentContainer;
        activeScrollTabIndex = currentTab;

        applyButton = new Button(0f, 0f, 0f, 0f, "Apply", this::onApply);
        addComponent(applyButton);

        cancelButton = new Button(0f, 0f, 0f, 0f, "Cancel", this::onCancel);
        addComponent(cancelButton);

        updateTabButtonStates();
    }

    private void updateLayout() {
        if (!componentsInitialized) {
            return;
        }

        float centerX = windowWidth / 2.0f;
        float effectiveScale = scaleManager != null ? scaleManager.getEffectiveScale() : 1.0f;

        // Title
        if (titleLabel != null) {
            titleLabel.setScale(effectiveScale);
            titleLabel.setPosition(centerX, scaleDimension(30f));
        }

        // Tab buttons
        float tabY = scaleDimension(70f);
        float tabWidth = scaleDimension(120f);
        float tabHeight = scaleDimension(35f);
        float tabSpacing = scaleDimension(5f);
        float totalTabWidth = tabWidth * TAB_COUNT + tabSpacing * (TAB_COUNT - 1);
        float tabStartX = centerX - totalTabWidth / 2f;

        if (tabButtons != null) {
            for (int i = 0; i < tabButtons.length; i++) {
                Button tabButton = tabButtons[i];
                if (tabButton != null) {
                    tabButton.setBounds(
                        tabStartX + i * (tabWidth + tabSpacing),
                        tabY,
                        tabWidth,
                        tabHeight
                    );
                }
            }
        }

        // Content container
        float contentWidth = scaleDimension(400f);
        float contentX = centerX - contentWidth / 2f;
        float contentY = scaleDimension(130f);
        float contentHeight = Math.max(scaleDimension(200f), windowHeight - contentY - scaleDimension(160f));

        if (tabContentContainer != null) {
            tabContentContainer.setBounds(contentX, contentY, contentWidth, contentHeight);
            tabContentContainer.requestLayout();
        }

        // Bottom buttons
        float buttonY = windowHeight - scaleDimension(60f);
        float buttonWidth = scaleDimension(100f);
        float buttonHeight = scaleDimension(40f);
        float buttonSpacing = scaleDimension(10f);

        if (applyButton != null) {
            applyButton.setBounds(
                centerX - buttonWidth - buttonSpacing / 2f,
                buttonY,
                buttonWidth,
                buttonHeight
            );
        }
        if (cancelButton != null) {
            cancelButton.setBounds(
                centerX + buttonSpacing / 2f,
                buttonY,
                buttonWidth,
                buttonHeight
            );
        }

        updateTabButtonStates();
    }

    private void rebuildTabContent() {
        if (tabContentContainer == null) {
            return;
        }

        float restoreOffset = tabScrollOffsets[currentTab];
        tabContentContainer.clearChildren();

        float contentY = tabContentContainer.getY();
        float contentX = tabContentContainer.getX();
        float controlWidth = tabContentContainer.getWidth();
        float controlHeight = scaleDimension(35f);
        float spacing = scaleDimension(50f);

        switch (currentTab) {
            case TAB_GRAPHICS -> buildGraphicsTab(tabContentContainer, contentX, contentY, controlWidth, controlHeight, spacing);
            case TAB_AUDIO -> buildAudioTab(tabContentContainer, contentX, contentY, controlWidth, controlHeight, spacing);
            case TAB_CONTROLS -> buildControlsTab(tabContentContainer, contentX, contentY, controlWidth, controlHeight, spacing);
            case TAB_AI -> buildAITab(tabContentContainer, contentX, contentY, controlWidth, controlHeight, spacing);
            default -> {}
        }

        tabContentContainer.requestLayout();
        tabContentContainer.setScrollOffsetForRestore(restoreOffset);
        activeScrollContainer = tabContentContainer;
        activeScrollTabIndex = currentTab;
    }

    private void updateTabButtonStates() {
        if (tabButtons == null) {
            return;
        }
        for (int i = 0; i < tabButtons.length; i++) {
            Button button = tabButtons[i];
            if (button != null) {
                button.setEnabled(i != currentTab);
            }
        }
    }

    private void ensureWorkingSettings() {
        if (workingSettings == null) {
            workingSettings = cloneSettings(originalSettings);
        }
    }

    private Settings cloneSettings(Settings source) {
        String json = gson.toJson(source);
        return gson.fromJson(json, Settings.class);
    }

    /**
     * Switches to a different tab.
     */
    private void switchTab(int tabIndex) {
        storeActiveScrollOffset();
        currentTab = tabIndex;
        rebuildLayout(); // Rebuild screen without resetting working settings
    }
    
    /**
     * Builds components for the current tab.
     */
    private void storeActiveScrollOffset() {
        if (activeScrollContainer != null
            && activeScrollTabIndex == currentTab
            && activeScrollTabIndex >= 0
            && activeScrollTabIndex < tabScrollOffsets.length) {
            tabScrollOffsets[activeScrollTabIndex] = activeScrollContainer.getScrollOffset();
        }
    }
    
    /**
     * Builds the Graphics tab.
     */
    private void buildGraphicsTab(ScrollContainer container, float x, float y, float width, float height, float spacing) {
        float currentY = y;

        // FOV slider
        Slider fovSlider = new Slider(x, currentY, width, height,
            "Field of View", 60, 110, workingSettings.graphics.fov,
            value -> workingSettings.graphics.fov = value);
        fovSlider.setDecimalPlaces(0);
        container.addChild(fovSlider);
        currentY += spacing;

        // Render distance slider
        Slider renderDistanceSlider = new Slider(x, currentY, width, height,
            "Render Distance (chunks)", 4, 16, workingSettings.graphics.renderDistance,
            value -> workingSettings.graphics.renderDistance = (int) value.floatValue());
        renderDistanceSlider.setDecimalPlaces(0);
        container.addChild(renderDistanceSlider);
        currentY += spacing;

        // Max FPS slider
        Slider maxFpsSlider = new Slider(x, currentY, width, height,
            "Max FPS", 30, 240, workingSettings.graphics.maxFps,
            value -> workingSettings.graphics.maxFps = (int) value.floatValue());
        maxFpsSlider.setDecimalPlaces(0);
        container.addChild(maxFpsSlider);
        currentY += spacing;

        // Target FPS slider
        Slider targetFpsSlider = new Slider(x, currentY, width, height,
            "Target FPS", 30, 144, workingSettings.graphics.targetFPS,
            value -> workingSettings.graphics.targetFPS = (int) value.floatValue());
        targetFpsSlider.setDecimalPlaces(0);
        container.addChild(targetFpsSlider);
        currentY += spacing;

        // Chunk load rate slider
        Slider chunkLoadSlider = new Slider(x, currentY, width, height,
            "Chunk Load Rate", 1, 8, workingSettings.graphics.chunkLoadRate,
            value -> workingSettings.graphics.chunkLoadRate = (int) value.floatValue());
        chunkLoadSlider.setDecimalPlaces(0);
        container.addChild(chunkLoadSlider);
        currentY += spacing;

        // Memory budget slider
        Slider memoryBudgetSlider = new Slider(x, currentY, width, height,
            "Memory Budget (MB)", 256, 2048, workingSettings.graphics.memoryBudgetMB,
            value -> workingSettings.graphics.memoryBudgetMB = (int) value.floatValue());
        memoryBudgetSlider.setDecimalPlaces(0);
        container.addChild(memoryBudgetSlider);
        currentY += spacing;

        // Render quality dropdown
        Label qualityLabel = new Label(x, currentY, "Terrain Quality:");
        container.addChild(qualityLabel);
        currentY += scaleDimension(25f);

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
        container.addChild(qualityDropdown);
        currentY += spacing;

        // VSync checkbox
        Checkbox vsyncCheckbox = new Checkbox(x, currentY, 20,
            "VSync", workingSettings.window.vsync,
            checked -> workingSettings.window.vsync = checked);
        container.addChild(vsyncCheckbox);
        currentY += spacing;

        // Fullscreen checkbox
        Checkbox fullscreenCheckbox = new Checkbox(x, currentY, 20,
            "Fullscreen", workingSettings.window.fullscreen,
            checked -> workingSettings.window.fullscreen = checked);
        container.addChild(fullscreenCheckbox);
        currentY += spacing;

        // Chunk compression checkbox
        Checkbox compressionCheckbox = new Checkbox(x, currentY, 20,
            "Chunk Compression", workingSettings.graphics.useChunkCompression,
            checked -> workingSettings.graphics.useChunkCompression = checked);
        container.addChild(compressionCheckbox);
        currentY += spacing;

        // Adaptive loading checkbox
        Checkbox adaptiveLoadingCheckbox = new Checkbox(x, currentY, 20,
            "Adaptive Loading", workingSettings.graphics.adaptiveLoading,
            checked -> workingSettings.graphics.adaptiveLoading = checked);
        container.addChild(adaptiveLoadingCheckbox);
        currentY += spacing;

        // Use UBOs checkbox
        Checkbox ubosCheckbox = new Checkbox(x, currentY, 20,
            "Use UBOs", workingSettings.graphics.useUBOs,
            checked -> workingSettings.graphics.useUBOs = checked);
        container.addChild(ubosCheckbox);
        currentY += spacing;

        // Aggressive culling checkbox
        Checkbox aggressiveCullingCheckbox = new Checkbox(x, currentY, 20,
            "Aggressive Culling", workingSettings.graphics.aggressiveCulling,
            checked -> workingSettings.graphics.aggressiveCulling = checked);
        container.addChild(aggressiveCullingCheckbox);
        currentY += spacing;
        
        // UI & Visual Effects section header
        Label uiEffectsLabel = new Label(x, currentY, "UI & Visual Effects", 0.9f, 0.9f, 0.7f, 1.0f);
        container.addChild(uiEffectsLabel);
        currentY += 30;
        
        // Animated Menu Background checkbox
        Checkbox animatedBgCheckbox = new Checkbox(x, currentY, 20,
            "Animated Menu Background", workingSettings.graphics.animatedMenuBackground,
            checked -> workingSettings.graphics.animatedMenuBackground = checked);
        container.addChild(animatedBgCheckbox);
        currentY += spacing;
        
        // Menu Animation Speed slider
        Slider menuSpeedSlider = new Slider(x, currentY, width, height,
            "Menu Animation Speed", 0.5f, 2.0f, workingSettings.graphics.menuBackgroundSpeed,
            value -> workingSettings.graphics.menuBackgroundSpeed = value);
        menuSpeedSlider.setDecimalPlaces(1);
        menuSpeedSlider.setEnabled(workingSettings.graphics.animatedMenuBackground);
        container.addChild(menuSpeedSlider);
        currentY += spacing;
        
        // Head Bobbing checkbox
        Checkbox headBobbingCheckbox = new Checkbox(x, currentY, 20,
            "Head Bobbing", workingSettings.graphics.headBobbing,
            checked -> workingSettings.graphics.headBobbing = checked);
        container.addChild(headBobbingCheckbox);
        currentY += spacing;
        
        // Head Bobbing Intensity slider
        Slider bobbingIntensitySlider = new Slider(x, currentY, width, height,
            "Head Bobbing Intensity", 0.0f, 2.0f, workingSettings.graphics.headBobbingIntensity,
            value -> workingSettings.graphics.headBobbingIntensity = value);
        bobbingIntensitySlider.setDecimalPlaces(1);
        bobbingIntensitySlider.setEnabled(workingSettings.graphics.headBobbing);
        container.addChild(bobbingIntensitySlider);
        currentY += spacing;
        
        // UI Scale slider with live preview
        Slider uiScaleSlider = new Slider(x, currentY, width, height,
            "UI Scale", 0.5f, 2.0f, workingSettings.graphics.uiScale,
            value -> {
                workingSettings.graphics.uiScale = value;
                // Live preview: update scaleManager and trigger layout refresh
                if (scaleManager != null) {
                    scaleManager.setUserScale(value);
                    int newSize = scaleManager.getFontSize();
                    if (uiManager != null && uiManager.getFontRenderer() != null
                        && uiManager.getFontRenderer().getFontSize() != newSize) {
                        uiManager.getFontRenderer().setFontSize(newSize);
                    }
                    // Rebuild layout with new scale without resetting workingSettings
                    rebuildLayout();
                }
            }
        );
        uiScaleSlider.setDecimalPlaces(2);
        container.addChild(uiScaleSlider);
        currentY += spacing;
        
        // Pause Menu Blur checkbox
        Checkbox pauseBlurCheckbox = new Checkbox(x, currentY, 20,
            "Pause Menu Blur", workingSettings.graphics.pauseMenuBlur,
            checked -> workingSettings.graphics.pauseMenuBlur = checked);
        container.addChild(pauseBlurCheckbox);
    }
    
    /**
     * Builds the Audio tab.
     */
    private void buildAudioTab(ScrollContainer container, float x, float y, float width, float height, float spacing) {
        float currentY = y;
        
        // Master volume slider
        Slider masterVolumeSlider = new Slider(x, currentY, width, height,
            "Master Volume", 0, 100, workingSettings.audio.masterVolume * 100,
            value -> workingSettings.audio.masterVolume = value / 100.0f);
        masterVolumeSlider.setDecimalPlaces(0);
        container.addChild(masterVolumeSlider);
        currentY += spacing;
        
        // Music volume slider
        Slider musicVolumeSlider = new Slider(x, currentY, width, height,
            "Music Volume", 0, 100, workingSettings.audio.musicVolume * 100,
            value -> workingSettings.audio.musicVolume = value / 100.0f);
        musicVolumeSlider.setDecimalPlaces(0);
        container.addChild(musicVolumeSlider);
        currentY += spacing;
        
        // SFX volume slider
        Slider sfxVolumeSlider = new Slider(x, currentY, width, height,
            "SFX Volume", 0, 100, workingSettings.audio.sfxVolume * 100,
            value -> workingSettings.audio.sfxVolume = value / 100.0f);
        sfxVolumeSlider.setDecimalPlaces(0);
        container.addChild(sfxVolumeSlider);
    }
    
    /**
     * Builds the Controls tab.
     */
    private void buildControlsTab(ScrollContainer container, float x, float y, float width, float height, float spacing) {
        float currentY = y;
        
        // Mouse sensitivity slider
        Slider sensitivitySlider = new Slider(x, currentY, width, height,
            "Mouse Sensitivity", 0.01f, 0.5f, workingSettings.controls.mouseSensitivity,
            value -> workingSettings.controls.mouseSensitivity = value);
        sensitivitySlider.setDecimalPlaces(2);
        container.addChild(sensitivitySlider);
        currentY += spacing;
        
        // Invert Y checkbox
        Checkbox invertYCheckbox = new Checkbox(x, currentY, 20,
            "Invert Y Axis", workingSettings.controls.invertY,
            checked -> workingSettings.controls.invertY = checked);
        container.addChild(invertYCheckbox);
        currentY += spacing;
        
        // Note about keybinds
        Label keybindNote = new Label(x, currentY,
            "Keybind customization coming soon!", 
            0.7f, 0.7f, 0.7f, 1.0f);
        container.addChild(keybindNote);
    }
    
    /**
     * Builds the AI tab.
     */
    private void buildAITab(ScrollContainer container, float x, float y, float width, float height, float spacing) {
        float currentY = y;
        
        // Enable AI checkbox
        Checkbox aiEnabledCheckbox = new Checkbox(x, currentY, 20,
            "Enable AI NPCs", workingSettings.ai.aiEnabled,
            checked -> workingSettings.ai.aiEnabled = checked);
        container.addChild(aiEnabledCheckbox);
        currentY += spacing;
        
        // AI provider dropdown
        Label providerLabel = new Label(x, currentY, "AI Provider:");
        container.addChild(providerLabel);
        currentY += 25;
        
        java.util.List<String> providerOptions = Arrays.asList("ollama", "gemini", "openrouter");
        int selectedProviderIndex = providerOptions.indexOf(workingSettings.ai.aiProvider);
        if (selectedProviderIndex < 0) {
            selectedProviderIndex = 0;
        }
        Dropdown providerDropdown = new Dropdown(x, currentY, width, height,
            providerOptions,
            selectedProviderIndex,
            index -> {
                String[] providers = {"ollama", "gemini", "openrouter"};
                workingSettings.ai.aiProvider = providers[index];
            });
        container.addChild(providerDropdown);
        currentY += spacing + 10;
        
        // Note
        Label aiNote = new Label(x, currentY,
            "Use the AI Companion settings screen for full configuration.", 
            0.7f, 0.7f, 0.7f, 1.0f);
        container.addChild(aiNote);
        currentY += spacing;

        float buttonHeight = scaleDimension(40f);
        Button advancedButton = new Button(x, currentY, width, buttonHeight,
            "Configure AI Companion…",
            () -> uiManager.setState(GameState.AI_COMPANION_SETTINGS));
        container.addChild(advancedButton);
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
        Settings newSettings = cloneSettings(workingSettings);
        
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

        // Keep working copy in sync with saved configuration
        workingSettings = cloneSettings(originalSettings);
        
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

        // Reset working copy so unsaved edits are cleared next time
        workingSettings = cloneSettings(originalSettings);
        
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
        rebuildLayout();
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw background
        renderer.drawRect(0, 0, windowWidth, windowHeight, 0.1f, 0.1f, 0.15f, 1.0f);
        
        // Draw all components
        super.render(renderer, fontRenderer);
    }
}
