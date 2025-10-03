package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * Player spawn packet sent by server when a player joins.
 * 
 * Server sends this to all clients when a new player joins.
 * Also sent to newly joined client for all existing players.
 * 
 * Flow when player joins:
 * 1. Player A connects and logs in
 * 2. Server sends PlayerSpawnPacket(A) to all existing players
 * 3. Server sends PlayerSpawnPacket(B), PlayerSpawnPacket(C), etc. to player A
 * 4. All clients now know about all players
 * 
 * Clients create a remote player entity and add to world for rendering.
 * Username is displayed above player entity (nametag).
 * 
 * It's like introducing yourself at a party. "Hi, I'm Steve, nice to meet you!"
 * Except you're a floating cube with a name above your head.
 */
public class PlayerSpawnPacket implements Packet {
    
    public static final int PACKET_ID = 0x21;
    
    public int playerId;
    public String username;
    public float x;
    public float y;
    public float z;
    public float yaw;
    public float pitch;
    
    /**
     * Default constructor for deserialization.
     */
    public PlayerSpawnPacket() {
    }
    
    /**
     * Constructor for spawning a player.
     * 
     * @param playerId Unique player ID
     * @param username Player username
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param yaw Yaw rotation in degrees
     * @param pitch Pitch rotation in degrees
     */
    public PlayerSpawnPacket(int playerId, String username, float x, float y, float z, float yaw, float pitch) {
        this.playerId = playerId;
        this.username = username;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(playerId);
        
        // Write username as length-prefixed UTF-8
        byte[] bytes = username.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
        
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }
    
    @Override
    public void read(ByteBuf buf) {
        playerId = buf.readInt();
        
        // Read username
        int length = buf.readShort();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        username = new String(bytes, StandardCharsets.UTF_8);
        
        x = buf.readFloat();
        y = buf.readFloat();
        z = buf.readFloat();
        yaw = buf.readFloat();
        pitch = buf.readFloat();
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
