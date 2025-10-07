package com.poorcraft.test;

import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.test.util.TestUtils;
import com.poorcraft.ui.UIRenderer;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@Tag("rendering")
class UIRendererTest {

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
    @DisplayName("UIRenderer initialises projection and shader")
    void testUIRendererInitialisation() throws Exception {
        UIRenderer renderer = new UIRenderer();
        int width = context.getSettings().window.width;
        int height = context.getSettings().window.height;

        renderer.init(width, height);

        assertNotNull(renderer.getShader(), "UI shader should be available after initialisation");

        Matrix4f projection = (Matrix4f) getPrivateField(renderer, "projectionMatrix");
        float[] expected = new Matrix4f().ortho(0f, width, height, 0f, -1f, 1f).get(new float[16]);
        float[] actual = projection.get(new float[16]);
        assertArrayEquals(expected, actual, 1e-5f, "Projection matrix should match orthographic configuration");

        renderer.begin();
        renderer.end();
        TestUtils.assertNoGLErrors();

        renderer.cleanup();
        TestUtils.assertNoGLErrors();
    }

    private Object getPrivateField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
