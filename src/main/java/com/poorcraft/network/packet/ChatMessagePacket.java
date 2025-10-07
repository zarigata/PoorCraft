package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Packet for transmitting chat messages between client and server.
 */
public class ChatMessagePacket implements Packet {
    public int senderId;
    public String senderName;
    public String message;
    public long timestamp;
    public boolean isSystemMessage;
    
    /**
     * Default constructor for packet deserialization.
     */
    public ChatMessagePacket() {
    }
    
    /**
     * Constructor for creating outgoing chat messages.
     * 
     * @param senderId Player ID of the sender (-1 for system messages)
     * @param senderName Display name of the sender
     * @param message The chat message content (max 256 characters)
     * @param timestamp Unix timestamp when message was sent
     * @param isSystemMessage Flag indicating if this is a system/server message
     */
    public ChatMessagePacket(int senderId, String senderName, String message, long timestamp, boolean isSystemMessage) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
        this.isSystemMessage = isSystemMessage;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(senderId);
        writeString(buf, senderName, 64);  // Max 64 chars for name
        writeString(buf, message, 256);    // Max 256 chars for message
        buf.writeLong(timestamp);
        buf.writeBoolean(isSystemMessage);
    }
    
    @Override
    public void read(ByteBuf buf) {
        senderId = buf.readInt();
        senderName = readString(buf);
        message = readString(buf);
        timestamp = buf.readLong();
        isSystemMessage = buf.readBoolean();
    }
    
    @Override
    public int getPacketId() {
        return 0x40;
    }
    
    /**
     * Helper method to write a string to the buffer with max length validation.
     * 
     * @param buf Buffer to write to
     * @param str String to write
     * @param maxLength Maximum allowed length in characters
     */
    private void writeString(ByteBuf buf, String str, int maxLength) {
        if (str == null) {
            buf.writeInt(-1);
            return;
        }
        
        // Truncate to max length for safety
        if (str.length() > maxLength) {
            str = str.substring(0, maxLength);
        }
        
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }
    
    /**
     * Helper method to read a string from the buffer with validation.
     */
    private String readString(ByteBuf buf) {
        int length = buf.readInt();
        if (length < 0) {
            return null;
        }
        
        // Validate length to prevent oversized payloads
        if (length > 1024) {
            throw new IllegalArgumentException("String length too large: " + length + " (max 1024)");
        }
        
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
