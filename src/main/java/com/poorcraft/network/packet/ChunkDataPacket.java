package com.poorcraft.network.packet;

import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import io.netty.buffer.ByteBuf;

/**
 * Chunk data packet for streaming chunk data from server to client.
 * 
 * Contains full chunk block data (16×256×16 = 65,536 blocks).
 * Block data is transmitted as byte array (block type IDs 0-255).
 * 
 * Optimization notes:
 * - Chunk data compresses well (mostly air blocks)
 * - Could use RLE encoding for further optimization
 * - Could send only non-air blocks with positions
 * - For v1, we're keeping it simple: raw block array
 * 
 * This is the biggest packet in the protocol (~64KB uncompressed).
 * Good thing Netty handles TCP framing for us automatically!
 * 
 * I tried to implement compression but it made my brain hurt.
 * Maybe in v2. For now, raw data go brrr.
 */
public class ChunkDataPacket implements Packet {
    
    public static final int PACKET_ID = 0x10;
    public static final int CHUNK_VOLUME = 16 * 256 * 16;  // 65,536 blocks
    
    public int chunkX;
    public int chunkZ;
    public byte[] blockData;
    
    /**
     * Default constructor for deserialization.
     */
    public ChunkDataPacket() {
    }
    
    /**
     * Constructor for sending chunk data.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @param blockData Block type array (65,536 bytes)
     */
    public ChunkDataPacket(int chunkX, int chunkZ, byte[] blockData) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blockData = blockData;
    }
    
    /**
     * Creates a ChunkDataPacket from a Chunk object.
     * Converts BlockType array to byte array.
     * 
     * @param chunk Chunk to convert
     * @return ChunkDataPacket with chunk data
     */
    public static ChunkDataPacket fromChunk(Chunk chunk) {
        ChunkPos pos = chunk.getPosition();
        byte[] blockData = new byte[CHUNK_VOLUME];
        
        // Convert BlockType array to byte array
        for (int i = 0; i < CHUNK_VOLUME; i++) {
            int x = i % 16;
            int y = i / (16 * 16);
            int z = (i / 16) % 16;
            BlockType type = chunk.getBlock(x, y, z);
            blockData[i] = (byte) type.getId();
        }
        
        return new ChunkDataPacket(pos.x, pos.z, blockData);
    }
    
    /**
     * Converts this packet to a Chunk object.
     * Creates new chunk and populates with block data.
     * 
     * @return Chunk object with block data
     */
    public Chunk toChunk() {
        Chunk chunk = new Chunk(new ChunkPos(chunkX, chunkZ));
        
        // Convert byte array to BlockType array
        for (int i = 0; i < CHUNK_VOLUME && i < blockData.length; i++) {
            int x = i % 16;
            int y = i / (16 * 16);
            int z = (i / 16) % 16;
            BlockType type = BlockType.fromId(blockData[i] & 0xFF);  // Unsigned byte
            chunk.setBlock(x, y, z, type);
        }
        
        return chunk;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeInt(blockData.length);  // Write length for variable-size data
        buf.writeBytes(blockData);
    }
    
    @Override
    public void read(ByteBuf buf) {
        chunkX = buf.readInt();
        chunkZ = buf.readInt();
        int length = buf.readInt();
        blockData = new byte[length];
        buf.readBytes(blockData);
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
