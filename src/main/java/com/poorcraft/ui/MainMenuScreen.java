package com.poorcraft.ui;

/**
 * Main menu screen with VAPORWAVE aesthetics.
 * 
 * The first screen shown when the game starts.
 * Now featuring sick 80s vibes, gradient backgrounds, and responsive design.
 * 
 * This menu will resize beautifully no matter what size window you throw at it.
 * Try it! Resize the window and watch the magic happen. It's like the menu knows
 * what you want before you even know it yourself. Spooky.
 * 
 * Also, I might have spent too much time on the background gradient.
 * No regrets though, it looks SICK.
 */
public class MainMenuScreen extends UIScreen {
    
    private UIManager uiManager;
    private float animationTime = 0.0f;  // For animated background effects
    
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
    }
    
    @Override
    public void init() {
        clearComponents();
        
        // Calculate centered layout with responsive sizing
        // This math ensures the menu looks good on any screen size
        float centerX = windowWidth / 2.0f;
        float usableHeight = windowHeight * 0.70f;
        float topOffset = windowHeight * 0.15f;

        // Button sizing scales with window size
        // Min: 280px, Max: 45% of screen width (capped at 500px for ultra-wide displays)
        float buttonWidth = clamp(windowWidth * 0.30f, 280f, Math.min(500f, windowWidth * 0.45f));
        float buttonHeight = clamp(windowHeight * 0.075f, 52f, 80f);
        float buttonSpacing = Math.max(buttonHeight * 0.30f, 16f);
        
        // Calculate total menu height and center it vertically
        float menuHeight = buttonHeight * 4 + buttonSpacing * 3;
        float centerY = topOffset + (usableHeight - menuHeight) / 2.0f;

        // Title positioning - scales with window
        float titleY = Math.max(topOffset * 0.4f, 40f);
        float titleSize = clamp(windowHeight * 0.08f, 40f, 80f);
        
        // Title label with vaporwave colors (cyan)
        Label titleLabel = new Label(centerX, titleY, "PoorCraft",
            0.0f, 0.95f, 0.95f, 1.0f);  // Electric cyan
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // Subtitle with pink accent
        float subtitleY = titleY + Math.max(titleSize * 0.6f, 30f);
        Label versionLabel = new Label(centerX, subtitleY, "RETRO EDITION",
            0.98f, 0.26f, 0.63f, 0.9f);  // Hot pink
        versionLabel.setCentered(true);
        addComponent(versionLabel);
        
        float buttonX = centerX - buttonWidth / 2;
        
        // Singleplayer button - now with STYLE
        VaporwaveButton singleplayerButton = new VaporwaveButton(
            buttonX, centerY, buttonWidth, buttonHeight,
            "SINGLEPLAYER",
            () -> uiManager.setState(GameState.WORLD_CREATION)
        );
        addComponent(singleplayerButton);
        
        // Multiplayer button
        VaporwaveButton multiplayerButton = new VaporwaveButton(
            buttonX, centerY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight,
            "MULTIPLAYER",
            () -> uiManager.setState(GameState.MULTIPLAYER_MENU)
        );
        addComponent(multiplayerButton);
        
        // Settings button
        VaporwaveButton settingsButton = new VaporwaveButton(
            buttonX, centerY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight,
            "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU)
        );
        addComponent(settingsButton);
        
        // Quit button
        VaporwaveButton quitButton = new VaporwaveButton(
            buttonX, centerY + (buttonHeight + buttonSpacing) * 3, buttonWidth, buttonHeight,
            "QUIT",
            () -> uiManager.quit()
        );
        addComponent(quitButton);
        
        // Footer label with subtle purple tint
        float footerY = Math.min(windowHeight - 35, windowHeight * 0.96f);
        Label footerLabel = new Label(centerX, footerY, 
            "~ A E S T H E T I C  M I N E C R A F T ~", 
            0.7f, 0.5f, 0.9f, 0.7f);
        footerLabel.setCentered(true);
        addComponent(footerLabel);
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
        this.windowWidth = width;
        this.windowHeight = height;
        init(); // Rebuild layout with new dimensions
    }
    
    @Override
    public void update(float deltaTime) {
        animationTime += deltaTime;
        super.update(deltaTime);
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw vaporwave gradient background
        // Dark purple at top, fading to dark blue at bottom
        // This creates that classic vaporwave sunset vibe
        int gradientSteps = 40;
        float stepHeight = windowHeight / (float) gradientSteps;
        
        // Top color: Deep purple
        float[] topColor = {0.15f, 0.05f, 0.25f};
        // Bottom color: Dark blue with a hint of cyan
        float[] bottomColor = {0.05f, 0.1f, 0.2f};
        
        for (int i = 0; i < gradientSteps; i++) {
            float t = i / (float) gradientSteps;
            // Smooth gradient interpolation
            float r = topColor[0] + (bottomColor[0] - topColor[0]) * t;
            float g = topColor[1] + (bottomColor[1] - topColor[1]) * t;
            float b = topColor[2] + (bottomColor[2] - topColor[2]) * t;
            
            renderer.drawRect(0, i * stepHeight, windowWidth, stepHeight + 1,
                r, g, b, 1.0f);
        }
        
        // Add subtle animated scan lines for that retro CRT effect
        // I don't know what I'm doing here but it looks cool so I'm keeping it
        float scanlineY = (animationTime * 50) % windowHeight;
        renderer.drawRect(0, scanlineY, windowWidth, 2,
            0.98f, 0.26f, 0.63f, 0.15f);  // Pink scanline
        
        // Draw all UI components
        super.render(renderer, fontRenderer);
    }
}
