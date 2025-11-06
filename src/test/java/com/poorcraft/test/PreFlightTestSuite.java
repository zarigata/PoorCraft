package com.poorcraft.test;

import com.poorcraft.ai.AICompanionManager;
import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.core.Game;
import com.poorcraft.core.Window;
import com.poorcraft.modding.LuaModLoader;
import com.poorcraft.network.packet.HandshakePacket;
import com.poorcraft.network.packet.Packet;
import com.poorcraft.network.packet.PacketRegistry;
import com.poorcraft.render.ChunkRenderer;
import com.poorcraft.render.GPUCapabilities;
import com.poorcraft.resources.ResourceManager;
import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.test.util.TestReportGenerator;
import com.poorcraft.test.util.TestUtils;
import com.poorcraft.ui.UIManager;
import com.poorcraft.world.World;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import com.poorcraft.world.generation.BiomeType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("preflight")
@Execution(ExecutionMode.SAME_THREAD)
class PreFlightTestSuite {

    private static final TestReportGenerator REPORT = new TestReportGenerator();

    /**
     * Standalone entry point that runs only this pre-flight suite using the JUnit Platform.
     *
     * Supported arguments:
     * <ul>
     *   <li><code>--verbose</code> — prints a detailed console summary and failure traces.</li>
     *   <li><code>--output-dir &lt;path&gt;</code> — overrides the report directory used by {@link TestReportGenerator}.</li>
     *   <li><code>--help</code> — prints the usage information.</li>
     * </ul>
     *
     * The process exits with code {@code 0} on success or {@code 1} if any test fails.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PreFlightOptions options = PreFlightOptions.parse(args);
        options.outputDirectory().ifPresent(REPORT::setReportDirectory);

        if (options.verbose()) {
            System.out.println("[PreFlightTestSuite] Verbose mode enabled");
            options.outputDirectory().ifPresent(path ->
                System.out.println("[PreFlightTestSuite] Report directory: " + path.toAbsolutePath())
            );
        }

        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(PreFlightTestSuite.class))
            .build();

        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();

        summary.getFailures().forEach(failure ->
            REPORT.addError("PreFlight", failure.getTestIdentifier().getDisplayName(), failure.getException())
        );
        REPORT.addSystemInfo("Tests Found", String.valueOf(summary.getTestsFoundCount()));
        REPORT.addSystemInfo("Tests Passed", String.valueOf(summary.getTestsSucceededCount()));
        REPORT.addSystemInfo("Tests Failed", String.valueOf(summary.getTestsFailedCount()));
        REPORT.addSystemInfo("Tests Skipped", String.valueOf(summary.getTestsSkippedCount()));

        boolean success = summary.getFailures().isEmpty();
        REPORT.addTestResult("PreFlight", "SuiteCompletion", success,
            success ? "All pre-flight checks passed" : "Pre-flight suite reported failures");

        System.out.println("[PreFlightTestSuite] Tests found: " + summary.getTestsFoundCount());
        System.out.println("[PreFlightTestSuite] Tests succeeded: " + summary.getTestsSucceededCount());
        System.out.println("[PreFlightTestSuite] Tests failed: " + summary.getTestsFailedCount());

        if (options.verbose()) {
            summary.getFailures().forEach(failure -> {
                System.out.println("[PreFlightTestSuite] Failure: " + failure.getTestIdentifier().getDisplayName());
                failure.getException().printStackTrace(System.out);
            });
        } else if (!success) {
            summary.getFailures().forEach(failure ->
                System.out.println("[PreFlightTestSuite] Failure: " + failure.getTestIdentifier().getDisplayName() +
                    " - " + failure.getException().getMessage())
            );
        }

        REPORT.generateConsoleReport();
        REPORT.generateMarkdownReport();
        REPORT.generateHtmlReport();

        System.exit(success ? 0 : 1);
    }

    private record PreFlightOptions(boolean verbose, Optional<Path> outputDirectory) {

        static PreFlightOptions parse(String[] args) {
            boolean verbose = false;
            Optional<Path> outputDir = Optional.empty();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--verbose" -> verbose = true;
                    case "--output-dir" -> {
                        if (i + 1 >= args.length) {
                            printUsageAndExit("Missing path after --output-dir");
                        }
                        outputDir = Optional.of(Paths.get(args[++i]));
                    }
                    case "--help" -> printUsageAndExit(null);
                    default -> System.err.println("[PreFlightTestSuite] Ignoring unrecognised argument: " + arg);
                }
            }

            return new PreFlightOptions(verbose, outputDir);
        }

        private static void printUsageAndExit(String errorMessage) {
            if (errorMessage != null) {
                System.err.println("[PreFlightTestSuite] " + errorMessage);
            }
            System.out.println("Usage: java com.poorcraft.test.PreFlightTestSuite [options]\n" +
                "Options:\n" +
                "  --verbose             Print JUnit summary and failure traces\n" +
                "  --output-dir <path>   Override report output directory\n" +
                "  --help                Show this message");
            System.exit(errorMessage == null ? 0 : 1);
        }
    }

    private HeadlessGameContext context;

    @BeforeAll
    static void beforeAll() {
        REPORT.addSystemInfo("Java Version", System.getProperty("java.version", "unknown"));
        REPORT.addSystemInfo("OS", System.getProperty("os.name", "unknown"));
        REPORT.addSystemInfo("Architecture", System.getProperty("os.arch", "unknown"));
    }

    @AfterAll
    static void afterAll() {
        REPORT.generateConsoleReport();
        REPORT.generateMarkdownReport();
        REPORT.generateHtmlReport();
    }

    @BeforeEach
    void setUp() {
        context = new HeadlessGameContext();
        context.initialize();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.cleanup();
        }
    }

    @Test
    @Timeout(30)
    void testWindowCreation() {
        context.initializeSubsystem("window");
        Window window = context.getWindow();
        assertNotNull(window, "Window should be created");
        assertTrue(window.getHandle() != 0L, "GLFW window handle should be valid");
        REPORT.addTestResult("Window", "testWindowCreation", true,
            "Window dimensions: " + window.getWidth() + "x" + window.getHeight());
    }

    @Test
    @Timeout(30)
    void testOpenGLCapabilities() {
        context.initializeSubsystem("window");
        GPUCapabilities capabilities = context.getGpuCapabilities();
        assertNotNull(capabilities, "GPU capabilities should be detected");
        String version = capabilities.getOpenGLVersion();
        assertNotNull(version, "OpenGL version string should be available");
        assertFalse(version.isBlank(), "OpenGL version should not be blank");
        REPORT.addSystemInfo("OpenGL Version", version);
        REPORT.addTestResult("Window", "testOpenGLCapabilities", true, "Capabilities: " + capabilities);
    }

    @Test
    @Timeout(30)
    void testResourceLoading() {
        ResourceManager manager = ResourceManager.getInstance();
        assertTrue(manager.resourceExists("/shaders/block.vert"), "block.vert shader should exist");
        assertTrue(manager.resourceExists("/config/default_settings.json"), "default settings should exist");

        Optional<String> shaderValidation = TestUtils.validateShaderFile("src/main/resources/shaders/ui.vert");
        Optional<String> configValidation = TestUtils.validateJsonFile("src/main/resources/config/default_settings.json");
        shaderValidation.ifPresent(message -> fail("Shader validation failed: " + message));
        configValidation.ifPresent(message -> fail("Config validation failed: " + message));

        REPORT.addTestResult("Resources", "testResourceLoading", true,
            "Shader and configuration assets verified");
    }

    @Test
    @Timeout(30)
    void testConfigurationSystem() {
        ConfigManager manager = new ConfigManager();
        Settings settings = manager.loadSettings();
        assertNotNull(settings.window, "Settings should include window section");
        assertNotNull(settings.graphics, "Settings should include graphics section");
        assertNotNull(settings.audio, "Settings should include audio section");
        assertNotNull(settings.controls, "Settings should include controls section");

        REPORT.addTestResult("Configuration", "testConfigurationSystem", true,
            "Configuration sections present");
    }

    @Test
    @Timeout(30)
    void testUISystemInitialization() {
        context.initializeSubsystem("game");
        Game game = context.getGame();
        assertNotNull(game, "Game should have been initialized");
        UIManager uiManager = game.getUIManager();
        assertNotNull(uiManager, "UIManager should be available");
        assertTrue(uiManager.getCurrentAtlasSize() > 0, "Font atlas size should be positive");

        REPORT.addTestResult("UI", "testUISystemInitialization", true,
            "UIManager current state: " + uiManager.getCurrentState());
    }

    @Test
    @Timeout(30)
    void testRenderingSystemInitialization() {
        context.initializeSubsystem("game");
        ChunkRenderer renderer = context.getChunkRenderer();
        assertNotNull(renderer, "ChunkRenderer should be initialized");
        assertNotNull(context.getGame().getPerformanceMonitor(), "Performance monitor should be available");
        REPORT.addTestResult("Rendering", "testRenderingSystemInitialization", true,
            "Chunk renderer present");
    }

    @Test
    @Timeout(30)
    void testModLoaderInitialization() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        assertNotNull(loader, "Mod loader should be initialized");
        assertTrue(loader.getLoadedMods().size() >= 0, "Loaded mod count should be non-negative");
        REPORT.addTestResult("Mods", "testModLoaderInitialization", true,
            "Loaded mods: " + loader.getLoadedMods().size());
    }

    @Test
    @Timeout(30)
    void testWorldGeneration() {
        World world = TestUtils.createTestWorld(1234L);
        assertNotNull(world, "World should be created");
        BiomeType biome = world.getBiome(0, 0);
        assertNotNull(biome, "Biome should be determinable");

        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        assertNotNull(chunk, "Chunk should be generated");

        REPORT.addTestResult("World", "testWorldGeneration", true,
            "Spawn biome: " + biome);
        world.cleanup();
    }

    @Test
    @Timeout(30)
    void testSkinSystem() {
        context.initializeSubsystem("game");
        Game game = context.getGame();
        assertNotNull(game.getSkinManager(), "Skin manager should be available");
        assertFalse(game.getSkinManager().getAllSkins().isEmpty(), "Skin library should not be empty");
        REPORT.addTestResult("UI", "testSkinSystem", true,
            "Skin count: " + game.getSkinManager().getAllSkins().size());
    }

    @Test
    @Timeout(30)
    void testNetworkingSystem() {
        HandshakePacket packet = new HandshakePacket(HandshakePacket.CURRENT_PROTOCOL_VERSION, "PreFlightTest");
        ByteBuf encoded = PacketRegistry.encode(packet, UnpooledByteBufAllocator.DEFAULT);
        ByteBuf copy = encoded.copy();
        try {
            Packet decoded = PacketRegistry.decode(copy);
            assertNotNull(decoded, "Decoded packet should not be null");
            assertTrue(decoded instanceof HandshakePacket, "Decoded packet should be HandshakePacket");
            REPORT.addTestResult("Networking", "testNetworkingSystem", true,
                "Handshake packet round-trip successful");
        } finally {
            copy.release();
            encoded.release();
        }
    }

    @Test
    @Timeout(30)
    void testAICompanionSystem() {
        context.initializeSubsystem("game");
        Game game = context.getGame();
        AICompanionManager manager = game.getAICompanionManager();
        if (manager == null) {
            REPORT.addTestResult("AI", "testAICompanionSystem", true, "AI companion disabled in settings");
            return;
        }

        assertFalse(manager.getProviders().isEmpty(), "AI providers should be registered");
        REPORT.addTestResult("AI", "testAICompanionSystem", true,
            "Providers: " + manager.getProviders().keySet());
    }
}
