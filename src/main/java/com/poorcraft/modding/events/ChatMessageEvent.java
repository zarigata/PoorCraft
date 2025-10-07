package com.poorcraft.modding.events;

/**
 * Event fired when a chat message is sent or received.
 * 
 * <p>This event is fired when chat messages are processed, allowing mods to:
 * <ul>
 *   <li>Listen to player chat messages</li>
 *   <li>Respond to specific keywords or patterns</li>
 *   <li>Log chat messages</li>
 *   <li>Create chat-based commands</li>
 * </ul>
 * 
 * <p><b>Cancellable:</b> No - this is an informational event.
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class ChatMessageEvent extends Event {
    
    private final int senderId;
    private final String senderName;
    private final String message;
    private final long messageTimestamp;
    private final boolean isSystemMessage;
    
    /**
     * Creates a new chat message event.
     * 
     * @param senderId Player ID of sender (-1 for system messages)
     * @param senderName Display name of sender
     * @param message Chat message content
     * @param messageTimestamp When message was sent
     * @param isSystemMessage Whether this is a system message
     */
    public ChatMessageEvent(int senderId, String senderName, String message, long messageTimestamp, boolean isSystemMessage) {
        super();
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.messageTimestamp = messageTimestamp;
        this.isSystemMessage = isSystemMessage;
    }
    
    @Override
    public String getEventName() {
        return "chat_message";
    }
    
    /**
     * Gets the sender's player ID.
     * @return Player ID, or -1 for system messages
     */
    public int getSenderId() {
        return senderId;
    }
    
    /**
     * Gets the sender's display name.
     * @return Sender name
     */
    public String getSenderName() {
        return senderName;
    }
    
    /**
     * Gets the message content.
     * @return Message text
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the message timestamp.
     * @return Timestamp when message was sent
     */
    public long getMessageTimestamp() {
        return messageTimestamp;
    }
    
    /**
     * Checks if this is a system message.
     * @return True if system message, false if player message
     */
    public boolean isSystemMessage() {
        return isSystemMessage;
    }
}
