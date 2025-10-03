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
        // Clear old components to start fresh
        clearComponents();
        
        // Layout calculations - adaptive but with sensible constraints
        float centerX = windowWidth / 2.0f;
        
        // Button dimensions that scale nicely
        float buttonWidth = clamp(windowWidth * 0.28f, 250f, 450f);
        float buttonHeight = clamp(windowHeight * 0.07f, 50f, 75f);
        float buttonSpacing = clamp(buttonHeight * 0.25f, 12f, 20f);
        
        // Calculate vertical centering
        float totalMenuHeight = (buttonHeight * 4) + (buttonSpacing * 3);
        float topMargin = (windowHeight - totalMenuHeight) / 2.0f - 60f;  // -60 for title space
        
        // Title
        float titleY = clamp(topMargin * 0.6f, 30f, 100f);
        Label titleLabel = new Label(centerX, titleY, "PoorCraft",
            0.0f, 0.95f, 0.95f, 1.0f);  // Cyan
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // Subtitle
        float subtitleY = titleY + 28f;
        Label subtitleLabel = new Label(centerX, subtitleY, "RETRO EDITION",
            0.98f, 0.26f, 0.63f, 0.9f);  // Pink
        subtitleLabel.setCentered(true);
        addComponent(subtitleLabel);
        
        // Button starting position
        float buttonX = centerX - buttonWidth / 2;
        float firstButtonY = topMargin + 80f;
        
        // Create buttons with MenuButton (not VaporwaveButton)
        MenuButton singleplayerButton = new MenuButton(
            buttonX, firstButtonY, 
            buttonWidth, buttonHeight,
            "SINGLEPLAYER",
            () -> uiManager.setState(GameState.WORLD_CREATION)
        );
        addComponent(singleplayerButton);
        
        MenuButton multiplayerButton = new MenuButton(
            buttonX, firstButtonY + (buttonHeight + buttonSpacing), 
            buttonWidth, buttonHeight,
            "MULTIPLAYER",
            () -> uiManager.setState(GameState.MULTIPLAYER_MENU)
        );
        addComponent(multiplayerButton);
        
        MenuButton settingsButton = new MenuButton(
            buttonX, firstButtonY + (buttonHeight + buttonSpacing) * 2, 
            buttonWidth, buttonHeight,
            "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU)
        );
        addComponent(settingsButton);
        
        MenuButton quitButton = new MenuButton(
            buttonX, firstButtonY + (buttonHeight + buttonSpacing) * 3, 
            buttonWidth, buttonHeight,
            "QUIT",
            () -> uiManager.quit()
        );
        addComponent(quitButton);
        
        // Footer
        float footerY = windowHeight - 25f;
        Label footerLabel = new Label(centerX, footerY,
            "~ Press buttons to navigate - they're the rectangles! ~",
            0.7f, 0.5f, 0.9f, 0.7f);
        footerLabel.setCentered(true);
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
        // Render textured background (block textures at 20% opacity)
        background.render(renderer, windowWidth, windowHeight);
        
        // Optional: Add subtle scanline effect for retro CRT feel
        float scanlineY = (animationTime * 30) % windowHeight;
        renderer.drawRect(0, scanlineY, windowWidth, 2,
            0.0f, 0.95f, 0.95f, 0.08f);  // Subtle cyan scanline
        
        // Render all UI components (buttons, labels, etc.)
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
