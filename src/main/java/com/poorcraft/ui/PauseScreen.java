package com.poorcraft.ui;

/**
 * In-game pause menu screen with ROBUST design.
 * 
 * Shown when ESC is pressed during gameplay.
 * Uses the same bulletproof button system as the main menu.
 * 
 * Features:
 * - Semi-transparent overlay that doesn't hide the game world
 * - Clear rectangular buttons that WORK
 * - Responsive layout that handles any window size
 * 
 * This pause menu WILL work. No ifs, ands, or buts about it.
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
        
        // Button sizing - MINECRAFT-STYLE BIG BUTTONS
        float buttonWidth = clamp(windowWidth * 0.35f, 350f, 550f);
        float buttonHeight = 40f;  // Fixed height like Minecraft
        float buttonSpacing = 10f;  // Standard spacing
        
        // Title above buttons
        float titleY = centerY - Math.max(buttonHeight * 2.0f, 100f);
        Label titleLabel = new Label(centerX, titleY, "GAME PAUSED",
            0.0f, 0.95f, 0.95f, 1.0f);  // Cyan
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // Button positions
        float buttonX = centerX - buttonWidth / 2;
        float firstButtonY = centerY - buttonHeight - buttonSpacing / 2;
        
        // Resume button
        MenuButton resumeButton = new MenuButton(
            buttonX, firstButtonY, 
            buttonWidth, buttonHeight,
            "RESUME",
            () -> uiManager.setState(GameState.IN_GAME)
        );
        addComponent(resumeButton);
        
        // Settings button
        MenuButton settingsButton = new MenuButton(
            buttonX, firstButtonY + buttonHeight + buttonSpacing, 
            buttonWidth, buttonHeight,
            "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU)
        );
        addComponent(settingsButton);
        
        // Save & Quit button
        MenuButton quitButton = new MenuButton(
            buttonX, firstButtonY + (buttonHeight + buttonSpacing) * 2, 
            buttonWidth, buttonHeight,
            "SAVE & QUIT",
            () -> {
                // TODO: Save the world here when world saving is implemented
                uiManager.setState(GameState.MAIN_MENU);
            }
        );
        addComponent(quitButton);
        
        // Hint label
        float hintY = centerY + Math.max(buttonHeight * 2.2f, 120f);
        Label hintLabel = new Label(centerX, hintY,
            "Press ESC to resume",
            0.7f, 0.5f, 0.9f, 0.7f);
        hintLabel.setCentered(true);
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
        // Draw semi-transparent dark overlay
        // This dims the game world without hiding it completely
        renderer.drawRect(0, 0, windowWidth, windowHeight,
            0.05f, 0.02f, 0.10f, 0.80f);  // Dark blue-purple, 80% opacity
        
        // Optional: Subtle scanline for retro effect
        float scanlineY = (animationTime * 20) % windowHeight;
        renderer.drawRect(0, scanlineY, windowWidth, 2,
            0.0f, 0.95f, 0.95f, 0.05f);  // Very subtle cyan
        
        // Render all UI components (buttons, labels)
        super.render(renderer, fontRenderer);
    }
}
