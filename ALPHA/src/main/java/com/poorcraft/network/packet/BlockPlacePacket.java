package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Block place packet sent by client when placing a block.
 * 
 * Client sends this when player places a block (right-click).
 * Server validates placement (range check, collision, inventory).
 * Server responds with BlockUpdatePacket if valid.
 * 
 * Validation checks:
 * - Range: Is block within reach distance? (typically 5 blocks)
 * - Collision: Is position already occupied by solid block?
 * - Inventory: Does player have this block type? (future feature)
 * - Permissions: Is player allowed to build here? (future feature)
 * 
 * Client may show preview locally before server confirms.
 * But actual block placement only happens after server sends BlockUpdatePacket.
 * 
 * It's like asking permission before taking a cookie from the jar.
 * Except the jar is the world and the cookie is a dirt block.
 */
public class BlockPlacePacket implements Packet {
    
    public static final int PACKET_ID = 0x31;
    
    public int x;
    public int y;
    public int z;
    public byte blockType;
    
    /**
     * Default constructor for deserialization.
     */
    public BlockPlacePacket() {
    }
    
    /**
     * Constructor for block placement.
     * 
     * @param x World X coordinate
     * @param y World Y coordinate
     * @param z World Z coordinate
     * @param blockType Block type to place (0-255)
     */
    public BlockPlacePacket(int x, int y, int z, byte blockType) {
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
