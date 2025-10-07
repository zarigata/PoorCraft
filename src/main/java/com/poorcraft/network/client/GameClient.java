package com.poorcraft.network.client;

import com.poorcraft.network.packet.*;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Network client class that connects to servers and syncs game state.
 * 
 * Client maintains a "remote world" that mirrors server state.
 * Client does NOT generate terrain - all data comes from server.
 * 
 * Architecture:
 * - Remote world: client-side copy of server world (receives chunk data)
 * - Remote players: visual representations of other clients
 * - Client-side prediction: apply movement locally, server corrects if needed
 * - Interpolation: smooth remote player movement despite network jitter
 * 
 * This is the thin client approach. Server does the heavy lifting, client just renders.
 * Like a web browser but for voxel games!
 */
public class GameClient {
    
    private final String host;
    private final int port;
    private final String username;
    private EventLoopGroup workerGroup;
    private Channel channel;
    private volatile boolean connected;
    private int localPlayerId;
    private World remoteWorld;
    private final Map<Integer, RemotePlayer> remotePlayers;
    private final List<ChatMessage> chatHistory;
    private Consumer<String> disconnectCallback;
    private Consumer<ChatMessage> chatMessageCallback;
    private long lastKeepAlive;
    
    /**
     * Creates a new game client.
     * 
     * @param host Server hostname/IP
     * @param port Server port
     * @param username Player username
     */
    public GameClient(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.connected = false;
        this.localPlayerId = -1;
        this.remoteWorld = null;
        this.remotePlayers = new ConcurrentHashMap<>();
        this.chatHistory = new ArrayList<>();
        this.disconnectCallback = null;
        this.chatMessageCallback = null;
        this.lastKeepAlive = System.currentTimeMillis();
    }

    /**
     * Connects to the server.
     */
    public void connect() {
        System.out.println("[Client] Connecting to " + host + ":" + port + "...");

        workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientChannelInitializer(this));
            
            // Connect to server
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            
            // Send handshake
            sendPacket(new HandshakePacket(HandshakePacket.CURRENT_PROTOCOL_VERSION, "0.1.0-SNAPSHOT"));
            
            // Send login request
            sendPacket(new LoginRequestPacket(username));
            
