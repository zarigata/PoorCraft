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

            assertTrue(fontRenderer.getTextHeight() > 0, "Font metrics should be populated");
            assertTrue(fontRenderer.getTextWidth("test") > 0, "Text width should be calculable");
            assertEquals(20, fontRenderer.getFontSize(), "Default font size should be 20px");

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

            assertEquals(16f, fontRenderer.getTextHeight(), "Fallback should use font size as height");
            assertEquals(16, fontRenderer.getFontSize(), "Fallback should retain requested font size");

            TestUtils.assertNoGLErrors();
        } finally {
            fontRenderer.cleanup();
            TestUtils.assertNoGLErrors();
        }
    }
}
