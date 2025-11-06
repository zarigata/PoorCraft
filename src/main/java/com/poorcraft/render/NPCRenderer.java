package com.poorcraft.render;

import com.poorcraft.camera.Camera;
import com.poorcraft.player.SkinAtlas;
import com.poorcraft.world.entity.NPCEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderer for NPC entities using billboard technique with player skins.
 */
public class NPCRenderer {

    private Shader shader;
    private SkinAtlas skinAtlas;
    private int vao;
    private int vbo;
    private boolean initialized;

    public void init() {
        if (initialized) {
            return;
        }

        shader = Shader.loadFromResources("/shaders/npc.vert", "/shaders/npc.frag");

        // Create billboard quad vertices (position + texCoord)
        // Billboard is 0.6 wide x 1.8 tall to match player dimensions
        float[] quadVertices = {
            -0.3f, 0.0f,  0.0f, 0.0f,  // Bottom-left
             0.3f, 0.0f,  1.0f, 0.0f,  // Bottom-right
             0.3f, 1.8f,  1.0f, 1.0f,  // Top-right

            -0.3f, 0.0f,  0.0f, 0.0f,  // Bottom-left
             0.3f, 1.8f,  1.0f, 1.0f,  // Top-right
            -0.3f, 1.8f,  0.0f, 1.0f   // Top-left
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);

        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // TexCoord attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        initialized = true;
        System.out.println("[NPCRenderer] Initialized");
    }

    public void setSkinAtlas(SkinAtlas skinAtlas) {
        this.skinAtlas = skinAtlas;
    }

    public void render(List<NPCEntity> npcs, Camera camera, Matrix4f view, Matrix4f projection) {
        if (!initialized || npcs == null || npcs.isEmpty() || shader == null || skinAtlas == null || camera == null) {
            return;
        }

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        shader.bind();
        shader.setUniform("uView", view);
        shader.setUniform("uProjection", projection);

        glBindVertexArray(vao);

        Vector3f cameraRight = camera.getRight();
        Vector3f cameraFront = camera.getFront();
        Vector3f cameraUp = new Vector3f(cameraRight).cross(cameraFront).normalize();
        cameraRight.normalize();

        for (NPCEntity npc : npcs) {
            try {
                Vector3f position = npc.getRenderPosition();
                String skinName = npc.getSkinName();

                // Get skin texture ID from atlas
                int textureId = skinAtlas.getTextureId(skinName);
                if (textureId <= 0) {
                    textureId = skinAtlas.getTextureId("steve"); // Fallback to steve
                }
                
                if (textureId > 0) {
                    glActiveTexture(GL_TEXTURE0);
                    glBindTexture(GL_TEXTURE_2D, textureId);
                    shader.setUniform("uTexture", 0);
                    
                    // Use full texture coordinates
                    float[] uvRect = new float[]{0.0f, 0.0f, 1.0f, 1.0f};

                    shader.setUniform("uPosition", position);
                    shader.setUniform("uCameraRight", cameraRight);
                    shader.setUniform("uCameraUp", cameraUp);
                    shader.setUniform("uUVRect", uvRect[0], uvRect[1], uvRect[2], uvRect[3]);

                    glDrawArrays(GL_TRIANGLES, 0, 6);
                }
            } catch (Exception e) {
                System.err.println("[NPCRenderer] Error rendering NPC #" + npc.getId() + ": " + e.getMessage());
            }
        }

        glBindVertexArray(0);
        shader.unbind();

        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
    }

    public void cleanup() {
        if (!initialized) {
            return;
        }
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        if (shader != null) {
            shader.cleanup();
        }
        initialized = false;
        System.out.println("[NPCRenderer] Cleaned up");
    }
}