            connected = true;
            System.out.println("[Client] Connected to server");
            
        } catch (Exception e) {
            System.err.println("[Client] Failed to connect: " + e.getMessage());
            e.printStackTrace();
            disconnect();
        }
    }

    /**
     * Returns the active Netty channel. Intended for integration tests that
     * need to manipulate the connection directly (e.g., simulate abrupt
     * disconnects). Gameplay code should not rely on this API.
     */
    public Channel getActiveChannelForTesting() {
        return channel;
    }

    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        if (connected) {
            sendPacket(new DisconnectPacket("Client disconnecting"));

            if (channel != null) {
                channel.close();
                channel = null;
            }

            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }

            connected = false;
            System.out.println("[Client] Disconnected from server");
        }
    }
    /**
     * Sends a packet to the server.
     * 
     * @param packet Packet to send
     */
    public void sendPacket(Packet packet) {
        if (connected && channel != null && channel.isActive()) {
            channel.writeAndFlush(packet);
        }
    }
    
    /**
     * Client tick method called every frame.
     */
    public void tick() {
        if (!connected) return;
        
        // Send keep-alive every 15 seconds
        long now = System.currentTimeMillis();
        if (now - lastKeepAlive > 15000) {
            sendPacket(new KeepAlivePacket(now, now));
            lastKeepAlive = now;
        }
        
        // Update remote player interpolation
        for (RemotePlayer player : remotePlayers.values()) {
            player.interpolate(0.2f);  // 20% toward target each frame
        }
    }
    
    /**
     * Called when login succeeds.
     */
    public void onLoginSuccess(int playerId, float spawnX, float spawnY, float spawnZ, long worldSeed) {
        this.localPlayerId = playerId;
        
        // Create remote world (no generation, data comes from server)
        this.remoteWorld = new World(worldSeed, false);
        
        System.out.println("[Client] Login successful! Player ID: " + playerId);
        System.out.println("[Client] Spawn position: " + spawnX + ", " + spawnY + ", " + spawnZ);
        
        // Request chunks around spawn
        int chunkX = (int) Math.floor(spawnX / 16);
        int chunkZ = (int) Math.floor(spawnZ / 16);
        int viewDistance = 8;
        
        for (int x = chunkX - viewDistance; x <= chunkX + viewDistance; x++) {
            for (int z = chunkZ - viewDistance; z <= chunkZ + viewDistance; z++) {
                sendChunkRequest(x, z);
            }
        }
    }
    
    /**
     * Called when login fails.
     */
    public void onLoginFailure(String message) {
        System.err.println("[Client] Login failed: " + message);
        
        if (disconnectCallback != null) {
            disconnectCallback.accept(message);
        }
        
        disconnect();
    }
    
    /**
     * Called when chunk data is received.
     */
    public void onChunkData(int chunkX, int chunkZ, byte[] blockData) {
        if (remoteWorld == null) return;
        
        // Convert packet data to Chunk
        ChunkDataPacket packet = new ChunkDataPacket(chunkX, chunkZ, blockData);
        Chunk chunk = packet.toChunk();
        
        // Add chunk to remote world
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        remoteWorld.getOrCreateChunk(pos);  // This will create the chunk
        
        // Copy block data
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    remoteWorld.setBlock(chunkX * 16 + x, y, chunkZ * 16 + z, type);
                }
            }
        }
        
        System.out.println("[Client] Received chunk " + chunkX + ", " + chunkZ);
    }
    
    /**
     * Called when chunk unload is received.
     */
    public void onChunkUnload(int chunkX, int chunkZ) {
        if (remoteWorld == null) return;
        
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        remoteWorld.unloadChunk(pos);
        
        System.out.println("[Client] Unloaded chunk " + chunkX + ", " + chunkZ);
    }
    
    /**
     * Called when a player spawns.
     */
    public void onPlayerSpawn(int playerId, String username, float x, float y, float z, float yaw, float pitch) {
        if (playerId == localPlayerId) return;  // Don't create remote player for self
        
        RemotePlayer player = new RemotePlayer(playerId, username, x, y, z, yaw, pitch);
        remotePlayers.put(playerId, player);
        
        System.out.println("[Client] Player " + username + " joined");
    }
    
    /**
     * Called when a player despawns.
     */
    public void onPlayerDespawn(int playerId) {
        RemotePlayer player = remotePlayers.remove(playerId);
        
        if (player != null) {
            System.out.println("[Client] Player " + player.getUsername() + " left");
        }
    }
    
    /**
     * Called when a player moves.
     */
    public void onPlayerMove(int playerId, float x, float y, float z, float yaw, float pitch, boolean onGround) {
        RemotePlayer player = remotePlayers.get(playerId);
        
        if (player != null) {
            player.setTargetPosition(x, y, z, yaw, pitch, onGround);
        }
    }
    
    /**
     * Called when a block is updated.
     */
    public void onBlockUpdate(int x, int y, int z, byte blockType) {
        if (remoteWorld == null) return;
        
        remoteWorld.setBlock(x, y, z, BlockType.fromId(blockType & 0xFF));
    }
    
    /**
     * Called when disconnected.
     */
    public void onDisconnect(String reason) {
        System.out.println("[Client] Disconnected: " + reason);
        
        if (disconnectCallback != null) {
            disconnectCallback.accept(reason);
        }
        
        connected = false;
    }
    
    /**
     * Sends player movement to server.
     */
    public void sendMovement(float x, float y, float z, float yaw, float pitch, boolean onGround) {
        sendPacket(new PlayerMovePacket(localPlayerId, x, y, z, yaw, pitch, onGround));
    }
    
    /**
     * Sends chunk request to server.
     */
    public void sendChunkRequest(int chunkX, int chunkZ) {
        sendPacket(new ChunkRequestPacket(chunkX, chunkZ));
    }
    
    /**
     * Sends block place to server.
     */
    public void sendBlockPlace(int x, int y, int z, byte blockType) {
        sendPacket(new BlockPlacePacket(x, y, z, blockType));
    }
    
    /**
     * Sends block break to server.
     */
    public void sendBlockBreak(int x, int y, int z) {
        sendPacket(new BlockBreakPacket(x, y, z));
    }
    
    /**
     * Called when a chat message is received.
     */
    public void onChatMessage(int senderId, String senderName, String message, long timestamp, boolean isSystemMessage) {
        ChatMessage chatMsg = new ChatMessage(senderId, senderName, message, timestamp, isSystemMessage);
        chatHistory.add(chatMsg);
        
        // Keep only last 100 messages
        if (chatHistory.size() > 100) {
            chatHistory.remove(0);
        }
        
        // Fire callback to notify UI/mods
        if (chatMessageCallback != null) {
            chatMessageCallback.accept(chatMsg);
        }
        
        System.out.println("[Chat] " + senderName + ": " + message);
    }
    
    /**
     * Sends a chat message to the server.
     */
    public void sendChatMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        sendPacket(new ChatMessagePacket(localPlayerId, "", message, System.currentTimeMillis(), false));
    }
    
    /**
     * Gets the chat message history.
     */
    public List<ChatMessage> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }
    
    public void setDisconnectCallback(Consumer<String> callback) {
        this.disconnectCallback = callback;
    }
    
    public void setChatMessageCallback(Consumer<ChatMessage> callback) {
        this.chatMessageCallback = callback;
    }
    
    public World getRemoteWorld() {
        return remoteWorld;
    }
    
    public Collection<RemotePlayer> getRemotePlayers() {
        return remotePlayers.values();
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public int getLocalPlayerId() {
        return localPlayerId;
    }
    
    /**
     * Represents a chat message in history.
     */
    public static class ChatMessage {
        public final int senderId;
        public final String senderName;
        public final String message;
        public final long timestamp;
        public final boolean isSystemMessage;
        
        public ChatMessage(int senderId, String senderName, String message, long timestamp, boolean isSystemMessage) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message;
            this.timestamp = timestamp;
            this.isSystemMessage = isSystemMessage;
        }
    }
}
