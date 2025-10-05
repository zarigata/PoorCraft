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
        panelWidth = clamp(windowWidth * 0.58f * uiScale, 520f * uiScale, Math.max(520f * uiScale, windowWidth - 180f));
        panelHeight = clamp(windowHeight * 0.7f * uiScale, 520f * uiScale, Math.max(520f * uiScale, windowHeight - 180f));
        panelX = (windowWidth - panelWidth) / 2.0f;
        panelY = (windowHeight - panelHeight) / 2.0f;

        float padding = Math.max(32f * uiScale, panelWidth * 0.06f);
        float centerX = panelX + panelWidth / 2f;
        float currentY = panelY + padding;

        float titleScale = clamp(panelWidth / 420f, 1.8f, 2.6f) * uiScale;
        float subtitleScale = clamp(titleScale * 0.55f, 0.9f, 1.4f);

        Label titleLabel = new Label(centerX, currentY, "POORCRAFT",
            0.96f, 0.98f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        addComponent(titleLabel);
        currentY += titleScale * 48f;

        Label subtitleLabel = new Label(centerX, currentY, "Retro Edition",
            0.82f, 0.48f, 0.88f, 0.92f);
        subtitleLabel.setCentered(true);
        subtitleLabel.setScale(subtitleScale);
        addComponent(subtitleLabel);
        currentY += Math.max(40f, subtitleScale * 30f);

        Label taglineLabel = new Label(centerX, currentY,
            "Choose a mode to begin your adventure",
            0.76f, 0.82f, 0.9f, 0.9f);
        taglineLabel.setCentered(true);
        taglineLabel.setScale(Math.max(0.95f, panelWidth / 760f));
        addComponent(taglineLabel);

        currentY += Math.max(56f * uiScale, panelHeight * 0.1f);

        float buttonWidth = panelWidth - padding * 2f;
        float buttonHeight = clamp(panelHeight * 0.17f, 88f * uiScale, 150f * uiScale);
        float buttonSpacing = Math.max(28f * uiScale, buttonHeight * 0.4f);
        float buttonX = panelX + (panelWidth - buttonWidth) / 2f;

        MenuButton singleplayerButton = new MenuButton(buttonX, currentY,
            buttonWidth, buttonHeight, "SINGLEPLAYER",
            () -> uiManager.setState(GameState.WORLD_CREATION));
        addComponent(singleplayerButton);
        currentY += buttonHeight + buttonSpacing;

        MenuButton multiplayerButton = new MenuButton(buttonX, currentY,
            buttonWidth, buttonHeight, "MULTIPLAYER",
            () -> uiManager.setState(GameState.MULTIPLAYER_MENU));
        addComponent(multiplayerButton);
        currentY += buttonHeight + buttonSpacing;

        MenuButton skinsButton = new MenuButton(buttonX, currentY,
            buttonWidth, buttonHeight, "SKINS",
            () -> uiManager.setState(GameState.SKIN_MANAGER));
        addComponent(skinsButton);
        currentY += buttonHeight + buttonSpacing;

        MenuButton settingsButton = new MenuButton(buttonX, currentY,
            buttonWidth, buttonHeight, "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU));
        addComponent(settingsButton);
        currentY += buttonHeight + buttonSpacing;

        MenuButton quitButton = new MenuButton(buttonX, currentY,
            buttonWidth, buttonHeight, "QUIT",
            () -> uiManager.quit());
        addComponent(quitButton);

        Label footerLabel = new Label(centerX, panelY + panelHeight - padding * 0.35f,
            "Press ENTER to confirm | ESC to exit | Customize your player in SKINS",
            0.7f, 0.75f, 0.85f, 0.85f);
        footerLabel.setCentered(true);
        footerLabel.setScale(Math.max(0.92f, panelWidth / 780f));
        addComponent(footerLabel);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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

        // Reduce opacity to show animated background better
        float panelOpacity = (worldRenderer != null && uiManager.getSettings().graphics.animatedMenuBackground) ? 0.85f : 0.92f;
        renderer.drawRect(panelX, panelY, panelWidth, panelHeight,
            0.08f, 0.09f, 0.12f, panelOpacity);

        float border = Math.max(3f, panelWidth * 0.004f);
        renderer.drawRect(panelX, panelY, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.82f);
        renderer.drawRect(panelX, panelY + panelHeight - border, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.82f);
        renderer.drawRect(panelX, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.82f);
        renderer.drawRect(panelX + panelWidth - border, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.82f);

        super.render(renderer, fontRenderer);
    }
}
