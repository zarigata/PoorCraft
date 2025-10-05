package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Chunk unload packet sent by server to tell client to unload a chunk.
 * 
 * Server sends this when player moves far from a chunk (outside view distance).
 * Client removes chunk from memory and frees GPU resources (VAO/VBO cleanup).
 * 
 * This is important for memory management on both client and server.
 * Without chunk unloading, memory usage grows unbounded as player explores.
 * 
 * Client should not request this chunk again unless player moves back into range.
 * Otherwise it's just wasting bandwidth unloading and reloading the same chunks.
 * 
 * It's like cleaning your room. You don't need it until your mom (the server) tells you to.
 */
public class ChunkUnloadPacket implements Packet {
    
    public static final int PACKET_ID = 0x12;
    
    public int chunkX;
    public int chunkZ;
    
    /**
     * Default constructor for deserialization.
     */
    public ChunkUnloadPacket() {
    }
    
    /**
     * Constructor for unloading a chunk.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    public ChunkUnloadPacket(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
    }
    
    @Override
    public void read(ByteBuf buf) {
        chunkX = buf.readInt();
        chunkZ = buf.readInt();
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
