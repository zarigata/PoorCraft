package com.poorcraft.ui;

import com.poorcraft.core.GameMode;

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
        float panelWidth = scaledWidth(windowWidth, 0.62f, 560f, 920f);
        float panelHeight = scaledHeight(windowHeight, 0.72f, 580f, 960f);
        float panelX = centerHorizontally(windowWidth, panelWidth);
        float panelY = centerVertically(windowHeight, panelHeight);

        float titleFontSizePx = clamp(windowHeight * 0.05f, 30f, 56f);
        float sectionFontSizePx = clamp(windowHeight * 0.03f, 20f, 34f);
        float labelFontSizePx = clamp(windowHeight * 0.026f, 18f, 30f);
        float helperFontSizePx = clamp(windowHeight * 0.022f, 14f, 22f);
        float titleScale = titleFontSizePx / BASE_FONT_SIZE;
        float sectionScale = sectionFontSizePx / BASE_FONT_SIZE;
        float labelScale = labelFontSizePx / BASE_FONT_SIZE;
        float helperScale = helperFontSizePx / BASE_FONT_SIZE;
        float fieldHeight = clamp(windowHeight * 0.058f, 52f, 76f);
        float buttonHeight = clamp(windowHeight * 0.064f, 56f, 82f);

        float innerPadding = Math.max(42f, panelWidth * 0.065f);
        float contentX = panelX + innerPadding;
        float contentWidth = panelWidth - innerPadding * 2f;
        float currentY = panelY + innerPadding;

        float labelSpacing = Math.max(14f, fieldHeight * 0.24f);
        float sectionSpacing = Math.max(44f, fieldHeight * 1.0f);

        Label titleLabel = new Label(panelX + panelWidth / 2f, currentY, "Create New World",
            0.96f, 0.98f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        titleLabel.setScale(titleScale);
        addComponent(titleLabel);

        currentY += titleFontSizePx + sectionSpacing * 0.8f;

        Label worldSettingsHeading = new Label(contentX, currentY,
            "World Settings", 0.75f, 0.88f, 1.0f, 0.95f);
        worldSettingsHeading.setScale(sectionScale);
        addComponent(worldSettingsHeading);

        currentY += sectionFontSizePx + labelSpacing;

        Label nameLabel = new Label(contentX, currentY, "World Name:",
            0.84f, 0.9f, 0.98f, 0.96f);
        nameLabel.setScale(labelScale);
        addComponent(nameLabel);

        float nameFieldY = currentY + labelFontSizePx + labelSpacing;
        worldNameField = new TextField(contentX, nameFieldY, contentWidth, fieldHeight, "Enter a name");
        worldNameField.setText("New World");
        addComponent(worldNameField);
        currentY = nameFieldY + fieldHeight + sectionSpacing * 0.9f;

        Label seedLabel = new Label(contentX, currentY, "Seed (leave empty for random):",
            0.84f, 0.9f, 0.98f, 0.96f);
        seedLabel.setScale(labelScale);
        addComponent(seedLabel);

        float seedInputY = currentY + labelFontSizePx + labelSpacing;
        float randomSpacing = Math.max(24f, fieldHeight * 0.35f);
        float randomButtonWidth = Math.max(fieldHeight * 2.7f, contentWidth * 0.22f);
        float seedFieldWidth = Math.max(contentWidth - randomButtonWidth - randomSpacing, contentWidth * 0.48f);

        seedField = new TextField(contentX, seedInputY, seedFieldWidth, fieldHeight, "Random seed");
        addComponent(seedField);

        MenuButton randomButton = new MenuButton(contentX + seedFieldWidth + randomSpacing,
            seedInputY, randomButtonWidth, fieldHeight, "RANDOM", this::onRandomSeed);
        addComponent(randomButton);
        currentY = seedInputY + fieldHeight + sectionSpacing * 0.9f;

        Label generationHeading = new Label(contentX, currentY,
            "Gameplay", 0.75f, 0.88f, 1.0f, 0.95f);
        generationHeading.setScale(sectionScale);
        addComponent(generationHeading);

        currentY += sectionFontSizePx + labelSpacing;

        Label gameModeLabel = new Label(contentX, currentY, "Game Mode:",
            0.84f, 0.9f, 0.98f, 0.96f);
        gameModeLabel.setScale(labelScale);
        addComponent(gameModeLabel);

        float gameModeY = currentY + labelFontSizePx + labelSpacing;
        gameModeDropdown = new Dropdown(contentX, gameModeY, contentWidth, fieldHeight,
            Arrays.asList("Survival", "Creative"), 0, index -> {});
        addComponent(gameModeDropdown);
        currentY = gameModeY + fieldHeight + sectionSpacing * 0.8f;

        generateStructuresCheckbox = new Checkbox(contentX, currentY,
            fieldHeight * 0.8f, "Generate Structures (trees, cacti, etc.)",
            true, checked -> {});
        addComponent(generateStructuresCheckbox);

        Label structuresHint = new Label(contentX + fieldHeight * 1.05f, currentY + fieldHeight * 0.95f,
            "Toggle to include villages, pyramids, strongholds, and more.",
            0.68f, 0.74f, 0.82f, 0.88f);
        structuresHint.setScale(helperScale);
        addComponent(structuresHint);

        currentY += fieldHeight * 1.35f + sectionSpacing;

        float buttonSpacing = Math.max(30f, contentWidth * 0.05f);
        float buttonWidth = (contentWidth - buttonSpacing) / 2f;
        float buttonY = currentY;

        MenuButton createButton = new MenuButton(contentX, buttonY,
            buttonWidth, buttonHeight, "CREATE WORLD", this::onCreateWorld);
        addComponent(createButton);

        MenuButton cancelButton = new MenuButton(contentX + buttonWidth + buttonSpacing, buttonY,
            buttonWidth, buttonHeight, "CANCEL",
            () -> uiManager.setState(GameState.MAIN_MENU));
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
        
        GameMode gameMode = GameMode.SURVIVAL;
        if (gameModeDropdown != null) {
            int selectedIndex = gameModeDropdown.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < GameMode.values().length) {
                gameMode = GameMode.values()[selectedIndex];
            }
        }

        // Create world
        uiManager.createWorld(seed, generateStructures, gameMode);
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
