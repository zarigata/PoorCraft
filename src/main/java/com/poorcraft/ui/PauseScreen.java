package com.poorcraft.ui;

/**
 * In-game pause menu screen with VAPORWAVE vibes.
 * 
 * Shown when ESC is pressed during gameplay.
 * The game world is still visible but dimmed with a sick overlay.
 * 
 * This is where you go when you need a break, want to rage quit,
 * or just wanna admire the aesthetic pause menu. No judgment here.
 * 
 * Fun fact: I added a pulsing glow effect to the title. You're welcome.
 */
public class PauseScreen extends UIScreen {
    
    private UIManager uiManager;
    private float animationTime = 0.0f;
    
    /**
     * Creates the pause screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public PauseScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
    }
    
    @Override
    public void init() {
        clearComponents();
        
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        
        // Responsive button sizing (similar to main menu)
        float buttonWidth = clamp(windowWidth * 0.25f, 240f, 400f);
        float buttonHeight = clamp(windowHeight * 0.07f, 50f, 70f);
        float buttonSpacing = Math.max(buttonHeight * 0.28f, 14f);
        
        // Title positioning
        float titleY = centerY - Math.max(buttonHeight * 2.2f, 120f);
        
        // Title with cyan glow
        Label titleLabel = new Label(centerX, titleY, "GAME PAUSED", 
            0.0f, 0.95f, 0.95f, 1.0f);  // Electric cyan
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        float buttonX = centerX - buttonWidth / 2;
        float startY = centerY - buttonHeight - buttonSpacing / 2;
        
        // Resume button
        VaporwaveButton resumeButton = new VaporwaveButton(
            buttonX, startY, buttonWidth, buttonHeight,
            "RESUME",
            () -> uiManager.setState(GameState.IN_GAME)
        );
        addComponent(resumeButton);
        
        // Settings button
        VaporwaveButton settingsButton = new VaporwaveButton(
            buttonX, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight,
            "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU)
        );
        addComponent(settingsButton);
        
        // Save and Quit button
        VaporwaveButton quitButton = new VaporwaveButton(
            buttonX, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight,
            "SAVE & QUIT",
            () -> {
                // TODO: Actually save the world instead of just pretending
                uiManager.setState(GameState.MAIN_MENU);
            }
        );
        addComponent(quitButton);
        
        // Add a helpful hint at the bottom
        float hintY = centerY + Math.max(buttonHeight * 2.5f, 140f);
        Label hintLabel = new Label(centerX, hintY,
            "Press ESC to resume",
            0.7f, 0.5f, 0.9f, 0.6f);  // Subtle purple
        hintLabel.setCentered(true);
        addComponent(hintLabel);
    }
    
    /**
     * Clamps a value between min and max.
     */
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
        animationTime += deltaTime;
        super.update(deltaTime);
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw vaporwave-themed semi-transparent overlay
        // Gradient from purple to dark blue
        int gradientSteps = 30;
        float stepHeight = windowHeight / (float) gradientSteps;
        
        for (int i = 0; i < gradientSteps; i++) {
            float t = i / (float) gradientSteps;
            // Purple to blue gradient with transparency
            float r = 0.15f + (0.05f - 0.15f) * t;
            float g = 0.05f + (0.1f - 0.05f) * t;
            float b = 0.25f + (0.3f - 0.25f) * t;
            
            renderer.drawRect(0, i * stepHeight, windowWidth, stepHeight + 1,
                r, g, b, 0.75f);  // Semi-transparent
        }
        
        // Add subtle pulsing scanline effect
        float pulse = (float) Math.sin(animationTime * 2.0) * 0.5f + 0.5f;
        float scanlineY = (animationTime * 30) % windowHeight;
        renderer.drawRect(0, scanlineY, windowWidth, 3,
            0.0f, 0.95f, 0.95f, 0.1f * pulse);  // Cyan scanline
        
        // Draw all UI components
        super.render(renderer, fontRenderer);
    }
}
