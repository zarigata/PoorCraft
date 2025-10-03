package com.poorcraft.ui;

/**
 * In-game pause menu screen.
 * 
 * Shown when ESC is pressed during gameplay.
 * The game world is still visible but gameplay is paused.
 * 
 * This is where you go when you need a break or want to rage quit.
 * We've all been there.
 */
public class PauseScreen extends UIScreen {
    
    private UIManager uiManager;
    
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
        
        // Title
        Label titleLabel = new Label(centerX, centerY - 100, "Game Paused", 
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // Button dimensions
        float buttonWidth = 200;
        float buttonHeight = 40;
        float buttonSpacing = 10;
        float buttonX = centerX - buttonWidth / 2;
        float startY = centerY - 40;
        
        // Resume button
        Button resumeButton = new Button(
            buttonX, startY, buttonWidth, buttonHeight,
            "Resume",
            () -> uiManager.setState(GameState.IN_GAME)
        );
        addComponent(resumeButton);
        
        // Settings button
        Button settingsButton = new Button(
            buttonX, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight,
            "Settings",
            () -> uiManager.setState(GameState.SETTINGS_MENU)
        );
        addComponent(settingsButton);
        
        // Save and Quit button
        Button quitButton = new Button(
            buttonX, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight,
            "Save and Quit",
            () -> {
                // TODO: Save world
                uiManager.setState(GameState.MAIN_MENU);
            }
        );
        addComponent(quitButton);
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init();
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw semi-transparent overlay
        renderer.drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.5f);
        
        // Draw all components
        super.render(renderer, fontRenderer);
    }
}
