package com.poorcraft.network.server;

import com.poorcraft.config.Settings;
import com.poorcraft.network.packet.*;
import com.poorcraft.world.ChunkManager;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main game server class that manages the authoritative world and player sessions.
 * 
 * The server owns the world state and validates all player actions.
 * Clients are thin views that receive updates from the server.
 * 
 * Architecture:
 * - Server runs in its own thread(s) separate from client rendering
 * - Netty handles all network I/O asynchronously
 * - Server tick rate is 20 TPS (50ms per tick) like Minecraft
 * - World is authoritative - clients cannot modify directly
 * 
 * This is basically a mini Minecraft server. Except way simpler.
 * And probably buggier. But hey, it's honest work!
 */
public class GameServer {
    
    private final int port;
    private final Settings settings;
    private World world;
    private ChunkManager chunkManager;
    private final Map<Integer, PlayerSession> players;
    private final AtomicInteger nextPlayerId;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running;
    private ScheduledExecutorService tickExecutor;
    private long lastKeepAlive;
    
    /**
     * Creates a new game server.
     * 
     * @param port Server port (default 25565)
     * @param settings Server settings
     */
    public GameServer(int port, Settings settings) {
        this.port = port;
        this.settings = settings;
        this.players = new ConcurrentHashMap<>();
        this.nextPlayerId = new AtomicInteger(1);
        this.running = false;
        this.lastKeepAlive = System.currentTimeMillis();
    }
    
    /**
     * Starts the server with the specified world settings.
     * 
     * @param worldSeed World seed (0 for random)
     * @param generateStructures Whether to generate structures
     */
    public void start(long worldSeed, boolean generateStructures) {
        System.out.println("[Server] Starting server on port " + port + "...");
        
        // Create world
        world = new World(worldSeed, generateStructures);
        chunkManager = new ChunkManager(world, 16, 20);
        
        // Set up Netty server
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerChannelInitializer(this));
            
            // Bind to port and wait for success
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            
            System.out.println("[Server] Server started on port " + port);
            System.out.println("[Server] World seed: " + world.getSeed());
            
            // Start server tick thread
            tickExecutor = Executors.newSingleThreadScheduledExecutor();
            tickExecutor.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
            
