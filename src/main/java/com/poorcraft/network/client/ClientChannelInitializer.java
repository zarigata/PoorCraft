package com.poorcraft.network.client;

import com.poorcraft.network.codec.PacketDecoder;
import com.poorcraft.network.codec.PacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * Netty channel initializer for client-side connection.
 * 
 * Identical pipeline structure to server (frame codecs + packet codecs + handler).
 * Client uses ClientPacketHandler instead of ServerPacketHandler.
 * 
 * Pipeline order is critical for correct packet processing.
 * Inbound: frameDecoder -> packetDecoder -> packetHandler
 * Outbound: packetHandler -> packetEncoder -> frameEncoder
 * 
 * This is the client-side version of ServerChannelInitializer.
 * Same plumbing, different handler. Copy-paste with minor tweaks!
 */
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {
    
    private final GameClient client;
    
    /**
     * Creates a new client channel initializer.
     * 
     * @param client Game client reference
     */
    public ClientChannelInitializer(GameClient client) {
        this.client = client;
    }
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // Frame decoder: reads length-prefixed packets
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
        
        // Frame encoder: prepends 4-byte length field
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        
        // Packet decoder: ByteBuf -> Packet
        pipeline.addLast("packetDecoder", new PacketDecoder());
        
        // Packet encoder: Packet -> ByteBuf
        pipeline.addLast("packetEncoder", new PacketEncoder());
        
        // Packet handler: processes received packets
        pipeline.addLast("packetHandler", new ClientPacketHandler(client));
        
        System.out.println("[ClientChannelInitializer] Channel initialized");
    }
}
