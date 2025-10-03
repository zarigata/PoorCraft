package com.poorcraft.ui;

import java.util.Arrays;
import java.util.Random;

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
        
        float centerX = windowWidth / 2.0f;
        float startY = windowHeight * 0.2f;
        float fieldWidth = 300;
        float fieldHeight = 35;
        float spacing = 60;
        
        // Title
        Label titleLabel = new Label(centerX, startY, "Create New World", 
            1.0f, 1.0f, 1.0f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);
        
        // World name
        float currentY = startY + 60;
        Label nameLabel = new Label(centerX - fieldWidth / 2, currentY, "World Name:");
        addComponent(nameLabel);
        
        worldNameField = new TextField(centerX - fieldWidth / 2, currentY + 25, 
            fieldWidth, fieldHeight, "My World");
        worldNameField.setText("New World");
        addComponent(worldNameField);
        
        // Seed
        currentY += spacing;
        Label seedLabel = new Label(centerX - fieldWidth / 2, currentY, "Seed (leave empty for random):");
        addComponent(seedLabel);
        
        seedField = new TextField(centerX - fieldWidth / 2, currentY + 25, 
            fieldWidth - 80, fieldHeight, "0");
        addComponent(seedField);
        
        // Random seed button
        Button randomButton = new Button(centerX + fieldWidth / 2 - 70, currentY + 25, 
            70, fieldHeight, "Random", this::onRandomSeed);
        addComponent(randomButton);
        
        // Game mode
        currentY += spacing;
        Label gameModeLabel = new Label(centerX - fieldWidth / 2, currentY, "Game Mode:");
        addComponent(gameModeLabel);
        
        gameModeDropdown = new Dropdown(centerX - fieldWidth / 2, currentY + 25, 
            fieldWidth, fieldHeight, 
            Arrays.asList("Survival", "Creative"), 
            0, 
            index -> {});
        addComponent(gameModeDropdown);
        
        // Generate structures
        currentY += spacing;
        generateStructuresCheckbox = new Checkbox(centerX - fieldWidth / 2, currentY, 
            20, "Generate Structures (trees, cacti, etc.)", 
            true, 
            checked -> {});
        addComponent(generateStructuresCheckbox);
        
        // Buttons
        currentY += spacing + 20;
        float buttonWidth = 140;
        float buttonHeight = 40;
        float buttonSpacing = 20;
        
        Button createButton = new Button(
            centerX - buttonWidth - buttonSpacing / 2, currentY, 
            buttonWidth, buttonHeight, 
            "Create World", 
            this::onCreateWorld
        );
        addComponent(createButton);
        
        Button cancelButton = new Button(
            centerX + buttonSpacing / 2, currentY, 
            buttonWidth, buttonHeight, 
            "Cancel", 
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
        // Draw background
        renderer.drawRect(0, 0, windowWidth, windowHeight, 0.1f, 0.1f, 0.15f, 1.0f);
        
        // Draw all components
        super.render(renderer, fontRenderer);
    }
}