            running = true;
            
        } catch (Exception e) {
            System.err.println("[Server] Failed to start server: " + e.getMessage());
            e.printStackTrace();
            stop();
        }
    }
    
    /**
     * Server tick method called at 20 TPS.
     */
    private void tick() {
        if (!running) return;
        
        try {
            // Send keep-alive packets every 15 seconds
            long now = System.currentTimeMillis();
            if (now - lastKeepAlive > 15000) {
                for (PlayerSession session : players.values()) {
                    session.sendPacket(new KeepAlivePacket(now, now));
                }
                lastKeepAlive = now;
            }
            
            // Check for timed out players
            for (PlayerSession session : players.values()) {
                if (session.isTimedOut()) {
                    session.disconnect("Connection timeout");
                }
            }
            
        } catch (Exception e) {
            System.err.println("[Server] Error in server tick: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Stops the server gracefully.
     */
    public void stop() {
        System.out.println("[Server] Stopping server...");
        running = false;
        
        // Shutdown tick executor
        if (tickExecutor != null) {
            tickExecutor.shutdown();
        }
        
        // Disconnect all players
        for (PlayerSession session : players.values()) {
            session.disconnect("Server shutting down");
        }
        players.clear();
        
        // Close server channel
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        // Shutdown Netty event loop groups
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        System.out.println("[Server] Server stopped");
    }
    
    /**
     * Called when a player connects (after handshake).
     * 
     * @param channel Netty channel
     * @return Player session
     */
    public PlayerSession onPlayerConnect(Channel channel) {
        int playerId = nextPlayerId.getAndIncrement();
        PlayerSession session = new PlayerSession(playerId, channel, this);
        players.put(playerId, session);
        System.out.println("[Server] Player " + playerId + " connected from " + channel.remoteAddress());
        return session;
    }
    
    /**
     * Called when a player sends login request.
     * 
     * @param session Player session
     * @param username Player username
     */
    public void onPlayerLogin(PlayerSession session, String username) {
        // Validate username
        if (username == null || username.trim().isEmpty()) {
            session.sendPacket(LoginResponsePacket.failure("Invalid username"));
            session.disconnect("Invalid username");
            return;
        }
        
        if (username.length() > 16) {
            session.sendPacket(LoginResponsePacket.failure("Username too long (max 16 characters)"));
            session.disconnect("Username too long");
            return;
        }
        
        // Check if username is already taken
        for (PlayerSession other : players.values()) {
            if (other != session && username.equals(other.getUsername())) {
                session.sendPacket(LoginResponsePacket.failure("Username already taken"));
                session.disconnect("Username already taken");
                return;
            }
        }
        
        // Set username and mark as logged in
        session.setUsername(username);
        session.setLoggedIn(true);
        
        // Calculate spawn position (0, 70, 0 for now)
        float spawnX = 0;
        float spawnY = 70;
        float spawnZ = 0;
        
        // Send login success
        session.sendPacket(new LoginResponsePacket(session.getPlayerId(), spawnX, spawnY, spawnZ, world.getSeed()));
        
        // Broadcast player spawn to all other players
        PlayerSpawnPacket spawnPacket = new PlayerSpawnPacket(
                session.getPlayerId(), username, spawnX, spawnY, spawnZ, 0, 0
        );
        for (PlayerSession other : players.values()) {
            if (other != session && other.isLoggedIn()) {
                other.sendPacket(spawnPacket);
            }
        }
        
        // Send existing players to new player
        for (PlayerSession other : players.values()) {
            if (other != session && other.isLoggedIn()) {
                session.sendPacket(new PlayerSpawnPacket(
                        other.getPlayerId(), other.getUsername(),
                        other.getX(), other.getY(), other.getZ(),
                        other.getYaw(), other.getPitch()
                ));
            }
        }
        
        System.out.println("[Server] Player " + username + " logged in");
    }
    
    /**
     * Called when a player disconnects.
     * 
     * @param session Player session
     */
    public void onPlayerDisconnect(PlayerSession session) {
        players.remove(session.getPlayerId());
        
        if (session.isLoggedIn()) {
            // Broadcast player despawn
            PlayerDespawnPacket despawnPacket = new PlayerDespawnPacket(session.getPlayerId());
            for (PlayerSession other : players.values()) {
                if (other.isLoggedIn()) {
                    other.sendPacket(despawnPacket);
                }
            }
            
            System.out.println("[Server] Player " + session.getUsername() + " disconnected");
        } else {
            System.out.println("[Server] Player " + session.getPlayerId() + " disconnected");
        }
    }
    
    /**
     * Called when a player moves.
     * 
     * @param session Player session
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param yaw Yaw rotation
     * @param pitch Pitch rotation
     * @param onGround On ground flag
     */
    public void onPlayerMove(PlayerSession session, float x, float y, float z, float yaw, float pitch, boolean onGround) {
        // Update session position
        session.updatePosition(x, y, z, yaw, pitch, onGround);
        
        // Broadcast to all other players
        PlayerMovePacket movePacket = new PlayerMovePacket(session.getPlayerId(), x, y, z, yaw, pitch, onGround);
        for (PlayerSession other : players.values()) {
            if (other != session && other.isLoggedIn()) {
                other.sendPacket(movePacket);
            }
        }
    }
    
    /**
     * Called when a player requests a chunk.
     * 
     * @param session Player session
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    public void onChunkRequest(PlayerSession session, int chunkX, int chunkZ) {
        // Get or generate chunk
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        Chunk chunk = world.getOrCreateChunk(pos);
        
        // Send chunk data to player
        ChunkDataPacket packet = ChunkDataPacket.fromChunk(chunk);
        session.sendPacket(packet);
        
        // Track that player has this chunk loaded
        session.loadChunk(pos);
    }
    
    /**
     * Called when a player places a block.
     * 
     * @param session Player session
     * @param x World X coordinate
     * @param y World Y coordinate
     * @param z World Z coordinate
     * @param blockType Block type to place
     */
    public void onBlockPlace(PlayerSession session, int x, int y, int z, byte blockType) {
        // TODO: Validate placement (range check, collision, permissions)
        
        // Update world
        world.setBlock(x, y, z, BlockType.fromId(blockType & 0xFF));
        
        // Broadcast to all players
        BlockUpdatePacket updatePacket = new BlockUpdatePacket(x, y, z, blockType);
        for (PlayerSession player : players.values()) {
            if (player.isLoggedIn()) {
                player.sendPacket(updatePacket);
            }
        }
    }
    
    /**
     * Called when a player breaks a block.
     * 
     * @param session Player session
     * @param x World X coordinate
     * @param y World Y coordinate
     * @param z World Z coordinate
     */
    public void onBlockBreak(PlayerSession session, int x, int y, int z) {
        // TODO: Validate break (range check, permissions, bedrock check)
        
        // Update world (set to AIR)
        world.setBlock(x, y, z, BlockType.AIR);
        
        // Broadcast to all players
        BlockUpdatePacket updatePacket = new BlockUpdatePacket(x, y, z, (byte) BlockType.AIR.getId());
        for (PlayerSession player : players.values()) {
            if (player.isLoggedIn()) {
                player.sendPacket(updatePacket);
            }
        }
    }
    
    public World getWorld() {
        return world;
    }
    
    public Collection<PlayerSession> getPlayers() {
        return players.values();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
}
