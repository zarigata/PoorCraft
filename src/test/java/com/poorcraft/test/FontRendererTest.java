package com.poorcraft.test;

import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.test.util.TestUtils;
import com.poorcraft.ui.FontRenderer;
import com.poorcraft.ui.UIRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("rendering")
class FontRendererTest {

    private HeadlessGameContext context;
    private UIRenderer uiRenderer;

    private static final String FONT_FIELD_ATLAS = "fontAtlasTexture";
    private static final String FONT_FIELD_CHAR_DATA = "charData";
    private static final String FONT_FIELD_USE_FALLBACK = "useFallback";

    @BeforeEach
    void setUp() {
        context = new HeadlessGameContext();
        context.initialize();
        context.initializeSubsystem("window");
        uiRenderer = new UIRenderer();
        uiRenderer.init(context.getSettings().window.width, context.getSettings().window.height);
    }

    @AfterEach
    void tearDown() {
        if (uiRenderer != null) {
            uiRenderer.cleanup();
        }
        if (context != null) {
            context.cleanup();
        }
    }

    @Test
    @DisplayName("FontRenderer loads atlas and metrics for Silkscreen")
    void testFontRendererSilkscreenLoad() {
        FontRenderer fontRenderer = new FontRenderer(uiRenderer, 16);
        try {
            fontRenderer.init("fonts/Silkscreen-Regular.ttf");

            int textureId = (int) getPrivateField(fontRenderer, FONT_FIELD_ATLAS);
            Object charBuffer = getPrivateField(fontRenderer, FONT_FIELD_CHAR_DATA);
            boolean fallback = (boolean) getPrivateField(fontRenderer, FONT_FIELD_USE_FALLBACK);

            assertTrue(textureId > 0, "Glyph atlas texture should be created");
            assertNotNull(charBuffer, "Glyph metrics buffer should be initialised");
            assertFalse(fallback, "Expected primary font path to be used");
            assertTrue(fontRenderer.getTextHeight() > 0, "Font metrics should be populated");

            TestUtils.assertNoGLErrors();
        } finally {
            fontRenderer.cleanup();
            TestUtils.assertNoGLErrors();
        }
    }

    @Test
    @DisplayName("FontRenderer switches to fallback when font missing")
    void testFontRendererFallback() {
        FontRenderer fontRenderer = new FontRenderer(uiRenderer, 16);
        try {
            fontRenderer.init("fonts/does-not-exist.ttf");

            int textureId = (int) getPrivateField(fontRenderer, FONT_FIELD_ATLAS);
            Object charBuffer = getPrivateField(fontRenderer, FONT_FIELD_CHAR_DATA);
            boolean fallback = (boolean) getPrivateField(fontRenderer, FONT_FIELD_USE_FALLBACK);

            assertEquals(0, textureId, "Fallback should not create a texture atlas");
            assertNull(charBuffer, "Fallback should not allocate glyph data");
            assertTrue(fallback, "Missing font should trigger fallback path");
            assertEquals(16f, fontRenderer.getTextHeight(), "Fallback should use font size as height");

            TestUtils.assertNoGLErrors();
        } finally {
            fontRenderer.cleanup();
            TestUtils.assertNoGLErrors();
        }
    }

    private Object getPrivateField(Object target, String fieldName) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            fail("Failed to inspect field '" + fieldName + "': " + e.getMessage());
            return null;
        }
    }
}
