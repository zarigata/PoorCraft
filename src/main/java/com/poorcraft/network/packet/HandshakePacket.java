package com.poorcraft.network.packet;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * Handshake packet sent by client to initiate connection.
 * 
 * This is the first packet sent after TCP connection is established.
 * Server validates protocol version and rejects incompatible clients.
 * 
 * Flow:
 * 1. Client connects to server (TCP handshake)
 * 2. Client sends HandshakePacket with protocol version
 * 3. Server checks if protocol version matches
 * 4. If mismatch, server sends DisconnectPacket and closes connection
 * 5. If match, server waits for LoginRequestPacket
 * 
 * Like the secret handshake to get into the cool kids club, except it's just version numbers.
 */
public class HandshakePacket implements Packet {
    
    public static final int PACKET_ID = 0x00;
    public static final int CURRENT_PROTOCOL_VERSION = 1;
    
    public int protocolVersion;
    public String clientVersion;
    
    /**
     * Default constructor for deserialization.
     */
    public HandshakePacket() {
    }
    
    /**
     * Constructor for sending handshake.
     * 
     * @param protocolVersion Protocol version number
     * @param clientVersion Client version string
     */
    public HandshakePacket(int protocolVersion, String clientVersion) {
        this.protocolVersion = protocolVersion;
        this.clientVersion = clientVersion;
    }
    
    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(protocolVersion);
        
        // Write string as length-prefixed UTF-8
        byte[] bytes = clientVersion.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }
    
    @Override
    public void read(ByteBuf buf) {
        protocolVersion = buf.readInt();
        
        // Read length-prefixed string
        int length = buf.readShort();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        clientVersion = new String(bytes, StandardCharsets.UTF_8);
    }
    
    @Override
    public int getPacketId() {
        return PACKET_ID;
    }
}
