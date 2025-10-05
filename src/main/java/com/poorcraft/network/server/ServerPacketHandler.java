package com.poorcraft.network.server;

import com.poorcraft.network.packet.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Server-side packet handler that processes incoming packets from clients.
 * 
 * This is the main packet processing entry point on server.
 * Each packet type has a dedicated handler method.
 * Handler validates packets and delegates to server methods.
 * 
 * Exceptions are caught and logged to prevent server crashes.
 * One misbehaving client shouldn't bring down the whole server!
 * 
 * This is where all the "what do I do with this packet?" logic lives.
 * It's like a switchboard operator routing calls. Except the calls are packets.
 */
public class ServerPacketHandler extends SimpleChannelInboundHandler<Packet> {
    
    private final GameServer server;
    private PlayerSession session;
    
    /**
     * Creates a new server packet handler.
     * 
     * @param server Game server reference
     */
    public ServerPacketHandler(GameServer server) {
        this.server = server;
        this.session = null;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Called when client connects
        session = server.onPlayerConnect(ctx.channel());
        System.out.println("[ServerPacketHandler] Client connected: " + ctx.channel().remoteAddress());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Called when client disconnects
        if (session != null) {
            server.onPlayerDisconnect(session);
        }
        System.out.println("[ServerPacketHandler] Client disconnected: " + ctx.channel().remoteAddress());
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        // Dispatch packet to appropriate handler
        if (packet instanceof HandshakePacket) {
            handleHandshake((HandshakePacket) packet);
        } else if (packet instanceof LoginRequestPacket) {
            handleLoginRequest((LoginRequestPacket) packet);
        } else if (packet instanceof KeepAlivePacket) {
            handleKeepAlive((KeepAlivePacket) packet);
        } else if (packet instanceof ChunkRequestPacket) {
            handleChunkRequest((ChunkRequestPacket) packet);
        } else if (packet instanceof PlayerMovePacket) {
            handlePlayerMove((PlayerMovePacket) packet);
        } else if (packet instanceof BlockPlacePacket) {
            handleBlockPlace((BlockPlacePacket) packet);
        } else if (packet instanceof BlockBreakPacket) {
            handleBlockBreak((BlockBreakPacket) packet);
        } else {
            System.out.println("[ServerPacketHandler] Unknown packet type: " + packet.getClass().getSimpleName());
        }
    }
    
    /**
     * Handles handshake packet.
     */
    private void handleHandshake(HandshakePacket packet) {
        System.out.println("[ServerPacketHandler] Handshake from client version " + packet.clientVersion);
        
        // Validate protocol version
        if (packet.protocolVersion != HandshakePacket.CURRENT_PROTOCOL_VERSION) {
            session.sendPacket(new DisconnectPacket("Incompatible protocol version. Server: " + 
                    HandshakePacket.CURRENT_PROTOCOL_VERSION + ", Client: " + packet.protocolVersion));
            session.getChannel().close();
            return;
        }
        
        // Handshake successful, wait for login request
    }
    
    /**
     * Handles login request packet.
     */
    private void handleLoginRequest(LoginRequestPacket packet) {
        System.out.println("[ServerPacketHandler] Login request from " + packet.username);
        server.onPlayerLogin(session, packet.username);
    }
    
    /**
     * Handles keep-alive packet.
     */
    private void handleKeepAlive(KeepAlivePacket packet) {
        session.updateKeepAlive();
    }
    
    /**
     * Handles chunk request packet.
     */
    private void handleChunkRequest(ChunkRequestPacket packet) {
        if (!session.isLoggedIn()) return;
        server.onChunkRequest(session, packet.chunkX, packet.chunkZ);
    }
    
    /**
     * Handles player move packet.
     */
    private void handlePlayerMove(PlayerMovePacket packet) {
        if (!session.isLoggedIn()) return;
        server.onPlayerMove(session, packet.x, packet.y, packet.z, packet.yaw, packet.pitch, packet.onGround);
    }
    
    /**
     * Handles block place packet.
     */
    private void handleBlockPlace(BlockPlacePacket packet) {
        if (!session.isLoggedIn()) return;
        server.onBlockPlace(session, packet.x, packet.y, packet.z, packet.blockType);
    }
    
    /**
     * Handles block break packet.
     */
    private void handleBlockBreak(BlockBreakPacket packet) {
        if (!session.isLoggedIn()) return;
        server.onBlockBreak(session, packet.x, packet.y, packet.z);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("[ServerPacketHandler] Exception caught: " + cause.getMessage());
        cause.printStackTrace();
        
        // Close connection on error
        if (session != null) {
            server.onPlayerDisconnect(session);
        }
        ctx.close();
    }
}
