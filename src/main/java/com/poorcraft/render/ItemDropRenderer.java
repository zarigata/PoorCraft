package com.poorcraft.render;

import com.poorcraft.camera.Camera;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.entity.ItemDrop;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Lightweight renderer for item drops.
 */
public class ItemDropRenderer {

    private static final float QUAD_SIZE = 0.4f;

    private Shader dropShader;
    private TextureAtlas textureAtlas;
    private int vao;
    private int vbo;
    private boolean initialized;

    public void init() {
        if (initialized) {
            return;
        }

        dropShader = Shader.loadFromResources("/shaders/item_drop.vert", "/shaders/item_drop.frag");

        float[] quadVertices = {
            -0.5f, 0.0f, 0.0f, 0.0f,
             0.5f, 0.0f, 1.0f, 0.0f,
             0.5f, 1.0f, 1.0f, 1.0f,

            -0.5f, 0.0f, 0.0f, 0.0f,
             0.5f, 1.0f, 1.0f, 1.0f,
            -0.5f, 1.0f, 0.0f, 1.0f
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        initialized = true;
    }

    public void setTextureAtlas(TextureAtlas textureAtlas) {
        this.textureAtlas = textureAtlas;
    }

    public void render(List<ItemDrop> drops, Camera camera, Matrix4f view, Matrix4f projection) {
        if (!initialized || drops == null || drops.isEmpty() || dropShader == null || textureAtlas == null || camera == null) {
            return;
        }

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        dropShader.bind();
        dropShader.setUniform("uView", view);
        dropShader.setUniform("uProjection", projection);

        textureAtlas.bind(0);
        dropShader.setUniform("uTexture", 0);

        glBindVertexArray(vao);

        Vector3f position = new Vector3f();
        Vector3f cameraRight = camera.getRight();
        Vector3f cameraFront = camera.getFront();
        Vector3f cameraUp = new Vector3f(cameraRight).cross(cameraFront).normalize();
        cameraRight.normalize();

        for (ItemDrop drop : drops) {
            BlockType blockType = drop.getBlockType();
            if (blockType == null || blockType == BlockType.AIR) {
                continue;
            }

            float[] uv = textureAtlas.getUVsForFace(blockType, 0);
            dropShader.setUniform("uUVRect", uv[0], uv[1], uv[2], uv[3]);

            position.set(drop.getOriginX(), drop.getRenderY(), drop.getOriginZ());
            dropShader.setUniform("uPosition", position);
            dropShader.setUniform("uCameraRight", cameraRight);
            dropShader.setUniform("uCameraUp", cameraUp);
            dropShader.setUniform("uSize", QUAD_SIZE);

            glDrawArrays(GL_TRIANGLES, 0, 6);
        }

        glBindVertexArray(0);
        dropShader.unbind();

        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
    }

    public void cleanup() {
        if (!initialized) {
            return;
        }
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        if (dropShader != null) {
            dropShader.cleanup();
        }
        initialized = false;
    }
}
