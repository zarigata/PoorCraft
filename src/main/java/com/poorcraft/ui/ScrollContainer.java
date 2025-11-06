package com.poorcraft.ui;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL11.GL_SCISSOR_BOX;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glScissor;

/**
 * Scrollable container that clips overflowing child components and exposes
 * vertical scrolling via mouse wheel or dragging the scrollbar.
 */
public class ScrollContainer extends UIComponent {

    private static final float SCROLLBAR_WIDTH = 12f;
    private static final float SCROLL_SPEED = 42f;
    private static final float MIN_THUMB_SIZE = 24f;

    private final List<UIComponent> children = new ArrayList<>();
    private float scrollOffset;
    private float contentHeight;
    private float lastMeasuredExtent;

    private boolean scrollbarHovered;
    private boolean scrollbarDragging;
    private float dragStartY;
    private float dragStartOffset;
    private boolean scissorApplied;
    private boolean scissorWasEnabled;
    private boolean previousScissorCaptured;
    private int prevScissorX;
    private int prevScissorY;
    private int prevScissorW;
    private int prevScissorH;

    public ScrollContainer(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.scrollOffset = 0f;
        this.contentHeight = height;
        this.lastMeasuredExtent = height;
    }

    public void addChild(UIComponent component) {
        children.add(Objects.requireNonNull(component));
        recalcContentHeight();
    }

    public void clearChildren() {
        children.clear();
        scrollOffset = 0f;
        contentHeight = height;
        lastMeasuredExtent = contentHeight;
    }

