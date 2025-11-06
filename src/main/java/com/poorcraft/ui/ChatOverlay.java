package com.poorcraft.ui;

import com.poorcraft.core.Game;
import com.poorcraft.modding.ModAPI;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Chat overlay UI component that displays chat messages and handles input.
 * Renders over the HUD in both IN_GAME and PAUSED states.
 */
public class ChatOverlay extends UIScreen {
    
    private static final int MAX_MESSAGES = 100;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    
    private final Game game;
    private final List<ChatMessage> messages;
    private final Object messagesLock = new Object();
    private final ConcurrentLinkedQueue<ChatMessage> pendingMessages;
    private TextField inputField;
    private boolean visible;
    private float scrollOffset;
    private ModAPI modAPI;
    
    public ChatOverlay(int windowWidth, int windowHeight, Game game, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.game = game;
        this.messages = new ArrayList<>();
        this.pendingMessages = new ConcurrentLinkedQueue<>();
        this.visible = false;
        this.scrollOffset = 0;
        
        // Get ModAPI from game's mod loader
        try {
            if (game != null && game.getModLoader() != null) {
                this.modAPI = game.getModLoader().getModAPI();
            }
        } catch (Exception e) {
            System.err.println("[ChatOverlay] Failed to get ModAPI: " + e.getMessage());
        }
    }
    
    @Override
    public void init() {
        components.clear();
        
        // Create text input field at bottom of screen with scaled dimensions
        float fieldX = scaleDimension(50f);
        float fieldY = windowHeight - scaleDimension(80f);
        float fieldWidth = windowWidth - scaleDimension(100f);
        float fieldHeight = scaleDimension(40f);
        
        inputField = new TextField(fieldX, fieldY, fieldWidth, fieldHeight, "Press T to chat...");
        inputField.setMaxLength(256);
        inputField.setVisible(false);  // Hidden by default
        // Text scale will be set during render when FontRenderer is available
        addComponent(inputField);
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        
        // Reposition input field with scaled dimensions
        if (inputField != null) {
            inputField.setX(scaleDimension(50f));
            inputField.setY(height - scaleDimension(80f));
            inputField.setWidth(width - scaleDimension(100f));
            inputField.setHeight(scaleDimension(40f));
            // Text scale will be set during render when FontRenderer is available
        }
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }
        
        // Draw semi-transparent background panel with scaled dimensions
        float panelWidth = Math.min(windowWidth - scaleDimension(100f), scaleDimension(600f));
        float panelHeight = scaleDimension(350f);
        float panelX = scaleDimension(50f);
        float panelY = windowHeight - scaleDimension(450f);
        
        renderer.drawRect(panelX, panelY, panelWidth, panelHeight, 0.05f, 0.05f, 0.08f, 0.7f);
        
        // Render message history
        renderMessageHistory(renderer, fontRenderer, panelX, panelY, panelWidth, panelHeight);
        
