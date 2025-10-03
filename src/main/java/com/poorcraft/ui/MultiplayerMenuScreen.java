package com.poorcraft.ui;

/**
 * Multiplayer menu screen with server list, direct connect, and host options.
 * 
 * This is the main multiplayer hub. From here you can:
 * - Browse server list (coming soon, disabled for v1)
 * - Direct connect to a server by IP:port
 * - Host your own integrated server
 * 
 * It's like the multiplayer menu in Minecraft but simpler. Way simpler.
 * No fancy server pinging or MOTD display. Just the basics!
 */
public class MultiplayerMenuScreen extends UIScreen {
    
    private UIManager uiManager;
    private TextField directConnectField;
    private TextField hostPortField;
    private TextField worldSeedField;
    private Checkbox generateStructuresCheckbox;
    private boolean showingDirectConnect;
    private boolean showingHostGame;
    
    /**
     * Creates the multiplayer menu screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public MultiplayerMenuScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
        this.showingDirectConnect = false;
        this.showingHostGame = false;
    }
    
    @Override
    public void init() {
        clearComponents();
        showingDirectConnect = false;
        showingHostGame = false;
        showMainMenu();
    }
    
    /**
     * Shows the main multiplayer menu.
     */
    private void showMainMenu() {
        clearComponents();
        
        float centerX = windowWidth / 2.0f;
        float startY = windowHeight * 0.25f;
        float buttonWidth = 200;
        float buttonHeight = 40;
        float spacing = 50;
        
        // Title
        Label titleLabel = new Label(centerX, startY, "Multiplayer", 
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        float currentY = startY + 80;
        
        // Server List button (disabled for v1)
        Button serverListButton = new Button(centerX - buttonWidth / 2, currentY, 
            buttonWidth, buttonHeight, "Server List (Coming Soon)", () -> {});
        serverListButton.setEnabled(false);
        addComponent(serverListButton);
        
        currentY += spacing;
        
        // Direct Connect button
        Button directConnectButton = new Button(centerX - buttonWidth / 2, currentY, 
            buttonWidth, buttonHeight, "Direct Connect", this::showDirectConnectDialog);
        addComponent(directConnectButton);
        
        currentY += spacing;
        
        // Host Game button
        Button hostGameButton = new Button(centerX - buttonWidth / 2, currentY, 
            buttonWidth, buttonHeight, "Host Game", this::showHostGameDialog);
        addComponent(hostGameButton);
        
        currentY += spacing + 20;
        
        // Back button
        Button backButton = new Button(centerX - buttonWidth / 2, currentY, 
            buttonWidth, buttonHeight, "Back", () -> uiManager.setState(GameState.MAIN_MENU));
        addComponent(backButton);
    }
    
    /**
     * Shows the direct connect dialog.
     */
    private void showDirectConnectDialog() {
        clearComponents();
        showingDirectConnect = true;
        
        float centerX = windowWidth / 2.0f;
        float startY = windowHeight * 0.3f;
        float fieldWidth = 300;
        float fieldHeight = 35;
        float buttonWidth = 140;
        float buttonHeight = 40;
        
        // Title
        Label titleLabel = new Label(centerX, startY, "Direct Connect", 
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // Server address label
        float currentY = startY + 60;
        Label addressLabel = new Label(centerX - fieldWidth / 2, currentY, 
            "Server Address (IP:Port):");
        addComponent(addressLabel);
        
        // Server address field
        directConnectField = new TextField(centerX - fieldWidth / 2, currentY + 25, 
            fieldWidth, fieldHeight, "localhost:25565");
        directConnectField.setText("localhost:25565");
        addComponent(directConnectField);
        
        // Buttons
        currentY += 100;
        float buttonSpacing = 20;
        
        Button connectButton = new Button(
            centerX - buttonWidth - buttonSpacing / 2, currentY, 
            buttonWidth, buttonHeight, "Connect", this::onDirectConnect);
        addComponent(connectButton);
        
        Button cancelButton = new Button(
            centerX + buttonSpacing / 2, currentY, 
            buttonWidth, buttonHeight, "Cancel", this::init);
        addComponent(cancelButton);
    }
    
    /**
     * Handles direct connect button click.
     */
    private void onDirectConnect() {
        String address = directConnectField.getText().trim();
        
        if (address.isEmpty()) {
            System.err.println("[MultiplayerMenu] Server address is empty");
            return;
        }
        
        // Parse IP and port
        String host;
        int port = 25565;  // Default port
        
        if (address.contains(":")) {
            String[] parts = address.split(":");
            host = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.err.println("[MultiplayerMenu] Invalid port number: " + parts[1]);
                return;
            }
        } else {
            host = address;
        }
        
        // Connect to server
        uiManager.connectToServer(host, port);
    }
    
    /**
     * Shows the host game dialog.
     */
    private void showHostGameDialog() {
        clearComponents();
        showingHostGame = true;
        
        float centerX = windowWidth / 2.0f;
        float startY = windowHeight * 0.2f;
        float fieldWidth = 300;
        float fieldHeight = 35;
        float spacing = 60;
        
        // Title
        Label titleLabel = new Label(centerX, startY, "Host Game", 
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        float currentY = startY + 60;
        
        // Server port
        Label portLabel = new Label(centerX - fieldWidth / 2, currentY, "Server Port:");
        addComponent(portLabel);
        
        hostPortField = new TextField(centerX - fieldWidth / 2, currentY + 25, 
            fieldWidth, fieldHeight, "25565");
        hostPortField.setText("25565");
        addComponent(hostPortField);
        
        // World seed
        currentY += spacing;
        Label seedLabel = new Label(centerX - fieldWidth / 2, currentY, 
            "World Seed (leave empty for random):");
        addComponent(seedLabel);
        
        worldSeedField = new TextField(centerX - fieldWidth / 2, currentY + 25, 
            fieldWidth, fieldHeight, "0");
        addComponent(worldSeedField);
        
        // Generate structures
        currentY += spacing;
        generateStructuresCheckbox = new Checkbox(centerX - fieldWidth / 2, currentY, 
            20, "Generate Structures (trees, cacti, etc.)", 
            true, checked -> {});
        addComponent(generateStructuresCheckbox);
        
        // Buttons
        currentY += spacing + 20;
        float buttonWidth = 140;
        float buttonHeight = 40;
        float buttonSpacing = 20;
        
        Button startButton = new Button(
            centerX - buttonWidth - buttonSpacing / 2, currentY, 
            buttonWidth, buttonHeight, "Start Server", this::onHostGame);
        addComponent(startButton);
        
        Button cancelButton = new Button(
            centerX + buttonSpacing / 2, currentY, 
            buttonWidth, buttonHeight, "Cancel", this::init);
        addComponent(cancelButton);
    }
    
    /**
     * Handles host game button click.
     */
    private void onHostGame() {
        // Parse port
        int port = 25565;
        try {
            String portText = hostPortField.getText().trim();
            if (!portText.isEmpty()) {
                port = Integer.parseInt(portText);
            }
        } catch (NumberFormatException e) {
            System.err.println("[MultiplayerMenu] Invalid port number");
            return;
        }
        
        // Parse seed
        long seed = 0;
        try {
            String seedText = worldSeedField.getText().trim();
            if (!seedText.isEmpty()) {
                seed = Long.parseLong(seedText);
            }
        } catch (NumberFormatException e) {
            // Try hashing the string as seed
            seed = worldSeedField.getText().hashCode();
        }
        
        boolean generateStructures = generateStructuresCheckbox.isChecked();
        
        // Host server
        uiManager.hostServer(port, seed, generateStructures);
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        
        // Rebuild current view
        if (showingDirectConnect) {
            showDirectConnectDialog();
        } else if (showingHostGame) {
            showHostGameDialog();
        } else {
            init();
        }
    }
}
