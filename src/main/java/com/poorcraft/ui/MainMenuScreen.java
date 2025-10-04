package com.poorcraft.ui;

/**
 * Main menu screen with ROBUST, RESIZE-PROOF design.
 * 
 * This menu WILL work at any window size. Period.
 * Features:
 * - Textured background with block textures (20% opacity)
 * - Clear rectangular buttons that ACTUALLY SHOW UP
 * - Bulletproof resize handling that WON'T break input
 * - Vaporwave color scheme (but simpler and more reliable)
 * 
 * I rewrote this three times to get it right. This version is THE ONE.
 * No more fancy gradients that don't render. Just solid, working UI.
 */
public class MainMenuScreen extends UIScreen {
    
    private UIManager uiManager;
    private MenuBackground background;
    private float animationTime = 0.0f;
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    
    /**
     * Creates the main menu screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager for state transitions
     */
    public MainMenuScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
        this.background = new MenuBackground();
    }
    
    @Override
    public void init() {
        clearComponents();
        
        panelWidth = clamp(windowWidth * 0.72f, 560f, Math.max(560f, windowWidth - 80f));
        panelHeight = clamp(windowHeight * 0.72f, 460f, Math.max(460f, windowHeight - 80f));
        panelX = (windowWidth - panelWidth) / 2.0f;
        panelY = (windowHeight - panelHeight) / 2.0f;
        
        float centerX = panelX + panelWidth / 2.0f;
        
        float titleScale = Math.max(1.8f, panelWidth / 520f);
        float subtitleScale = Math.max(1.05f, titleScale * 0.55f);
        
        float titleBaseline = panelY + panelHeight * 0.2f;
        Label titleLabel = new Label(centerX, titleBaseline, "PoorCraft",
            0.0f, 0.95f, 0.95f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        addComponent(titleLabel);
        
        float subtitleBaseline = titleBaseline + 48f * (titleScale * 0.55f);
        Label subtitleLabel = new Label(centerX, subtitleBaseline, "Retro Edition",
            0.98f, 0.26f, 0.63f, 0.92f);
        subtitleLabel.setCentered(true);
        subtitleLabel.setScale(subtitleScale);
        addComponent(subtitleLabel);
        
        float buttonWidth = panelWidth - 96f;
        float buttonHeight = clamp(panelHeight * 0.16f, 82f, 158f);
        float buttonSpacing = Math.max(32f, buttonHeight * 0.38f);
        float buttonX = panelX + (panelWidth - buttonWidth) / 2.0f;
        float firstButtonY = subtitleBaseline + buttonSpacing;
        
        MenuButton singleplayerButton = new MenuButton(buttonX, firstButtonY,
            buttonWidth, buttonHeight, "SINGLEPLAYER",
            () -> uiManager.setState(GameState.WORLD_CREATION));
        addComponent(singleplayerButton);
        
        MenuButton multiplayerButton = new MenuButton(buttonX, firstButtonY + buttonHeight + buttonSpacing,
            buttonWidth, buttonHeight, "MULTIPLAYER",
            () -> uiManager.setState(GameState.MULTIPLAYER_MENU));
        addComponent(multiplayerButton);
        
        MenuButton settingsButton = new MenuButton(buttonX, firstButtonY + (buttonHeight + buttonSpacing) * 2,
            buttonWidth, buttonHeight, "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU));
        addComponent(settingsButton);
        
        MenuButton quitButton = new MenuButton(buttonX, firstButtonY + (buttonHeight + buttonSpacing) * 3,
            buttonWidth, buttonHeight, "QUIT",
            () -> uiManager.quit());
        addComponent(quitButton);
        
        float footerBaseline = panelY + panelHeight - 30f;
        Label footerLabel = new Label(centerX, footerBaseline,
            "Press a button to explore PoorCraft",
            0.7f, 0.5f, 0.9f, 0.85f);
        footerLabel.setCentered(true);
        footerLabel.setScale(Math.max(0.95f, panelWidth / 760f));
        addComponent(footerLabel);
        
        System.out.println("[MainMenuScreen] Layout initialized for " + windowWidth + "x" + windowHeight);
    }

    /**
     * Clamps a value between min and max.
     * This is used everywhere for responsive sizing. Math is cool!
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    @Override
    public void onResize(int width, int height) {
        System.out.println("[MainMenuScreen] Resize detected: " + width + "x" + height);
        
        // Update dimensions
        this.windowWidth = width;
        this.windowHeight = height;
        
        // Completely rebuild the layout
        // This ensures all component positions and sizes are recalculated
        init();
        
        System.out.println("[MainMenuScreen] Resize complete, components rebuilt");
    }
    
    @Override
    public void update(float deltaTime) {
        animationTime += deltaTime;
        super.update(deltaTime);
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        background.render(renderer, windowWidth, windowHeight);
        
        renderer.drawRect(panelX, panelY, panelWidth, panelHeight,
            0.07f, 0.09f, 0.12f, 0.92f);
        
        float border = Math.max(2f, panelWidth * 0.003f);
        renderer.drawRect(panelX, panelY, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX, panelY + panelHeight - border, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.8f);
        renderer.drawRect(panelX + panelWidth - border, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.8f);
        
        super.render(renderer, fontRenderer);
    }
    
    /**
     * Cleanup resources when screen is destroyed.
     */
    public void cleanup() {
        if (background != null) {
            background.cleanup();
        }
    }
}
