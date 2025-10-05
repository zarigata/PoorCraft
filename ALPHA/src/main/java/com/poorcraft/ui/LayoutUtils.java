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
}
