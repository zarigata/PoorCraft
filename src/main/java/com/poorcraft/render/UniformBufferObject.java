package com.poorcraft.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

/**
 * Uniform Buffer Object wrapper that stores camera and lighting information shared across chunk draw calls.
 */
public class UniformBufferObject {

    private static final int MAT4_SIZE = 16 * Float.BYTES;
    private static final int FLOAT_SIZE = Float.BYTES;

    private static final int OFFSET_PROJECTION = 0;
    private static final int OFFSET_VIEW = OFFSET_PROJECTION + MAT4_SIZE;
    private static final int OFFSET_LIGHT_DIRECTION = OFFSET_VIEW + MAT4_SIZE;
    private static final int OFFSET_LIGHT_COLOR = OFFSET_LIGHT_DIRECTION + alignVec3();
    private static final int OFFSET_AMBIENT_COLOR = OFFSET_LIGHT_COLOR + alignVec3();
    private static final int OFFSET_AMBIENT_STRENGTH = OFFSET_AMBIENT_COLOR + alignVec3();
    private static final int OFFSET_FOG_COLOR = OFFSET_AMBIENT_STRENGTH + 16; // padded to vec4
    private static final int OFFSET_FOG_START = OFFSET_FOG_COLOR + alignVec3();
    private static final int OFFSET_FOG_END = OFFSET_FOG_START + 16;

    private static final int TOTAL_SIZE = 256;

    private final int bindingPoint;
    private int uboHandle;
    private boolean allocated;

    private final ByteBuffer stagingBuffer = BufferUtils.createByteBuffer(TOTAL_SIZE);
    private final FloatBuffer floatView = stagingBuffer.asFloatBuffer();

    public UniformBufferObject(int bindingPoint) {
        this.bindingPoint = bindingPoint;
    }

    public void init() {
        if (allocated) {
            return;
        }
        uboHandle = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, uboHandle);
        glBufferData(GL_UNIFORM_BUFFER, TOTAL_SIZE, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, uboHandle);
        allocated = true;
    }

    public void updateMatrices(Matrix4f projection, Matrix4f view) {
        ensureAllocated();
        projection.get(OFFSET_PROJECTION / Float.BYTES, floatView);
        view.get(OFFSET_VIEW / Float.BYTES, floatView);
        uploadRange(OFFSET_PROJECTION, MAT4_SIZE * 2);
    }

    public void updateLighting(Vector3f direction, Vector3f color, Vector3f ambientColor, float ambientStrength) {
        ensureAllocated();
        putVec3(OFFSET_LIGHT_DIRECTION, direction);
        putVec3(OFFSET_LIGHT_COLOR, color);
        putVec3(OFFSET_AMBIENT_COLOR, ambientColor);
        stagingBuffer.putFloat(OFFSET_AMBIENT_STRENGTH, ambientStrength);
        uploadRange(OFFSET_LIGHT_DIRECTION, alignVec3() * 3 + 16);
    }

    public void updateFog(Vector3f color, float start, float end) {
        ensureAllocated();
        putVec3(OFFSET_FOG_COLOR, color);
        stagingBuffer.putFloat(OFFSET_FOG_START, start);
        stagingBuffer.putFloat(OFFSET_FOG_END, end);
        uploadRange(OFFSET_FOG_COLOR, alignVec3() + 32);
    }

    public void bind() {
        if (!allocated) {
            return;
        }
        glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, uboHandle);
    }

    public void cleanup() {
        if (allocated) {
            glDeleteBuffers(uboHandle);
            uboHandle = 0;
            allocated = false;
        }
    }

    private void uploadRange(int offset, int length) {
        glBindBuffer(GL_UNIFORM_BUFFER, uboHandle);
        stagingBuffer.position(offset);
        stagingBuffer.limit(offset + length);
        glBufferSubData(GL_UNIFORM_BUFFER, offset, stagingBuffer);
        stagingBuffer.clear();
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private void ensureAllocated() {
        if (!allocated) {
            throw new IllegalStateException("UniformBufferObject not initialised");
        }
    }

    private void putVec3(int offset, Vector3f value) {
        stagingBuffer.putFloat(offset, value.x);
        stagingBuffer.putFloat(offset + FLOAT_SIZE, value.y);
        stagingBuffer.putFloat(offset + FLOAT_SIZE * 2, value.z);
        stagingBuffer.putFloat(offset + FLOAT_SIZE * 3, 0f);
    }

    private static int alignVec3() {
        return 4 * Float.BYTES;
    }
}
