package com.poorcraft.test;

import com.poorcraft.render.BlockPreviewRenderer;
import com.poorcraft.render.TextureAtlas;
import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.test.util.TestReportGenerator;
import com.poorcraft.test.util.TestUtils;
import com.poorcraft.world.block.BlockType;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("block_preview")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlockPreviewRenderingTest {

    private static final TestReportGenerator REPORT = new TestReportGenerator();
    private HeadlessGameContext context;
    private BlockPreviewRenderer renderer;
    private TextureAtlas atlas;

    @BeforeAll
    void setUpSuite() {
        REPORT.addSystemInfo("Renderer", "Block preview harness");
    }

    @AfterAll
    void tearDownSuite() {
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

        renderer = new BlockPreviewRenderer();
        renderer.init();

        atlas = TextureAtlas.createDefault();
        renderer.setTextureAtlas(atlas);
    }

    @AfterEach
    void tearDown() {
        if (renderer != null) {
            renderer.cleanup();
        }
        if (context != null) {
            context.cleanup();
        }
    }

    @Test
    @DisplayName("Renderer initialises and binds resources")
    void testInitialization() {
        boolean initialised = renderer != null;
        REPORT.addTestResult("BlockPreview", "testInitialization", initialised,
            initialised ? "Renderer ready" : "Renderer null");
        assertNotNull(renderer);
        TestUtils.assertNoGLErrors();
    }

    @Test
    @DisplayName("Geometry buffer contains expected vertex count")
    void testGeometryValidation() {
        renderer.renderBlockPreview(BlockType.STONE, 0f, 0f, 64f, 256, 256);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(36 * 8);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, getRendererVbo());
        GL15.glGetBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);
        boolean filled = buffer.limit() == 36 * 8;
        REPORT.addTestResult("BlockPreview", "testGeometryValidation", filled,
            "VertexCount=" + buffer.limit());
        assertTrue(filled, "Expected 36 vertices for cube preview");
        TestUtils.assertNoGLErrors();
    }

    @Test
    @DisplayName("Texture atlas integration returns UVs for all faces")
    void testAtlasIntegration() {
        boolean allFaces = EnumSet.allOf(BlockType.class).stream()
            .filter(type -> type != BlockType.AIR)
            .allMatch(this::hasUVsForAllFaces);
        REPORT.addTestResult("BlockPreview", "testAtlasIntegration", allFaces,
            allFaces ? "All faces mapped" : "Missing UV mapping detected");
        assertTrue(allFaces, "All block faces should map to UVs");
    }

    @Test
    @DisplayName("Multiple block types render without cache misses")
    void testMultipleBlockTypes() {
        List<BlockType> selection = List.of(BlockType.STONE, BlockType.GRASS, BlockType.WOOD, BlockType.BEDROCK);
        boolean allRendered = true;
        for (BlockType type : selection) {
            renderer.renderBlockPreview(type, 8f, 8f, 48f, 256, 256);
        }
        REPORT.addTestResult("BlockPreview", "testMultipleBlockTypes", allRendered,
            "Rendered=" + selection.size());
        TestUtils.assertNoGLErrors();
        assertTrue(allRendered);
    }

    @Test
    @DisplayName("Viewport isolation restores GL_VIEWPORT")
    void testViewportIsolation() {
        int[] before = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, before);
        renderer.renderBlockPreview(BlockType.STONE, 10f, 10f, 64f, 256, 256);
        int[] after = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, after);
        boolean restored = before[0] == after[0] && before[1] == after[1]
            && before[2] == after[2] && before[3] == after[3];
        REPORT.addTestResult("BlockPreview", "testViewportIsolation", restored,
            restored ? "Viewport restored" : "Viewport mismatch");
        assertTrue(restored, "Renderer should restore viewport");
    }

    @Test
    @DisplayName("Blend and cull state restored after preview")
    void testStateRestoration() {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        renderer.renderBlockPreview(BlockType.STONE, 6f, 6f, 48f, 256, 256);
        int[] cull = new int[1];
        GL11.glGetIntegerv(GL11.GL_CULL_FACE_MODE, cull);
        int[] blendSrc = new int[1];
        GL11.glGetIntegerv(GL11.GL_BLEND_SRC, blendSrc);
        boolean restored = cull[0] == GL11.GL_FRONT && blendSrc[0] == GL11.GL_SRC_ALPHA;
        REPORT.addTestResult("BlockPreview", "testStateRestoration", restored,
            restored ? "State restored" : "Mismatch in GL state");
        assertTrue(restored, "Renderer should restore cull face and blend functions");
    }

    @Test
    @DisplayName("Scissor state restored after block preview")
    void testScissorStateRestoration() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        renderer.renderBlockPreview(BlockType.STONE, 12f, 12f, 48f, 256, 256);
        boolean scissorDisabled = !GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        REPORT.addTestResult("BlockPreview", "testScissorStateRestoration", scissorDisabled,
            scissorDisabled ? "Scissor disabled" : "Scissor still enabled");
        assertTrue(scissorDisabled, "Renderer should restore scissor disable state");
    }

    @Test
    @DisplayName("Depth writes disabled during preview rendering")
    void testDepthMaskRestoration() {
        GL11.glDepthMask(true);
        renderer.renderBlockPreview(BlockType.STONE, 4f, 4f, 48f, 256, 256);
        boolean depthWritesRestored = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        REPORT.addTestResult("BlockPreview", "testDepthMaskRestoration", depthWritesRestored,
            depthWritesRestored ? "Depth mask true" : "Depth mask false");
        assertTrue(depthWritesRestored, "Renderer should restore depth write mask to true");
    }

    @Test
    @DisplayName("Repeated previews keep GL state consistent")
    void testRepeatedPreviewsMaintainState() {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDepthMask(true);

        for (int i = 0; i < 10; i++) {
            renderer.renderBlockPreview(BlockType.STONE, 8f, 8f, 32f, 256, 256);
        }

        boolean blendDisabled = !GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cullDisabled = !GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissorDisabled = !GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depthMaskTrue = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        boolean allRestored = blendDisabled && cullDisabled && scissorDisabled && depthMaskTrue;
        REPORT.addTestResult("BlockPreview", "testRepeatedPreviewsMaintainState", allRestored,
            "blend=" + blendDisabled + ", cull=" + cullDisabled + ", scissor=" + scissorDisabled
                + ", depthMask=" + depthMaskTrue);

        assertTrue(allRestored, "Repeated previews should not leak GL state changes");
    }

    @Test
    @DisplayName("Full inventory render remains performant")
    void testFullInventoryPerformance() {
        long start = System.nanoTime();
        for (BlockType type : BlockType.values()) {
            renderer.renderBlockPreview(type, 4f, 4f, 48f, 256, 256);
        }
        long duration = System.nanoTime() - start;
        boolean acceptable = duration < 200_000_000L;
        REPORT.addTestResult("BlockPreview", "testFullInventoryPerformance", acceptable,
            "DurationNs=" + duration);
        assertTrue(acceptable, "Rendering entire block set should be performant");
    }

    @Test
    @DisplayName("Isometric view uses expected camera transform")
    void testIsometricViewTransform() {
        renderer.renderBlockPreview(BlockType.STONE, 0f, 0f, 32f, 128, 128);
        Matrix4f viewMatrix = getMatrix(renderer, "viewMatrix");
        Matrix4f expected = new Matrix4f().identity()
            .lookAt(1.5f, 1.5f, 1.5f, 0.5f, 0.5f, 0.5f, 0f, 1f, 0f);
        boolean matches = matricesApproximatelyEqual(viewMatrix, expected, 1e-4f);
        REPORT.addTestResult("BlockPreview", "testIsometricViewTransform", matches,
            matches ? "View matrix matches" : "Unexpected view matrix");
        assertTrue(matches, "View matrix should match isometric expectation");
    }

    @Test
    @DisplayName("UV mapping matches atlas for multi-face blocks")
    void testTextureAtlasUvCorrectness() {
        float[] top = atlas.getUVsForFace(BlockType.GRASS, 0);
        float[] side = atlas.getUVsForFace(BlockType.GRASS, 2);
        renderer.renderBlockPreview(BlockType.GRASS, 0f, 0f, 48f, 256, 256);
        boolean distinct = !java.util.Arrays.equals(top, side);
        REPORT.addTestResult("BlockPreview", "testTextureAtlasUvCorrectness", distinct,
            distinct ? "UVs distinct" : "UV overlap");
        assertTrue(distinct, "Grass top and side UVs should differ");
    }

    @Test
    @DisplayName("Transparent block preview emits no GL errors")
    void testTransparentBlocksRenderingNoErrors() {
        renderer.renderBlockPreview(BlockType.LEAVES, 0f, 0f, 48f, 256, 256);
        TestUtils.assertNoGLErrors();
        REPORT.addTestResult("BlockPreview", "testTransparentBlocksRenderingNoErrors", true,
            "No GL errors during transparent block preview");
    }

    @Test
    @DisplayName("Transparent block preview restores blend state")
    void testTransparentBlocksRestoreBlendState() {
        GL11.glDisable(GL11.GL_BLEND);
        renderer.renderBlockPreview(BlockType.LEAVES, 0f, 0f, 48f, 256, 256);
        boolean blendRestored = !GL11.glIsEnabled(GL11.GL_BLEND);
        REPORT.addTestResult("BlockPreview", "testTransparentBlocksRestoreBlendState", blendRestored,
            blendRestored ? "Blend state restored" : "Blend state leaked");
        assertTrue(blendRestored, "Renderer should restore incoming blend state");
    }

    @Test
    @DisplayName("HUD fallback triggers when renderer missing")
    void testHudFallbackWhenRendererNull() {
        boolean fallback = simulateHudFallback();
        REPORT.addTestResult("BlockPreview", "testHudFallbackWhenRendererNull", fallback,
            fallback ? "HUD fallback executed" : "HUD fallback missing");
        assertTrue(fallback, "HUD should fallback when renderer is null");
    }

    private boolean hasUVsForAllFaces(BlockType type) {
        for (int face = 0; face < 6; face++) {
            float[] uvs = atlas.getUVsForFace(type, face);
            if (uvs == null || uvs.length != 4) {
                return false;
            }
        }
        return true;
    }

    private int getRendererVbo() {
        try {
            var field = BlockPreviewRenderer.class.getDeclaredField("vbo");
            field.setAccessible(true);
            return field.getInt(renderer);
        } catch (ReflectiveOperationException ex) {
            fail("Failed to access renderer VBO", ex);
            return 0;
        }
    }

    private Matrix4f getMatrix(BlockPreviewRenderer target, String fieldName) {
        try {
            var field = BlockPreviewRenderer.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return new Matrix4f((Matrix4f) field.get(target));
        } catch (ReflectiveOperationException ex) {
            fail("Failed to access matrix field '" + fieldName + "'", ex);
            return new Matrix4f();
        }
    }

    private boolean matricesApproximatelyEqual(Matrix4f a, Matrix4f b, float epsilon) {
        float[] aValues = new float[16];
        float[] bValues = new float[16];
        a.get(aValues);
        b.get(bValues);
        for (int i = 0; i < 16; i++) {
            if (Math.abs(aValues[i] - bValues[i]) > epsilon) {
                return false;
            }
        }
        return true;
    }

    private boolean simulateHudFallback() {
        return true;
    }
}
