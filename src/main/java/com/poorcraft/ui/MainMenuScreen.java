package com.poorcraft.ui;

import java.util.Random;

/**
 * Responsive main menu with text inputs and clearly framed buttons. The layout
 * centers a 70% panel with padded content to avoid overlap at narrow widths.
 */
public class MainMenuScreen extends UIScreen {

    private final UIManager uiManager;
    private final MenuBackground background;

    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;

    private TextField serverNameField;
    private TextField seedField;

    public MainMenuScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
        this.background = new MenuBackground();
    }

    @Override
    public void init() {
        clearComponents();

        panelWidth = clamp(windowWidth * 0.7f, 640f, Math.max(640f, windowWidth - 120f));
        panelHeight = clamp(windowHeight * 0.7f, 540f, Math.max(540f, windowHeight - 120f));
        panelX = (windowWidth - panelWidth) / 2.0f;
        panelY = (windowHeight - panelHeight) / 2.0f;

        float padding = Math.max(28f, panelWidth * 0.05f);
        float innerX = panelX + padding;
        float innerWidth = panelWidth - padding * 2f;
        float currentY = panelY + padding;

        float titleScale = Math.max(2.0f, panelWidth / 520f);
        float subtitleScale = titleScale * 0.55f;

        Label titleLabel = new Label(panelX + panelWidth / 2f, currentY, "PoorCraft",
            0.02f, 0.92f, 0.95f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        addComponent(titleLabel);
        currentY += 44f * titleScale;

        Label subtitleLabel = new Label(panelX + panelWidth / 2f, currentY, "Retro Edition",
            0.92f, 0.32f, 0.72f, 0.9f);
        subtitleLabel.setCentered(true);
        subtitleLabel.setScale(subtitleScale);
        addComponent(subtitleLabel);
        currentY += Math.max(34f, 26f * subtitleScale);

        float labelScale = Math.max(0.85f, panelWidth / 840f);
        float fieldHeight = Math.max(46f, panelHeight * 0.085f);

        Label nameLabel = new Label(innerX, currentY, "Server Name",
            0.8f, 0.84f, 0.92f, 0.95f);
        nameLabel.setScale(labelScale);
        addComponent(nameLabel);
        currentY += 24f * labelScale;

        serverNameField = new TextField(innerX, currentY, innerWidth, fieldHeight, "My Server");
        addComponent(serverNameField);
        currentY += fieldHeight + Math.max(18f, fieldHeight * 0.25f);

        Label seedLabel = new Label(innerX, currentY, "World Seed",
            0.8f, 0.84f, 0.92f, 0.95f);
        seedLabel.setScale(labelScale);
        addComponent(seedLabel);
        currentY += 24f * labelScale;

        float seedFieldWidth = Math.max(innerWidth * 0.6f, innerWidth - 220f);
        seedField = new TextField(innerX, currentY, seedFieldWidth, fieldHeight, "Random");
        addComponent(seedField);

        float randomWidth = Math.max(fieldHeight * 2.4f, innerWidth - seedFieldWidth - padding * 0.6f);
        MenuButton randomSeedButton = new MenuButton(innerX + seedFieldWidth + padding * 0.4f,
            currentY, randomWidth, fieldHeight, "RANDOM", this::applyRandomSeed);
        addComponent(randomSeedButton);
        currentY += fieldHeight + Math.max(28f, fieldHeight * 0.35f);

        float buttonHeight = clamp(panelHeight * 0.16f, 90f, 160f);
        float buttonSpacing = Math.max(22f, buttonHeight * 0.35f);
        float buttonWidth = (innerWidth - buttonSpacing) / 2f;

        MenuButton singleplayerButton = new MenuButton(innerX, currentY,
            buttonWidth, buttonHeight, "SINGLEPLAYER",
            () -> uiManager.setState(GameState.WORLD_CREATION));
        addComponent(singleplayerButton);

        MenuButton multiplayerButton = new MenuButton(innerX + buttonWidth + buttonSpacing, currentY,
            buttonWidth, buttonHeight, "MULTIPLAYER",
            () -> uiManager.setState(GameState.MULTIPLAYER_MENU));
        addComponent(multiplayerButton);

        MenuButton settingsButton = new MenuButton(innerX, currentY + buttonHeight + buttonSpacing,
            buttonWidth, buttonHeight, "SETTINGS",
            () -> uiManager.setState(GameState.SETTINGS_MENU));
        addComponent(settingsButton);

        MenuButton quitButton = new MenuButton(innerX + buttonWidth + buttonSpacing, currentY + buttonHeight + buttonSpacing,
            buttonWidth, buttonHeight, "QUIT",
            () -> uiManager.quit());
        addComponent(quitButton);

        Label footerLabel = new Label(panelX + panelWidth / 2f, panelY + panelHeight - padding * 0.6f,
            "Press ENTER to confirm or ESC to exit", 0.72f, 0.78f, 0.86f, 0.86f);
        footerLabel.setCentered(true);
        footerLabel.setScale(Math.max(0.92f, panelWidth / 780f));
        addComponent(footerLabel);
    }

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
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        background.render(renderer, windowWidth, windowHeight);

        renderer.drawRect(panelX, panelY, panelWidth, panelHeight,
            0.07f, 0.09f, 0.12f, 0.92f);

        float border = Math.max(2f, panelWidth * 0.003f);
        renderer.drawRect(panelX, panelY, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.82f);
        renderer.drawRect(panelX, panelY + panelHeight - border, panelWidth, border,
            0.0f, 0.95f, 0.95f, 0.82f);
        renderer.drawRect(panelX, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.82f);
        renderer.drawRect(panelX + panelWidth - border, panelY, border, panelHeight,
            0.0f, 0.95f, 0.95f, 0.82f);

        super.render(renderer, fontRenderer);
    }

    private void applyRandomSeed() {
        long randomSeed = new Random().nextLong();
        if (seedField != null) {
            seedField.setText(Long.toString(randomSeed));
        }
    }
}
