package com.poorcraft.ui;

/**
 * Main menu screen.
 * 
 * The first screen shown when the game starts.
 * Provides access to singleplayer, multiplayer (disabled), settings, and quit.
 * 
 * This is the face of the game. First impressions matter!
 * Keep it simple, clean, and functional.
 */
public class MainMenuScreen extends UIScreen {
    
    private UIManager uiManager;
    
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
        
        // Calculate centered layout
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight * 0.4f;
        
        // Title label
        Label titleLabel = new Label(centerX, windowHeight * 0.15f, "PoorCraft", 
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // Version label
        Label versionLabel = new Label(centerX, windowHeight * 0.15f + 30, "v0.1.0-SNAPSHOT", 
            0.7f, 0.7f, 0.7f, 1.0f);
        versionLabel.setCentered(true);
        addComponent(versionLabel);
        
        // Button dimensions
        float buttonWidth = 200;
        float buttonHeight = 40;
        float buttonSpacing = 10;
        float buttonX = centerX - buttonWidth / 2;
        
        // Singleplayer button
        Button singleplayerButton = new Button(
            buttonX, centerY, buttonWidth, buttonHeight,
            "Singleplayer",
            () -> uiManager.setState(GameState.WORLD_CREATION)
        );
        addComponent(singleplayerButton);
        
        // Multiplayer button - now enabled with networking support
        Button multiplayerButton = new Button(
            buttonX, centerY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight,
            "Multiplayer",
            () -> uiManager.setState(GameState.MULTIPLAYER_MENU)
        );
        addComponent(multiplayerButton);
        
        // Settings button
        Button settingsButton = new Button(
            buttonX, centerY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight,
            "Settings",
            () -> uiManager.setState(GameState.SETTINGS_MENU)
        );
        addComponent(settingsButton);
        
        // Quit button
        Button quitButton = new Button(
            buttonX, centerY + (buttonHeight + buttonSpacing) * 3, buttonWidth, buttonHeight,
            "Quit",
            () -> uiManager.quit()
        );
        addComponent(quitButton);
        
        // Footer label
        Label footerLabel = new Label(centerX, windowHeight - 30, 
            "A Minecraft clone made in one week", 
            0.5f, 0.5f, 0.5f, 0.8f);
        footerLabel.setCentered(true);
        addComponent(footerLabel);
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init(); // Rebuild layout
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw background
        renderer.drawRect(0, 0, windowWidth, windowHeight, 0.1f, 0.1f, 0.15f, 1.0f);
        
        // Draw all components
        super.render(renderer, fontRenderer);
    }
}
