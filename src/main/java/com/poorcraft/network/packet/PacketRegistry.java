package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Central registry for all packet types.
 * 
 * Maps packet IDs to packet classes and provides factory methods for encoding/decoding.
 * All packet types must be registered here with unique IDs.
 * 
 * Packet ID allocation:
 * - 0x00-0x0F: Connection packets (handshake, login, disconnect, keep-alive)
 * - 0x10-0x1F: Chunk packets (chunk data, requests, unload)
 * - 0x20-0x2F: Player packets (movement, spawn, despawn)
 * - 0x30-0x3F: Block packets (updates, place, break)
 * - 0x40-0x4F: Chat packets (chat messages)
 * 
 * This registry is static and initialized at class load time.
 * Thread-safe because it's immutable after initialization.
 * 
 * I don't know if this is the best way to do it but it's how I've seen it done
 * in like every Minecraft server implementation so... yeah. If it ain't broke!
 */
public class PacketRegistry {
    
    private static final Map<Integer, Supplier<Packet>> PACKET_FACTORIES = new HashMap<>();
    private static final Map<Class<? extends Packet>, Integer> PACKET_IDS = new HashMap<>();
    
    // Static initialization block - registers all packet types
    static {
        // Connection packets (0x00-0x0F)
        registerPacket(0x00, HandshakePacket.class, HandshakePacket::new);
        registerPacket(0x01, LoginRequestPacket.class, LoginRequestPacket::new);
        registerPacket(0x02, LoginResponsePacket.class, LoginResponsePacket::new);
        registerPacket(0x03, DisconnectPacket.class, DisconnectPacket::new);
        registerPacket(0x04, KeepAlivePacket.class, KeepAlivePacket::new);
        
        // Chunk packets (0x10-0x1F)
        registerPacket(0x10, ChunkDataPacket.class, ChunkDataPacket::new);
        registerPacket(0x11, ChunkRequestPacket.class, ChunkRequestPacket::new);
        registerPacket(0x12, ChunkUnloadPacket.class, ChunkUnloadPacket::new);
        
        // Player packets (0x20-0x2F)
        registerPacket(0x20, PlayerMovePacket.class, PlayerMovePacket::new);
        registerPacket(0x21, PlayerSpawnPacket.class, PlayerSpawnPacket::new);
        registerPacket(0x22, PlayerDespawnPacket.class, PlayerDespawnPacket::new);
        
        // Block packets (0x30-0x3F)
        registerPacket(0x30, BlockUpdatePacket.class, BlockUpdatePacket::new);
        registerPacket(0x31, BlockPlacePacket.class, BlockPlacePacket::new);
        registerPacket(0x32, BlockBreakPacket.class, BlockBreakPacket::new);
        
        // Chat packets (0x40-0x4F)
        registerPacket(0x40, ChatMessagePacket.class, ChatMessagePacket::new);
    }
    
    /**
     * Registers a packet type with its ID and factory.
     * 
     * @param id Unique packet ID
     * @param packetClass Packet class
     * @param factory Factory function to create new instances
     * @throws IllegalArgumentException if ID is already registered
     */
    public static void registerPacket(int id, Class<? extends Packet> packetClass, Supplier<Packet> factory) {
        if (PACKET_FACTORIES.containsKey(id)) {
            throw new IllegalArgumentException("Packet ID " + id + " is already registered!");
        }
        PACKET_FACTORIES.put(id, factory);
        PACKET_IDS.put(packetClass, id);
    }
    
    /**
     * Creates a new packet instance from its ID.
     * 
     * @param id Packet ID
     * @return New packet instance, or null if ID is unknown
     */
    public static Packet createPacket(int id) {
        Supplier<Packet> factory = PACKET_FACTORIES.get(id);
        return factory != null ? factory.get() : null;
    }
    
    /**
     * Gets the packet ID for a packet instance.
     * 
     * @param packet Packet instance
     * @return Packet ID, or -1 if packet is not registered
     */
    public static int getPacketId(Packet packet) {
        Integer id = PACKET_IDS.get(packet.getClass());
        return id != null ? id : -1;
    }
    
    /**
     * Encodes a packet to ByteBuf.
     * Writes packet ID followed by packet data.
     * 
     * @param packet Packet to encode
     * @param allocator ByteBuf allocator
     * @return Encoded ByteBuf (caller must release)
     */
    public static ByteBuf encode(Packet packet, ByteBufAllocator allocator) {
        int id = getPacketId(packet);
        if (id == -1) {
            throw new IllegalArgumentException("Packet " + packet.getClass().getName() + " is not registered!");
        }
        
        ByteBuf buf = allocator.buffer();
        buf.writeByte(id);  // Write packet ID as single byte
        packet.write(buf);  // Write packet data
        return buf;
    }
    
    /**
     * Decodes a packet from ByteBuf.
     * Reads packet ID and creates appropriate packet instance.
     * 
     * @param buf ByteBuf containing packet data
     * @return Decoded packet, or null if ID is unknown
     */
    public static Packet decode(ByteBuf buf) {
        int id = buf.readByte() & 0xFF;  // Read packet ID as unsigned byte
        Packet packet = createPacket(id);
        
        if (packet == null) {
            return null;  // Unknown packet ID
        }
        
        packet.read(buf);  // Read packet data
        return packet;
    }
}
