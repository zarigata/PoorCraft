package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * Disconnect packet sent by either client or server to gracefully close connection.
 * 
 * This packet is sent before closing the TCP connection to inform the other party
 * of the disconnect reason. This allows displaying user-friendly messages.
 * 
 * Graceful disconnect flow:
 * 1. Send DisconnectPacket with reason
 * 2. Wait for packet to be sent (flush)
 * 3. Close TCP connection
 * 
 * If TCP connection drops without this packet, treat as timeout/crash.
 * 
 * Common reasons:
 * - "Client disconnecting" (player quit)
 * - "Server shutting down" (server stop)
 * - "Kicked by admin" (moderation)
 * - "Connection timeout" (no keep-alive response)
 * - "Incompatible version" (protocol mismatch)
 * 
 * It's like saying goodbye before hanging up the phone. Polite networking!
 */
public class DisconnectPacket implements Packet {
    
    public static final int PACKET_ID = 0x03;
    
    public String reason;
    
    /**
     * Default constructor for deserialization.
     */
    public DisconnectPacket() {
    }
    
    /**
     * Constructor with disconnect reason.
     * 
     * @param reason Human-readable disconnect reason
     */
    public DisconnectPacket(String reason) {
        this.reason = reason;
    }
    
    @Override
    public void write(ByteBuf buf) {
        // Write reason as length-prefixed UTF-8
        byte[] bytes = reason.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }
    
    @Override
    public void read(ByteBuf buf) {
        // Read length-prefixed string
        int length = buf.readShort();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        reason = new String(bytes, StandardCharsets.UTF_8);
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