    public List<UIComponent> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }

        if (applyScissor(renderer)) {
            try {
                renderChildren(renderer, fontRenderer);
            } finally {
                restoreScissorState();
            }
        } else {
            renderChildren(renderer, fontRenderer);
        }

        if (requiresScrollbar()) {
            renderScrollbar(renderer);
        }
    }

    private void renderChildren(UIRenderer renderer, FontRenderer fontRenderer) {
        float viewTop = y + scrollOffset;
        float viewBottom = viewTop + height;

        for (UIComponent child : children) {
            if (!child.isVisible()) {
                continue;
            }

            float childTop = child.getY();
            float childBottom = childTop + child.getHeight();

            if (childBottom < viewTop) {
                continue;
            }
            if (childTop > viewBottom) {
                // Children are typically added in order; break for efficiency.
                break;
            }

            float originalY = child.getY();
            child.setY(originalY - scrollOffset);
            child.render(renderer, fontRenderer);
            child.setY(originalY);
        }
    }

    private void renderScrollbar(UIRenderer renderer) {
        float thumbHeight = getThumbHeight();
        float trackHeight = height;
        float trackX = x + width - SCROLLBAR_WIDTH;
        float trackY = y;

        renderer.drawRect(trackX, trackY, SCROLLBAR_WIDTH, trackHeight,
            0.05f, 0.05f, 0.08f, 0.45f);

        float thumbY = trackY + normalizedScroll() * (trackHeight - thumbHeight);
        float r = scrollbarHovered || scrollbarDragging ? 0.45f : 0.35f;
        float g = scrollbarHovered || scrollbarDragging ? 0.75f : 0.65f;
        float b = scrollbarHovered || scrollbarDragging ? 0.85f : 0.80f;

        renderer.drawRect(trackX + 2f, thumbY, SCROLLBAR_WIDTH - 4f, thumbHeight,
            r, g, b, 0.85f);
    }

    @Override
    public void update(float deltaTime) {
        float preUpdateExtent = lastMeasuredExtent;
        boolean anyVisible = false;
        float maxBottom = 0f;
        for (UIComponent child : children) {
            if (!child.isVisible()) {
                continue;
            }
            anyVisible = true;
            child.update(deltaTime);
            float bottom = (child.getY() + child.getHeight()) - y;
            if (bottom > maxBottom) {
                maxBottom = bottom;
            }
        }

        if (!anyVisible) {
            maxBottom = 0f;
        }

        float newExtent = Math.max(height, maxBottom);
        if (Math.abs(newExtent - preUpdateExtent) > 0.5f) {
            contentHeight = newExtent;
            setScrollOffset(scrollOffset);
            lastMeasuredExtent = contentHeight;
        }
    }

    @Override
    public void onMouseMove(float mouseX, float mouseY) {
        super.onMouseMove(mouseX, mouseY);

        boolean inside = isMouseOver(mouseX, mouseY);
        scrollbarHovered = inside && requiresScrollbar()
            && mouseX >= x + width - SCROLLBAR_WIDTH && mouseX <= x + width
            && mouseY >= y && mouseY <= y + height;

        float translatedY = mouseY + scrollOffset;
        float viewTop = y + scrollOffset;
        float viewBottom = viewTop + height;
        if (!inside && !scrollbarDragging) {
            return;
        }

        for (UIComponent child : children) {
            if (!child.isVisible()) {
                continue;
            }
            float childTop = child.getY();
            float childBottom = childTop + child.getHeight();
            if (childBottom < viewTop || childTop > viewBottom) {
                continue;
            }
            if (child.isMouseOver(mouseX, translatedY)) {
                child.onMouseMove(mouseX, translatedY);
            } else {
                child.hovered = false;
            }
        }

        if (scrollbarDragging) {
            float dragDelta = mouseY - dragStartY;
            float trackRange = height - getThumbHeight();
            if (trackRange > 0f) {
                float deltaNormalized = dragDelta / trackRange;
                float newOffset = dragStartOffset + deltaNormalized * (contentHeight - height);
                setScrollOffset(newOffset);
            }
        }
    }

    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (button == 0 && requiresScrollbar()) {
            boolean insideScrollbar = isMouseOver(mouseX, mouseY)
                && mouseX >= x + width - SCROLLBAR_WIDTH && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
            if (insideScrollbar) {
                scrollbarHovered = true;
                scrollbarDragging = true;
                dragStartY = mouseY;
                dragStartOffset = scrollOffset;
                return;
            }
        }

        if (!isMouseOver(mouseX, mouseY)) {
            return;
        }

        forwardToChildren(mouseX, mouseY, button, UIComponent::onMouseClick);
    }

    @Override
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        if (button == 0) {
            scrollbarDragging = false;
        }

        if (!isMouseOver(mouseX, mouseY)) {
            return;
        }

        forwardToChildren(mouseX, mouseY, button, UIComponent::onMouseRelease);
    }

    private interface ChildMouseAction {
        void apply(UIComponent component, float mouseX, float mouseY, int button);
    }

    private void forwardToChildren(float mouseX, float mouseY, int button, ChildMouseAction action) {
        float translatedY = mouseY + scrollOffset;
        float viewTop = y + scrollOffset;
        float viewBottom = viewTop + height;
        for (UIComponent child : children) {
            if (!child.isVisible()) {
                continue;
            }
            float childTop = child.getY();
            float childBottom = childTop + child.getHeight();
            if (childBottom < viewTop || childTop > viewBottom) {
                continue;
            }
            if (!child.isMouseOver(mouseX, translatedY)) {
                continue;
            }
            action.apply(child, mouseX, translatedY, button);
        }
    }

    public void onScroll(double yOffset) {
        if (!requiresScrollbar()) {
            return;
        }
        float delta = (float) (-yOffset * SCROLL_SPEED);
        setScrollOffset(scrollOffset + delta);
    }

    private void setScrollOffset(float value) {
        float maxOffset = Math.max(0f, contentHeight - height);
        scrollOffset = Math.max(0f, Math.min(maxOffset, value));
    }

    public float getScrollOffset() {
        return scrollOffset;
    }

    private boolean requiresScrollbar() {
        return contentHeight > height + 1f;
    }

    private float getThumbHeight() {
        if (!requiresScrollbar()) {
            return height;
        }
        float ratio = height / contentHeight;
        return Math.max(MIN_THUMB_SIZE, ratio * height);
    }

    private float normalizedScroll() {
        float maxOffset = Math.max(0f, contentHeight - height);
        if (maxOffset <= 0f) {
            return 0f;
        }
        return scrollOffset / maxOffset;
    }

    private void recalcContentHeight() {
        float maxBottom = 0f;
        for (UIComponent child : children) {
            float bottom = (child.getY() + child.getHeight()) - y;
            if (bottom > maxBottom) {
                maxBottom = bottom;
            }
        }
        contentHeight = Math.max(height, maxBottom);
        lastMeasuredExtent = contentHeight;
        setScrollOffset(scrollOffset);
    }

    void setScrollOffsetForRestore(float offset) {
        setScrollOffset(offset);
    }

    public void requestLayout() {
        recalcContentHeight();
    }

    /** @VisibleForTesting */
    float getScrollOffsetForTest() {
        return getScrollOffset();
    }

    /** @VisibleForTesting */
    float getContentHeightForTest() {
        return contentHeight;
    }

    private boolean applyScissor(UIRenderer renderer) {
        int windowHeight = renderer.getWindowHeight();
        int windowWidth = renderer.getWindowWidth();
        if (windowHeight <= 0 || windowWidth <= 0) {
            return false;
        }

        int scissorX = Math.max(0, Math.round(x));
        int scissorY = Math.max(0, windowHeight - Math.round(y + height));
        int scissorWidth = Math.max(1, Math.min(windowWidth - scissorX, Math.round(width)));
        int scissorHeight = Math.max(1, Math.min(windowHeight - scissorY, Math.round(height)));

        if (scissorWidth <= 0 || scissorHeight <= 0) {
            return false;
        }

        boolean wasEnabled = glIsEnabled(GL_SCISSOR_TEST);
        if (wasEnabled) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(4);
                glGetIntegerv(GL_SCISSOR_BOX, buffer);
                prevScissorX = buffer.get(0);
                prevScissorY = buffer.get(1);
                prevScissorW = buffer.get(2);
                prevScissorH = buffer.get(3);
                previousScissorCaptured = true;
            }
        } else {
            previousScissorCaptured = false;
            glEnable(GL_SCISSOR_TEST);
        }
        glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        scissorApplied = true;
        scissorWasEnabled = wasEnabled;
        return true;
    }

    private void restoreScissorState() {
        if (!scissorApplied) {
            return;
        }
        if (scissorWasEnabled) {
            if (previousScissorCaptured) {
                glScissor(prevScissorX, prevScissorY, prevScissorW, prevScissorH);
            }
        } else {
            glDisable(GL_SCISSOR_TEST);
        }
        scissorApplied = false;
    }
}