        // Render input field
        if (inputField != null && inputField.isVisible()) {
            inputField.render(renderer, fontRenderer);
        }
    }
    
    private void renderMessageHistory(UIRenderer renderer, FontRenderer fontRenderer, 
                                      float panelX, float panelY, float panelWidth, float panelHeight) {
        float messageScale = getTextScale(fontRenderer);
        float lineHeight = fontRenderer.getTextHeight() * messageScale + scaleDimension(4f);
        
        // Update input field text scale if needed
        if (inputField != null && scaleManager != null) {
            inputField.setTextScale(messageScale);
        }
        float textX = panelX + scaleDimension(10f);
        float startY = panelY + panelHeight - scaleDimension(20f);
        
        List<ChatMessage> snapshot = getMessagesSnapshot();

        // Calculate max visible messages dynamically based on panel height and line height
        float padding = scaleDimension(20f);
        int maxVisible = Math.max(1, (int)Math.floor((panelHeight - padding) / lineHeight));
        
        // Render messages from bottom to top (newest at bottom)
        int startIndex = Math.max(0, snapshot.size() - maxVisible - (int)scrollOffset);
        int endIndex = Math.min(snapshot.size(), startIndex + maxVisible);
        
        float currentY = startY;
        for (int i = endIndex - 1; i >= startIndex; i--) {
            ChatMessage msg = snapshot.get(i);
            
            // Format: [HH:MM] PlayerName: message
            String timeStr = TIME_FORMAT.format(new Date(msg.timestamp));
            String fullMsg = "[" + timeStr + "] " + msg.senderName + ": " + msg.message;
            
            // Color based on message type
            float r, g, b, a;
            if (msg.isSystemMessage) {
                r = 1.0f; g = 0.9f; b = 0.2f; a = 1.0f;  // Yellow for system
            } else {
                r = 1.0f; g = 1.0f; b = 1.0f; a = 1.0f;  // White for players
            }
            
            // Word wrap if message is too long
            float maxWidth = panelWidth - scaleDimension(20f);
            float textWidth = fontRenderer.getTextWidth(fullMsg) * messageScale;
            
            if (textWidth > maxWidth) {
                // Simple truncate for now (could implement proper word wrap later)
                int maxChars = (int)(maxWidth / (fontRenderer.getTextWidth("W") * messageScale));
                if (fullMsg.length() > maxChars) {
                    fullMsg = fullMsg.substring(0, maxChars - 3) + "...";
                }
            }
            
            fontRenderer.drawText(fullMsg, textX, currentY, messageScale, r, g, b, a);
            currentY -= lineHeight;
            
            if (currentY < panelY + scaleDimension(10f)) {
                break;  // Stop if we've run out of space
            }
        }
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        ChatMessage nextMessage;
        while ((nextMessage = pendingMessages.poll()) != null) {
            addMessageInternal(nextMessage);
        }
    }
    
    @Override
    public void onKeyPress(int key, int mods) {
        if (key == GLFW_KEY_ENTER) {
            sendMessage();
            return;
        }
        
        if (key == GLFW_KEY_ESCAPE) {
            toggleVisibility();
            return;
        }
        
        if (key == GLFW_KEY_T && !visible) {
            toggleVisibility();
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
                focusedComponent = inputField;
            } else {
                focusedComponent = null;
            }
        }
    }
    
    public void sendMessage() {
        if (inputField == null) {
            return;
        }
        
        String message = inputField.getText().trim();
        if (message.isEmpty()) {
            toggleVisibility();
            return;
        }
        
        // Send via GameClient if in multiplayer, otherwise add locally
        if (game != null && game.getUIManager() != null) {
            var uiManager = game.getUIManager();
            if (uiManager.getGameClient() != null && uiManager.getGameClient().isConnected()) {
                uiManager.getGameClient().sendChatMessage(message);
            } else {
                // Single-player: add message locally
                addMessage(-1, "Player", message, System.currentTimeMillis(), false);
            }
        } else {
            // Single-player: add message locally
            addMessage(-1, "Player", message, System.currentTimeMillis(), false);
        }
        
        inputField.setText("");
        toggleVisibility();
    }
    
    public void addMessage(int senderId, String senderName, String message, long timestamp, boolean isSystem) {
        ChatMessage chatMsg = new ChatMessage(senderId, senderName, message, timestamp, isSystem);
        addMessageInternal(chatMsg);
    }

    public void enqueueMessage(int senderId, String senderName, String message, long timestamp, boolean isSystem) {
        pendingMessages.offer(new ChatMessage(senderId, senderName, message, timestamp, isSystem));
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Handles mouse scroll events for scrolling through message history.
     * 
     * @param yOffset Scroll amount (positive = up, negative = down)
     */
    public void onScroll(double yOffset) {
        if (!visible) {
            return;
        }
        
        // Calculate max visible based on current panel size and line height
        // This requires approximating the line height since FontRenderer isn't available here
        float panelHeight = scaleDimension(350f);
        float padding = scaleDimension(20f);
        // Approximate line height: base text height (20px at 1.0 scale) + spacing (4px)
        float approxLineHeight = scaleDimension(24f);
        int maxVisible = Math.max(1, (int)Math.floor((panelHeight - padding) / approxLineHeight));
        
        // Calculate max scroll offset
        int maxScroll;
        synchronized (messagesLock) {
            maxScroll = Math.max(0, messages.size() - maxVisible);
        }
        
        // Update scroll offset (inverted: positive scroll = scroll up = increase offset)
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (float)yOffset));
    }

    private void addMessageInternal(ChatMessage chatMsg) {
        ModAPI api = modAPI;
        synchronized (messagesLock) {
            messages.add(chatMsg);
            if (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
        }

        if (api != null && !chatMsg.isSystemMessage) {
            api.fireChatMessage(chatMsg.senderId, chatMsg.senderName, chatMsg.message, chatMsg.timestamp, chatMsg.isSystemMessage);
        }

        if (game != null && !chatMsg.isSystemMessage) {
            var aiCompanionManager = game.getAICompanionManager();
            if (aiCompanionManager != null) {
                aiCompanionManager.handleChatMessage(chatMsg.message, chatMsg.senderName, reply -> {
                    game.postToMainThread(() -> {
                        var ui = game.getUIManager();
                        if (ui == null || ui.getChatOverlay() == null) {
                            return;
                        }

                        var npcManager = game.getNPCManager();
                        int id = aiCompanionManager.getCompanionNpcId();
                        var npc = npcManager != null ? npcManager.getNPC(id) : null;
                        var gameSettings = game.getSettings();
                        var aiSettings = gameSettings != null ? gameSettings.ai : null;
                        String npcFallbackName = aiSettings != null ? aiSettings.companionName : "Companion";
                        String npcName = npc != null ? npc.getName() : npcFallbackName;
                        ui.getChatOverlay().enqueueMessage(id, npcName, reply, System.currentTimeMillis(), false);
                    });
                });
            }
        }
    }

    private List<ChatMessage> getMessagesSnapshot() {
        synchronized (messagesLock) {
            return new ArrayList<>(messages);
        }
    }

    /**
     * Represents a chat message.
     */
    public static class ChatMessage {
        public final int senderId;
        public final String senderName;
        public final String message;
        public final long timestamp;
        public final boolean isSystemMessage;
        
        public ChatMessage(int senderId, String senderName, String message, long timestamp, boolean isSystemMessage) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message;
            this.timestamp = timestamp;
            this.isSystemMessage = isSystemMessage;
        }
    }
}
