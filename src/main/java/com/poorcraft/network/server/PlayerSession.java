package com.poorcraft.network.server;

import com.poorcraft.network.packet.ChunkDataPacket;
import com.poorcraft.network.packet.ChunkUnloadPacket;
import com.poorcraft.network.packet.DisconnectPacket;
import com.poorcraft.network.packet.Packet;
import com.poorcraft.world.chunk.ChunkPos;
import io.netty.channel.Channel;

import java.util.HashSet;
import java.util.Set;

/**
 * Player session class that represents a connected player on the server.
 * 
 * Each connected client has one PlayerSession on the server.
 * Session tracks player state and manages communication.
 * 
 * Lifecycle:
 * 1. Created on connect (after TCP handshake)
 * 2. Handshake validated
 * 3. Login processed, username set, loggedIn = true
 * 4. Player plays, session tracks position and loaded chunks
 * 5. Disconnect, session destroyed
 * 
 * This is basically a player object but on the server side.
 * The client has a Camera, the server has a PlayerSession. Different perspectives!
 */
public class PlayerSession {
    
    private final int playerId;
    private final Channel channel;
    private final GameServer server;
    private String username;
    private float x, y, z;
    private float yaw, pitch;
    private boolean onGround;
    private final Set<ChunkPos> loadedChunks;
    private long lastKeepAlive;
    private boolean loggedIn;
    
    /**
     * Creates a new player session.
     * 
     * @param playerId Unique player ID
     * @param channel Netty channel for communication
     * @param server Reference to game server
     */
    public PlayerSession(int playerId, Channel channel, GameServer server) {
        this.playerId = playerId;
        this.channel = channel;
        this.server = server;
        this.username = null;
        this.x = 0;
        this.y = 70;
        this.z = 0;
        this.yaw = 0;
        this.pitch = 0;
        this.onGround = false;
        this.loadedChunks = new HashSet<>();
        this.lastKeepAlive = System.currentTimeMillis();
        this.loggedIn = false;
    }
    
    /**
     * Sends a packet to this player.
     * 
     * @param packet Packet to send
     */
    public void sendPacket(Packet packet) {
        if (channel.isActive()) {
            channel.writeAndFlush(packet).addListener(future -> {
                if (!future.isSuccess()) {
                    System.err.println("[PlayerSession] Failed to send packet to player " + playerId);
                    future.cause().printStackTrace();
                }
            });
        }
    }
    
    /**
     * Disconnects this player with a reason.
     * 
     * @param reason Disconnect reason
     */
    public void disconnect(String reason) {
        sendPacket(new DisconnectPacket(reason));
        channel.close();
        server.onPlayerDisconnect(this);
    }
    
    /**
     * Updates player position.
     * 
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param yaw Yaw rotation
     * @param pitch Pitch rotation
     * @param onGround On ground flag
     */
    public void updatePosition(float x, float y, float z, float yaw, float pitch, boolean onGround) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }
    
    /**
     * Marks a chunk as loaded for this player.
     * 
     * @param pos Chunk position
     */
    public void loadChunk(ChunkPos pos) {
        loadedChunks.add(pos);
    }
    
    /**
     * Unloads a chunk for this player.
     * 
     * @param pos Chunk position
     */
    public void unloadChunk(ChunkPos pos) {
        loadedChunks.remove(pos);
        sendPacket(new ChunkUnloadPacket(pos.x, pos.z));
    }
    
    /**
     * Checks if player has a chunk loaded.
     * 
     * @param pos Chunk position
     * @return True if chunk is loaded
     */
    public boolean hasChunkLoaded(ChunkPos pos) {
        return loadedChunks.contains(pos);
    }
    
    /**
     * Updates keep-alive timestamp.
     */
    public void updateKeepAlive() {
        this.lastKeepAlive = System.currentTimeMillis();
    }
    
    /**
     * Checks if player has timed out (no keep-alive for 30 seconds).
     * 
     * @return True if timed out
     */
    public boolean isTimedOut() {
        return System.currentTimeMillis() - lastKeepAlive > 30000;
    }
    
    // Getters and setters
    
    public int getPlayerId() {
        return playerId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getZ() {
        return z;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public boolean isOnGround() {
        return onGround;
    }
    
    public boolean isLoggedIn() {
        return loggedIn;
    }
    
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
    
    public Channel getChannel() {
        return channel;
    }
}
