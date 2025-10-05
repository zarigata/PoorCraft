package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Block update packet for syncing block changes.
 * 
 * Server sends this to all clients when a block changes.
 * Used for both player-initiated changes and world events.
 * 
 * Flow:
 * 1. Client sends BlockPlacePacket or BlockBreakPacket
 * 2. Server validates the change
 * 3. Server updates authoritative world state
 * 4. Server broadcasts BlockUpdatePacket to all clients in range
 * 5. Clients update their local world and regenerate chunk mesh
 * 
 * Clients should NOT apply block changes locally before server confirmation.
 * Otherwise you get desync issues where client thinks block is placed but server rejected it.
 * 
 * I learned this the hard way when I tried client-side prediction for blocks.
 * Turns out it's way more complicated than movement prediction. Just let the server handle it!
 */
public class BlockUpdatePacket implements Packet {
    
    public static final int PACKET_ID = 0x30;
    
    public int x;
    public int y;
    public int z;
    public byte blockType;
    
    /**
     * Default constructor for deserialization.
     */
    public BlockUpdatePacket() {
    }
    
    /**
     * Constructor for block update.
     * 
     * @param x World X coordinate
     * @param y World Y coordinate
     * @param z World Z coordinate
     * @param blockType New block type ID (0-255)
     */
    public BlockUpdatePacket(int x, int y, int z, byte blockType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(blockType);
    }
    
    @Override
    public void read(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        blockType = buf.readByte();
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
