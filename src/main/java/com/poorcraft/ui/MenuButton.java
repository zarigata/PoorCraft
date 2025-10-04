package com.poorcraft.ui;

import com.poorcraft.render.Texture;
import com.poorcraft.resources.ResourceManager;

/**
 * Textured menu button rendered with the asset in {@code UI_FILES/button.png}.
 * Typography scales with the component height so captions remain legible across
 * resolutions while tint overlays provide hover and press feedback.
 */
public class MenuButton extends UIComponent {
    
    private static final float[] ACTIVE_TEXT = {1f, 1f, 1f, 1f};
    private static final float[] DISABLED_TEXT = {0.65f, 0.65f, 0.65f, 0.7f};
    private static final float[] BASE_TINT = {0.18f, 0.18f, 0.18f};
    private static final float[] HOVER_TINT = {0.05f, 0.26f, 0.34f};
    private static final String BUTTON_TEXTURE_PATH = "UI_FILES/button.png";
    
    private static Texture buttonTexture;
    private static boolean textureLoadAttempted;
    
    private String text;
    private Runnable onClick;
    private boolean pressed;
    private float hoverTransition;
    
    /**
     * Creates a new menu button.
     * 
     * @param x X position (top-left corner)
     * @param y Y position (top-left corner)
     * @param width Button width
     * @param height Button height
     * @param text Button text
     * @param onClick Click callback
     */
    public MenuButton(float x, float y, float width, float height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
        this.pressed = false;
        this.hoverTransition = 0.0f;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }
        
        ensureTextureLoaded();
        
        float tintR;
        float tintG;
        float tintB;
        float tintAlpha;
        float[] textColor;
        
        if (!enabled) {
            tintR = tintG = tintB = 0f;
            tintAlpha = 0.55f;
            textColor = DISABLED_TEXT;
        } else if (pressed) {
            tintR = tintG = tintB = 0f;
            tintAlpha = 0.38f;
            textColor = ACTIVE_TEXT;
        } else {
            tintR = lerp(BASE_TINT[0], HOVER_TINT[0], hoverTransition);
            tintG = lerp(BASE_TINT[1], HOVER_TINT[1], hoverTransition);
            tintB = lerp(BASE_TINT[2], HOVER_TINT[2], hoverTransition);
            tintAlpha = 0.22f + hoverTransition * 0.18f;
            textColor = ACTIVE_TEXT;
        }
        
        if (buttonTexture != null) {
            renderer.drawTexturedRect(x, y, width, height, buttonTexture.getId());
            renderer.drawRect(x, y, width, height, tintR, tintG, tintB, tintAlpha);
        } else {
            renderer.drawRect(x, y, width, height, 0.32f, 0.32f, 0.32f, 0.95f);
            float border = Math.max(3f, height * 0.05f);
            renderer.drawRect(x, y, width, border, 0.82f, 0.82f, 0.82f, 0.9f);
            renderer.drawRect(x, y + height - border, width, border, 0.12f, 0.12f, 0.12f, 0.9f);
            renderer.drawRect(x, y, border, height, 0.7f, 0.7f, 0.7f, 0.9f);
            renderer.drawRect(x + width - border, y, border, height, 0.12f, 0.12f, 0.12f, 0.9f);
        }
        
        if (text != null && !text.isEmpty()) {
            float baseHeight = Math.max(1f, fontRenderer.getTextHeight());
            float targetCapHeight = Math.max(height * 0.52f, 24f);
            float textScale = Math.max(1.0f, targetCapHeight / baseHeight);
            
            float textWidth = fontRenderer.getTextWidth(text) * textScale;
            float drawX = x + (width - textWidth) / 2.0f;
            float drawBaseline = y + (height + baseHeight * textScale * 0.2f) / 2.0f;
            
            fontRenderer.drawText(text, drawX + 2f * textScale, drawBaseline + 2f * textScale,
                textScale, 0f, 0f, 0f, 0.6f);
            fontRenderer.drawText(text, drawX, drawBaseline, textScale,
                textColor[0], textColor[1], textColor[2], textColor[3]);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        float target = hovered ? 1.0f : 0.0f;
        float speed = 8.5f;
        hoverTransition += (target - hoverTransition) * deltaTime * speed;
        if (Math.abs(hoverTransition - target) < 0.01f) {
            hoverTransition = target;
        }
    }
    
    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (enabled && isMouseOver(mouseX, mouseY) && button == 0) {
            pressed = true;
        }
    }
    
    @Override
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        if (pressed && button == 0) {
            pressed = false;
            // Only trigger callback if mouse is still over button
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
    
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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
        } catch (Exception diskFailure) {
            try {
                buttonTexture = Texture.loadFromResource("/" + BUTTON_TEXTURE_PATH);
                System.out.println("[MenuButton] Loaded textured button from classpath fallback");
            } catch (Exception fallbackFailure) {
                System.err.println("[MenuButton] Failed to load button texture: " + fallbackFailure.getMessage());
                buttonTexture = null;
            }
        }
    }
}
