package com.poorcraft.network.codec;

import com.poorcraft.network.packet.Packet;
import com.poorcraft.network.packet.PacketRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Netty decoder that converts ByteBuf to Packet objects.
 * 
 * This is a ChannelInboundHandler that sits in the Netty pipeline.
 * Automatically called when data arrives on the socket.
 * 
 * Pipeline flow:
 * 1. Data arrives on TCP socket
 * 2. LengthFieldBasedFrameDecoder extracts complete packet
 * 3. PacketDecoder.decode() is called
 * 4. ByteBuf is deserialized to Packet object
 * 5. Packet is passed to PacketHandler
 * 
 * Handles partial packets gracefully:
 * - If not enough data available, waits for more
 * - Uses mark/reset to avoid consuming incomplete packets
 * 
 * This is where the magic happens. Or where everything breaks if you mess up the framing.
 * Good thing Netty's LengthFieldBasedFrameDecoder handles that for us!
 */
public class PacketDecoder extends ByteToMessageDecoder {
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Check if we have at least 1 byte (packet ID)
        if (in.readableBytes() < 1) {
            return;  // Wait for more data
        }
        
        // Mark reader index in case we need to reset
        in.markReaderIndex();
        
        // Read packet ID as unsigned byte
        int id = in.readByte() & 0xFF;
        
        // Create packet from registry
        Packet packet = PacketRegistry.createPacket(id);
        
        if (packet == null) {
            // Unknown packet ID - this is a protocol error
            throw new IllegalArgumentException("Unknown packet ID: 0x" + Integer.toHexString(id));
        }
        
        try {
            // Try to read packet data
            packet.read(in);
            
            // Success! Add packet to output list
            out.add(packet);
            
            // Optional: Log packet receive for debugging
            // System.out.println("[PacketDecoder] Received packet: " + packet.getClass().getSimpleName());
            
        } catch (IndexOutOfBoundsException e) {
            // Not enough data available, reset and wait for more
            in.resetReaderIndex();
        }
    }
}
