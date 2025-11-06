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
    private static Texture buttonTexture;
    private static boolean textureLoadAttempted;
    private static boolean textureLoadFailed;

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

    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }

        ensureTextureLoaded();

        float disabledMul = enabled ? 1.0f : 0.4f;

        float hoverScale = 1.0f + (hoverBlend * 0.02f);
        if (pressed) {
            hoverScale *= 0.985f;
        }
        float renderWidth = width * hoverScale;
        float renderHeight = height * hoverScale;
        float renderX = x + (width - renderWidth) / 2f;
        float renderY = y + (height - renderHeight) / 2f;

        if (!pressed) {
            renderer.drawDropShadow(renderX - 3f, renderY - 3f, renderWidth, renderHeight, 6f, 0.4f * disabledMul);
        }

        // Improved fallback rendering when texture is missing
        if (buttonTexture == null && textureLoadFailed) {
            // Draw gradient background for depth
            float baseBrightness = 0.17f + 0.05f * hoverBlend;
            float topBrightness = baseBrightness * 1.08f;
            float bottomBrightness = baseBrightness * 0.94f;

            if (pressed) {
                topBrightness *= 0.82f;
                bottomBrightness *= 0.82f;
            }

            renderer.drawRect(renderX, renderY, renderWidth, renderHeight / 2f,
                topBrightness * disabledMul, topBrightness * disabledMul, topBrightness * disabledMul, 0.96f);
            renderer.drawRect(renderX, renderY + renderHeight / 2f, renderWidth, renderHeight / 2f,
                bottomBrightness * disabledMul, bottomBrightness * disabledMul, bottomBrightness * disabledMul, 0.96f);
        } else {
            // Original rendering with texture
            renderer.drawRect(renderX, renderY, renderWidth, renderHeight,
                0.07f * disabledMul, 0.11f * disabledMul, 0.14f * disabledMul, 0.92f);

            if (buttonTexture != null) {
                renderer.drawTexturedRect(renderX, renderY, renderWidth, renderHeight, buttonTexture.getId());
            }

            float overlayAlpha = 0.18f + 0.25f * hoverBlend + (pressed ? 0.15f : 0f);
            renderer.drawRect(renderX, renderY, renderWidth, renderHeight,
                0.06f + 0.18f * hoverBlend,
                0.26f + 0.2f * hoverBlend,
                0.32f + 0.22f * hoverBlend,
                overlayAlpha * disabledMul);
        }

        // Enhanced border with 3D effect
        float border = clamp(Math.min(renderWidth, renderHeight) * 0.095f, 3f, 12f);
        float highlightFactor = 1.1f * (pressed ? 0.85f : 1.0f);
        float shadowFactor = 0.85f * (pressed ? 1.2f : 1.0f);

        float highlightR = Math.min(1f, 0.08f * disabledMul * highlightFactor);
        float highlightG = Math.min(1f, 0.95f * disabledMul * highlightFactor);
        float highlightB = Math.min(1f, 0.95f * disabledMul * highlightFactor);

        float shadowR = Math.min(1f, 0.05f * disabledMul * shadowFactor);
        float shadowG = Math.min(1f, 0.72f * disabledMul * shadowFactor);
        float shadowB = Math.min(1f, 0.72f * disabledMul * shadowFactor);

        renderer.drawRect(renderX, renderY, renderWidth, border,
            highlightR, highlightG, highlightB, 0.88f);
        renderer.drawRect(renderX, renderY, border, renderHeight,
            highlightR * 0.9f, highlightG, highlightB, 0.86f);

        renderer.drawRect(renderX, renderY + renderHeight - border, renderWidth, border,
            shadowR, shadowG, shadowB, 0.78f);
        renderer.drawRect(renderX + renderWidth - border, renderY, border, renderHeight,
            shadowR * 1.05f, shadowG, shadowB, 0.76f);

        float cornerSize = Math.max(2f, border * 0.5f);
        renderer.drawRect(renderX, renderY, cornerSize, cornerSize,
            highlightR, highlightG, highlightB, 0.92f);
        renderer.drawRect(renderX + renderWidth - cornerSize, renderY + renderHeight - cornerSize,
            cornerSize, cornerSize,
            shadowR, shadowG, shadowB, 0.82f);

        if (text == null || text.isEmpty()) {
            return;
        }

        float baseHeight = Math.max(1f, fontRenderer.getTextHeight());
        float availableWidth = Math.max(12f, renderWidth - paddingX * 2f);
        float targetCapHeight = clamp(renderHeight * 0.5f, 22f, renderHeight * 0.68f) * 1.05f;
        float textScale = Math.max(1.0f, targetCapHeight / baseHeight);
        float textWidth = fontRenderer.getTextWidth(text) * textScale;
        if (textWidth > availableWidth) {
            textScale = Math.max(0.7f, availableWidth / textWidth);
            textWidth = fontRenderer.getTextWidth(text) * textScale;
        }

        float textX = renderX + (renderWidth - textWidth) / 2f;
        float textBaseline = renderY + (renderHeight + baseHeight * textScale * 0.28f) / 2f;

        float[] color = enabled ? ACTIVE_TEXT_COLOR : DISABLED_TEXT_COLOR;
        fontRenderer.drawTextWithShadow(text, textX, textBaseline, textScale,
            color[0], color[1], color[2], color[3] * disabledMul, 2.5f, 0.65f * disabledMul);
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

    public void setBounds(float x, float y, float width, float height) {
        super.setBounds(x, y, width, height);
    }

    public void setPosition(float x, float y) {
        super.setPosition(x, y);
    }

    public void setSize(float width, float height) {
        super.setSize(width, height);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void ensureTextureLoaded() {
        if (textureLoadAttempted) {
            return;
        }

        textureLoadAttempted = true;
        textureLoadFailed = false;
        
        // Try loading from filesystem first (UI_FILES/button.png)
        ResourceManager resourceManager = ResourceManager.getInstance();
        String absolutePath = resourceManager.getResourcePath(BUTTON_TEXTURE_PATH);
        try {
            buttonTexture = Texture.loadFromFile(absolutePath);
            System.out.println("[MenuButton] Loaded textured button from " + absolutePath);
            return;
        } catch (Exception diskFailure) {
            // Not an error - file might not exist yet
        }

        // Try loading from classpath as fallback
        try {
            buttonTexture = Texture.loadFromResource("/" + BUTTON_TEXTURE_PATH);
            System.out.println("[MenuButton] Loaded textured button from classpath fallback");
        } catch (Exception fallbackFailure) {
            // Use procedural fallback rendering
            System.out.println("[MenuButton] Button texture not found, using procedural rendering");
            buttonTexture = null;
            textureLoadFailed = true;
        }
    }
}
