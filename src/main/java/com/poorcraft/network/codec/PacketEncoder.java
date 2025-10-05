package com.poorcraft.network.codec;

import com.poorcraft.network.packet.Packet;
import com.poorcraft.network.packet.PacketRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty encoder that converts Packet objects to ByteBuf for transmission.
 * 
 * This is a ChannelOutboundHandler that sits in the Netty pipeline.
 * Automatically called when channel.writeAndFlush(packet) is invoked.
 * 
 * Pipeline flow:
 * 1. Application calls channel.writeAndFlush(packet)
 * 2. PacketEncoder.encode() is called
 * 3. Packet is serialized to ByteBuf
 * 4. LengthFieldPrepender adds length prefix
 * 5. ByteBuf is written to TCP socket
 * 
 * Netty handles all the buffer management and TCP framing for us.
 * We just convert Packet -> ByteBuf. Easy!
 * 
 * I love Netty. It makes networking so much easier than raw sockets.
 * Remember when we had to manually handle TCP framing? Yeah, me neither. Blocked that out.
 */
public class PacketEncoder extends MessageToByteEncoder<Packet> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
        // Get packet ID from registry
        int id = PacketRegistry.getPacketId(packet);
        
        if (id == -1) {
            throw new IllegalArgumentException("Packet " + packet.getClass().getName() + " is not registered!");
        }
        
        // Write packet ID as single byte
        out.writeByte(id);
        
        // Write packet data
        packet.write(out);
        
        // Optional: Log packet send for debugging
        // System.out.println("[PacketEncoder] Sent packet: " + packet.getClass().getSimpleName());
    }
}
