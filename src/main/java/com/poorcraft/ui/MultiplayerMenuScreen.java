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

    private enum ViewState {
        MAIN,
        DIRECT_CONNECT,
        HOST_GAME
    }

    private final UIManager uiManager;
    private TextField directConnectField;
    private TextField hostPortField;
    private TextField worldSeedField;
    private Checkbox generateStructuresCheckbox;

    private Label mainTitleLabel;
    private Button serverListButton;
    private Button directConnectButton;
    private Button hostGameButton;
    private Button backButton;

    private Label directTitleLabel;
    private Label directAddressLabel;
    private Button directConnectActionButton;
    private Button directCancelButton;

    private Label hostTitleLabel;
    private Label hostPortLabel;
    private Label hostSeedLabel;
    private Button hostStartButton;
    private Button hostCancelButton;

    private ViewState viewState;
    private boolean componentsInitialized;
    private boolean layoutDirty;
    
    /**
     * Creates the multiplayer menu screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public MultiplayerMenuScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.viewState = ViewState.MAIN;
        this.componentsInitialized = false;
        this.layoutDirty = true;
    }

    @Override
    public void init() {
        if (!componentsInitialized) {
            clearComponents();
            createComponents();
            componentsInitialized = true;
        }
        setViewState(ViewState.MAIN);
        layoutDirty = true;
        updateLayout();
    }

    private void createComponents() {
        float norm = scaleManager.getTextScaleForFontSize(uiManager.getCurrentAtlasSize());

        // Main view components
        mainTitleLabel = new Label(0f, 0f, "Multiplayer", 1.0f, 1.0f, 1.0f, 1.0f);
        mainTitleLabel.setCentered(true);
        mainTitleLabel.setScale(2.0f * norm);
        addComponent(mainTitleLabel);

        serverListButton = new Button(0f, 0f, 0f, 0f, "Server List (Coming Soon)", () -> {});
        serverListButton.setEnabled(false);
        addComponent(serverListButton);

        directConnectButton = new Button(0f, 0f, 0f, 0f, "Direct Connect",
            () -> setViewState(ViewState.DIRECT_CONNECT));
        addComponent(directConnectButton);

        hostGameButton = new Button(0f, 0f, 0f, 0f, "Host Game",
            () -> setViewState(ViewState.HOST_GAME));
        addComponent(hostGameButton);

        backButton = new Button(0f, 0f, 0f, 0f, "Back",
            () -> uiManager.setState(GameState.MAIN_MENU));
        addComponent(backButton);

        // Direct connect components
        directTitleLabel = new Label(0f, 0f, "Direct Connect", 1.0f, 1.0f, 1.0f, 1.0f);
        directTitleLabel.setCentered(true);
        directAddressLabel = new Label(0f, 0f, "Server Address (IP:Port):");
        addComponent(directTitleLabel);
        addComponent(directAddressLabel);

        directConnectField = new TextField(0f, 0f, 0f, 0f, "localhost:25565");
        directConnectField.setText("localhost:25565");
        addComponent(directConnectField);

        directConnectActionButton = new Button(0f, 0f, 0f, 0f, "Connect", this::onDirectConnect);
        addComponent(directConnectActionButton);
        directCancelButton = new Button(0f, 0f, 0f, 0f, "Cancel",
            () -> setViewState(ViewState.MAIN));
        addComponent(directCancelButton);

        // Host game components
        hostTitleLabel = new Label(0f, 0f, "Host Game", 1.0f, 1.0f, 1.0f, 1.0f);
        hostTitleLabel.setCentered(true);
        hostPortLabel = new Label(0f, 0f, "Server Port:");
        hostSeedLabel = new Label(0f, 0f, "World Seed (leave empty for random):");
        addComponent(hostTitleLabel);
        addComponent(hostPortLabel);
        addComponent(hostSeedLabel);

        hostPortField = new TextField(0f, 0f, 0f, 0f, "25565");
        hostPortField.setText("25565");
        addComponent(hostPortField);

        worldSeedField = new TextField(0f, 0f, 0f, 0f, "0");
        addComponent(worldSeedField);

        generateStructuresCheckbox = new Checkbox(0f, 0f, scaleDimension(20f),
            "Generate Structures (trees, cacti, etc.)", true, checked -> {});
        addComponent(generateStructuresCheckbox);

        hostStartButton = new Button(0f, 0f, 0f, 0f, "Start Server", this::onHostGame);
        addComponent(hostStartButton);
        hostCancelButton = new Button(0f, 0f, 0f, 0f, "Cancel",
            () -> setViewState(ViewState.MAIN));
        addComponent(hostCancelButton);

        updateViewVisibility();
    }

    private void setViewState(ViewState newState) {
        if (viewState == newState) {
            return;
        }
        viewState = newState;
        if (viewState == ViewState.DIRECT_CONNECT && directConnectField != null && directConnectField.getText().isEmpty()) {
            directConnectField.setText("localhost:25565");
        }
        if (viewState == ViewState.HOST_GAME) {
            if (hostPortField != null && hostPortField.getText().isEmpty()) {
                hostPortField.setText("25565");
            }
            if (generateStructuresCheckbox != null) {
                generateStructuresCheckbox.setChecked(true);
            }
        }
        updateViewVisibility();
        layoutDirty = true;
    }

    private void updateViewVisibility() {
        boolean mainVisible = viewState == ViewState.MAIN;
        boolean directVisible = viewState == ViewState.DIRECT_CONNECT;
        boolean hostVisible = viewState == ViewState.HOST_GAME;

        setMainViewVisible(mainVisible);
        setDirectViewVisible(directVisible);
        setHostViewVisible(hostVisible);
    }

    private void setMainViewVisible(boolean visible) {
        if (mainTitleLabel != null) mainTitleLabel.setVisible(visible);
        if (serverListButton != null) serverListButton.setVisible(visible);
        if (directConnectButton != null) directConnectButton.setVisible(visible);
        if (hostGameButton != null) hostGameButton.setVisible(visible);
        if (backButton != null) backButton.setVisible(visible);
    }

    private void setDirectViewVisible(boolean visible) {
        if (directTitleLabel != null) directTitleLabel.setVisible(visible);
        if (directAddressLabel != null) directAddressLabel.setVisible(visible);
        if (directConnectField != null) directConnectField.setVisible(visible);
        if (directConnectActionButton != null) directConnectActionButton.setVisible(visible);
        if (directCancelButton != null) directCancelButton.setVisible(visible);
    }

    private void setHostViewVisible(boolean visible) {
        if (hostTitleLabel != null) hostTitleLabel.setVisible(visible);
        if (hostPortLabel != null) hostPortLabel.setVisible(visible);
        if (hostPortField != null) hostPortField.setVisible(visible);
        if (hostSeedLabel != null) hostSeedLabel.setVisible(visible);
        if (worldSeedField != null) worldSeedField.setVisible(visible);
        if (generateStructuresCheckbox != null) generateStructuresCheckbox.setVisible(visible);
        if (hostStartButton != null) hostStartButton.setVisible(visible);
        if (hostCancelButton != null) hostCancelButton.setVisible(visible);
    }

    private void updateLayout() {
        if (!layoutDirty || !componentsInitialized) {
            return;
        }

        float centerX = windowWidth / 2.0f;
        float norm = scaleManager.getTextScaleForFontSize(uiManager.getCurrentAtlasSize());

        switch (viewState) {
            case MAIN -> layoutMainView(centerX, norm);
            case DIRECT_CONNECT -> layoutDirectConnectView(centerX, norm);
            case HOST_GAME -> layoutHostGameView(centerX, norm);
        }

        layoutDirty = false;
    }

    private void layoutMainView(float centerX, float norm) {
        float startY = windowHeight * 0.25f;
        float buttonWidth = scaleDimension(200f);
        float buttonHeight = scaleDimension(40f);
        float spacing = scaleDimension(50f);

        if (mainTitleLabel != null) {
            mainTitleLabel.setScale(2.0f * norm);
            mainTitleLabel.setPosition(centerX, startY);
        }

        float currentY = startY + scaleDimension(80f);

        if (serverListButton != null) {
            serverListButton.setBounds(centerX - buttonWidth / 2f, currentY, buttonWidth, buttonHeight);
        }
        currentY += spacing;

        if (directConnectButton != null) {
            directConnectButton.setBounds(centerX - buttonWidth / 2f, currentY, buttonWidth, buttonHeight);
        }
        currentY += spacing;

        if (hostGameButton != null) {
            hostGameButton.setBounds(centerX - buttonWidth / 2f, currentY, buttonWidth, buttonHeight);
        }
        currentY += spacing + scaleDimension(20f);

        if (backButton != null) {
            backButton.setBounds(centerX - buttonWidth / 2f, currentY, buttonWidth, buttonHeight);
        }
    }

    private void layoutDirectConnectView(float centerX, float norm) {
        float startY = windowHeight * 0.3f;
        float fieldWidth = scaleDimension(300f);
        float fieldHeight = scaleDimension(35f);
        float buttonWidth = scaleDimension(140f);
        float buttonHeight = scaleDimension(40f);
        float buttonSpacing = scaleDimension(20f);

        if (directTitleLabel != null) {
            directTitleLabel.setScale(1.8f * norm);
            directTitleLabel.setPosition(centerX, startY);
        }

        float currentY = startY + scaleDimension(60f);

        if (directAddressLabel != null) {
            directAddressLabel.setScale(norm);
            directAddressLabel.setPosition(centerX - fieldWidth / 2f, currentY);
        }

        if (directConnectField != null) {
            directConnectField.setBounds(centerX - fieldWidth / 2f,
                currentY + scaleDimension(25f), fieldWidth, fieldHeight);
        }

        currentY += scaleDimension(100f);

        if (directConnectActionButton != null) {
            directConnectActionButton.setBounds(
                centerX - buttonWidth - buttonSpacing / 2f,
                currentY,
                buttonWidth,
                buttonHeight);
        }
        if (directCancelButton != null) {
            directCancelButton.setBounds(
                centerX + buttonSpacing / 2f,
                currentY,
                buttonWidth,
                buttonHeight);
        }
    }

    private void layoutHostGameView(float centerX, float norm) {
        float startY = windowHeight * 0.2f;
        float fieldWidth = scaleDimension(300f);
        float fieldHeight = scaleDimension(35f);
        float spacing = scaleDimension(60f);
        float buttonWidth = scaleDimension(140f);
        float buttonHeight = scaleDimension(40f);
        float buttonSpacing = scaleDimension(20f);

        if (hostTitleLabel != null) {
            hostTitleLabel.setScale(1.8f * norm);
            hostTitleLabel.setPosition(centerX, startY);
        }

        float currentY = startY + scaleDimension(60f);

        if (hostPortLabel != null) {
            hostPortLabel.setScale(norm);
            hostPortLabel.setPosition(centerX - fieldWidth / 2f, currentY);
        }

        if (hostPortField != null) {
            hostPortField.setBounds(centerX - fieldWidth / 2f,
                currentY + scaleDimension(25f), fieldWidth, fieldHeight);
        }

        currentY += spacing;

        if (hostSeedLabel != null) {
            hostSeedLabel.setScale(norm);
            hostSeedLabel.setPosition(centerX - fieldWidth / 2f, currentY);
        }

        if (worldSeedField != null) {
            worldSeedField.setBounds(centerX - fieldWidth / 2f,
                currentY + scaleDimension(25f), fieldWidth, fieldHeight);
        }

        currentY += spacing;

        if (generateStructuresCheckbox != null) {
            generateStructuresCheckbox.setBounds(centerX - fieldWidth / 2f,
                currentY, scaleDimension(20f), scaleDimension(20f));
        }

        currentY += spacing + scaleDimension(20f);

        if (hostStartButton != null) {
            hostStartButton.setBounds(
                centerX - buttonWidth - buttonSpacing / 2f,
                currentY,
                buttonWidth,
                buttonHeight);
        }

        if (hostCancelButton != null) {
            hostCancelButton.setBounds(
                centerX + buttonSpacing / 2f,
                currentY,
                buttonWidth,
                buttonHeight);
        }
    }

    private void onDirectConnect() {
        if (directConnectField == null) {
            return;
        }
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

        boolean generateStructures = generateStructuresCheckbox != null && generateStructuresCheckbox.isChecked();

        // Host server
        uiManager.hostServer(port, seed, generateStructures);
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        layoutDirty = true;
    }

    @Override
    public void update(float deltaTime) {
        if (layoutDirty) {
            updateLayout();
        }
        super.update(deltaTime);
    }
}
