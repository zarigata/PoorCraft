package com.poorcraft.render;

import com.poorcraft.core.MiningSystem;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders an outline and translucent overlay over the block currently targeted for mining.
 */
public class BlockHighlightRenderer {

    private static final float[] FILL_VERTICES = {
        // position (x,y,z)      local (lx,ly,lz)
        // Top face (y = 1)
        0f, 1f, 0f,             0f, 1f, 0f,
        1f, 1f, 0f,             1f, 1f, 0f,
        1f, 1f, 1f,             1f, 1f, 1f,
        0f, 1f, 0f,             0f, 1f, 0f,
        1f, 1f, 1f,             1f, 1f, 1f,
        0f, 1f, 1f,             0f, 1f, 1f,

        // Bottom face (y = 0)
        0f, 0f, 0f,             0f, 0f, 0f,
        1f, 0f, 1f,             1f, 0f, 1f,
        1f, 0f, 0f,             1f, 0f, 0f,
        0f, 0f, 0f,             0f, 0f, 0f,
        0f, 0f, 1f,             0f, 0f, 1f,
        1f, 0f, 1f,             1f, 0f, 1f,

        // Front face (z = 1)
        0f, 0f, 1f,             0f, 0f, 1f,
        1f, 1f, 1f,             1f, 1f, 1f,
        1f, 0f, 1f,             1f, 0f, 1f,
        0f, 0f, 1f,             0f, 0f, 1f,
        0f, 1f, 1f,             0f, 1f, 1f,
        1f, 1f, 1f,             1f, 1f, 1f,

        // Back face (z = 0)
        0f, 0f, 0f,             0f, 0f, 0f,
        1f, 0f, 0f,             1f, 0f, 0f,
        1f, 1f, 0f,             1f, 1f, 0f,
        0f, 0f, 0f,             0f, 0f, 0f,
        1f, 1f, 0f,             1f, 1f, 0f,
        0f, 1f, 0f,             0f, 1f, 0f,

        // Left face (x = 0)
        0f, 0f, 0f,             0f, 0f, 0f,
        0f, 1f, 1f,             0f, 1f, 1f,
        0f, 1f, 0f,             0f, 1f, 0f,
        0f, 0f, 0f,             0f, 0f, 0f,
        0f, 0f, 1f,             0f, 0f, 1f,
        0f, 1f, 1f,             0f, 1f, 1f,

        // Right face (x = 1)
        1f, 0f, 0f,             1f, 0f, 0f,
        1f, 1f, 0f,             1f, 1f, 0f,
        1f, 1f, 1f,             1f, 1f, 1f,
        1f, 0f, 0f,             1f, 0f, 0f,
        1f, 1f, 1f,             1f, 1f, 1f,
        1f, 0f, 1f,             1f, 0f, 1f
    };

    private static final float[] LINE_VERTICES = {
        // Bottom square
        0f, 0f, 0f,
        1f, 0f, 0f,

        1f, 0f, 0f,
        1f, 0f, 1f,

        1f, 0f, 1f,
        0f, 0f, 1f,

        0f, 0f, 1f,
        0f, 0f, 0f,

        // Top square
        0f, 1f, 0f,
        1f, 1f, 0f,

        1f, 1f, 0f,
        1f, 1f, 1f,

        1f, 1f, 1f,
        0f, 1f, 1f,

        0f, 1f, 1f,
        0f, 1f, 0f,

        // Vertical lines
        0f, 0f, 0f,
        0f, 1f, 0f,

        1f, 0f, 0f,
        1f, 1f, 0f,

        1f, 0f, 1f,
        1f, 1f, 1f,

        0f, 0f, 1f,
        0f, 1f, 1f
    };

    private final Matrix4f modelMatrix = new Matrix4f();

    private Shader overlayShader;
    private int fillVao;
    private int fillVbo;
    private int lineVao;
    private int lineVbo;

    public void init() {
        overlayShader = Shader.loadFromResources("/shaders/block_overlay.vert", "/shaders/block_overlay.frag");

        fillVao = glGenVertexArrays();
        fillVbo = glGenBuffers();
        glBindVertexArray(fillVao);
        glBindBuffer(GL_ARRAY_BUFFER, fillVbo);
        glBufferData(GL_ARRAY_BUFFER, FILL_VERTICES, GL_STATIC_DRAW);
        int stride = 6 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);

        lineVao = glGenVertexArrays();
        lineVbo = glGenBuffers();
        glBindVertexArray(lineVao);
        glBindBuffer(GL_ARRAY_BUFFER, lineVbo);
        glBufferData(GL_ARRAY_BUFFER, LINE_VERTICES, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void render(MiningSystem.Target target, float progress, Matrix4f view, Matrix4f projection) {
        if (target == null || overlayShader == null) {
            return;
        }

        boolean wasCullEnabled = glIsEnabled(GL_CULL_FACE);
        boolean wasBlendEnabled = glIsEnabled(GL_BLEND);
        boolean wasPolygonOffsetFillEnabled = glIsEnabled(GL_POLYGON_OFFSET_FILL);
        boolean wasDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        if (!wasPolygonOffsetFillEnabled) {
            glEnable(GL_POLYGON_OFFSET_FILL);
        }
        glPolygonOffset(-1.0f, -1.0f);

        overlayShader.bind();
        overlayShader.setUniform("uView", view);
        overlayShader.setUniform("uProjection", projection);

        modelMatrix.identity()
            .translate(target.getX(), target.getY(), target.getZ())
            .scale(1.005f); // Slightly expanded to reduce z-fighting
        overlayShader.setUniform("uModel", modelMatrix);
        overlayShader.setUniform("uProgress", progress);

        // Draw translucent fill
        overlayShader.setUniform("uIsOutline", false);
        glBindVertexArray(fillVao);
        glDrawArrays(GL_TRIANGLES, 0, FILL_VERTICES.length / 6);

        // Draw outline
        overlayShader.setUniform("uIsOutline", true);
        glLineWidth(2.0f);
        glBindVertexArray(lineVao);
        glDrawArrays(GL_LINES, 0, LINE_VERTICES.length / 3);
        glLineWidth(1.0f);

        glBindVertexArray(0);
        overlayShader.unbind();

        glPolygonOffset(0f, 0f);
        if (wasPolygonOffsetFillEnabled) {
            glEnable(GL_POLYGON_OFFSET_FILL);
        } else {
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
        glDepthMask(wasDepthMask);
        if (wasCullEnabled) {
            glEnable(GL_CULL_FACE);
        } else {
            glDisable(GL_CULL_FACE);
        }
        if (wasBlendEnabled) {
            glEnable(GL_BLEND);
        } else {
            glDisable(GL_BLEND);
        }
    }

    public void cleanup() {
        glDeleteVertexArrays(fillVao);
        glDeleteBuffers(fillVbo);
        glDeleteVertexArrays(lineVao);
        glDeleteBuffers(lineVbo);
        if (overlayShader != null) {
            overlayShader.cleanup();
        }
    }
}
