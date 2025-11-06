package com.poorcraft.ui;

import com.poorcraft.core.GameMode;

import java.util.Arrays;
import java.util.Random;

import static com.poorcraft.ui.LayoutUtils.centerHorizontally;
import static com.poorcraft.ui.LayoutUtils.centerVertically;

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
    
    private UIManager uiManager;
    private TextField worldNameField;
    private TextField seedField;
    private Checkbox generateStructuresCheckbox;
    private Dropdown gameModeDropdown;
    private boolean layoutDirty = false;
    private boolean componentsInitialized = false;

    private Label titleLabel;
    private Label worldSettingsHeading;
    private Label nameLabel;
    private Label seedLabel;
    private Label generationHeading;
    private Label structuresHint;
    private Label gameModeLabel;
    private MenuButton randomButton;
    private MenuButton createButton;
    private MenuButton cancelButton;
    
    /**
     * Creates the world creation screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param uiManager UI manager
     */
    public WorldCreationScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
    }
    
    @Override
    public void init() {
        componentsInitialized = true;
        clearComponents();

        titleLabel = new Label(0f, 0f, "Create New World",
            0.96f, 0.98f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);

        worldSettingsHeading = new Label(0f, 0f,
            "World Settings", 0.75f, 0.88f, 1.0f, 0.95f);
        addComponent(worldSettingsHeading);

        nameLabel = new Label(0f, 0f, "World Name:",
            0.84f, 0.9f, 0.98f, 0.96f);
        addComponent(nameLabel);

        worldNameField = new TextField(0f, 0f, 0f, 0f, "Enter a name");
        worldNameField.setText("New World");
        addComponent(worldNameField);

        seedLabel = new Label(0f, 0f, "Seed (leave empty for random):",
            0.84f, 0.9f, 0.98f, 0.96f);
        addComponent(seedLabel);

        seedField = new TextField(0f, 0f, 0f, 0f, "Random seed");
        addComponent(seedField);

        randomButton = new MenuButton(0f, 0f, 0f, 0f, "RANDOM", this::onRandomSeed);
        addComponent(randomButton);

        generationHeading = new Label(0f, 0f,
            "Gameplay", 0.75f, 0.88f, 1.0f, 0.95f);
        addComponent(generationHeading);

        gameModeLabel = new Label(0f, 0f, "Game Mode:",
            0.84f, 0.9f, 0.98f, 0.96f);
        addComponent(gameModeLabel);

        gameModeDropdown = new Dropdown(0f, 0f, 0f, 0f,
            Arrays.asList("Survival", "Creative"), 0, index -> {});
        addComponent(gameModeDropdown);

        generateStructuresCheckbox = new Checkbox(0f, 0f,
            0f, "Generate Structures (trees, cacti, etc.)",
            true, checked -> {});
        addComponent(generateStructuresCheckbox);

        structuresHint = new Label(0f, 0f,
            "Toggle to include villages, pyramids, strongholds, and more.",
            0.68f, 0.74f, 0.82f, 0.88f);
        addComponent(structuresHint);

        createButton = new MenuButton(0f, 0f, 0f, 0f,
            "CREATE WORLD", this::onCreateWorld);
        addComponent(createButton);

        cancelButton = new MenuButton(0f, 0f, 0f, 0f,
            "CANCEL",
            () -> uiManager.setState(GameState.MAIN_MENU));
        addComponent(cancelButton);

        recalculateLayout();
        layoutDirty = false;
    }

    private void recalculateLayout() {
        if (!componentsInitialized) {
            return;
        }

        float panelWidth = scaleManager.scaleWidth(0.62f);
        float panelHeight = scaleManager.scaleHeight(0.72f);
        float panelX = centerHorizontally(windowWidth, panelWidth);
        float panelY = centerVertically(windowHeight, panelHeight);

        float norm = scaleManager.getTextScaleForFontSize(uiManager.getCurrentAtlasSize());
        float titleScale = 2.5f * norm;
        float sectionScale = 1.5f * norm;
        float labelScale = 1.2f * norm;
        float helperScale = 0.9f * norm;
        float fieldHeight = scaleDimension(60f);
        float buttonHeight = scaleDimension(65f);

        float innerPadding = scaleDimension(42f);
        float contentX = panelX + innerPadding;
        float contentWidth = panelWidth - innerPadding * 2f;
        float currentY = panelY + innerPadding;

        float labelSpacing = scaleDimension(14f);
        float sectionSpacing = scaleDimension(44f);

        float titleCenterX = panelX + panelWidth / 2f;
        if (titleLabel != null) {
            titleLabel.setScale(titleScale);
            titleLabel.setPosition(titleCenterX, currentY);
        }

        currentY += scaleDimension(60f) + sectionSpacing * 0.8f;

        if (worldSettingsHeading != null) {
            worldSettingsHeading.setScale(sectionScale);
            worldSettingsHeading.setPosition(contentX, currentY);
        }

        currentY += scaleDimension(30f) + labelSpacing;

        if (nameLabel != null) {
            nameLabel.setScale(labelScale);
            nameLabel.setPosition(contentX, currentY);
        }

        float nameFieldY = currentY + scaleDimension(24f) + labelSpacing;
        if (worldNameField != null) {
            worldNameField.setBounds(contentX, nameFieldY, contentWidth, fieldHeight);
        }
        currentY = nameFieldY + fieldHeight + sectionSpacing * 0.9f;

        if (seedLabel != null) {
            seedLabel.setScale(labelScale);
            seedLabel.setPosition(contentX, currentY);
        }

        float seedInputY = currentY + scaleDimension(24f) + labelSpacing;
        float randomSpacing = scaleDimension(24f);
        float randomButtonWidth = fieldHeight * 2.7f;
        float seedFieldWidth = Math.max(120f, contentWidth - randomButtonWidth - randomSpacing);

        if (seedField != null) {
            seedField.setBounds(contentX, seedInputY, seedFieldWidth, fieldHeight);
        }
        if (randomButton != null) {
            randomButton.setBounds(contentX + seedFieldWidth + randomSpacing, seedInputY, randomButtonWidth, fieldHeight);
        }
        currentY = seedInputY + fieldHeight + sectionSpacing * 0.9f;

        if (generationHeading != null) {
            generationHeading.setScale(sectionScale);
            generationHeading.setPosition(contentX, currentY);
        }

        currentY += scaleDimension(30f) + labelSpacing;

        if (gameModeLabel != null) {
            gameModeLabel.setScale(labelScale);
            gameModeLabel.setPosition(contentX, currentY);
        }

        float gameModeY = currentY + scaleDimension(24f) + labelSpacing;
        if (gameModeDropdown != null) {
            gameModeDropdown.setBounds(contentX, gameModeY, contentWidth, fieldHeight);
        }
        currentY = gameModeY + fieldHeight + sectionSpacing * 0.8f;

        float checkboxSize = fieldHeight * 0.8f;
        if (generateStructuresCheckbox != null) {
            generateStructuresCheckbox.setBounds(contentX, currentY, checkboxSize, checkboxSize);
        }
        if (structuresHint != null) {
            structuresHint.setScale(helperScale);
            structuresHint.setPosition(contentX + checkboxSize * 1.05f, currentY + checkboxSize * 0.95f);
        }

        currentY += checkboxSize * 1.35f + sectionSpacing;

        float buttonSpacing = scaleDimension(30f);
        float buttonWidth = Math.max(120f, (contentWidth - buttonSpacing) / 2f);
        float buttonY = currentY;

        if (createButton != null) {
            createButton.setBounds(contentX, buttonY, buttonWidth, buttonHeight);
        }
        if (cancelButton != null) {
            cancelButton.setBounds(contentX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight);
        }
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

    @Override
    public void update(float deltaTime) {
        if (layoutDirty) {
            recalculateLayout();
            layoutDirty = false;
        }
        super.update(deltaTime);
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
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw darkened backdrop
        renderer.drawRect(0, 0, windowWidth, windowHeight, 0.05f, 0.1f, 0.15f, 0.92f);

        // Draw panel behind inputs (stone-inspired card)
        float panelWidth = scaleManager.scaleWidth(0.62f);
        float panelHeight = scaleManager.scaleHeight(0.72f);
        float panelX = centerHorizontally(windowWidth, panelWidth);
        float panelY = centerVertically(windowHeight, panelHeight);

        renderer.drawRect(panelX, panelY, panelWidth, panelHeight,
            0.18f, 0.18f, 0.2f, 0.95f);

        float borderSize = scaleDimension(4f);
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
