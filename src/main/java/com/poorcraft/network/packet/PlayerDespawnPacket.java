package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Player despawn packet sent by server when a player leaves.
 * 
 * Server sends this when:
 * - Player disconnects (quit, timeout, kick)
 * - Player moves too far away (out of render distance)
 * 
 * Clients remove the player entity from world and free resources.
 * 
 * It's like when someone leaves the Discord call and you hear the "bloop" sound.
 * Except there's no sound. Yet. Maybe in v2 we'll add sound effects.
 * For now it's just silent despawning. Sad.
 */
public class PlayerDespawnPacket implements Packet {
    
    public static final int PACKET_ID = 0x22;
    
    public int playerId;
    
    /**
     * Default constructor for deserialization.
     */
    public PlayerDespawnPacket() {
    }
    
    /**
     * Constructor for despawning a player.
     * 
     * @param playerId Player ID to remove
     */
    public PlayerDespawnPacket(int playerId) {
        this.playerId = playerId;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(playerId);
    }
    
    @Override
    public void read(ByteBuf buf) {
        playerId = buf.readInt();
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
