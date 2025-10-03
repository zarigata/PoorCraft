package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Player movement packet for syncing player position and rotation.
 * 
 * Client sends this every tick (~60 Hz) with local player movement.
 * Server validates movement (anti-cheat: speed, distance, physics).
 * Server broadcasts to other clients with corrected position if needed.
 * 
 * Client-side prediction:
 * - Client applies movement locally immediately (responsive controls)
 * - Server validates and sends correction if movement was invalid
 * - Client reconciles position if server correction differs significantly
 * 
 * Remote player interpolation:
 * - Clients receive position updates at ~20 Hz (server tick rate)
 * - Clients interpolate between positions for smooth 60 FPS rendering
 * - Interpolation alpha of 0.2 means 20% toward target each frame
 * 
 * This is basically how every multiplayer FPS works. Client prediction + server authority.
 * Quake did it in 1996 and we're still using the same technique. If it ain't broke!
 */
public class PlayerMovePacket implements Packet {
    
    public static final int PACKET_ID = 0x20;
    
    public int playerId;
    public float x;
    public float y;
    public float z;
    public float yaw;
    public float pitch;
    public boolean onGround;
    
    /**
     * Default constructor for deserialization.
     */
    public PlayerMovePacket() {
    }
    
    /**
     * Constructor for sending player movement.
     * 
     * @param playerId Player ID (0 for local player when sent by client)
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param yaw Yaw rotation in degrees
     * @param pitch Pitch rotation in degrees
     * @param onGround Whether player is on ground (for physics)
     */
    public PlayerMovePacket(int playerId, float x, float y, float z, float yaw, float pitch, boolean onGround) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeBoolean(onGround);
    }
    
    @Override
    public void read(ByteBuf buf) {
        playerId = buf.readInt();
        x = buf.readFloat();
        y = buf.readFloat();
        z = buf.readFloat();
        yaw = buf.readFloat();
        pitch = buf.readFloat();
        onGround = buf.readBoolean();
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
