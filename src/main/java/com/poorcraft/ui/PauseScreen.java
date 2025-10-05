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
        
        float centerX = windowWidth / 2.0f;
        float uiScale = settings.graphics.uiScale;

        // Simplified single-column layout
        panelWidth = clamp(windowWidth * 0.45f * uiScale, 450f * uiScale, Math.max(450f * uiScale, windowWidth - 120f));
        panelHeight = clamp(windowHeight * 0.7f * uiScale, 500f * uiScale, Math.max(500f * uiScale, windowHeight - 120f));
        panelX = (windowWidth - panelWidth) / 2.0f;
        panelY = (windowHeight - panelHeight) / 2.0f;
        contentPadding = Math.max(26f * uiScale, panelWidth * 0.05f);

        float titleScale = Math.max(1.7f, panelWidth / 520f) * uiScale;
        float titleBaseline = panelY + contentPadding;
        Label titleLabel = new Label(centerX, titleBaseline, "GAME PAUSED",
            0.02f, 0.96f, 0.96f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        addComponent(titleLabel);

        float topContentY = titleBaseline + Math.max(52f, 34f * titleScale);
        float controlWidth = panelWidth - contentPadding * 2f;
        float leftPanelX = panelX + contentPadding;
        float contentY = topContentY;
        
        // Core action buttons (simplified layout)
        float buttonHeight = Math.max(52f * uiScale, panelHeight * 0.095f);
        float buttonSpacing = Math.max(16f * uiScale, buttonHeight * 0.28f);
        float buttonWidth = controlWidth;
        float buttonX = leftPanelX;
        
        MenuButton resumeButton = new MenuButton(buttonX, contentY, buttonWidth, buttonHeight,
            "RESUME GAME", () -> uiManager.setState(GameState.IN_GAME));
        addComponent(resumeButton);
        contentY += buttonHeight + buttonSpacing;

        MenuButton settingsButton = new MenuButton(buttonX, contentY, buttonWidth, buttonHeight,
            "SETTINGS",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                uiManager.setState(GameState.SETTINGS_MENU);
            });
        addComponent(settingsButton);
        contentY += buttonHeight + buttonSpacing;

        MenuButton modsButton = new MenuButton(buttonX, contentY, buttonWidth, buttonHeight,
            "MODS",
            () -> {
                // TODO: Navigate to dedicated mods screen
                System.out.println("[PauseScreen] Mods screen not yet implemented");
            });
        addComponent(modsButton);
        contentY += buttonHeight + buttonSpacing;

        MenuButton saveButton = new MenuButton(buttonX, contentY, buttonWidth, buttonHeight,
            "SAVE SETTINGS",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                System.out.println("[PauseScreen] Settings saved!");
            });
        addComponent(saveButton);
        contentY += buttonHeight + buttonSpacing;

        MenuButton quitButton = new MenuButton(buttonX, contentY, buttonWidth, buttonHeight,
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
        float footerBaseline = panelY + panelHeight - contentPadding * 0.8f;
        Label hintLabel = new Label(centerX, footerBaseline,
            "Press ESC to resume",
            0.7f, 0.5f, 0.9f, 0.7f);
        hintLabel.setCentered(true);
        hintLabel.setScale(Math.max(0.9f, panelWidth / 600f));
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
        // Adjust overlay alpha based on blur state
        // When blur is active, use lighter overlay; otherwise use darker overlay for readability
        float overlayAlpha = settings.graphics.pauseMenuBlur ? 0.40f : 0.88f;
        renderer.drawRect(0, 0, windowWidth, windowHeight,
            0.03f, 0.01f, 0.08f, overlayAlpha);
        
        // Slightly more opaque panel
        renderer.drawRect(panelX, panelY, panelWidth, panelHeight,
            0.08f, 0.05f, 0.12f, 0.97f);
        float border = Math.max(2f, panelWidth * 0.003f);
        renderer.drawRect(panelX, panelY, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX, panelY + panelHeight - border, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX + panelWidth - border, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.8f);
        // Remove center divider for cleaner look
        // renderer.drawRect(panelX + panelWidth / 2f, panelY + contentPadding * 1.2f,
        //     2f, panelHeight - contentPadding * 2.4f, 0.0f, 0.95f, 0.95f, 0.32f);
        
        super.render(renderer, fontRenderer);
    }
}
