package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * Login response packet sent by server after login request.
 * 
 * Contains either:
 * - Success: player ID, spawn position, world seed
 * - Failure: error message
 * 
 * Server assigns unique player IDs to each connected client.
 * World seed is sent to client for client-side biome queries (optimization).
 * 
 * Failure reasons: username taken, server full, version mismatch, banned, etc.
 * Though we don't have bans yet. Or server capacity limits. Or... most features really.
 */
public class LoginResponsePacket implements Packet {
    
    public static final int PACKET_ID = 0x02;
    
    public boolean success;
    public int playerId;
    public float spawnX;
    public float spawnY;
    public float spawnZ;
    public long worldSeed;
    public String message;  // Error message if failure
    
    /**
     * Default constructor for deserialization.
     */
    public LoginResponsePacket() {
    }
    
    /**
     * Constructor for successful login.
     * 
     * @param playerId Unique player ID assigned by server
     * @param x Spawn X coordinate
     * @param y Spawn Y coordinate
     * @param z Spawn Z coordinate
     * @param seed World seed
     */
    public LoginResponsePacket(int playerId, float x, float y, float z, long seed) {
        this.success = true;
        this.playerId = playerId;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.worldSeed = seed;
        this.message = "";
    }
    
    /**
     * Factory method for failed login.
     * 
     * @param message Error message
     * @return LoginResponsePacket with failure flag
     */
    public static LoginResponsePacket failure(String message) {
        LoginResponsePacket packet = new LoginResponsePacket();
        packet.success = false;
        packet.message = message;
        return packet;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeBoolean(success);
        
        if (success) {
            buf.writeInt(playerId);
            buf.writeFloat(spawnX);
            buf.writeFloat(spawnY);
            buf.writeFloat(spawnZ);
            buf.writeLong(worldSeed);
        } else {
            // Write error message
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            buf.writeShort(bytes.length);
            buf.writeBytes(bytes);
        }
    }
    
    @Override
    public void read(ByteBuf buf) {
        success = buf.readBoolean();
        
        if (success) {
            playerId = buf.readInt();
            spawnX = buf.readFloat();
            spawnY = buf.readFloat();
            spawnZ = buf.readFloat();
            worldSeed = buf.readLong();
            message = "";
        } else {
            // Read error message
            int length = buf.readShort();
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            message = new String(bytes, StandardCharsets.UTF_8);
        }
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
