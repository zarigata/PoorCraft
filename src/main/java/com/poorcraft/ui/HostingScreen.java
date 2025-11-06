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
    private Label titleLabel;
    private Label statusLabel;
    private Label loadingLabel;
    private Button cancelButton;
    private String statusMessage;
    private float animationTimer;
    private boolean layoutDirty;
    private boolean componentsInitialized;
    
    /**
     * Creates the hosting screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public HostingScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.statusMessage = "Initializing server...";
        this.animationTimer = 0;
    }
    
    @Override
    public void init() {
        if (!componentsInitialized) {
            clearComponents();
            createComponents();
            componentsInitialized = true;
        }

        layoutDirty = true;
        recalculateLayout();
    }
    
    @Override
    public void update(float deltaTime) {
        if (layoutDirty) {
            recalculateLayout();
        }

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
        if (!componentsInitialized) {
            init();
            return;
        }
        layoutDirty = true;
    }

    private void createComponents() {
        titleLabel = new Label(0f, 0f, "Starting Server",
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);

        statusLabel = new Label(0f, 0f, statusMessage,
            0.8f, 0.8f, 0.8f, 1.0f);
        statusLabel.setCentered(true);
        addComponent(statusLabel);

        loadingLabel = new Label(0f, 0f, "...",
            0.6f, 0.6f, 0.6f, 1.0f);
        loadingLabel.setCentered(true);
        addComponent(loadingLabel);

        cancelButton = new Button(0f, 0f, 0f, 0f,
            "Cancel", this::onCancel);
        addComponent(cancelButton);
    }

    private void recalculateLayout() {
        if (!componentsInitialized) {
            layoutDirty = false;
            return;
        }

        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        float norm = scaleManager.getTextScaleForFontSize(uiManager.getCurrentAtlasSize());

        titleLabel.setScale(1.8f * norm);
        titleLabel.setPosition(centerX, centerY - scaleDimension(60f));

        statusLabel.setScale(norm);
        statusLabel.setPosition(centerX, centerY);

        loadingLabel.setScale(norm);
        loadingLabel.setPosition(centerX, centerY + scaleDimension(40f));

        float buttonWidth = scaleDimension(140f);
        float buttonHeight = scaleDimension(40f);
        float buttonX = centerX - buttonWidth / 2f;
        float buttonY = centerY + scaleDimension(100f);
        cancelButton.setBounds(buttonX, buttonY, buttonWidth, buttonHeight);

        layoutDirty = false;
    }
}
