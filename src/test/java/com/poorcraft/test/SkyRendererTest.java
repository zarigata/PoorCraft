package com.poorcraft.test;

import com.poorcraft.camera.Camera;
import com.poorcraft.render.SkyRenderer;
import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.test.util.TestUtils;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("rendering")
class SkyRendererTest {

    private HeadlessGameContext context;

    @BeforeEach
    void setUp() {
        context = new HeadlessGameContext();
        context.initialize();
        context.initializeSubsystem("window");
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.cleanup();
        }
    }

    @Test
    @DisplayName("SkyRenderer initialises and computes sun position")
    void testSkyRendererInitialisation() {
        SkyRenderer renderer = new SkyRenderer();
        renderer.init();

        Camera camera = new Camera(new Vector3f(0f, 0f, 0f), 5f, 0.2f);
        Vector3f sunDirection = new Vector3f(0.3f, 0.8f, 0.4f).normalize();

        assertDoesNotThrow(() -> renderer.render(camera, 70f, 16f / 9f, sunDirection));
        renderer.update(0.016f);
        renderer.render(camera, 70f, 16f / 9f, sunDirection);

        TestUtils.assertNoGLErrors();
        renderer.cleanup();
    }
}
