package com.poorcraft.render;

import com.poorcraft.camera.Camera;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.GL_FLOAT;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

/**
 * Renders the sky dome with animated clouds and a sun disc.
 */
public class SkyRenderer {

    private static final float[] FULLSCREEN_QUAD = new float[]{
        -1.0f, -1.0f,
         1.0f, -1.0f,
         1.0f,  1.0f,
        -1.0f, -1.0f,
         1.0f,  1.0f,
        -1.0f,  1.0f
    };

    private Shader skyShader;
    private int vao;
    private int vbo;
    private float cloudTime;

    public void init() {
        skyShader = Shader.loadFromResources("/shaders/sky.vert", "/shaders/sky.frag");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer quadBuffer = BufferUtils.createFloatBuffer(FULLSCREEN_QUAD.length);
        quadBuffer.put(FULLSCREEN_QUAD).flip();
        glBufferData(GL_ARRAY_BUFFER, quadBuffer, org.lwjgl.opengl.GL15.GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void update(float deltaTime) {
        cloudTime += deltaTime;
    }

    public void render(Camera camera, float fovDegrees, float aspect, Vector3f sunDirection) {
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);

        skyShader.bind();

        Vector3f forward = camera.getFront();
        Vector3f right = camera.getRight();
        Vector3f up = new Vector3f(right).cross(forward).normalize();

        skyShader.setUniform("uCameraForward", forward);
        skyShader.setUniform("uCameraRight", right);
        skyShader.setUniform("uCameraUp", up);
        skyShader.setUniform("uFov", fovDegrees);
        skyShader.setUniform("uAspect", aspect);
        skyShader.setUniform("uSunDirection", sunDirection);
        skyShader.setUniform("uTime", cloudTime);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        skyShader.unbind();

        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
    }

    public void cleanup() {
        if (skyShader != null) {
            skyShader.cleanup();
        }
        glDisableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vbo);
        glBindVertexArray(0);
        glDeleteVertexArrays(vao);
    }
}
