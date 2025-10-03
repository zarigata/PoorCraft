package com.poorcraft.ui;

/**
 * Loading screen shown while starting an integrated server.
 * 
 * Displays server startup status and animated loading indicator.
 * Shows status updates as server initializes:
 * - "Initializing server..."
 * - "Generating world..."
 * - "Starting network listener..."
 * - "Connecting to local server..."
 * 
 * Similar to ConnectingScreen but for server startup.
 * The server starts in the background while this screen is shown.
 * Once ready, client connects to localhost and transitions to IN_GAME.
 * 
 * It's like watching a pot boil. Except the pot is a Minecraft server.
 * And you're also the water. This metaphor got weird.
 */
public class HostingScreen extends UIScreen {
    
    private UIManager uiManager;
    private Label statusLabel;
    private String statusMessage;
    private float animationTimer;
    
    /**
     * Creates the hosting screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public HostingScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
        this.statusMessage = "Initializing server...";
        this.animationTimer = 0;
    }
    
    @Override
    public void init() {
        clearComponents();
        
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        
        // Title
        Label titleLabel = new Label(centerX, centerY - 60, "Starting Server", 
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // Status label
        statusLabel = new Label(centerX, centerY, statusMessage, 
            0.8f, 0.8f, 0.8f, 1.0f);
        statusLabel.setCentered(true);
        addComponent(statusLabel);
        
        // Loading animation label (dots)
        Label loadingLabel = new Label(centerX, centerY + 40, "...", 
            0.6f, 0.6f, 0.6f, 1.0f);
        loadingLabel.setCentered(true);
        addComponent(loadingLabel);
        
        // Cancel button
        float buttonWidth = 140;
        float buttonHeight = 40;
        Button cancelButton = new Button(centerX - buttonWidth / 2, centerY + 100, 
            buttonWidth, buttonHeight, "Cancel", this::onCancel);
        addComponent(cancelButton);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // Update animation timer for loading dots
        animationTimer += deltaTime;
        
        // Update status label with current message
        if (statusLabel != null) {
            // Add animated dots
            int dotCount = ((int)(animationTimer * 2) % 4);
            String dots = ".".repeat(dotCount);
            statusLabel.setText(statusMessage + dots);
        }
    }
    
    /**
     * Sets the server startup status message.
     * 
     * @param message Status message
     */
    public void setStatus(String message) {
        this.statusMessage = message;
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    /**
     * Handles cancel button click.
     */
    private void onCancel() {
        // Stop server and return to multiplayer menu
        uiManager.disconnectFromServer();
        uiManager.setState(GameState.MULTIPLAYER_MENU);
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init();
    }
}
