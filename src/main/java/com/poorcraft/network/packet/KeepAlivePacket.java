package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * Keep-alive packet for connection health monitoring.
 * 
 * Server sends keep-alive packets every 15 seconds.
 * Client must respond with the same ID within 30 seconds or be disconnected.
 * 
 * This serves two purposes:
 * 1. Detect dead connections (client crashed, network dropped, etc.)
 * 2. Measure round-trip latency (timestamp difference)
 * 
 * Also prevents idle connections from being closed by routers/firewalls.
 * Some NAT routers close TCP connections after 60 seconds of inactivity.
 * 
 * It's like poking your friend to make sure they're still awake during a boring lecture.
 * Except the lecture is your game and the friend is the network connection.
 */
public class KeepAlivePacket implements Packet {
    
    public static final int PACKET_ID = 0x04;
    
    public long id;         // Random ID for matching request/response
    public long timestamp;  // Timestamp when packet was sent (for latency calculation)
    
    /**
     * Default constructor for deserialization.
     */
    public KeepAlivePacket() {
    }
    
    /**
     * Constructor for sending keep-alive.
     * 
     * @param id Random ID for matching
     * @param timestamp Current timestamp in milliseconds
     */
    public KeepAlivePacket(long id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(id);
        buf.writeLong(timestamp);
    }
    
    @Override
    public void read(ByteBuf buf) {
        id = buf.readLong();
        timestamp = buf.readLong();
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
