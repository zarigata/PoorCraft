package com.poorcraft.test;

import com.poorcraft.ui.ScrollContainer;
import com.poorcraft.ui.UIComponent;
import com.poorcraft.ui.UIRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ScrollContainerTest {

    private static class DummyComponent extends UIComponent {
        private final AtomicBoolean clickReceived = new AtomicBoolean(false);
        private final AtomicReference<Float> lastMouseY = new AtomicReference<>();
        private final AtomicReference<Float> lastMouseX = new AtomicReference<>();
        private final AtomicBoolean moveReceived = new AtomicBoolean(false);

        DummyComponent(float x, float y, float width, float height) {
            super(x, y, width, height);
        }

        @Override
        public void render(UIRenderer renderer, com.poorcraft.ui.FontRenderer fontRenderer) {
            // no-op
        }

        @Override
        public void update(float deltaTime) {
            // no-op
        }

        @Override
        public void onMouseClick(float mouseX, float mouseY, int button) {
            clickReceived.set(true);
            lastMouseX.set(mouseX);
            lastMouseY.set(mouseY);
        }

        @Override
        public void onMouseMove(float mouseX, float mouseY) {
            moveReceived.set(true);
            lastMouseX.set(mouseX);
            lastMouseY.set(mouseY);
        }
    }

    @Test
    @DisplayName("ScrollContainer clamps scroll offset within bounds")
    void testScrollClamping() {
        ScrollContainer container = new ScrollContainer(0f, 0f, 200f, 100f);

        for (int i = 0; i < 5; i++) {
            DummyComponent child = new DummyComponent(0f, i * 60f, 180f, 50f);
            container.addChild(child);
        }

        float maxOffset = Math.max(0f, getContentHeight(container) - 100f);

        container.onScroll(-10); // scroll down (positive offset)
        assertEquals(maxOffset, getScrollOffset(container), 0.001f);
        container.onScroll(-10);
        assertEquals(maxOffset, getScrollOffset(container), 0.001f);
        container.onScroll(-10);
        assertEquals(maxOffset, getScrollOffset(container), 0.001f);

        container.onScroll(100); // attempt to scroll beyond start
        assertEquals(0f, getScrollOffset(container), 0.001f);
        container.onScroll(100);
        assertEquals(0f, getScrollOffset(container), 0.001f);

        container.onScroll(-1000); // attempt to scroll beyond end
        assertEquals(maxOffset, getScrollOffset(container), 0.001f);

        // No direct getter; simulate by scrolling up and ensuring we didn't overshoot
        container.onScroll(1000);
        assertEquals(0f, getScrollOffset(container), 0.001f);
        // If clamping failed, scrolling back with huge delta would not return to start
        // The container should now be at top, so further positive scrolls shouldn't change offset
        container.onScroll(10);
        assertEquals(0f, getScrollOffset(container), 0.001f);
        container.onScroll(10);
        assertEquals(0f, getScrollOffset(container), 0.001f);
    }

    @Test
    @DisplayName("ScrollContainer routes clicks using translated coordinates")
    void testClickTranslation() {
        ScrollContainer container = new ScrollContainer(0f, 0f, 200f, 100f);
        DummyComponent child = new DummyComponent(10f, 150f, 50f, 40f);
        container.addChild(child);

        container.onScroll(-10);
        container.onScroll(-10);

        container.onMouseClick(20f, 60f, 0);
        assertTrue(child.clickReceived.get(), "Child should receive translated click");
        assertNotNull(child.lastMouseY.get());
        float expectedTranslatedY = 60f + getScrollOffset(container);
        assertEquals(expectedTranslatedY, child.lastMouseY.get(), 0.001f);
    }

    @Test
    @DisplayName("Scrollbar drag updates scroll offset")
    void testScrollbarDrag() {
        ScrollContainer container = new ScrollContainer(0f, 0f, 200f, 100f);
        for (int i = 0; i < 4; i++) {
            container.addChild(new DummyComponent(0f, i * 80f, 150f, 60f));
        }

        assertEquals(0f, getScrollOffset(container), 0.001f);
        float maxOffset = Math.max(0f, getContentHeight(container) - 100f);

        // Hover over scrollbar and press mouse
        float scrollbarX = 200f - 6f;
        container.onMouseMove(scrollbarX, 10f);
        container.onMouseClick(scrollbarX, 10f, 0);

        // Drag down
        container.onMouseMove(scrollbarX, 80f);
        container.onMouseRelease(scrollbarX, 80f, 0);
        assertEquals(maxOffset, getScrollOffset(container), 0.5f);
    }

    @Test
    @DisplayName("ScrollContainer ignores clicks outside bounds")
    void testClickOutside() {
        ScrollContainer container = new ScrollContainer(20f, 30f, 100f, 80f);
        DummyComponent child = new DummyComponent(30f, 40f, 50f, 50f);
        container.addChild(child);

        container.onMouseClick(5f, 5f, 0);
        assertFalse(child.clickReceived.get(), "Click outside container should be ignored");
    }

    @Test
    @DisplayName("ScrollContainer skips forwarding when cursor misses child horizontally")
    void testClickOutsideChildHorizontalBounds() {
        ScrollContainer container = new ScrollContainer(0f, 0f, 200f, 120f);
        DummyComponent child = new DummyComponent(80f, 10f, 50f, 80f);
        container.addChild(child);

        // Move and click within container but left of child
        container.onMouseMove(40f, 40f);
        container.onMouseClick(40f, 40f, 0);

        assertFalse(child.moveReceived.get(), "Mouse move should not reach child outside horizontal bounds");
        assertFalse(child.clickReceived.get(), "Click should not be forwarded when outside child bounds");
        assertNull(child.lastMouseX.get(), "Child should not record coordinates when not hit");
    }

    private static float getScrollOffset(ScrollContainer container) {
        return invokeFloatGetter(container, "getScrollOffsetForTest");
    }

    private static float getContentHeight(ScrollContainer container) {
        return invokeFloatGetter(container, "getContentHeightForTest");
    }

    private static float invokeFloatGetter(ScrollContainer container, String methodName) {
        try {
            Method method = ScrollContainer.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return (float) method.invoke(container);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke " + methodName, e);
        }
    }
}
