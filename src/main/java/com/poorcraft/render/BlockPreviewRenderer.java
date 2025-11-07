package com.poorcraft.render;

import com.poorcraft.world.block.BlockType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BlockPreviewRenderer {

    private static final int COMPONENTS_PER_VERTEX = 8;
    private static final int VERTICES_PER_CUBE = 36;
    private static final int BYTES_PER_FLOAT = Float.BYTES;

    private static final Vector3f LIGHT_DIRECTION = new Vector3f(0.3f, -0.7f, 0.5f).normalize();
    private static final Vector3f LIGHT_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);
    private static final float AMBIENT_STRENGTH = 0.5f;

    private static final int[] FACE_MAP = {0, 1, 3, 2, 4, 5};

    private static final float[] BASE_CUBE_VERTICES = {
        // Top face (y = 1) - normal (0, 1, 0)
        0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f,
        1f, 1f, 0f, 1f, 0f, 0f, 1f, 0f,
        1f, 1f, 1f, 1f, 1f, 0f, 1f, 0f,
        0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f,
        1f, 1f, 1f, 1f, 1f, 0f, 1f, 0f,
        0f, 1f, 1f, 0f, 1f, 0f, 1f, 0f,

        // Bottom face (y = 0) - normal (0, -1, 0)
        0f, 0f, 0f, 0f, 0f, 0f, -1f, 0f,
        1f, 0f, 1f, 1f, 1f, 0f, -1f, 0f,
        1f, 0f, 0f, 1f, 0f, 0f, -1f, 0f,
        0f, 0f, 0f, 0f, 0f, 0f, -1f, 0f,
        0f, 0f, 1f, 0f, 1f, 0f, -1f, 0f,
        1f, 0f, 1f, 1f, 1f, 0f, -1f, 0f,

        // Front face (z = 1) - normal (0, 0, 1)
        0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f,
        1f, 1f, 1f, 1f, 1f, 0f, 0f, 1f,
        1f, 0f, 1f, 1f, 0f, 0f, 0f, 1f,
        0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f,
        0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f,
        1f, 1f, 1f, 1f, 1f, 0f, 0f, 1f,

        // Back face (z = 0) - normal (0, 0, -1)
        0f, 0f, 0f, 1f, 0f, 0f, 0f, -1f,
        1f, 0f, 0f, 0f, 0f, 0f, 0f, -1f,
        1f, 1f, 0f, 0f, 1f, 0f, 0f, -1f,
        0f, 0f, 0f, 1f, 0f, 0f, 0f, -1f,
        1f, 1f, 0f, 0f, 1f, 0f, 0f, -1f,
        0f, 1f, 0f, 1f, 1f, 0f, 0f, -1f,

        // Left face (x = 0) - normal (-1, 0, 0)
        0f, 0f, 0f, 1f, 0f, -1f, 0f, 0f,
        0f, 1f, 1f, 0f, 1f, -1f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f, -1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f, -1f, 0f, 0f,
        0f, 0f, 1f, 1f, 1f, -1f, 0f, 0f,
        0f, 1f, 1f, 0f, 1f, -1f, 0f, 0f,

        // Right face (x = 1) - normal (1, 0, 0)
        1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f,
        1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f,
        1f, 1f, 1f, 1f, 1f, 1f, 0f, 0f,
        1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f,
        1f, 1f, 1f, 1f, 1f, 1f, 0f, 0f,
        1f, 0f, 1f, 1f, 0f, 1f, 0f, 0f
    };

    private final Matrix4f modelMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();

    private Shader previewShader;
    private TextureAtlas textureAtlas;
    private boolean initialized;

    private int vao;
    private int vbo;

    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(VERTICES_PER_CUBE * COMPONENTS_PER_VERTEX);
    private final Map<BlockType, float[]> vertexCache = new EnumMap<>(BlockType.class);

    public void init() {
        if (initialized) {
            return;
        }

        previewShader = Shader.loadFromResources("/shaders/block_preview.vert", "/shaders/block_preview.frag");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, VERTICES_PER_CUBE * COMPONENTS_PER_VERTEX * BYTES_PER_FLOAT, GL_DYNAMIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, COMPONENTS_PER_VERTEX * BYTES_PER_FLOAT, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, COMPONENTS_PER_VERTEX * BYTES_PER_FLOAT, 3L * BYTES_PER_FLOAT);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, COMPONENTS_PER_VERTEX * BYTES_PER_FLOAT, 5L * BYTES_PER_FLOAT);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        initialized = true;
        System.out.println("[BlockPreviewRenderer] Initialized");
    }

    public void setTextureAtlas(TextureAtlas textureAtlas) {
        this.textureAtlas = textureAtlas;
    }

    public void renderBlockPreview(BlockType blockType, float x, float y, float size, int windowWidth, int windowHeight) {
        if (!initialized || blockType == null || blockType == BlockType.AIR || textureAtlas == null) {
            return;
        }

        int viewportX = Math.round(x);
        int viewportY = Math.round(windowHeight - (y + size));
        int viewportSize = Math.max(1, Math.round(size));

        boolean wasDepthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean wasCullFace = glIsEnabled(GL_CULL_FACE);
        boolean wasBlend = glIsEnabled(GL_BLEND);
        boolean wasScissor = glIsEnabled(GL_SCISSOR_TEST);

        int[] prevViewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, prevViewport);
        int[] prevScissor = new int[4];
        glGetIntegerv(GL_SCISSOR_BOX, prevScissor);
        int[] prevCullFaceMode = new int[1];
        glGetIntegerv(GL_CULL_FACE_MODE, prevCullFaceMode);

        glDepthMask(false);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_SCISSOR_TEST);

        glViewport(viewportX, viewportY, viewportSize, viewportSize);
        glScissor(viewportX, viewportY, viewportSize, viewportSize);
        glClear(GL_DEPTH_BUFFER_BIT);

        setupMatrices();

        previewShader.bind();
        previewShader.setUniform("uProjection", projectionMatrix);
        previewShader.setUniform("uView", viewMatrix);
        previewShader.setUniform("uModel", modelMatrix.identity());
        previewShader.setUniform("uLightDirection", LIGHT_DIRECTION);
        previewShader.setUniform("uLightColor", LIGHT_COLOR);
        previewShader.setUniform("uAmbientStrength", AMBIENT_STRENGTH);

        textureAtlas.bind(0);
        previewShader.setUniform("uTexture", 0);

        float[] vertexData = vertexCache.computeIfAbsent(blockType, this::buildVertexData);
        vertexBuffer.clear();
        vertexBuffer.put(vertexData).flip();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        glDrawArrays(GL_TRIANGLES, 0, VERTICES_PER_CUBE);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        previewShader.unbind();

        glDepthMask(true);
        glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        if (wasScissor) {
            glScissor(prevScissor[0], prevScissor[1], prevScissor[2], prevScissor[3]);
        } else {
            glDisable(GL_SCISSOR_TEST);
        }

        glCullFace(prevCullFaceMode[0]);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (!wasDepthTest) {
            glDisable(GL_DEPTH_TEST);
        }
        if (!wasCullFace) {
            glDisable(GL_CULL_FACE);
        }
        if (!wasBlend) {
            glDisable(GL_BLEND);
        }
        if (wasScissor) {
            glEnable(GL_SCISSOR_TEST);
        }
    }

    private void setupMatrices() {
        modelMatrix.identity();

        viewMatrix.identity()
            .lookAt(new Vector3f(1.5f, 1.5f, 1.5f), new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(0f, 1f, 0f));

        float halfExtent = 1.0f;
        projectionMatrix.identity().ortho(-halfExtent, halfExtent, -halfExtent, halfExtent, 0.1f, 10f);
    }

    private float[] buildVertexData(BlockType blockType) {
        float[] data = new float[VERTICES_PER_CUBE * COMPONENTS_PER_VERTEX];
        float[] uvs;
        int vertexIndex = 0;

        for (int face = 0; face < 6; face++) {
            uvs = textureAtlas.getUVsForFace(blockType, FACE_MAP[face]);
            float u0 = uvs[0];
            float v0 = uvs[1];
            float u1 = uvs[2];
            float v1 = uvs[3];

            for (int vert = 0; vert < 6; vert++) {
                int baseIndex = (face * 6 + vert) * COMPONENTS_PER_VERTEX;
                data[vertexIndex++] = BASE_CUBE_VERTICES[baseIndex];
                data[vertexIndex++] = BASE_CUBE_VERTICES[baseIndex + 1];
                data[vertexIndex++] = BASE_CUBE_VERTICES[baseIndex + 2];

                float baseU = BASE_CUBE_VERTICES[baseIndex + 3];
                float baseV = BASE_CUBE_VERTICES[baseIndex + 4];
                float tU = baseU == 0f ? u0 : u1;
                float tV = baseV == 0f ? v0 : v1;

                data[vertexIndex++] = tU;
                data[vertexIndex++] = tV;

                data[vertexIndex++] = BASE_CUBE_VERTICES[baseIndex + 5];
                data[vertexIndex++] = BASE_CUBE_VERTICES[baseIndex + 6];
                data[vertexIndex++] = BASE_CUBE_VERTICES[baseIndex + 7];
            }
        }

        return data;
    }

    public void cleanup() {
        if (!initialized) {
            return;
        }

        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        if (previewShader != null) {
            previewShader.cleanup();
        }

        vertexCache.clear();
        initialized = false;
    }
}
