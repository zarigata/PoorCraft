package com.poorcraft.ui;

/**
 * Clean main menu inspired by classic Minecraft layouts. Focuses on a centered
 * logo with a vertical stack of primary navigation buttons for clear, readable
 * interaction regardless of resolution.
 */
public class MainMenuScreen extends UIScreen {

    private final UIManager uiManager;
    private final MenuBackground background;
    private MenuWorldRenderer worldRenderer;

    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;

    public MainMenuScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
        this.background = new MenuBackground();
        this.worldRenderer = null;
    }

    @Override
    public void init() {
        clearComponents();
        
        // Initialize animated background if enabled
        if (worldRenderer == null && uiManager.getSettings().graphics.animatedMenuBackground) {
            try {
                Object[] components = uiManager.getMenuRenderingComponents();
                if (components[0] != null && components[1] != null && components[2] != null) {
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

        float uiScale = uiManager.getSettings().graphics.uiScale;
        panelWidth = LayoutUtils.getMinecraftPanelWidth(windowWidth, uiScale);
        panelHeight = LayoutUtils.getMinecraftPanelHeight(windowHeight, uiScale);
        panelX = LayoutUtils.centerHorizontally(windowWidth, panelWidth);
        panelY = LayoutUtils.centerVertically(windowHeight, panelHeight);

        float padding = LayoutUtils.getMinecraftPanelPadding(panelWidth);
        float centerX = panelX + panelWidth / 2f;

        float titleScale = LayoutUtils.MINECRAFT_TITLE_SCALE * uiScale;
        float subtitleScale = LayoutUtils.MINECRAFT_SUBTITLE_SCALE * uiScale;
        float taglineScale = LayoutUtils.MINECRAFT_LABEL_SCALE * uiScale;

        float buttonWidth = LayoutUtils.getMinecraftButtonWidth(windowWidth, uiScale);
        float buttonHeight = LayoutUtils.getMinecraftButtonHeight(windowHeight, uiScale);
        float buttonSpacing = LayoutUtils.getMinecraftButtonSpacing(buttonHeight);
        int buttonCount = 5;
        float buttonStackHeight = LayoutUtils.calculateButtonStackHeight(buttonCount, buttonHeight, buttonSpacing);
        float buttonStartY = panelY + LayoutUtils.centerButtonStack((int) panelHeight, buttonStackHeight);
        float buttonX = panelX + (panelWidth - buttonWidth) / 2f;

        float titleY = panelY + padding;
        Label titleLabel = new Label(centerX, titleY, "POORCRAFT",
            0.96f, 0.98f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        titleLabel.setUseTextShadow(true);
        addComponent(titleLabel);

        float subtitleY = titleY + Math.max(60f * uiScale, titleScale * 48f * 1.5f);
        Label subtitleLabel = new Label(centerX, subtitleY, "Retro Edition",
            0.82f, 0.48f, 0.88f, 0.92f);
        subtitleLabel.setCentered(true);
        subtitleLabel.setScale(subtitleScale);
        subtitleLabel.setUseTextShadow(true);
        addComponent(subtitleLabel);

        float taglineY = subtitleY + Math.max(60f * uiScale, subtitleScale * 30f * 1.5f);
        Label taglineLabel = new Label(centerX, taglineY,
            "Choose a mode to begin your adventure",
            0.76f, 0.82f, 0.9f, 0.9f);
        taglineLabel.setCentered(true);
        taglineLabel.setScale(taglineScale);
        taglineLabel.setUseTextShadow(true);
        addComponent(taglineLabel);

        float adjustedButtonStartY = Math.max(buttonStartY, taglineY + buttonSpacing * 0.75f);

        MenuButton singleplayerButton = new MenuButton(buttonX, adjustedButtonStartY,
            buttonWidth, buttonHeight, "SINGLEPLAYER",
            () -> uiManager.setState(GameState.WORLD_CREATION));
        addComponent(singleplayerButton);

        MenuButton multiplayerButton = new MenuButton(buttonX, adjustedButtonStartY + (buttonHeight + buttonSpacing),
            buttonWidth, buttonHeight, "MULTIPLAYER",
            () -> uiManager.setState(GameState.MULTIPLAYER_MENU));
        addComponent(multiplayerButton);

        MenuButton skinsButton = new MenuButton(buttonX, adjustedButtonStartY + 2f * (buttonHeight + buttonSpacing),
            buttonWidth, buttonHeight, "SKINS",
            () -> uiManager.setState(GameState.SKIN_MANAGER));
        addComponent(skinsButton);

        MenuButton settingsButton = new MenuButton(buttonX, adjustedButtonStartY + 3f * (buttonHeight + buttonSpacing),
            buttonWidth, buttonHeight, "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU));
        addComponent(settingsButton);

        MenuButton quitButton = new MenuButton(buttonX, adjustedButtonStartY + 4f * (buttonHeight + buttonSpacing),
            buttonWidth, buttonHeight, "QUIT",
            () -> uiManager.quit());
        addComponent(quitButton);

        float footerY = panelY + panelHeight - 20f;
        Label footerLabel = new Label(centerX, footerY,
            "Press ENTER to confirm | ESC to exit | Customize your player in SKINS",
            0.7f, 0.75f, 0.85f, 0.94f);
        footerLabel.setCentered(true);
        footerLabel.setScale(LayoutUtils.MINECRAFT_LABEL_SCALE * uiScale);
        footerLabel.setUseTextShadow(true);
        addComponent(footerLabel);
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init();
    }

    @Override
    public void update(float deltaTime) {
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
            0.08f, 0.09f, 0.12f, 0.94f);

        super.render(renderer, fontRenderer);
    }
}
