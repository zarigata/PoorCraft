package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Chunk request packet sent by client to request chunk data.
 * 
 * Client sends this when:
 * - Entering a new chunk (player moved)
 * - Increasing view distance (settings changed)
 * - Reconnecting to server (reload world)
 * 
 * Server responds with ChunkDataPacket if chunk is loaded.
 * If chunk needs generation, server may queue the request (async generation).
 * 
 * Client should track pending requests to avoid duplicate requests.
 * Otherwise you end up requesting the same chunk 60 times per second.
 * Ask me how I know. (Hint: my server crashed)
 */
public class ChunkRequestPacket implements Packet {
    
    public static final int PACKET_ID = 0x11;
    
    public int chunkX;
    public int chunkZ;
    
    /**
     * Default constructor for deserialization.
     */
    public ChunkRequestPacket() {
    }
    
    /**
     * Constructor for requesting a chunk.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    public ChunkRequestPacket(int chunkX, int chunkZ) {
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
