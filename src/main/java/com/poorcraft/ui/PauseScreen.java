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
    
    private static final float PANEL_OPACITY = 0.45f;
    
    private UIManager uiManager;
    private Settings settings;
    private float animationTime = 0.0f;
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float contentPadding;
    private boolean layoutDirty = false;
    private boolean componentsInitialized = false;
    
    private Label titleLabel;
    private Label hintLabel;
    private MenuButton resumeButton;
    private MenuButton settingsButton;
    private MenuButton modsButton;
    private MenuButton saveButton;
    private MenuButton quitButton;
    private ConfirmationDialog quitConfirmDialog;
    
    /**
     * Creates the pause screen with all the bells and whistles.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public PauseScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.settings = uiManager.getSettings();
    }
    @Override
    public void init() {
        componentsInitialized = true;
        clearComponents();

        titleLabel = new Label(0f, 0f, "GAME PAUSED",
            0.02f, 0.96f, 0.96f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setUseTextShadow(true);
        addComponent(titleLabel);

        resumeButton = new MenuButton(0f, 0f, 0f, 0f,
            "RESUME GAME", () -> uiManager.setState(GameState.IN_GAME));
        addComponent(resumeButton);

        settingsButton = new MenuButton(0f, 0f, 0f, 0f,
            "SETTINGS",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                uiManager.setState(GameState.SETTINGS_MENU);
            });
        addComponent(settingsButton);

        modsButton = new MenuButton(0f, 0f, 0f, 0f,
            "MODS",
            () -> {
                System.out.println("[PauseScreen] Mods screen not yet implemented");
            });
        addComponent(modsButton);

        saveButton = new MenuButton(0f, 0f, 0f, 0f,
            "SAVE SETTINGS",
            () -> {
                uiManager.getConfigManager().saveSettings(settings);
                System.out.println("[PauseScreen] Settings saved!");
            });
        addComponent(saveButton);

        quitButton = new MenuButton(0f, 0f, 0f, 0f,
            "QUIT TO MENU",
            () -> {
                if (quitConfirmDialog != null) {
                    quitConfirmDialog.show(windowWidth, windowHeight);
                }
            });
        addComponent(quitButton);

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

        hintLabel = new Label(0f, 0f,
            "Press ESC to resume",
            0.7f, 0.5f, 0.9f, 0.7f);
        hintLabel.setCentered(true);
        hintLabel.setUseTextShadow(true);
        addComponent(hintLabel);

        recalculateLayout();
        layoutDirty = false;
        System.out.println("[PauseScreen] Layout initialized for " + windowWidth + "x" + windowHeight);
    }

    private void recalculateLayout() {
        if (!componentsInitialized) {
            return;
        }

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
        titleLabel.setScale(titleScale);
        titleLabel.setPosition(centerX, titleBaseline);

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

        resumeButton.setBounds(buttonX, buttonStartY, buttonWidth, buttonHeight);
        settingsButton.setBounds(buttonX, buttonStartY + (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        modsButton.setBounds(buttonX, buttonStartY + 2f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        saveButton.setBounds(buttonX, buttonStartY + 3f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        quitButton.setBounds(buttonX, buttonStartY + 4f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);

        float footerBaseline = panelY + panelHeight - 16f;
        hintLabel.setScale(Math.max(1.0f, LayoutUtils.MINECRAFT_LABEL_SCALE * uiScale));
        hintLabel.setPosition(centerX, footerBaseline);
    }

    @Override
    public void onResize(int width, int height) {
        System.out.println("[PauseScreen] Resize detected: " + width + "x" + height);
        
        // Update dimensions
        this.windowWidth = width;
        this.windowHeight = height;
        if (!componentsInitialized) {
            init();
            return;
        }
        layoutDirty = true;
        System.out.println("[PauseScreen] Layout marked dirty");
    }
    
    @Override
    public void update(float deltaTime) {
        if (layoutDirty) {
            recalculateLayout();
            layoutDirty = false;
        }
        animationTime += deltaTime;
        super.update(deltaTime);
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Adjust overlay alpha based on blur state
        // When blur is active, use lighter overlay; otherwise use darker overlay for readability
        float overlayAlpha = settings.graphics.pauseMenuBlur ? 0.40f : 0.48f;
        renderer.drawRect(0, 0, windowWidth, windowHeight,
            0.03f, 0.01f, 0.08f, overlayAlpha);
        
        renderer.drawDropShadow(panelX, panelY, panelWidth, panelHeight, 8f, 0.5f);
        renderer.drawOutsetPanel(panelX, panelY, panelWidth, panelHeight,
            0.10f, 0.08f, 0.14f, PANEL_OPACITY);
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
