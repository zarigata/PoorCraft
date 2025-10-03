package com.poorcraft.network.server;

import com.poorcraft.network.codec.PacketDecoder;
import com.poorcraft.network.codec.PacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * Netty channel initializer for server-side connections.
 * 
 * Sets up the Netty pipeline for each incoming connection.
 * Pipeline order matters: decoder before handler, encoder after handler.
 * 
 * Pipeline flow (inbound):
 * TCP socket -> LengthFieldBasedFrameDecoder -> PacketDecoder -> ServerPacketHandler
 * 
 * Pipeline flow (outbound):
 * ServerPacketHandler -> PacketEncoder -> LengthFieldPrepender -> TCP socket
 * 
 * Frame codecs handle packet boundaries (length-prefixed framing).
 * Packet codecs handle serialization/deserialization.
 * Packet handler processes received packets.
 * 
 * This is the plumbing that makes networking work. Not glamorous, but essential!
 * Like the pipes in your house. You don't think about them until they break.
 */
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    
    private final GameServer server;
    
    /**
     * Creates a new server channel initializer.
     * 
     * @param server Game server reference
     */
    public ServerChannelInitializer(GameServer server) {
        this.server = server;
    }
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // Frame decoder: reads length-prefixed packets
        // Max frame size: 1MB (for large chunk packets)
        // Length field: 4 bytes (int) at offset 0
        // Strip length field after reading
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
        
        // Frame encoder: prepends 4-byte length field
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        
        // Packet decoder: ByteBuf -> Packet
        pipeline.addLast("packetDecoder", new PacketDecoder());
        
        // Packet encoder: Packet -> ByteBuf
        pipeline.addLast("packetEncoder", new PacketEncoder());
        
        // Packet handler: processes received packets
        pipeline.addLast("packetHandler", new ServerPacketHandler(server));
        
        System.out.println("[ServerChannelInitializer] Channel initialized for " + ch.remoteAddress());
    }
}
