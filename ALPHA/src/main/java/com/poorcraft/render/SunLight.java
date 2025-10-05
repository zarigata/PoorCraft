package com.poorcraft.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Represents the primary directional light in the scene (the sun).
 * Holds parameters we will later reuse for shadow mapping / ray tracing.
 */
public class SunLight {

    private final Vector3f direction = new Vector3f(0.3f, -0.7f, 0.5f).normalize();
    private final Vector3f color = new Vector3f(1.0f, 0.95f, 0.85f);
    private float intensity = 1.0f;

    // Placeholder matrices for future shadow/ray-tracing pipelines
    private final Matrix4f shadowViewMatrix = new Matrix4f().identity();
    private final Matrix4f shadowProjectionMatrix = new Matrix4f().identity();

    public Vector3f getDirection() {
        return new Vector3f(direction);
    }

    public void setDirection(Vector3f dir) {
        if (dir != null && dir.lengthSquared() > 0.0001f) {
            this.direction.set(dir).normalize();
        }
    }

    public Vector3f getColor() {
        return new Vector3f(color);
    }

    public void setColor(Vector3f newColor) {
        if (newColor != null) {
            this.color.set(newColor);
        }
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = Math.max(0.0f, intensity);
    }

    public Matrix4f getShadowViewMatrix() {
        return new Matrix4f(shadowViewMatrix);
    }

    public Matrix4f getShadowProjectionMatrix() {
        return new Matrix4f(shadowProjectionMatrix);
    }

    /**
     * Updates the sun direction and precomputes placeholder shadow matrices
     * using the given time and focus point. Designed so real shadow mapping
     * logic can reuse these values without changing higher-level code.
     */
    public void update(float timeOfDay, Vector3f focusPoint, float coverageDistance) {
        float wrapped = (timeOfDay % 1.0f + 1.0f) % 1.0f;
        float angle = wrapped * (float) (Math.PI * 2.0);
        direction.set(
            (float) Math.cos(angle) * 0.6f,
            (float) Math.sin(angle),
            (float) Math.sin(angle * 0.5f)
        ).normalize();

        // Placeholder intensity curve: brighter near noon, softer at sunset.
        intensity = Math.max(0.25f, direction.y * 0.75f + 0.5f);

        prepareShadowMatrices(focusPoint, coverageDistance);
    }

    /**
     * Prepares placeholder shadow matrices. We keep the method so future shadow
     * mapping can plug in without changing higher level code.
     */
    public void prepareShadowMatrices(Vector3f focusPoint, float coverageDistance) {
        // TODO: Fill with cascaded shadow matrix generation once ray-tracing/shadow
        // pipeline is introduced. For now we simply reset to identity to avoid
        // accidental stale state usage.
        shadowViewMatrix.identity();
        shadowProjectionMatrix.identity();
    }
}
