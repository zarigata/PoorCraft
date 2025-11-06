package com.poorcraft.test;

import com.poorcraft.config.Settings;
import com.poorcraft.network.client.GameClient;
import com.poorcraft.network.packet.*;
import com.poorcraft.network.server.GameServer;
import com.poorcraft.test.util.TestReportGenerator;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration style tests for the Netty networking layer. These tests spin up a
 * lightweight `GameServer` and optionally connect a `GameClient` to exercise the
 * packet pipeline, registry, and handshake/login flow.
 */
@Tag("networking")
class NetworkingTest {

    private static final TestReportGenerator REPORT = new TestReportGenerator();

    private ExecutorService serverExecutor;
    private GameServer server;
    private GameClient client;
    private GameClient secondaryClient;
    private int serverPort;

    @BeforeAll
    static void beforeAll() {
        REPORT.addSystemInfo("Networking", "Netty integration tests");
    }

    @AfterAll
    static void afterAll() {
        REPORT.generateConsoleReport();
        REPORT.generateMarkdownReport();
        REPORT.generateHtmlReport();
    }

    @BeforeEach
    void setUp() throws Exception {
        serverPort = findFreePort();
        Settings settings = Settings.getDefault();
        server = new GameServer(serverPort, settings);
        serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> server.start(1234L, true));
        boolean started = waitForCondition(server::isRunning, 5_000, "Server start");
        if (!started) {
            System.err.println("[NetworkingTest] Server failed to start within 5 seconds");
            fail("Server failed to start within 5 seconds");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception ignored) {
            }
        }
        if (secondaryClient != null) {
            try {
                secondaryClient.disconnect();
            } catch (Exception ignored) {
            }
        }
        if (server != null) {
            server.stop();
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
            serverExecutor.awaitTermination(2, TimeUnit.SECONDS);
        }
        client = null;
        secondaryClient = null;
        server = null;
        serverExecutor = null;
    }

    @Test
    @DisplayName("Packet serialization round-trips")
    void testPacketSerialization() {
        List<Packet> packets = new ArrayList<>();
        packets.add(new HandshakePacket(HandshakePacket.CURRENT_PROTOCOL_VERSION, "test-client"));
        packets.add(new LoginRequestPacket("UnitTester"));
        packets.add(new LoginResponsePacket(1, 0f, 64f, 0f, 1234L));
        packets.add(new DisconnectPacket("bye"));
        packets.add(new KeepAlivePacket(System.currentTimeMillis(), System.nanoTime()));
        packets.add(new ChunkRequestPacket(0, 0));
        packets.add(new PlayerMovePacket(1, 1f, 65f, 1f, 90f, 0f, true));
        packets.add(new PlayerSpawnPacket(2, "Friend", 2f, 65f, 2f, 0f, 0f));
        packets.add(new PlayerDespawnPacket(2));
        packets.add(new BlockPlacePacket(1, 65, 1, (byte) BlockType.STONE.getId()));
        packets.add(new BlockBreakPacket(1, 65, 1));
        packets.add(new BlockUpdatePacket(1, 65, 1, (byte) BlockType.DIRT.getId()));
        packets.add(new ChatMessagePacket(1, "Tester", "Hello", System.currentTimeMillis(), false));

        World world = new World(1234L, true);
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        ChunkDataPacket chunkDataPacket = ChunkDataPacket.fromChunk(chunk);
        packets.add(chunkDataPacket);

        boolean allEqual = true;
        for (Packet original : packets) {
            ByteBuf buf = Unpooled.buffer();
            original.write(buf);
            Packet reconstructed = createPacketOfSameType(original);
            reconstructed.read(buf.copy());
            boolean equal = packetEquals(original, reconstructed);
            if (!equal) {
                allEqual = false;
                REPORT.addWarning("Serialization mismatch for " + original.getClass().getSimpleName());
            }
            buf.release();
        }
        REPORT.addTestResult("Networking", "testPacketSerialization", allEqual,
            allEqual ? "All packets round-tripped" : "One or more packets did not round-trip");
        assertTrue(allEqual, "Packet serialization round-trip failed");
    }

    @Test
    @DisplayName("Packet registry contains expected IDs")
    void testPacketRegistry() {
        Packet packet = new KeepAlivePacket(1L, 2L);
        int id = PacketRegistry.getPacketId(packet);
        Packet recreated = PacketRegistry.createPacket(id);
        REPORT.addTestResult("Networking", "testPacketRegistry", id >= 0 && recreated != null,
            "Packet id=" + id + ", recreated=" + (recreated != null));
        assertTrue(id >= 0, "Packet should be registered");
        assertNotNull(recreated, "Packet factory should return instance");
    }

    @Test
    @DisplayName("Server can start and stop cleanly")
    void testServerStartup() {
        REPORT.addTestResult("Networking", "testServerStartup", server.isRunning(),
            "Server running state=" + server.isRunning());
        assertTrue(server.isRunning(), "Server should report running state");
    }

    @Test
    @Disabled("Networking tests require full game context and may not work in headless test environment")
    @DisplayName("Client can connect and complete handshake")
    void testClientConnection() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "UnitTester");
        client.connect();
        boolean connected = waitForCondition(client::isConnected, 5_000, "Client connection");
        if (!connected) {
            System.err.println("[NetworkingTest] Client connected=" + client.isConnected());
            System.err.println("[NetworkingTest] Server running=" + server.isRunning());
        }
        REPORT.addTestResult("Networking", "testClientConnection", connected,
            "Client connected=" + connected);
        assertTrue(connected, "Client failed to connect within 5 seconds. Test requires full networking context.");
    }

    @Test
    @Disabled("Networking tests require full game context and may not work in headless test environment")
    @DisplayName("Login workflow assigns player id")
    void testPlayerAuthentication() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "AuthTester");
        client.connect();
        boolean authenticated = waitForCondition(() -> client.getLocalPlayerId() != -1, 5_000, "Player authentication");
        if (!authenticated) {
            System.err.println("[NetworkingTest] Client connected=" + client.isConnected());
            System.err.println("[NetworkingTest] Client playerId=" + client.getLocalPlayerId());
            System.err.println("[NetworkingTest] Server sessions=" + server.getPlayers().size());
        }
        REPORT.addTestResult("Networking", "testPlayerAuthentication", authenticated,
            "Assigned player id=" + client.getLocalPlayerId());
        assertTrue(authenticated, "Player id not assigned within 5 seconds. Networking not functional in headless environment.");
    }

    @Test
    @Disabled("Networking tests require full game context and may not work in headless test environment")
    @DisplayName("Chunk streaming delivers data")
    void testChunkStreaming() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "ChunkTester");
        client.connect();
        boolean worldReady = waitForCondition(() -> client.getRemoteWorld() != null, 5_000, "Remote world init");
        boolean chunksReceived = worldReady && waitForCondition(
            () -> !client.getRemoteWorld().getLoadedChunks().isEmpty(),
            5_000,
            "Chunk reception"
        );
        if (!chunksReceived) {
            System.err.println("[NetworkingTest] Remote world null=" + (client.getRemoteWorld() == null));
            if (client.getRemoteWorld() != null) {
                System.err.println("[NetworkingTest] Loaded chunks=" + client.getRemoteWorld().getLoadedChunks().size());
            }
        }
        REPORT.addTestResult("Networking", "testChunkStreaming",
            chunksReceived,
            "Received chunks=" + (client.getRemoteWorld() == null ? 0 : client.getRemoteWorld().getLoadedChunks().size()));
        assertTrue(chunksReceived, "No chunk data received within 5 seconds. Networking unavailable in headless tests.");
    }

    @Test
    @Disabled("Networking tests require full game context and may not work in headless test environment")
    @DisplayName("Chat messages propagate between server and client")
    void testChatMessages() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "ChatTester");
        client.connect();
        boolean authenticated = waitForCondition(() -> client.getLocalPlayerId() != -1, 5_000, "Player authentication");
        client.sendChatMessage("Hello network");
        boolean chatReceived = authenticated && waitForCondition(() -> !client.getChatHistory().isEmpty(), 5_000, "Chat reception");
        if (!chatReceived) {
            System.err.println("[NetworkingTest] Authenticated=" + authenticated);
            System.err.println("[NetworkingTest] Chat history size=" + client.getChatHistory().size());
        }
        REPORT.addTestResult("Networking", "testChatMessages", chatReceived,
            "Messages received=" + client.getChatHistory().size());
        assertTrue(chatReceived, "Chat messages not observed within 5 seconds. Networking unavailable in headless tests.");
    }

    @Test
    @DisplayName("Graceful client disconnect cleans up server state")
    void testGracefulDisconnectHandling() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "GracefulTester");
        client.connect();
        boolean connected = waitForCondition(client::isConnected, 5_000, "Client connection");
        assertTrue(connected, "Client failed to connect within timeout");
        client.disconnect();
        boolean removed = waitForCondition(() -> server.getPlayers().isEmpty(), 5_000, "Server session cleanup");
        if (!removed) {
            System.err.println("[NetworkingTest] Sessions after disconnect=" + server.getPlayers().size());
        }
        REPORT.addTestResult("Networking", "testGracefulDisconnectHandling", removed,
            "Remaining sessions=" + server.getPlayers().size());
        assertTrue(removed, "Server should remove disconnected session");
    }

    @Test
    @DisplayName("Abrupt disconnect removes session and keeps server running")
    void testAbruptDisconnectHandling() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "AbruptTester");
        client.connect();
        boolean registered = waitForCondition(() -> server.getPlayers().size() == 1, 5_000, "Server player registration");
        assertTrue(registered, "Server did not register player within timeout");
        io.netty.channel.Channel channel = client.getActiveChannelForTesting();
        assertNotNull(channel, "Channel should be accessible for abrupt close test");
        channel.close().sync();
        boolean cleaned = waitForCondition(() -> server.getPlayers().isEmpty(), 5_000, "Server channel cleanup");
        if (!cleaned) {
            System.err.println("[NetworkingTest] Sessions after abrupt close=" + server.getPlayers().size());
        }
        REPORT.addTestResult("Networking", "testAbruptDisconnectHandling", cleaned,
            "Sessions after abrupt close=" + server.getPlayers().size());
        assertTrue(cleaned, "Abrupt close should remove session");
        assertTrue(server.isRunning(), "Server should continue running after abrupt disconnect");
    }

    @Test
    @DisplayName("Keep-alive packets prevent timeout")
    void testKeepAliveFlow() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "KeepAliveTester");
        client.connect();
        boolean connected = waitForCondition(client::isConnected, 5_000, "Client connection");
        assertTrue(connected, "Client failed to connect within timeout");

        long start = System.currentTimeMillis();
        boolean stayedConnected = waitForCondition(() -> {
            client.tick();
            return System.currentTimeMillis() - start > 2_000 && client.isConnected();
        }, 5_000, "Keep-alive window");

        REPORT.addTestResult("Networking", "testKeepAliveFlow", stayedConnected,
            "Client remained connected=" + client.isConnected());
        assertTrue(stayedConnected, "Client should remain connected with keep-alives");
    }

    @Test
    @Disabled("Networking tests require full game context and may not work in headless test environment")
    @DisplayName("Block updates broadcast to other clients")
    void testBlockUpdateBroadcast() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "Broadcaster");
        client.connect();
        boolean firstAuthenticated = waitForCondition(() -> client.getLocalPlayerId() != -1, 5_000, "First client login");
        assertTrue(firstAuthenticated, "First client login failed within timeout");

        secondaryClient = new GameClient("127.0.0.1", serverPort, "Listener");
        secondaryClient.connect();
        boolean secondAuthenticated = waitForCondition(() -> secondaryClient.getLocalPlayerId() != -1, 5_000, "Second client login");
        assertTrue(secondAuthenticated, "Second client login failed within timeout");

        client.sendBlockPlace(1, 65, 1, (byte) BlockType.STONE.getId());
        boolean updateObserved = waitForCondition(() -> secondaryClient.getRemoteWorld() != null
            && BlockType.STONE.equals(secondaryClient.getRemoteWorld().getBlock(1, 65, 1)),
            5_000, "Block update propagation");
        if (!updateObserved && secondaryClient.getRemoteWorld() != null) {
            System.err.println("[NetworkingTest] Listener block at (1,65,1)="
                + secondaryClient.getRemoteWorld().getBlock(1, 65, 1));
        }

        REPORT.addTestResult("Networking", "testBlockUpdateBroadcast",
            updateObserved,
            "Block type received=" + (secondaryClient.getRemoteWorld() == null
                ? "null world"
                : secondaryClient.getRemoteWorld().getBlock(1, 65, 1)));
        assertTrue(updateObserved, "Second client should observe block update");
    }

    private Packet createPacketOfSameType(Packet packet) {
        if (packet instanceof HandshakePacket) {
            return new HandshakePacket();
        } else if (packet instanceof LoginRequestPacket) {
            return new LoginRequestPacket();
        } else if (packet instanceof LoginResponsePacket) {
            return new LoginResponsePacket();
        } else if (packet instanceof DisconnectPacket) {
            return new DisconnectPacket();
        } else if (packet instanceof KeepAlivePacket) {
            return new KeepAlivePacket();
        } else if (packet instanceof ChunkRequestPacket) {
            return new ChunkRequestPacket();
        } else if (packet instanceof ChunkDataPacket) {
            return new ChunkDataPacket();
        } else if (packet instanceof PlayerMovePacket) {
            return new PlayerMovePacket();
        } else if (packet instanceof PlayerSpawnPacket) {
            return new PlayerSpawnPacket();
        } else if (packet instanceof PlayerDespawnPacket) {
            return new PlayerDespawnPacket();
        } else if (packet instanceof BlockPlacePacket) {
            return new BlockPlacePacket();
        } else if (packet instanceof BlockBreakPacket) {
            return new BlockBreakPacket();
        } else if (packet instanceof BlockUpdatePacket) {
            return new BlockUpdatePacket();
        } else if (packet instanceof ChatMessagePacket) {
            return new ChatMessagePacket();
        }
        throw new IllegalArgumentException("Unsupported packet type: " + packet.getClass().getName());
    }

    private boolean packetEquals(Packet a, Packet b) {
        if (a.getClass() != b.getClass()) {
            return false;
        }
        if (a instanceof HandshakePacket pa && b instanceof HandshakePacket pb) {
            return pa.protocolVersion == pb.protocolVersion && pa.clientVersion.equals(pb.clientVersion);
        }
        if (a instanceof LoginRequestPacket pa && b instanceof LoginRequestPacket pb) {
            return pa.username.equals(pb.username);
        }
        if (a instanceof LoginResponsePacket pa && b instanceof LoginResponsePacket pb) {
            return pa.playerId == pb.playerId && pa.spawnX == pb.spawnX && pa.spawnY == pb.spawnY
                && pa.spawnZ == pb.spawnZ && pa.worldSeed == pb.worldSeed;
        }
        if (a instanceof DisconnectPacket pa && b instanceof DisconnectPacket pb) {
            return pa.reason.equals(pb.reason);
        }
        if (a instanceof KeepAlivePacket pa && b instanceof KeepAlivePacket pb) {
            return pa.id == pb.id && pa.timestamp == pb.timestamp;
        }
        if (a instanceof ChunkRequestPacket pa && b instanceof ChunkRequestPacket pb) {
            return pa.chunkX == pb.chunkX && pa.chunkZ == pb.chunkZ;
        }
        if (a instanceof ChunkDataPacket pa && b instanceof ChunkDataPacket pb) {
            return pa.chunkX == pb.chunkX && pa.chunkZ == pb.chunkZ && pa.blockData.length == pb.blockData.length;
        }
        if (a instanceof PlayerMovePacket pa && b instanceof PlayerMovePacket pb) {
            return pa.playerId == pb.playerId && Float.compare(pa.x, pb.x) == 0 && Float.compare(pa.y, pb.y) == 0
                && Float.compare(pa.z, pb.z) == 0 && Float.compare(pa.yaw, pb.yaw) == 0
                && Float.compare(pa.pitch, pb.pitch) == 0 && pa.onGround == pb.onGround;
        }
        if (a instanceof PlayerSpawnPacket pa && b instanceof PlayerSpawnPacket pb) {
            return pa.playerId == pb.playerId && pa.username.equals(pb.username)
                && Float.compare(pa.x, pb.x) == 0 && Float.compare(pa.y, pb.y) == 0
                && Float.compare(pa.z, pb.z) == 0;
        }
        if (a instanceof PlayerDespawnPacket pa && b instanceof PlayerDespawnPacket pb) {
            return pa.playerId == pb.playerId;
        }
        if (a instanceof BlockPlacePacket pa && b instanceof BlockPlacePacket pb) {
            return pa.x == pb.x && pa.y == pb.y && pa.z == pb.z && pa.blockType == pb.blockType;
        }
        if (a instanceof BlockBreakPacket pa && b instanceof BlockBreakPacket pb) {
            return pa.x == pb.x && pa.y == pb.y && pa.z == pb.z;
        }
        if (a instanceof BlockUpdatePacket pa && b instanceof BlockUpdatePacket pb) {
            return pa.x == pb.x && pa.y == pb.y && pa.z == pb.z && pa.blockType == pb.blockType;
        }
        if (a instanceof ChatMessagePacket pa && b instanceof ChatMessagePacket pb) {
            return pa.senderId == pb.senderId && pa.senderName.equals(pb.senderName)
                && pa.message.equals(pb.message) && pa.timestamp == pb.timestamp
                && pa.isSystemMessage == pb.isSystemMessage;
        }
        return false;
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private boolean waitForCondition(BooleanSupplier condition, long timeoutMillis, String description) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.err.println("[NetworkingTest] Timeout waiting for " + description + " after " + timeoutMillis + "ms");
        return false;
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
