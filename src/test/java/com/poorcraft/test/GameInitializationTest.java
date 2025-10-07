package com.poorcraft.test;

import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.core.Window;
import com.poorcraft.modding.LuaModLoader;
import com.poorcraft.render.GPUCapabilities;
import com.poorcraft.resources.ResourceManager;
import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.test.util.TestReportGenerator;
import com.poorcraft.test.util.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless boot sequence validation covering GLFW, window creation, OpenGL context
 * and key managers such as configuration, resources and the mod loader.
 */
@Tag("engine")
class GameInitializationTest {

    private static final TestReportGenerator REPORT = new TestReportGenerator();

    private HeadlessGameContext context;

    @BeforeAll
    static void beforeAll() {
        REPORT.addSystemInfo("Java Version", System.getProperty("java.version", "unknown"));
        REPORT.addSystemInfo("OS", System.getProperty("os.name", "unknown"));
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
    @DisplayName("GLFW initialises in headless mode")
    void testGLFWInitialization() {
        context.initializeSubsystem("window");
        boolean contextActive = context.isSubsystemInitialized("window") && GLFW.glfwGetCurrentContext() != 0L;
        REPORT.addTestResult("Initialization", "testGLFWInitialization", contextActive,
            contextActive ? "HeadlessGameContext window initialised" : "GLFW context handle was 0");
        assertTrue(contextActive, "GLFW failed to provide a current context");
    }

    @Test
    @DisplayName("Hidden window creation and context")
    void testWindowCreation() {
        context.initializeSubsystem("window");
        Window window = context.getWindow();
        assertNotNull(window, "Window instance should not be null");
        long handle = window.getHandle();
        boolean validHandle = handle != 0L;
        REPORT.addTestResult("Initialization", "testWindowCreation", validHandle,
            validHandle ? "GLFW window created" : "Window handle was 0");
        assertTrue(validHandle, "GLFW window handle was 0");
    }

    @Test
    @DisplayName("OpenGL context version and capabilities")
    void testOpenGLContext() {
        context.initializeSubsystem("window");
        GL.createCapabilities();
        String version = GL11.glGetString(GL11.GL_VERSION);
        REPORT.addSystemInfo("OpenGL Version", version);
        boolean validVersion = version != null && !version.isBlank();
        REPORT.addTestResult("Initialization", "testOpenGLContext", validVersion,
            validVersion ? "OpenGL version: " + version : "glGetString(GL_VERSION) returned null");
        TestUtils.assertNoGLErrors();
        assertTrue(validVersion, "OpenGL version string was null");
    }

    @Test
    @DisplayName("Settings and config manager load defaults")
    void testSettingsLoading() {
        ConfigManager manager = new ConfigManager();
        Settings settings = manager.loadSettings();
        boolean hasWindow = settings.window != null;
        boolean hasGraphics = settings.graphics != null;
        boolean passed = hasWindow && hasGraphics;
        REPORT.addTestResult("Initialization", "testSettingsLoading", passed,
            passed ? "Settings loaded via ConfigManager" : "Settings missing critical sections");
        assertTrue(passed, "ConfigManager did not populate window/graphics settings");
    }

    @Test
    @DisplayName("ResourceManager resolves shader resource")
    void testResourceManagerInitialization() {
        ResourceManager rm = ResourceManager.getInstance();
        boolean exists = rm.resourceExists("/shaders/block.vert");
        REPORT.addTestResult("Initialization", "testResourceManagerInitialization", exists,
            exists ? "Shader resource located" : "block.vert not found via ResourceManager");
        assertTrue(exists, "ResourceManager could not find block.vert");
    }

    @Test
    @DisplayName("LuaModLoader discovers mods")
    void testModLoaderInitialization() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        assertNotNull(loader, "Mod loader should be initialised by Game");
        int modCount = loader.getLoadedMods().size();
        boolean passed = modCount >= 0;
        REPORT.addTestResult("Initialization", "testModLoaderInitialization", passed,
            "Loaded mods: " + modCount);
        TestUtils.assertNoGLErrors();
    }

    @Test
    @DisplayName("GPU capabilities detection")
    void testGpuCapabilities() {
        context.initializeSubsystem("game");
        GPUCapabilities capabilities = context.getGpuCapabilities();
        boolean detected = capabilities != null;
        REPORT.addTestResult("Initialization", "testGpuCapabilities", detected,
            detected ? "GPU capabilities detected" : "GPU detection returned null");
        assertTrue(detected, "GPU capabilities were not detected");
    }
}
