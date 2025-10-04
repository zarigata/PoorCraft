package com.poorcraft.ui;

import java.util.Arrays;
import java.util.Random;

import static com.poorcraft.ui.LayoutUtils.centerHorizontally;
import static com.poorcraft.ui.LayoutUtils.centerVertically;
import static com.poorcraft.ui.LayoutUtils.clamp;
import static com.poorcraft.ui.LayoutUtils.scaledHeight;
import static com.poorcraft.ui.LayoutUtils.scaledWidth;

/**
 * World creation screen.
 * 
 * Configure world generation parameters before creating a new world.
 * Allows setting seed, world name, game mode, and structure generation.
 * 
 * Seeds are fun! You can share them with friends to play the same world.
 * Or just mash the keyboard and see what happens. Both valid strategies.
 */
public class WorldCreationScreen extends UIScreen {
    
    private static final float BASE_FONT_SIZE = 20f;

    private UIManager uiManager;
    private TextField worldNameField;
    private TextField seedField;
    private Checkbox generateStructuresCheckbox;
    private Dropdown gameModeDropdown;
    
    /**
     * Creates the world creation screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public WorldCreationScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
    }
    
    @Override
    public void init() {
        clearComponents();
        float panelWidth = scaledWidth(windowWidth, 0.6f, 520f, 860f);
        float panelHeight = scaledHeight(windowHeight, 0.65f, 520f, 900f);
        float panelX = centerHorizontally(windowWidth, panelWidth);
        float panelY = centerVertically(windowHeight, panelHeight);

        float titleFontSizePx = clamp(windowHeight * 0.04f, 26f, 48f);
        float labelFontSizePx = clamp(windowHeight * 0.024f, 18f, 28f);
        float titleScale = titleFontSizePx / BASE_FONT_SIZE;
        float labelScale = labelFontSizePx / BASE_FONT_SIZE;
        float fieldHeight = clamp(windowHeight * 0.05f, 42f, 64f);
        float buttonHeight = clamp(windowHeight * 0.055f, 48f, 68f);

        float innerPadding = fieldHeight * 0.8f;
        float contentX = panelX + innerPadding;
        float contentWidth = panelWidth - innerPadding * 2;
        float currentY = panelY + innerPadding;
        float rowSpacing = fieldHeight * 1.35f;

        // Title
        Label titleLabel = new Label(panelX + panelWidth / 2, currentY, "Create New World",
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        addComponent(titleLabel);

        currentY += titleFontSizePx + fieldHeight * 0.6f;

        Label nameLabel = new Label(contentX, currentY, "World Name:");
        nameLabel.setScale(labelScale);
        addComponent(nameLabel);

        worldNameField = new TextField(contentX, currentY + labelFontSizePx + 10f,
            contentWidth, fieldHeight, "My World");
        worldNameField.setText("New World");
        addComponent(worldNameField);

        // Seed
        currentY += rowSpacing;
        Label seedLabel = new Label(contentX, currentY, "Seed (leave empty for random):");
        seedLabel.setScale(labelScale);
        addComponent(seedLabel);

        float seedFieldWidth = contentWidth - fieldHeight * 1.8f;
        seedField = new TextField(contentX, currentY + labelFontSizePx + 10f,
            seedFieldWidth, fieldHeight, "0");
        addComponent(seedField);

        // Random seed button (classic menu button)
        float randomButtonWidth = fieldHeight * 2.2f;
        MenuButton randomButton = new MenuButton(contentX + seedFieldWidth + fieldHeight * 0.6f,
            currentY + labelFontSizePx + 10f, randomButtonWidth, fieldHeight, "RANDOM", this::onRandomSeed);
        addComponent(randomButton);

        // Game mode
        currentY += rowSpacing;
        Label gameModeLabel = new Label(contentX, currentY, "Game Mode:");
        gameModeLabel.setScale(labelScale);
        addComponent(gameModeLabel);

        gameModeDropdown = new Dropdown(contentX, currentY + labelFontSizePx + 10f, 
            contentWidth, fieldHeight, 
            Arrays.asList("Survival", "Creative"), 
            0, 
            index -> {});
        addComponent(gameModeDropdown);

        // Generate structures
        currentY += rowSpacing;
        generateStructuresCheckbox = new Checkbox(contentX, currentY + labelFontSizePx * 0.3f, 
            fieldHeight * 0.8f, "Generate Structures (trees, cacti, etc.)", 
            true, 
            checked -> {});
        addComponent(generateStructuresCheckbox);
        currentY += rowSpacing + fieldHeight * 0.6f;
        float buttonWidth = (contentWidth - fieldHeight * 0.6f) / 2f;
        float buttonY = currentY;

        MenuButton createButton = new MenuButton(
            contentX, buttonY, 
            buttonWidth, buttonHeight, 
            "CREATE WORLD", 
            this::onCreateWorld
        );
        addComponent(createButton);

        MenuButton cancelButton = new MenuButton(
            contentX + buttonWidth + fieldHeight * 0.6f, buttonY, 
            buttonWidth, buttonHeight, 
            "CANCEL", 
            () -> uiManager.setState(GameState.MAIN_MENU)
        );
        addComponent(cancelButton);
    }
    
    /**
     * Called when Create World button is clicked.
     */
    private void onCreateWorld() {
        // Validate and parse inputs
        String worldName = worldNameField.getText().trim();
        if (worldName.isEmpty()) {
            worldName = "New World";
        }
        
        // Parse seed
        long seed = 0;
        String seedText = seedField.getText().trim();
        if (!seedText.isEmpty()) {
            try {
                seed = Long.parseLong(seedText);
            } catch (NumberFormatException e) {
                // Use string hash as seed
                seed = seedText.hashCode();
            }
        } else {
            // Generate random seed
            seed = new Random().nextLong();
        }
        
        boolean generateStructures = generateStructuresCheckbox.isChecked();
        
        System.out.println("[WorldCreation] Creating world: " + worldName + 
            ", seed: " + seed + ", structures: " + generateStructures);
        
        // Create world
        uiManager.createWorld(seed, generateStructures);
    }
    
    /**
     * Called when Random button is clicked.
     */
    private void onRandomSeed() {
        long randomSeed = new Random().nextLong();
        seedField.setText(String.valueOf(randomSeed));
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init();
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw darkened backdrop
        renderer.drawRect(0, 0, windowWidth, windowHeight, 0.05f, 0.1f, 0.15f, 0.92f);

        // Draw panel behind inputs (stone-inspired card)
        float panelWidth = scaledWidth(windowWidth, 0.6f, 520f, 860f);
        float panelHeight = scaledHeight(windowHeight, 0.65f, 520f, 900f);
        float panelX = centerHorizontally(windowWidth, panelWidth);
        float panelY = centerVertically(windowHeight, panelHeight);

        renderer.drawRect(panelX, panelY, panelWidth, panelHeight,
            0.18f, 0.18f, 0.2f, 0.95f);

        float borderSize = 4f;
        renderer.drawRect(panelX, panelY, panelWidth, borderSize,
            0.6f, 0.6f, 0.65f, 0.9f);
        renderer.drawRect(panelX, panelY + panelHeight - borderSize, panelWidth, borderSize,
            0.12f, 0.12f, 0.14f, 0.9f);
        renderer.drawRect(panelX, panelY, borderSize, panelHeight,
            0.6f, 0.6f, 0.65f, 0.9f);
        renderer.drawRect(panelX + panelWidth - borderSize, panelY, borderSize, panelHeight,
            0.12f, 0.12f, 0.14f, 0.9f);

        super.render(renderer, fontRenderer);
    }
}
