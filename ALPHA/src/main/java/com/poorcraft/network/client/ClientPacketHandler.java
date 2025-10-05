package com.poorcraft.network.client;

import com.poorcraft.network.packet.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Client-side packet handler that processes incoming packets from server.
 * 
 * This is the main packet processing entry point on client.
 * Each packet type has a dedicated handler method.
 * Handler delegates to client methods for state updates.
 * 
 * Exceptions are caught and logged to prevent client crashes.
 * Network errors shouldn't crash the whole game!
 * 
 * This is like ServerPacketHandler but for the client side.
 * Different packets, same pattern. Consistency is key!
 */
public class ClientPacketHandler extends SimpleChannelInboundHandler<Packet> {
    
    private final GameClient client;
    
    /**
     * Creates a new client packet handler.
     * 
     * @param client Game client reference
     */
    public ClientPacketHandler(GameClient client) {
        this.client = client;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Called when connection to server succeeds
        System.out.println("[ClientPacketHandler] Connected to server: " + ctx.channel().remoteAddress());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Called when connection to server closes
        client.onDisconnect("Connection lost");
        System.out.println("[ClientPacketHandler] Disconnected from server");
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        // Dispatch packet to appropriate handler
        if (packet instanceof LoginResponsePacket) {
            handleLoginResponse((LoginResponsePacket) packet);
        } else if (packet instanceof DisconnectPacket) {
            handleDisconnect((DisconnectPacket) packet);
        } else if (packet instanceof KeepAlivePacket) {
            handleKeepAlive((KeepAlivePacket) packet);
        } else if (packet instanceof ChunkDataPacket) {
            handleChunkData((ChunkDataPacket) packet);
        } else if (packet instanceof ChunkUnloadPacket) {
            handleChunkUnload((ChunkUnloadPacket) packet);
        } else if (packet instanceof PlayerSpawnPacket) {
            handlePlayerSpawn((PlayerSpawnPacket) packet);
        } else if (packet instanceof PlayerDespawnPacket) {
            handlePlayerDespawn((PlayerDespawnPacket) packet);
        } else if (packet instanceof PlayerMovePacket) {
            handlePlayerMove((PlayerMovePacket) packet);
        } else if (packet instanceof BlockUpdatePacket) {
            handleBlockUpdate((BlockUpdatePacket) packet);
        } else {
            System.out.println("[ClientPacketHandler] Unknown packet type: " + packet.getClass().getSimpleName());
        }
    }
    
    /**
     * Handles login response packet.
     */
    private void handleLoginResponse(LoginResponsePacket packet) {
        if (packet.success) {
            client.onLoginSuccess(packet.playerId, packet.spawnX, packet.spawnY, packet.spawnZ, packet.worldSeed);
        } else {
            client.onLoginFailure(packet.message);
        }
    }
    
    /**
     * Handles disconnect packet.
     */
    private void handleDisconnect(DisconnectPacket packet) {
        client.onDisconnect(packet.reason);
    }
    
    /**
     * Handles keep-alive packet (echo back).
     */
    private void handleKeepAlive(KeepAlivePacket packet) {
        // Echo packet back to server (ping-pong)
        client.sendPacket(packet);
        
        // Calculate latency (optional)
        long latency = System.currentTimeMillis() - packet.timestamp;
        // Could store this for display in HUD
    }
    
    /**
     * Handles chunk data packet.
     */
    private void handleChunkData(ChunkDataPacket packet) {
        client.onChunkData(packet.chunkX, packet.chunkZ, packet.blockData);
    }
    
    /**
     * Handles chunk unload packet.
     */
    private void handleChunkUnload(ChunkUnloadPacket packet) {
        client.onChunkUnload(packet.chunkX, packet.chunkZ);
    }
    
    /**
     * Handles player spawn packet.
     */
    private void handlePlayerSpawn(PlayerSpawnPacket packet) {
        client.onPlayerSpawn(packet.playerId, packet.username, packet.x, packet.y, packet.z, packet.yaw, packet.pitch);
    }
    
    /**
     * Handles player despawn packet.
     */
    private void handlePlayerDespawn(PlayerDespawnPacket packet) {
        client.onPlayerDespawn(packet.playerId);
    }
    
    /**
     * Handles player move packet.
     */
    private void handlePlayerMove(PlayerMovePacket packet) {
        client.onPlayerMove(packet.playerId, packet.x, packet.y, packet.z, packet.yaw, packet.pitch, packet.onGround);
    }
    
    /**
     * Handles block update packet.
     */
    private void handleBlockUpdate(BlockUpdatePacket packet) {
        client.onBlockUpdate(packet.x, packet.y, packet.z, packet.blockType);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("[ClientPacketHandler] Exception caught: " + cause.getMessage());
        cause.printStackTrace();
        
        // Disconnect on error
        client.onDisconnect("Connection error: " + cause.getMessage());
        ctx.close();
    }
}
