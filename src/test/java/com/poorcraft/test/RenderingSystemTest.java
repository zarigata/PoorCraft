package com.poorcraft.test;

import com.poorcraft.render.ChunkRenderer;
import com.poorcraft.render.GPUCapabilities;
import com.poorcraft.render.Shader;
import com.poorcraft.render.TextureAtlas;
import com.poorcraft.render.TextureGenerator;
import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.test.util.TestReportGenerator;
import com.poorcraft.test.util.TestUtils;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rendering subsystem smoke tests. These focus on resource compilation and
 * OpenGL state transitions using the headless context harness.
 */
@Tag("rendering")
class RenderingSystemTest {

    private static final TestReportGenerator REPORT = new TestReportGenerator();

    private HeadlessGameContext context;

    @BeforeAll
    static void beforeAll() {
        REPORT.addSystemInfo("Renderer", "Headless OpenGL validation");
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
        context.initializeSubsystem("window");
        GL.createCapabilities();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.cleanup();
        }
    }

    @Test
    @DisplayName("Core shaders compile successfully")
    void testShaderCompilation() {
        String[][] shaderPairs = {
            {"/shaders/block.vert", "/shaders/block.frag"},
            {"/shaders/sky.vert", "/shaders/sky.frag"},
            {"/shaders/ui.vert", "/shaders/ui.frag"},
            {"/shaders/blur.vert", "/shaders/blur.frag"},
            {"/shaders/block_overlay.vert", "/shaders/block_overlay.frag"},
            {"/shaders/item_drop.vert", "/shaders/item_drop.frag"}
        };
        boolean allCompiled = true;
        for (String[] pair : shaderPairs) {
            Shader shader = null;
            try {
                shader = Shader.loadFromResources(pair[0], pair[1]);
                shader.bind();
                shader.unbind();
            } catch (Exception ex) {
                allCompiled = false;
                REPORT.addError("Rendering", "testShaderCompilation", ex);
            } finally {
                if (shader != null) {
                    shader.cleanup();
                }
            }
        }
        REPORT.addTestResult("Rendering", "testShaderCompilation", allCompiled,
            allCompiled ? "All shaders compiled" : "Shader compilation failures logged");
        TestUtils.assertNoGLErrors();
        assertTrue(allCompiled, "One or more shaders failed to compile");
    }

    @Test
    @DisplayName("Texture atlas builds and binds")
    void testTextureAtlasCreation() {
        TextureGenerator.ensureDefaultBlockTextures();
        TextureGenerator.ensureAuxiliaryTextures();
        TextureAtlas atlas = new TextureAtlas();
        Map<String, ByteBuffer> generated = TextureGenerator.ensureDefaultBlockTextures();
        generated.forEach((name, buffer) -> {
            if (buffer != null) {
                atlas.addTexture(name, buffer, TextureAtlas.TEXTURE_SIZE, TextureAtlas.TEXTURE_SIZE);
            }
        });
        atlas.build();
        atlas.bind(0);
        boolean valid = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) != 0;
        REPORT.addTestResult("Rendering", "testTextureAtlasCreation", valid,
            valid ? "Texture atlas bound to GL_TEXTURE_2D" : "Texture atlas binding returned 0");
        TestUtils.assertNoGLErrors();
        assertTrue(valid, "Texture atlas was not bound");
    }

    @Test
    @DisplayName("Chunk renderer initialises and cleans up")
    void testChunkRendererInitialization() {
        context.initializeSubsystem("renderer");
        ChunkRenderer renderer = context.getChunkRenderer();
        assertNotNull(renderer, "ChunkRenderer should be initialised");
        GPUCapabilities caps = context.getGpuCapabilities();
        assertNotNull(caps, "GPU capabilities should be detected during renderer init");
        assertDoesNotThrow(renderer::cleanup, "Renderer cleanup should not throw");
        REPORT.addTestResult("Rendering", "testChunkRendererInitialization", true,
            "ChunkRenderer init/cleanup executed with GPU capabilities=" + caps);
        TestUtils.assertNoGLErrors();
    }

    @Test
    @DisplayName("Chunk renderer configures UBO when supported")
    void testChunkRendererUniformBlockSupport() {
        context.initializeSubsystem("window");
        GPUCapabilities caps = context.getGpuCapabilities();
        Assumptions.assumeTrue(caps != null && caps.supportsUniformBufferObjects(),
            "Environment lacks UBO support");

        ChunkRenderer renderer = new ChunkRenderer();
        renderer.setSettings(context.getSettings());
        renderer.setGPUCapabilities(caps);
        renderer.init();
        boolean usesUBO = isUsingUBO(renderer);
        REPORT.addTestResult("Rendering", "testChunkRendererUniformBlockSupport", usesUBO,
            "useUBO=" + usesUBO);
        assertTrue(usesUBO, "Renderer should enable UBO when supported");
        TestUtils.assertNoGLErrors();
        assertDoesNotThrow(renderer::cleanup);
    }

    @Test
    @DisplayName("Shader uniforms set without GL errors")
    void testShaderUniformsSetWithoutErrors() {
        Shader shader = null;
        try {
            shader = Shader.loadFromResources("/shaders/ui.vert", "/shaders/ui.frag");
            shader.bind();

            Matrix4f identity = new Matrix4f().identity();
            shader.setUniform("uProjection", identity);
            shader.setUniform("uModel", identity);
            shader.setUniform("uColor", 1.0f, 1.0f, 1.0f, 1.0f);
            shader.setUniform("uUseTexture", true);
            shader.setUniform("uTexture", 0);

            TestUtils.assertNoGLErrors();
        } finally {
            if (shader != null) {
                shader.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Chunk renderer falls back when UBO unsupported")
    void testChunkRendererFallbackWithoutUBO() {
        context.initializeSubsystem("window");
        ChunkRenderer renderer = new ChunkRenderer();
        renderer.setSettings(context.getSettings());
        renderer.init();
        boolean usesUBO = isUsingUBO(renderer);
        REPORT.addTestResult("Rendering", "testChunkRendererFallbackWithoutUBO", !usesUBO,
            "useUBO=" + usesUBO);
        assertFalse(usesUBO, "Renderer should fall back without GPU capabilities");
        TestUtils.assertNoGLErrors();
        assertDoesNotThrow(renderer::cleanup);
    }

    private boolean isUsingUBO(ChunkRenderer renderer) {
        try {
            Field useUBOField = ChunkRenderer.class.getDeclaredField("useUBO");
            useUBOField.setAccessible(true);
            return useUBOField.getBoolean(renderer);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to inspect ChunkRenderer.useUBO", e);
        }
    }
}
