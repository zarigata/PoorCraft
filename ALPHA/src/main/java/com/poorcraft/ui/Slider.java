package com.poorcraft.ui;

import java.util.function.Consumer;

/**
 * Responsive slider with padded layout so the track, handle, and value label do
 * not bleed outside their container. Values are stored normalized (0..1) and
 * mapped to [min,max] when queried.
 */
public class Slider extends UIComponent {

    private static final float[] TRACK_COLOR = {0.22f, 0.22f, 0.25f, 0.84f};
    private static final float[] TRACK_BORDER = {0.05f, 0.86f, 0.96f, 0.4f};
    private static final float[] HANDLE_COLOR = {0.55f, 0.92f, 0.96f, 1.0f};
    private static final float[] HANDLE_HOVER_COLOR = {0.72f, 0.98f, 1.0f, 1.0f};
    private static final float[] LABEL_COLOR = {0.96f, 0.97f, 0.99f, 1.0f};
    private static final float[] VALUE_COLOR = {0.8f, 0.82f, 0.85f, 0.92f};

    private final String label;
    private final float minValue;
    private final float maxValue;
    private final Consumer<Float> onChange;

    private float value; // normalized 0..1
    private boolean dragging;
    private int decimalPlaces = 2;
    private float labelScale = 1.0f;
    private float valueScale = 1.0f;

    private float cachedTrackX;
    private float cachedTrackWidth;

    public Slider(float x, float y, float width, float height, String label,
                  float minValue, float maxValue, float initialValue,
                  Consumer<Float> onChange) {
        super(x, y, width, height);
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.onChange = onChange;
        setValue(initialValue);
    }

    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }

        float baseHeight = Math.max(1f, fontRenderer.getTextHeight());
        float appliedLabelScale = Math.max(0.75f, labelScale);
        float appliedValueScale = Math.max(0.65f, valueScale);
        float padding = Math.max(10f, height * 0.2f);

        float labelBaseline = y + padding + baseHeight * appliedLabelScale;
        fontRenderer.drawText(label, x + padding, labelBaseline, appliedLabelScale,
            LABEL_COLOR[0], LABEL_COLOR[1], LABEL_COLOR[2], LABEL_COLOR[3]);

        float actualValue = minValue + value * (maxValue - minValue);
        String valueText = formatValue(actualValue);
        float valueTextWidth = fontRenderer.getTextWidth(valueText) * appliedValueScale;
        float valueSlotWidth = Math.max(valueTextWidth + padding * 1.3f, Math.min(160f, width * 0.33f));

        float trackX = x + padding;
        float trackWidth = Math.max(60f, width - padding * 2f - valueSlotWidth);
        float trackHeight = Math.max(6f, height * 0.18f);
        float trackY = labelBaseline + Math.max(8f, padding * 0.55f);
        float trackCenterY = trackY + trackHeight / 2f;

        renderer.drawRect(trackX - 2f, trackCenterY - trackHeight / 2f - 2f,
            trackWidth + 4f, trackHeight + 4f,
            TRACK_BORDER[0], TRACK_BORDER[1], TRACK_BORDER[2], TRACK_BORDER[3]);
        renderer.drawRect(trackX, trackCenterY - trackHeight / 2f,
            trackWidth, trackHeight,
            TRACK_COLOR[0], TRACK_COLOR[1], TRACK_COLOR[2], TRACK_COLOR[3]);

        float handleWidth = Math.min(trackWidth, Math.max(trackHeight * 2.1f, 20f));
        float handleHeight = Math.max(trackHeight * 2.4f, 26f);
        float normalized = Math.max(0f, Math.min(1f, value));
        float handleX = trackX + (trackWidth - handleWidth) * normalized;
        float handleY = trackCenterY - handleHeight / 2f;
        float[] handleColor = (hovered || dragging) ? HANDLE_HOVER_COLOR : HANDLE_COLOR;
        renderer.drawRect(handleX, handleY, handleWidth, handleHeight,
            handleColor[0], handleColor[1], handleColor[2], handleColor[3]);

        float valueX = trackX + trackWidth + padding * 0.6f;
        float valueBaseline = trackCenterY + baseHeight * appliedValueScale * 0.35f;
        fontRenderer.drawText(valueText, valueX, valueBaseline, appliedValueScale,
            VALUE_COLOR[0], VALUE_COLOR[1], VALUE_COLOR[2], VALUE_COLOR[3]);

        cachedTrackX = trackX;
        cachedTrackWidth = trackWidth;
    }

    @Override
    public void update(float deltaTime) {
        // No animation beyond dragging
    }

    @Override
    public void onMouseMove(float mouseX, float mouseY) {
        super.onMouseMove(mouseX, mouseY);
        if (dragging) {
            updateValueFromMouse(mouseX);
        }
    }

    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            dragging = true;
            updateValueFromMouse(mouseX);
        }
    }

    @Override
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    private void updateValueFromMouse(float mouseX) {
        float trackX = cachedTrackX > 0f ? cachedTrackX : x + Math.max(10f, width * 0.1f);
        float trackWidth = cachedTrackWidth > 0f ? cachedTrackWidth : Math.max(80f, width - 80f);

        float normalized = (mouseX - trackX) / trackWidth;
        normalized = Math.max(0f, Math.min(1f, normalized));
        if (normalized != value) {
            value = normalized;
            if (onChange != null) {
                float actual = minValue + value * (maxValue - minValue);
                onChange.accept(actual);
            }
        }
    }

    private String formatValue(float value) {
        if (decimalPlaces == 0) {
            return String.valueOf((int) value);
        }
        return String.format("%." + decimalPlaces + "f", value);
    }

    public float getValue() {
        return minValue + value * (maxValue - minValue);
    }

    public void setValue(float value) {
        this.value = (value - minValue) / (maxValue - minValue);
        this.value = Math.max(0.0f, Math.min(1.0f, this.value));
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public void setFontScale(float labelScale, float valueScale) {
        this.labelScale = Math.max(0.6f, labelScale);
        this.valueScale = Math.max(0.6f, valueScale);
    }
}
