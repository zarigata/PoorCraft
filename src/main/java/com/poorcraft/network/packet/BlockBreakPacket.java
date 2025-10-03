package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Block break packet sent by client when breaking a block.
 * 
 * Client sends this when player breaks a block (left-click).
 * Server validates break (range check, permissions).
 * Server responds with BlockUpdatePacket setting block to AIR.
 * 
 * Validation checks:
 * - Range: Is block within reach distance? (typically 5 blocks)
 * - Permissions: Is player allowed to break blocks here? (future feature)
 * - Bedrock: Can't break bedrock (special case)
 * 
 * Future features:
 * - Break progress (multi-tick breaking like Minecraft)
 * - Tool requirements (need pickaxe for stone, etc.)
 * - Drop items when broken
 * 
 * For v1, it's instant breaking. Click, block gone. Simple.
 * Like creative mode but it's the only mode we have!
 */
public class BlockBreakPacket implements Packet {
    
    public static final int PACKET_ID = 0x32;
    
    public int x;
    public int y;
    public int z;
    
    /**
     * Default constructor for deserialization.
     */
    public BlockBreakPacket() {
    }
    
    /**
     * Constructor for block breaking.
     * 
     * @param x World X coordinate
     * @param y World Y coordinate
     * @param z World Z coordinate
     */
    public BlockBreakPacket(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }
    
    @Override
    public void read(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
