package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * Login request packet sent by client after handshake.
 * 
 * Contains player username for identification.
 * Server validates username and responds with LoginResponsePacket.
 * 
 * Note: This is a simplified login system with no authentication.
 * Suitable for LAN play or trusted players only.
 * Future versions could add UUID, authentication tokens, encryption, etc.
 * 
 * But for now? Just a username. Keep it simple, stupid (KISS principle).
 */
public class LoginRequestPacket implements Packet {
    
    public static final int PACKET_ID = 0x01;
    
    public String username;
    
    /**
     * Default constructor for deserialization.
     */
    public LoginRequestPacket() {
    }
    
    /**
     * Constructor for sending login request.
     * 
     * @param username Player username (max 16 characters recommended)
     */
    public LoginRequestPacket(String username) {
        this.username = username;
    }
    
    @Override
    public void write(ByteBuf buf) {
        // Write username as length-prefixed UTF-8
        byte[] bytes = username.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }
    
    @Override
    public void read(ByteBuf buf) {
        // Read length-prefixed string
        int length = buf.readShort();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        username = new String(bytes, StandardCharsets.UTF_8);
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
