package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Base interface for all network packets.
 * 
 * Packets are stateless DTOs (Data Transfer Objects) that carry data between client and server.
 * Each packet type has a unique ID for type identification during deserialization.
 * 
 * Serialization contract:
 * - write() and read() methods must be symmetric (write order = read order)
 * - Packet IDs must be registered in PacketRegistry
 * - Packets should be immutable after construction (no setters)
 * 
 * This is basically how Minecraft does it. Simple, effective, battle-tested.
 * I mean, if it works for them with millions of players, it'll work for us with like... 3 players max.
 */
public interface Packet {
    
    /**
     * Serializes packet data to ByteBuf.
     * Write order must match read order in read() method.
     * 
     * @param buf Netty ByteBuf to write data to
     */
    void write(ByteBuf buf);
    
    /**
     * Deserializes packet data from ByteBuf.
     * Read order must match write order in write() method.
     * 
     * @param buf Netty ByteBuf to read data from
     */
    void read(ByteBuf buf);
    
    /**
     * Returns the unique packet type ID.
     * This ID is used for packet type identification during deserialization.
     * 
     * @return Packet ID (must be registered in PacketRegistry)
     */
    int getPacketId();
}
