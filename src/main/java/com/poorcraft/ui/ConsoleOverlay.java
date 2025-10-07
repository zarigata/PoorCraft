package com.poorcraft.ui;

import com.poorcraft.core.Game;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Console/terminal overlay for executing debug commands.
 */
public class ConsoleOverlay extends UIScreen {
    
    private static final int MAX_OUTPUT_LINES = 50;
    
    private final Game game;
    private final List<String> outputLines;
    private final List<String> commandHistory;
    private TextField inputField;
    private boolean visible;
    private float scrollOffset;
    private int historyIndex;
    
    public ConsoleOverlay(int windowWidth, int windowHeight, Game game) {
        super(windowWidth, windowHeight);
        this.game = game;
        this.outputLines = new ArrayList<>();
        this.commandHistory = new ArrayList<>();
        this.visible = false;
        this.scrollOffset = 0;
        this.historyIndex = -1;
        
        addOutput("Console ready. Type /help for available commands.");
    }
    
    @Override
    public void init() {
        components.clear();
        
        // Create command input field at bottom
        inputField = new TextField(20, windowHeight - 60, windowWidth - 40, 35, "Enter command...");
        inputField.setMaxLength(256);
        inputField.setVisible(false);  // Hidden by default
        addComponent(inputField);
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        
        // Reposition input field
        if (inputField != null) {
            inputField.setX(20);
            inputField.setY(height - 60);
            inputField.setWidth(width - 40);
            inputField.setHeight(35);
        }
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }
        
        // Draw console background (darker than chat)
        float panelWidth = windowWidth - 40;
        float panelHeight = windowHeight / 2 - 40;
        float panelX = 20;
        float panelY = 20;
        
        renderer.drawRect(panelX, panelY, panelWidth, panelHeight, 0.02f, 0.02f, 0.03f, 0.85f);
        
        // Render output lines
        renderOutput(renderer, fontRenderer, panelX, panelY, panelWidth, panelHeight);
        
