package com.poorcraft.ui;

import com.poorcraft.config.Settings;

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
    private float animationTime = 0.0f;
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float contentPadding;
    
    private ConfirmationDialog quitConfirmDialog;
    
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
    }
    @Override
    public void init() {
        clearComponents();
        
        float uiScale = settings.graphics.uiScale;
        panelWidth = LayoutUtils.getMinecraftPanelWidth(windowWidth, uiScale) * 0.85f;
        panelHeight = LayoutUtils.getMinecraftPanelHeight(windowHeight, uiScale) * 0.8f;
        panelWidth = LayoutUtils.clamp(panelWidth, 340f * uiScale, windowWidth - 100f);
        panelHeight = LayoutUtils.clamp(panelHeight, 320f * uiScale, windowHeight - 100f);
        panelX = LayoutUtils.centerHorizontally(windowWidth, panelWidth);
        panelY = LayoutUtils.centerVertically(windowHeight, panelHeight);
        contentPadding = LayoutUtils.getMinecraftPanelPadding(panelWidth);

        float centerX = panelX + panelWidth / 2f;

        float titleScale = LayoutUtils.MINECRAFT_TITLE_SCALE * uiScale;
        float titleBaseline = panelY + contentPadding;
        Label titleLabel = new Label(centerX, titleBaseline, "GAME PAUSED",
            0.02f, 0.96f, 0.96f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        titleLabel.setUseTextShadow(true);
        addComponent(titleLabel);


        float postTitleSpacing = Math.max(52f * uiScale, titleScale * 32f) * 1.5f;

        float buttonWidth = LayoutUtils.getMinecraftButtonWidth(windowWidth, uiScale) * 0.9f;
        float buttonHeight = LayoutUtils.getMinecraftButtonHeight(windowHeight, uiScale);
        float buttonSpacing = LayoutUtils.getMinecraftButtonSpacing(buttonHeight);
        float buttonStackHeight = LayoutUtils.calculateButtonStackHeight(5, buttonHeight, buttonSpacing);
        float defaultButtonOffset = LayoutUtils.centerVertically((int) panelHeight, buttonStackHeight);
        float defaultButtonStart = panelY + defaultButtonOffset;
        float contentTop = titleBaseline + postTitleSpacing;
        float buttonStartY = Math.max(contentTop, defaultButtonStart);
        float buttonX = panelX + (panelWidth - buttonWidth) / 2f;

        MenuButton resumeButton = new MenuButton(buttonX, buttonStartY, buttonWidth, buttonHeight,
            "RESUME GAME", () -> uiManager.setState(GameState.IN_GAME));
        addComponent(resumeButton);

        MenuButton settingsButton = new MenuButton(buttonX, buttonStartY + (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
            "SETTINGS",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                uiManager.setState(GameState.SETTINGS_MENU);
            });
        addComponent(settingsButton);

        MenuButton modsButton = new MenuButton(buttonX, buttonStartY + 2f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
            "MODS",
            () -> {
                // TODO: Navigate to dedicated mods screen
                System.out.println("[PauseScreen] Mods screen not yet implemented");
            });
        addComponent(modsButton);

        MenuButton saveButton = new MenuButton(buttonX, buttonStartY + 3f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
            "SAVE SETTINGS",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                System.out.println("[PauseScreen] Settings saved!");
            });
        addComponent(saveButton);

        MenuButton quitButton = new MenuButton(buttonX, buttonStartY + 4f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
            "QUIT TO MENU",
            () -> {
                if (quitConfirmDialog != null) {
                    quitConfirmDialog.show(windowWidth, windowHeight);
                }
            });
        addComponent(quitButton);
        
        // Create quit confirmation dialog
        quitConfirmDialog = new ConfirmationDialog(
            "Are you sure you want to quit to the main menu?",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                System.out.println("[PauseScreen] Saving and returning to main menu...");
                uiManager.setState(GameState.MAIN_MENU);
            },
            () -> {
                System.out.println("[PauseScreen] Quit cancelled");
            }
        );
        addComponent(quitConfirmDialog);
        
        // Footer hint
        float footerBaseline = panelY + panelHeight - 16f;
        Label hintLabel = new Label(centerX, footerBaseline,
            "Press ESC to resume",
            0.7f, 0.5f, 0.9f, 0.7f);
        hintLabel.setCentered(true);
        hintLabel.setScale(Math.max(1.0f, LayoutUtils.MINECRAFT_LABEL_SCALE * uiScale));
        hintLabel.setUseTextShadow(true);
        addComponent(hintLabel);
        
        System.out.println("[PauseScreen] Layout initialized for " + windowWidth + "x" + windowHeight);
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
        // Adjust overlay alpha based on blur state
        // When blur is active, use lighter overlay; otherwise use darker overlay for readability
        float overlayAlpha = settings.graphics.pauseMenuBlur ? 0.40f : 0.92f;
        renderer.drawRect(0, 0, windowWidth, windowHeight,
            0.03f, 0.01f, 0.08f, overlayAlpha);
        
        renderer.drawDropShadow(panelX, panelY, panelWidth, panelHeight, 8f, 0.5f);
        renderer.drawOutsetPanel(panelX, panelY, panelWidth, panelHeight,
            0.10f, 0.08f, 0.14f, 0.98f);
        renderer.drawBorderedRect(panelX, panelY, panelWidth, panelHeight, 3f,
            new float[]{0f, 0f, 0f, 0f}, new float[]{0.0f, 0.95f, 0.95f, 0.4f});

        float innerX = panelX + 2f;
        float innerY = panelY + 2f;
        float innerW = panelWidth - 4f;
        float innerH = panelHeight - 4f;
        renderer.drawBorderedRect(innerX, innerY, innerW, innerH, 2f,
            new float[]{0f, 0f, 0f, 0f}, new float[]{0f, 0f, 0f, 0.3f});

        super.render(renderer, fontRenderer);
    }
}
