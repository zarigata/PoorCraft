package com.poorcraft.ui;

/**
 * Clean main menu inspired by classic Minecraft layouts. Focuses on a centered
 * logo with a vertical stack of primary navigation buttons for clear, readable
 * interaction regardless of resolution.
 */
public class MainMenuScreen extends UIScreen {

    private static final float PANEL_OPACITY = 0.40f;

    private final UIManager uiManager;
    private final MenuBackground background;
    private MenuWorldRenderer worldRenderer;
    private boolean animatedBackgroundEnabledCached = false;

    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private boolean layoutDirty = false;
    private boolean componentsInitialized = false;

    private Label titleLabel;
    private Label subtitleLabel;
    private Label taglineLabel;
    private Label footerLabel;
    private MenuButton singleplayerButton;
    private MenuButton multiplayerButton;
    private MenuButton skinsButton;
    private MenuButton settingsButton;
    private MenuButton quitButton;

    public MainMenuScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.background = new MenuBackground();
        this.worldRenderer = null;
    }

    @Override
    public void init() {
        componentsInitialized = true;
        clearComponents();

        animatedBackgroundEnabledCached = uiManager.getSettings().graphics.animatedMenuBackground;

        // Initialize animated background if enabled
        if (worldRenderer == null && animatedBackgroundEnabledCached) {
            initialiseWorldRenderer();
        }

        float placeholderX = 0f;
        float placeholderY = 0f;
        float placeholderSize = 0f;

        titleLabel = new Label(placeholderX, placeholderY, "POORCRAFT",
            0.96f, 0.98f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setUseTextShadow(true);
        addComponent(titleLabel);

        subtitleLabel = new Label(placeholderX, placeholderY, "Retro Edition",
            0.82f, 0.48f, 0.88f, 0.92f);
        subtitleLabel.setCentered(true);
        subtitleLabel.setUseTextShadow(true);
        addComponent(subtitleLabel);

        taglineLabel = new Label(placeholderX, placeholderY,
            "Choose a mode to begin your adventure",
            0.76f, 0.82f, 0.9f, 0.9f);
        taglineLabel.setCentered(true);
        taglineLabel.setUseTextShadow(true);
        addComponent(taglineLabel);

        singleplayerButton = new MenuButton(placeholderX, placeholderY,
            placeholderSize, placeholderSize, "SINGLEPLAYER",
            () -> uiManager.setState(GameState.WORLD_CREATION));
        addComponent(singleplayerButton);

        multiplayerButton = new MenuButton(placeholderX, placeholderY,
            placeholderSize, placeholderSize, "MULTIPLAYER",
            () -> uiManager.setState(GameState.MULTIPLAYER_MENU));
        addComponent(multiplayerButton);

        skinsButton = new MenuButton(placeholderX, placeholderY,
            placeholderSize, placeholderSize, "SKINS",
            () -> uiManager.setState(GameState.SKIN_MANAGER));
        addComponent(skinsButton);

        settingsButton = new MenuButton(placeholderX, placeholderY,
            placeholderSize, placeholderSize, "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU));
        addComponent(settingsButton);

        quitButton = new MenuButton(placeholderX, placeholderY,
            placeholderSize, placeholderSize, "QUIT",
            () -> uiManager.quit());
        addComponent(quitButton);

        footerLabel = new Label(placeholderX, placeholderY,
            "Press ENTER to confirm | ESC to exit | Customize your player in SKINS",
            0.7f, 0.75f, 0.85f, 0.94f);
        footerLabel.setCentered(true);
        footerLabel.setUseTextShadow(true);
        addComponent(footerLabel);

        recalculateLayout();
        layoutDirty = false;
    }

    private void recalculateLayout() {
        if (!componentsInitialized) {
            return;
        }

        panelWidth = scaleManager != null ?
            LayoutUtils.getScaledPanelWidth(scaleManager) :
            LayoutUtils.getMinecraftPanelWidth(windowWidth, uiManager.getSettings().graphics.uiScale);
        panelHeight = scaleManager != null ?
            LayoutUtils.getScaledPanelHeight(scaleManager) :
            LayoutUtils.getMinecraftPanelHeight(windowHeight, uiManager.getSettings().graphics.uiScale);
        panelX = LayoutUtils.centerHorizontally(windowWidth, panelWidth);
        panelY = LayoutUtils.centerVertically(windowHeight, panelHeight);

        float padding = scaleManager != null ?
            LayoutUtils.getScaledPadding(scaleManager) :
            LayoutUtils.getMinecraftPanelPadding(panelWidth);
        float centerX = panelX + panelWidth / 2f;

        float norm = scaleManager != null
            ? scaleManager.getTextScaleForFontSize(uiManager.getCurrentAtlasSize())
            : 1.0f;
        float titleScale = LayoutUtils.MINECRAFT_TITLE_SCALE * norm;
        float subtitleScale = LayoutUtils.MINECRAFT_SUBTITLE_SCALE * norm;
        float taglineScale = LayoutUtils.MINECRAFT_LABEL_SCALE * norm;

        float titleY = panelY + padding;
        float subtitleY = titleY + Math.max(scaleDimension(60f), titleScale * 48f * 1.5f);
        float taglineY = subtitleY + Math.max(scaleDimension(60f), subtitleScale * 30f * 1.5f);

        if (titleLabel != null) {
            titleLabel.setScale(titleScale);
            titleLabel.setPosition(centerX, titleY);
        }
        if (subtitleLabel != null) {
            subtitleLabel.setScale(subtitleScale);
            subtitleLabel.setPosition(centerX, subtitleY);
        }
        if (taglineLabel != null) {
            taglineLabel.setScale(taglineScale);
            taglineLabel.setPosition(centerX, taglineY);
        }

        float buttonWidth = scaleManager != null ?
            LayoutUtils.getScaledButtonWidth(scaleManager) :
            LayoutUtils.getMinecraftButtonWidth(windowWidth, uiManager.getSettings().graphics.uiScale);
        float buttonHeight = scaleManager != null ?
            LayoutUtils.getScaledButtonHeight(scaleManager) :
            LayoutUtils.getMinecraftButtonHeight(windowHeight, uiManager.getSettings().graphics.uiScale);
        float buttonSpacing = LayoutUtils.getMinecraftButtonSpacing(buttonHeight);
        int buttonCount = 5;
        float buttonStackHeight = LayoutUtils.calculateButtonStackHeight(buttonCount, buttonHeight, buttonSpacing);
        float buttonStartY = panelY + LayoutUtils.centerButtonStack((int) panelHeight, buttonStackHeight);
        float buttonX = panelX + (panelWidth - buttonWidth) / 2f;
        float adjustedButtonStartY = Math.max(buttonStartY, taglineY + buttonSpacing * 0.75f);

        if (singleplayerButton != null) {
            singleplayerButton.setBounds(buttonX, adjustedButtonStartY, buttonWidth, buttonHeight);
        }
        if (multiplayerButton != null) {
            multiplayerButton.setBounds(buttonX, adjustedButtonStartY + (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        }
        if (skinsButton != null) {
            skinsButton.setBounds(buttonX, adjustedButtonStartY + 2f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        }
        if (settingsButton != null) {
            settingsButton.setBounds(buttonX, adjustedButtonStartY + 3f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        }
        if (quitButton != null) {
            quitButton.setBounds(buttonX, adjustedButtonStartY + 4f * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        }

        float footerY = panelY + panelHeight - 20f;
        if (footerLabel != null) {
            footerLabel.setScale(LayoutUtils.MINECRAFT_LABEL_SCALE * norm);
            footerLabel.setPosition(centerX, footerY);
        }
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        if (!componentsInitialized) {
            init();
            return;
        }
        layoutDirty = true;
        System.out.println("[MainMenuScreen] Layout marked dirty, will recalculate on next update");
        if (worldRenderer != null) {
            worldRenderer.onResize(width, height);
        }
    }

    @Override
    public void update(float deltaTime) {
        boolean animatedEnabled = uiManager.getSettings().graphics.animatedMenuBackground;
        if (animatedEnabled != animatedBackgroundEnabledCached) {
            animatedBackgroundEnabledCached = animatedEnabled;
            if (animatedEnabled) {
                if (worldRenderer == null) {
                    initialiseWorldRenderer();
                }
            } else if (worldRenderer != null) {
                worldRenderer.cleanup();
                worldRenderer = null;
            }
        }

        if (layoutDirty) {
            recalculateLayout();
            layoutDirty = false;
        }
        super.update(deltaTime);
        if (worldRenderer != null && uiManager.getSettings().graphics.animatedMenuBackground) {
            worldRenderer.update(deltaTime);
        } else {
            background.update(deltaTime);
        }
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Render animated background or fallback to static background
        if (worldRenderer != null && uiManager.getSettings().graphics.animatedMenuBackground) {
            worldRenderer.render(windowWidth, windowHeight);
        } else {
            background.render(renderer, windowWidth, windowHeight);
        }

        renderer.drawDropShadow(panelX, panelY, panelWidth, panelHeight, 8f, 0.45f);
        renderer.drawOutsetPanel(panelX, panelY, panelWidth, panelHeight,
            0.08f, 0.09f, 0.12f, PANEL_OPACITY);

        super.render(renderer, fontRenderer);
    }

    MenuWorldRenderer getWorldRenderer() {
        return worldRenderer;
    }

    private void initialiseWorldRenderer() {
        try {
            Object[] components = uiManager.getMenuRenderingComponents();
            if (components != null && components.length >= 3
                && components[0] != null && components[1] != null && components[2] != null) {
                worldRenderer = new MenuWorldRenderer(
                    (com.poorcraft.render.ChunkRenderer) components[0],
                    (com.poorcraft.render.SkyRenderer) components[1],
                    (com.poorcraft.render.SunLight) components[2],
                    uiManager.getSettings().graphics.menuBackgroundSpeed
                );
                worldRenderer.init();
                System.out.println("[MainMenuScreen] Animated background initialized");
            }
        } catch (Exception e) {
            System.err.println("[MainMenuScreen] Failed to initialize animated background: " + e.getMessage());
            worldRenderer = null;
        }
    }
}