        // Render input field
        if (inputField != null && inputField.isVisible()) {
            inputField.render(renderer, fontRenderer);
        }
    }
    
    private void renderOutput(UIRenderer renderer, FontRenderer fontRenderer,
                             float panelX, float panelY, float panelWidth, float panelHeight) {
        float textScale = 0.75f;
        float lineHeight = fontRenderer.getTextHeight() * textScale + 2;
        float textX = panelX + 10;
        float startY = panelY + panelHeight - 20;
        
        // Render from bottom to top (newest at bottom)
        int maxVisibleLines = (int)((panelHeight - 30) / lineHeight);
        int startIndex = Math.max(0, outputLines.size() - maxVisibleLines - (int)scrollOffset);
        int endIndex = Math.min(outputLines.size(), startIndex + maxVisibleLines);
        
        float currentY = startY;
        for (int i = endIndex - 1; i >= startIndex; i--) {
            String line = outputLines.get(i);
            
            // Color based on prefix
            float r, g, b, a;
            if (line.startsWith("[ERROR]")) {
                r = 0.9f; g = 0.2f; b = 0.2f; a = 1.0f;  // Red for errors
            } else if (line.startsWith(">")) {
                r = 0.5f; g = 0.5f; b = 0.9f; a = 1.0f;  // Blue for commands
            } else {
                r = 0.2f; g = 0.9f; b = 0.3f; a = 1.0f;  // Green for success
            }
            
            fontRenderer.drawText(line, textX, currentY, textScale, r, g, b, a);
            currentY -= lineHeight;
            
            if (currentY < panelY + 10) {
                break;
            }
        }
    }
    
    @Override
    public void onKeyPress(int key, int mods) {
        if (key == GLFW_KEY_ENTER) {
            executeCommand();
            return;
        }
        
        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_F1) {
            toggleVisibility();
            return;
        }
        
        if (key == GLFW_KEY_UP) {
            // Navigate command history
            if (!commandHistory.isEmpty() && historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                inputField.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            }
            return;
        }
        
        if (key == GLFW_KEY_DOWN) {
            // Navigate command history
            if (historyIndex > 0) {
                historyIndex--;
                inputField.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            } else if (historyIndex == 0) {
                historyIndex = -1;
                inputField.setText("");
            }
            return;
        }
        
        // Forward to input field
        super.onKeyPress(key, mods);
    }
    
    public void toggleVisibility() {
        visible = !visible;
        
        if (inputField != null) {
            inputField.setVisible(visible);
            inputField.setFocused(visible);
            
            if (visible) {
                inputField.setText("");
                historyIndex = -1;
                focusedComponent = inputField;
            } else {
                focusedComponent = null;
            }
        }
    }
    
    private void executeCommand() {
        if (inputField == null) {
            return;
        }
        
        String command = inputField.getText().trim();
        if (command.isEmpty()) {
            return;
        }
        
        // Add to history
        commandHistory.add(command);
        historyIndex = -1;
        
        // Echo command
        addOutput("> " + command);
        
        // Parse and execute
        executeCommandString(command);
        
        inputField.setText("");
    }
    
    private void executeCommandString(String cmd) {
        String[] parts = cmd.split("\\s+");
        String commandName = parts[0].toLowerCase();
        
        try {
            switch (commandName) {
                case "/help" -> {
                    addOutput("Available commands:");
                    addOutput("  /help - Show this help message");
                    addOutput("  /time set <value> - Set time of day (0.0-1.0)");
                    addOutput("  /gamemode <mode> - Change game mode (survival/creative/adventure)");
                    addOutput("  /tp <x> <y> <z> - Teleport player");
                    addOutput("  /clear - Clear console output");
                    addOutput("  /seed - Display world seed");
                    addOutput("  /biome - Display current biome");
                }
                case "/clear" -> {
                    outputLines.clear();
                    addOutput("Console cleared.");
                }
                case "/time" -> {
                    if (parts.length < 3) {
                        addOutput("[ERROR] Usage: /time set <value>");
                        return;
                    }
                    if (parts[1].equalsIgnoreCase("set")) {
                        float value = Float.parseFloat(parts[2]);
                        if (value < 0 || value > 1) {
                            addOutput("[ERROR] Time value must be between 0.0 and 1.0");
                            return;
                        }
                        if (game != null) {
                            game.setTimeOfDay(value);
                            if (game.isWorldLoaded()) {
                                addOutput("Time set to " + value);
                            } else {
                                addOutput("Time set to " + value + " (world will apply after load)");
                            }
                        } else {
                            addOutput("[ERROR] Game instance unavailable");
                        }
                    }
                }
                case "/gamemode" -> {
                    if (parts.length < 2) {
                        addOutput("[ERROR] Usage: /gamemode <mode>");
                        return;
                    }
                    String mode = parts[1].toLowerCase();
                    if (!mode.equals("survival") && !mode.equals("creative") && !mode.equals("adventure")) {
                        addOutput("[ERROR] Valid modes: survival, creative, adventure");
                        return;
                    }
                    if (game != null && game.getPlayerController() != null) {
                        // Note: GameMode setting would need to be implemented in PlayerController
                        addOutput("Game mode changed to " + mode + " (Not fully implemented yet)");
                    } else {
                        addOutput("[ERROR] Player controller not available");
                    }
                }
                case "/tp" -> {
                    if (parts.length < 4) {
                        addOutput("[ERROR] Usage: /tp <x> <y> <z>");
                        return;
                    }
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float z = Float.parseFloat(parts[3]);
                    
                    if (game != null && game.getPlayerController() != null) {
                        game.getPlayerController().setPosition(x, y, z);
                        addOutput("Teleported to " + x + ", " + y + ", " + z);
                    } else {
                        addOutput("[ERROR] Player controller not available");
                    }
                }
                case "/seed" -> {
                    if (game != null && game.getWorld() != null) {
                        addOutput("World seed: " + game.getWorld().getSeed());
                    } else {
                        addOutput("[ERROR] World not loaded");
                    }
                }
                case "/biome" -> {
                    if (game != null && game.getWorld() != null && game.getPlayerPosition() != null) {
                        var pos = game.getPlayerPosition();
                        var biome = game.getWorld().getBiome((int)pos.x, (int)pos.z);
                        addOutput("Current biome: " + biome.toString());
                    } else {
                        addOutput("[ERROR] World or player position not available");
                    }
                }
                default -> addOutput("[ERROR] Unknown command: " + commandName + " (Type /help for available commands)");
            }
        } catch (NumberFormatException e) {
            addOutput("[ERROR] Invalid number format");
        } catch (Exception e) {
            addOutput("[ERROR] Command execution failed: " + e.getMessage());
        }
    }
    
    public void addOutput(String line) {
        outputLines.add(line);
        
        // Keep only last 50 lines
        if (outputLines.size() > MAX_OUTPUT_LINES) {
            outputLines.remove(0);
        }
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Handles mouse scroll events for scrolling through console output.
     * 
     * @param yOffset Scroll amount (positive = up, negative = down)
     */
    public void onScroll(double yOffset) {
        if (!visible) {
            return;
        }
        
        // Calculate max visible lines
        float panelHeight = windowHeight / 2 - 40;
        int maxVisibleLines = (int)((panelHeight - 30) / 20);  // Approximate line height
        int maxScroll = Math.max(0, outputLines.size() - maxVisibleLines);
        
        // Update scroll offset (inverted: positive scroll = scroll up = increase offset)
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (float)yOffset));
    }
}
