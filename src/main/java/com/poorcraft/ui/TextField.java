package com.poorcraft.ui;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Text input field UI component with adjustable padding and text scale so that
 * values remain legible on high-resolution displays.
 */
public class TextField extends UIComponent {

    private static final float[] BG_COLOR = {0.1f, 0.1f, 0.12f, 0.92f};
    private static final float[] BG_FOCUSED_COLOR = {0.16f, 0.18f, 0.22f, 0.96f};
    private static final float[] BORDER_COLOR = {0.35f, 0.42f, 0.48f, 1.0f};
    private static final float[] BORDER_FOCUSED_COLOR = {0.55f, 0.75f, 0.85f, 1.0f};
    private static final float[] TEXT_COLOR = {0.96f, 0.98f, 1.0f, 1.0f};
    private static final float[] PLACEHOLDER_COLOR = {0.7f, 0.76f, 0.84f, 0.88f};
    private static final float[] CURSOR_COLOR = {0.95f, 0.98f, 1.0f, 0.95f};

    private String text;
    private String placeholder;
    private boolean focused;
    private int cursorPosition;
    private float cursorBlinkTimer;
    private int maxLength;
    private float baseScale;
    private float paddingX;
    private float paddingY;

    public TextField(float x, float y, float width, float height, String placeholder) {
        super(x, y, width, height);
        this.text = "";
        this.placeholder = placeholder == null ? "" : placeholder;
        this.focused = false;
        this.cursorPosition = 0;
        this.cursorBlinkTimer = 0f;
        this.maxLength = 64;
        this.baseScale = 1.0f;
        this.paddingX = 16f;
        this.paddingY = 12f;
    }

    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }

        float[] bg = focused ? BG_FOCUSED_COLOR : BG_COLOR;
        renderer.drawRect(x, y, width, height, bg[0], bg[1], bg[2], bg[3]);

        float[] border = focused ? BORDER_FOCUSED_COLOR : BORDER_COLOR;
        float borderW = 2f;
        renderer.drawRect(x, y, width, borderW, border[0], border[1], border[2], border[3]);
        renderer.drawRect(x, y + height - borderW, width, borderW, border[0], border[1], border[2], border[3]);
        renderer.drawRect(x, y, borderW, height, border[0], border[1], border[2], border[3]);
        renderer.drawRect(x + width - borderW, y, borderW, height, border[0], border[1], border[2], border[3]);

        float fontHeight = Math.max(1f, fontRenderer.getTextHeight());
        float usableHeight = Math.max(16f, height - paddingY * 2f);
        float scale = Math.max(baseScale, usableHeight / fontHeight);

        float textX = x + paddingX;
        float textBaseline = y + (height + fontHeight * scale * 0.3f) / 2f;

        if (text.isEmpty() && !focused && !placeholder.isEmpty()) {
            fontRenderer.drawText(placeholder, textX, textBaseline, scale,
                PLACEHOLDER_COLOR[0], PLACEHOLDER_COLOR[1], PLACEHOLDER_COLOR[2], PLACEHOLDER_COLOR[3]);
        } else {
            fontRenderer.drawText(text, textX, textBaseline, scale,
                TEXT_COLOR[0], TEXT_COLOR[1], TEXT_COLOR[2], TEXT_COLOR[3]);

            if (focused && (cursorBlinkTimer % 1.0f) < 0.5f) {
                String before = text.substring(0, cursorPosition);
                float cursorX = textX + fontRenderer.getTextWidth(before) * scale;
                float cursorY = y + paddingY * 0.6f;
                float cursorH = height - paddingY * 1.2f;
                renderer.drawRect(cursorX, cursorY, 2f, cursorH,
                    CURSOR_COLOR[0], CURSOR_COLOR[1], CURSOR_COLOR[2], CURSOR_COLOR[3]);
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        if (focused) {
            cursorBlinkTimer += deltaTime;
        }
    }

    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            boolean inside = isMouseOver(mouseX, mouseY);
            setFocused(inside);
            if (inside) {
                cursorPosition = text.length();
            }
        }
    }

    @Override
    public void onKeyPress(int key, int mods) {
        if (!focused) {
            return;
        }

        switch (key) {
            case GLFW_KEY_BACKSPACE -> {
                if (cursorPosition > 0) {
                    text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                    cursorPosition--;
                }
            }
            case GLFW_KEY_DELETE -> {
                if (cursorPosition < text.length()) {
                    text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
                }
            }
            case GLFW_KEY_LEFT -> {
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
            }
            case GLFW_KEY_RIGHT -> {
                if (cursorPosition < text.length()) {
                    cursorPosition++;
                }
            }
            case GLFW_KEY_HOME -> cursorPosition = 0;
            case GLFW_KEY_END -> cursorPosition = text.length();
            default -> {
            }
        }

        cursorBlinkTimer = 0f;
    }

    @Override
    public void onCharInput(char character) {
        if (!focused) {
            return;
        }

        if (character >= 32 && character < 127 && text.length() < maxLength) {
            text = text.substring(0, cursorPosition) + character + text.substring(cursorPosition);
            cursorPosition++;
            cursorBlinkTimer = 0f;
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        this.cursorPosition = Math.min(this.text.length(), cursorPosition);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            cursorBlinkTimer = 0f;
            cursorPosition = Math.min(cursorPosition, text.length());
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
        if (text.length() > this.maxLength) {
            text = text.substring(0, this.maxLength);
            cursorPosition = Math.min(cursorPosition, text.length());
        }
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setTextScale(float scale) {
        this.baseScale = Math.max(0.7f, scale);
    }

    public void setPadding(float paddingX, float paddingY) {
        this.paddingX = Math.max(4f, paddingX);
        this.paddingY = Math.max(4f, paddingY);
    }
}
