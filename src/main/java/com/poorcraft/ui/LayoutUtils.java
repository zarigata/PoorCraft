package com.poorcraft.ui;

/**
 * Utility methods for responsive UI layout calculations.
 *
 * <p>Centralising these helpers keeps sizing rules consistent across
 * different screens while avoiding duplicated math. The methods favour
 * percentage-based sizing with sensible minimum and maximum clamps so
 * elements remain readable on both low and high resolution displays.</p>
 */
public final class LayoutUtils {

    /**
     * Standard Minecraft-style layout constants. These mirror the sizing
     * conventions of classic menu screens to keep PoorCraft's UI familiar
     * while still allowing responsive scaling.
     */
    public static final float MINECRAFT_BUTTON_WIDTH = 200f;
    public static final float MINECRAFT_BUTTON_HEIGHT = 20f;
    public static final float MINECRAFT_BUTTON_SPACING = 24f;
    public static final float MINECRAFT_PANEL_PADDING = 32f;
    public static final float MINECRAFT_TITLE_SCALE = 2.0f;
    public static final float MINECRAFT_SUBTITLE_SCALE = 1.2f;
    public static final float MINECRAFT_LABEL_SCALE = 1.0f;

    private LayoutUtils() {
        // Static utility class
    }

    /**
     * Scales a value based on window width and clamps it to the provided bounds.
     *
     * @param windowWidth current window width
     * @param fraction    portion of the width to use (e.g. 0.5 == 50%)
     * @param min         minimum allowed value
     * @param max         maximum allowed value
     * @return scaled and clamped value
     */
    public static float scaledWidth(int windowWidth, float fraction, float min, float max) {
        return clamp(windowWidth * fraction, min, max);
    }

    /**
     * Scales a value based on window height and clamps it to the provided bounds.
     *
     * @param windowHeight current window height
     * @param fraction     portion of the height to use
     * @param min          minimum allowed value
     * @param max          maximum allowed value
     * @return scaled and clamped value
     */
    public static float scaledHeight(int windowHeight, float fraction, float min, float max) {
        return clamp(windowHeight * fraction, min, max);
    }

    /**
     * Clamps the provided value between {@code min} and {@code max}.
     *
     * @param value value to clamp
     * @param min   minimum allowed value
     * @param max   maximum allowed value
     * @return clamped value
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Centers an element horizontally inside the window.
     *
     * @param windowWidth  total window width
     * @param elementWidth element width
     * @return X coordinate for the element's left edge
     */
    public static float centerHorizontally(int windowWidth, float elementWidth) {
        return (windowWidth - elementWidth) / 2.0f;
    }

    /**
     * Centers an element vertically inside the window.
     *
     * @param windowHeight  total window height
     * @param elementHeight element height
     * @return Y coordinate for the element's top edge
     */
    public static float centerVertically(int windowHeight, float elementHeight) {
        return (windowHeight - elementHeight) / 2.0f;
    }

    /**
     * Computes a responsive Minecraft-style button width.
     *
     * @param windowWidth current window width
     * @param uiScale     global UI scale factor
     * @return clamped button width between 180px and 400px
     */
    public static float getMinecraftButtonWidth(int windowWidth, float uiScale) {
        float targetWidth = MINECRAFT_BUTTON_WIDTH * uiScale;
        return clamp(targetWidth, 180f, 400f);
    }

    /**
     * Computes a responsive Minecraft-style button height.
     *
     * @param windowHeight current window height
     * @param uiScale      global UI scale factor
     * @return clamped button height between 40px and 80px
     */
    public static float getMinecraftButtonHeight(int windowHeight, float uiScale) {
        float targetHeight = MINECRAFT_BUTTON_HEIGHT * uiScale;
        return clamp(targetHeight, 40f, 80f);
    }

    /**
     * Derives the spacing between stacked buttons based on button height.
     *
     * @param buttonHeight computed button height
     * @return spacing value equal to 30% of the button height
     */
    public static float getMinecraftButtonSpacing(float buttonHeight) {
        return buttonHeight * 0.3f;
    }

    /**
     * Computes the ideal panel width for menu layouts.
     *
     * @param windowWidth current window width
     * @param uiScale     global UI scale factor
     * @return clamped panel width covering roughly half the window
     */
    public static float getMinecraftPanelWidth(int windowWidth, float uiScale) {
        float targetWidth = windowWidth * 0.5f * uiScale;
        return clamp(targetWidth, 400f, 600f);
    }

    /**
     * Computes the ideal panel height for menu layouts.
     *
     * @param windowHeight current window height
     * @param uiScale      global UI scale factor
     * @return clamped panel height covering roughly 60% of the window
     */
    public static float getMinecraftPanelHeight(int windowHeight, float uiScale) {
        float targetHeight = windowHeight * 0.6f * uiScale;
        return clamp(targetHeight, 400f, 700f);
    }

    /**
     * Determines the internal padding for panels.
     *
     * @param panelWidth current panel width
     * @return padding value equal to 8% of the panel width, clamped sensibly
     */
    public static float getMinecraftPanelPadding(float panelWidth) {
        float padding = panelWidth * 0.08f;
        return clamp(padding, 24f, 48f);
    }

    /**
     * Calculates the total height required for a stack of buttons.
     *
     * @param buttonCount number of buttons in the stack
     * @param buttonHeight height of each button
     * @param spacing      spacing between buttons
     * @return total vertical space required for the stack
     */
    public static float calculateButtonStackHeight(int buttonCount, float buttonHeight, float spacing) {
        if (buttonCount <= 0) {
            return 0f;
        }
        return (buttonHeight * buttonCount) + (spacing * (buttonCount - 1));
    }

    /**
     * Determines the Y coordinate required to center a button stack vertically.
     *
     * @param windowHeight total window height
     * @param stackHeight  precomputed stack height
     * @return top Y coordinate for a centered stack
     */
    public static float centerButtonStack(int windowHeight, float stackHeight) {
        return centerVertically(windowHeight, stackHeight);
    }
}
