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
        waitForCondition(server::isRunning, 5_000, "Server failed to start");
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
    @DisplayName("Client can connect and complete handshake")
    void testClientConnection() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "UnitTester");
        client.connect();
        waitForCondition(client::isConnected, 5_000, "Client failed to connect");
        REPORT.addTestResult("Networking", "testClientConnection", client.isConnected(),
            "Client connected=" + client.isConnected());
        assertTrue(client.isConnected(), "Client should be connected");
    }

    @Test
    @DisplayName("Login workflow assigns player id")
    void testPlayerAuthentication() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "AuthTester");
        client.connect();
        waitForCondition(() -> client.getLocalPlayerId() != -1, 5_000, "Player id not assigned");
        REPORT.addTestResult("Networking", "testPlayerAuthentication", client.getLocalPlayerId() != -1,
            "Assigned player id=" + client.getLocalPlayerId());
        assertNotEquals(-1, client.getLocalPlayerId(), "Player id should be assigned");
    }

    @Test
    @DisplayName("Chunk streaming delivers data")
    void testChunkStreaming() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "ChunkTester");
        client.connect();
        waitForCondition(() -> client.getRemoteWorld() != null, 5_000, "Remote world not ready");
        waitForCondition(() -> !client.getRemoteWorld().getLoadedChunks().isEmpty(), 5_000, "No chunks received");
        REPORT.addTestResult("Networking", "testChunkStreaming",
            !client.getRemoteWorld().getLoadedChunks().isEmpty(),
            "Received chunks=" + client.getRemoteWorld().getLoadedChunks().size());
        assertFalse(client.getRemoteWorld().getLoadedChunks().isEmpty(), "Chunk data should arrive");
    }

    @Test
    @DisplayName("Chat messages propagate between server and client")
    void testChatMessages() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "ChatTester");
        client.connect();
        waitForCondition(() -> client.getLocalPlayerId() != -1, 5_000, "Player id not assigned");
        client.sendChatMessage("Hello network");
        waitForCondition(() -> !client.getChatHistory().isEmpty(), 5_000, "Chat history empty");
        REPORT.addTestResult("Networking", "testChatMessages", !client.getChatHistory().isEmpty(),
            "Messages received=" + client.getChatHistory().size());
        assertFalse(client.getChatHistory().isEmpty(), "Chat history should contain messages");
    }

    @Test
    @DisplayName("Graceful client disconnect cleans up server state")
    void testGracefulDisconnectHandling() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "GracefulTester");
        client.connect();
        waitForCondition(client::isConnected, 5_000, "Client failed to connect");
        client.disconnect();
        waitForCondition(() -> server.getPlayers().isEmpty(), 5_000, "Server did not remove session");
        REPORT.addTestResult("Networking", "testGracefulDisconnectHandling", server.getPlayers().isEmpty(),
            "Remaining sessions=" + server.getPlayers().size());
        assertTrue(server.getPlayers().isEmpty(), "Server should remove disconnected session");
    }

    @Test
    @DisplayName("Abrupt disconnect removes session and keeps server running")
    void testAbruptDisconnectHandling() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "AbruptTester");
        client.connect();
        waitForCondition(() -> server.getPlayers().size() == 1, 5_000, "Server did not register player");
        io.netty.channel.Channel channel = client.getActiveChannelForTesting();
        assertNotNull(channel, "Channel should be accessible for abrupt close test");
        channel.close().sync();
        waitForCondition(() -> server.getPlayers().isEmpty(), 5_000, "Server did not drop closed channel");
        REPORT.addTestResult("Networking", "testAbruptDisconnectHandling", server.getPlayers().isEmpty(),
            "Sessions after abrupt close=" + server.getPlayers().size());
        assertTrue(server.getPlayers().isEmpty(), "Abrupt close should remove session");
        assertTrue(server.isRunning(), "Server should continue running after abrupt disconnect");
    }

    @Test
    @DisplayName("Keep-alive packets prevent timeout")
    void testKeepAliveFlow() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "KeepAliveTester");
        client.connect();
        waitForCondition(client::isConnected, 5_000, "Client failed to connect");

        long start = System.currentTimeMillis();
        waitForCondition(() -> {
            client.tick();
            return System.currentTimeMillis() - start > 2_000 && client.isConnected();
        }, 5_000, "Client did not stay connected after keep-alive window");

        REPORT.addTestResult("Networking", "testKeepAliveFlow", client.isConnected(), "Client remained connected");
        assertTrue(client.isConnected(), "Client should remain connected with keep-alives");
    }

    @Test
    @DisplayName("Block updates broadcast to other clients")
    void testBlockUpdateBroadcast() throws Exception {
        client = new GameClient("127.0.0.1", serverPort, "Broadcaster");
        client.connect();
        waitForCondition(() -> client.getLocalPlayerId() != -1, 5_000, "First client login failed");

        secondaryClient = new GameClient("127.0.0.1", serverPort, "Listener");
        secondaryClient.connect();
        waitForCondition(() -> secondaryClient.getLocalPlayerId() != -1, 5_000, "Second client login failed");

        client.sendBlockPlace(1, 65, 1, (byte) BlockType.STONE.getId());
        waitForCondition(() -> secondaryClient.getRemoteWorld() != null
            && BlockType.STONE.equals(secondaryClient.getRemoteWorld().getBlock(1, 65, 1)),
            5_000, "Listener did not receive block update");

        REPORT.addTestResult("Networking", "testBlockUpdateBroadcast",
            BlockType.STONE.equals(secondaryClient.getRemoteWorld().getBlock(1, 65, 1)),
            "Block type received=" + secondaryClient.getRemoteWorld().getBlock(1, 65, 1));
        assertEquals(BlockType.STONE, secondaryClient.getRemoteWorld().getBlock(1, 65, 1),
            "Second client should observe block update");
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

    private void waitForCondition(BooleanSupplier condition, long timeoutMillis, String failureMessage) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        fail(failureMessage);
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
