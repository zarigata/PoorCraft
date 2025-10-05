package com.poorcraft.discord;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple Discord IPC implementation for Rich Presence.
 * 
 * No external dependencies needed! Just pure Java and Discord's IPC protocol.
 * This talks directly to Discord via named pipes (Windows) or Unix sockets.
 * 
 * Because sometimes you gotta do things yourself when Maven won't cooperate.
 */
public class SimpleDiscordIPC {
    
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    
    private SocketChannel channel;
    private final Gson gson;
    private final long clientId;
    private boolean connected;
    
    public SimpleDiscordIPC(long clientId) {
        this.clientId = clientId;
        this.gson = new Gson();
        this.connected = false;
    }
    
    /**
     * Connects to Discord via IPC.
     * Tries pipes 0-9 until one works.
     * 
     * @throws IOException if connection fails
     */
    public void connect() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        
        // Try to connect to Discord IPC pipes
        for (int i = 0; i < 10; i++) {
            try {
                if (os.contains("win")) {
                    // Windows: Try to use temp path with discord-ipc-{i}
                    String pipePath = System.getenv("TEMP") + "\\discord-ipc-" + i;
                    Path pipeFile = Paths.get(pipePath);
                    if (!Files.exists(pipeFile)) continue;
                    
                    // Try to connect using Unix domain socket (Java 16+)
                    channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    channel.connect(UnixDomainSocketAddress.of(pipeFile));
                } else {
                    // Unix/Linux/Mac: Use Unix domain socket
                    String socketPath = getUnixSocketPath(i);
                    if (socketPath == null) continue;
                    
                    Path socketFile = Paths.get(socketPath);
                    channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    channel.connect(UnixDomainSocketAddress.of(socketFile));
                }
                
                if (channel != null && channel.isConnected()) {
                    // Send handshake
                    sendHandshake();
                    
                    // Read handshake response
                    readPacket();
                    
                    connected = true;
                    System.out.println("[Discord IPC] Connected via pipe " + i);
                    return;
                }
            } catch (Exception e) {
                // Try next pipe
                if (channel != null) {
                    try { channel.close(); } catch (Exception ignored) {}
                }
            }
        }
        
        throw new IOException("Could not connect to Discord. Is Discord running?");
    }
    
    /**
     * Gets the Unix socket path for Discord IPC.
     */
    private String getUnixSocketPath(int pipe) {
        String[] paths = {
            System.getenv("XDG_RUNTIME_DIR"),
            System.getenv("TMPDIR"),
            System.getenv("TMP"),
            System.getenv("TEMP"),
            "/tmp"
        };
        
        for (String path : paths) {
            if (path == null) continue;
            Path socketPath = Paths.get(path, "discord-ipc-" + pipe);
            if (Files.exists(socketPath)) {
                return socketPath.toString();
            }
        }
        
        return null;
    }
    
    /**
     * Sends handshake packet.
     */
    private void sendHandshake() throws IOException {
        JsonObject handshake = new JsonObject();
        handshake.addProperty("v", 1);
        handshake.addProperty("client_id", String.valueOf(clientId));
        
        sendPacket(OP_HANDSHAKE, handshake);
    }
    
    /**
     * Sends a packet to Discord.
     */
    private void sendPacket(int opcode, JsonObject data) throws IOException {
        String json = gson.toJson(data);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        
        ByteBuffer buffer = ByteBuffer.allocate(8 + jsonBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(opcode);
        buffer.putInt(jsonBytes.length);
        buffer.put(jsonBytes);
        
        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }
    
    /**
     * Reads a packet from Discord.
     */
    private JsonObject readPacket() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(8);
        header.order(ByteOrder.LITTLE_ENDIAN);
        
        while (header.hasRemaining()) {
            if (channel.read(header) < 0) {
                throw new IOException("Channel closed");
            }
        }
        
        header.flip();
        int opcode = header.getInt();
        int length = header.getInt();
        
        ByteBuffer data = ByteBuffer.allocate(length);
        while (data.hasRemaining()) {
            if (channel.read(data) < 0) {
                throw new IOException("Channel closed");
            }
        }
        
        data.flip();
        byte[] jsonBytes = new byte[data.remaining()];
        data.get(jsonBytes);
        
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        return gson.fromJson(json, JsonObject.class);
    }
    
    /**
     * Updates Rich Presence.
     */
    public void updatePresence(RichPresenceData presence) throws IOException {
        if (!connected) {
            throw new IOException("Not connected to Discord");
        }
        
        JsonObject frame = new JsonObject();
        frame.addProperty("cmd", "SET_ACTIVITY");
        frame.addProperty("nonce", String.valueOf(System.currentTimeMillis()));
        
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", presence.toJson());
        frame.add("args", args);
        
        sendPacket(OP_FRAME, frame);
    }
    
    /**
     * Closes the connection.
     */
    public void close() {
        if (channel != null) {
            try {
                JsonObject close = new JsonObject();
                sendPacket(OP_CLOSE, close);
            } catch (Exception ignored) {}
            
            try { channel.close(); } catch (Exception ignored) {}
        }
        connected = false;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Rich Presence data structure.
     */
    public static class RichPresenceData {
        public String state;
        public String details;
        public long startTimestamp;
        public String largeImageKey;
        public String largeImageText;
        public String smallImageKey;
        public String smallImageText;
        
        public JsonObject toJson() {
            JsonObject activity = new JsonObject();
            
            if (state != null) activity.addProperty("state", state);
            if (details != null) activity.addProperty("details", details);
            
            if (startTimestamp > 0) {
                JsonObject timestamps = new JsonObject();
                timestamps.addProperty("start", startTimestamp);
                activity.add("timestamps", timestamps);
            }
            
            if (largeImageKey != null || smallImageKey != null) {
                JsonObject assets = new JsonObject();
                if (largeImageKey != null) {
                    assets.addProperty("large_image", largeImageKey);
                    if (largeImageText != null) assets.addProperty("large_text", largeImageText);
                }
                if (smallImageKey != null) {
                    assets.addProperty("small_image", smallImageKey);
                    if (smallImageText != null) assets.addProperty("small_text", smallImageText);
                }
                activity.add("assets", assets);
            }
            
            return activity;
        }
    }
}
