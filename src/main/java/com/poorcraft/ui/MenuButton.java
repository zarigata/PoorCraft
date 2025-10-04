package com.poorcraft.ui;

import com.poorcraft.render.Texture;
import com.poorcraft.resources.ResourceManager;

/**
 * Menu button rendered using the textured asset in {@code UI_FILES/button.png}.
 * The texture is scaled uniformly and a hover overlay keeps the button legible
 * across bright backdrops. Typography scales with button height while honoring
 * horizontal padding.
 */
public class MenuButton extends UIComponent {

    private static final String BUTTON_TEXTURE_PATH = "UI_FILES/button.png";
    private static final float[] ACTIVE_TEXT_COLOR = {0.98f, 0.98f, 0.98f, 1.0f};
    private static final float[] DISABLED_TEXT_COLOR = {0.62f, 0.62f, 0.62f, 0.7f};
    private static final float[] SHADOW_COLOR = {0f, 0f, 0f, 0.55f};

    private static Texture buttonTexture;
    private static boolean textureLoadAttempted;

    private String text;
    private Runnable onClick;
    private boolean pressed;
    private float hoverBlend;
    private float paddingX = 20f;

    public MenuButton(float x, float y, float width, float height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
    }

    public void setHorizontalPadding(float padding) {
        this.paddingX = Math.max(6f, padding);
    }

    @Override
    public void update(float deltaTime) {
        float target = hovered ? 1.0f : 0.0f;
        float speed = pressed ? 11.0f : 7.5f;
        hoverBlend += (target - hoverBlend) * deltaTime * speed;
        if (Math.abs(hoverBlend - target) < 0.01f) {
            hoverBlend = target;
        }
    }

    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }

        ensureTextureLoaded();

        float disabledMul = enabled ? 1.0f : 0.4f;
        renderer.drawRect(x, y, width, height,
            0.07f * disabledMul, 0.11f * disabledMul, 0.14f * disabledMul, 0.92f);

        if (buttonTexture != null) {
            renderer.drawTexturedRect(x, y, width, height, buttonTexture.getId());
        }

        float overlayAlpha = 0.18f + 0.25f * hoverBlend + (pressed ? 0.15f : 0f);
        renderer.drawRect(x, y, width, height,
            0.06f + 0.18f * hoverBlend,
            0.26f + 0.2f * hoverBlend,
            0.32f + 0.22f * hoverBlend,
            overlayAlpha * disabledMul);

        float border = clamp(Math.min(width, height) * 0.085f, 3f, 10f);
        renderer.drawRect(x, y, width, border,
            0.0f, 0.93f * disabledMul, 0.93f * disabledMul, 0.86f);
        renderer.drawRect(x, y + height - border, width, border,
            0.0f, 0.82f * disabledMul, 0.82f * disabledMul, 0.72f);
        renderer.drawRect(x, y, border, height,
            0.02f, 0.78f * disabledMul, 0.82f * disabledMul, 0.78f);
        renderer.drawRect(x + width - border, y, border, height,
            0.02f, 0.78f * disabledMul, 0.82f * disabledMul, 0.78f);

        if (text == null || text.isEmpty()) {
            return;
        }

        float baseHeight = Math.max(1f, fontRenderer.getTextHeight());
        float availableWidth = Math.max(12f, width - paddingX * 2f);
        float targetCapHeight = clamp(height * 0.5f, 22f, height * 0.68f);
        float textScale = Math.max(1.0f, targetCapHeight / baseHeight);
        float textWidth = fontRenderer.getTextWidth(text) * textScale;
        if (textWidth > availableWidth) {
            textScale = Math.max(0.7f, availableWidth / textWidth);
            textWidth = fontRenderer.getTextWidth(text) * textScale;
        }

        float textX = x + (width - textWidth) / 2f;
        float textBaseline = y + (height + baseHeight * textScale * 0.3f) / 2f;

        fontRenderer.drawText(text, textX + 2f * textScale, textBaseline + 2f * textScale,
            textScale, SHADOW_COLOR[0], SHADOW_COLOR[1], SHADOW_COLOR[2], SHADOW_COLOR[3] * disabledMul);

        float[] color = enabled ? ACTIVE_TEXT_COLOR : DISABLED_TEXT_COLOR;
        fontRenderer.drawText(text, textX, textBaseline, textScale,
            color[0], color[1], color[2], color[3] * disabledMul);
    }

    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (enabled && button == 0 && isMouseOver(mouseX, mouseY)) {
            pressed = true;
        }
    }

    @Override
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        if (pressed && button == 0) {
            pressed = false;
            if (enabled && isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.run();
            }
        }
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void ensureTextureLoaded() {
        if (textureLoadAttempted) {
            return;
        }

        textureLoadAttempted = true;
        ResourceManager resourceManager = ResourceManager.getInstance();
        String absolutePath = resourceManager.getResourcePath(BUTTON_TEXTURE_PATH);
        try {
            buttonTexture = Texture.loadFromFile(absolutePath);
            System.out.println("[MenuButton] Loaded textured button from " + absolutePath);
            return;
        } catch (Exception diskFailure) {
            System.err.println("[MenuButton] Disk load failed for button texture: " + diskFailure.getMessage());
        }

        try {
            buttonTexture = Texture.loadFromResource("/" + BUTTON_TEXTURE_PATH);
            System.out.println("[MenuButton] Loaded textured button from classpath fallback");
        } catch (Exception fallbackFailure) {
            System.err.println("[MenuButton] Failed to load button texture: " + fallbackFailure.getMessage());
            buttonTexture = null;
        }
    }
}
