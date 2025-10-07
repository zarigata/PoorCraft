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
    private static final int MAX_VISIBLE_MESSAGES = 10;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    
    private final Game game;
    private final List<ChatMessage> messages;
    private final Object messagesLock = new Object();
    private final ConcurrentLinkedQueue<ChatMessage> pendingMessages;
    private TextField inputField;
    private boolean visible;
    private float scrollOffset;
    private ModAPI modAPI;
    
    public ChatOverlay(int windowWidth, int windowHeight, Game game) {
        super(windowWidth, windowHeight);
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
        
        // Create text input field at bottom of screen
        inputField = new TextField(50, windowHeight - 80, windowWidth - 100, 40, "Press T to chat...");
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
            inputField.setX(50);
            inputField.setY(height - 80);
            inputField.setWidth(width - 100);
            inputField.setHeight(40);
        }
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }
        
        // Draw semi-transparent background panel
        float panelWidth = Math.min(windowWidth - 100, 600);
        float panelHeight = 350;
        float panelX = 50;
        float panelY = windowHeight - 450;
        
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
        float messageScale = 0.8f;
        float lineHeight = fontRenderer.getTextHeight() * messageScale + 4;
        float textX = panelX + 10;
        float startY = panelY + panelHeight - 20;
        
        List<ChatMessage> snapshot = getMessagesSnapshot();

        // Render messages from bottom to top (newest at bottom)
        int startIndex = Math.max(0, snapshot.size() - MAX_VISIBLE_MESSAGES - (int)scrollOffset);
        int endIndex = Math.min(snapshot.size(), startIndex + MAX_VISIBLE_MESSAGES);
        
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
            float maxWidth = panelWidth - 20;
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
            
            if (currentY < panelY + 10) {
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
        
        // Calculate max scroll offset
        int maxScroll;
        synchronized (messagesLock) {
            maxScroll = Math.max(0, messages.size() - MAX_VISIBLE_MESSAGES);
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

        if (api != null) {
            api.fireChatMessage(chatMsg.senderId, chatMsg.senderName, chatMsg.message, chatMsg.timestamp, chatMsg.isSystemMessage);
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
