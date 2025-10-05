package com.poorcraft.render;

import com.poorcraft.config.Settings;
import com.poorcraft.modding.ModLoader;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkMesh;
import com.poorcraft.world.chunk.ChunkPos;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;

/**
 * Main chunk rendering system that orchestrates rendering all visible chunks.
 * 
 * This class manages:
 * - Shader programs and texture atlases
 * - Per-chunk GPU buffers (VAO/VBO/EBO)
 * - Frustum culling to skip invisible chunks
 * - Lighting uniforms
 * - OpenGL state management
 * 
 * The rendering pipeline:
 * 1. Update frustum from view-projection matrix
 * 2. Bind shader and set global uniforms (matrices, lighting)
 * 3. For each chunk:
 *    a. Test against frustum (cull if outside)
 *    b. Update mesh if dirty
 *    c. Set per-chunk uniforms (model matrix)
 *    d. Render
 * 
 * @author PoorCraft Team
 */
public class ChunkRenderer {

    // Fog defaults - overridden by settings when available
    private static final Vector3f DEFAULT_FOG_COLOR = new Vector3f(0.74f, 0.84f, 0.93f);
    private static final float DEFAULT_FOG_START = 48.0f;
    private static final float DEFAULT_FOG_END = 96.0f;

    private Shader blockShader;
    private TextureAtlas textureAtlas;
    private ModLoader modLoader;
    private Settings settings;
    private SunLight sunLight;
    private Map<ChunkPos, ChunkRenderData> chunkRenderData;
    private Frustum frustum;
    private Matrix4f modelMatrix;
    private Matrix4f viewProjectionMatrix;
    private GPUCapabilities gpuCaps;
    private PerformanceMonitor perfMon;
    private UniformBufferObject ubo;
    private boolean useUBO;
    
    // Lighting configuration
    // These values create a nice warm sunlight with cool ambient lighting
    private static final Vector3f DEFAULT_LIGHT_DIRECTION = new Vector3f(0.3f, -0.7f, 0.5f).normalize();
    private static final Vector3f LIGHT_COLOR = new Vector3f(1.0f, 0.95f, 0.8f);
    private static final Vector3f AMBIENT_COLOR = new Vector3f(0.6f, 0.7f, 0.8f);
    private static final float AMBIENT_STRENGTH = 0.4f;

    /**
     * Creates a new chunk renderer.
     */
    public ChunkRenderer() {
        this.chunkRenderData = new ConcurrentHashMap<>();
        this.modelMatrix = new Matrix4f();
        this.viewProjectionMatrix = new Matrix4f();
        this.sunLight = new SunLight();
    }

    /**
     * Sets the mod loader reference so we can sneak in procedural textures before rendering starts.
     *
     * @param modLoader Active mod loader
     */
    public void setModLoader(ModLoader modLoader) {
        this.modLoader = modLoader;
    }
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public void setSunDirection(Vector3f direction) {
        if (direction != null) {
            sunLight.setDirection(direction);
        }
    }

    public Vector3f getSunDirection() {
        return sunLight.getDirection();
    }

    public void setSunLight(SunLight sunLight) {
        this.sunLight = sunLight;
    }

    public SunLight getSunLight() {
        return sunLight;
    }

    public void setGPUCapabilities(GPUCapabilities caps) {
        this.gpuCaps = caps;
    }

    public void setPerformanceMonitor(PerformanceMonitor monitor) {
        this.perfMon = monitor;
    }

    /**
     * Loads shaders, creates texture atlas, and sets up rendering resources.
     */
    public void init() {
        System.out.println("[ChunkRenderer] Initializing...");
        
        // Load and compile shaders
        boolean supportsUBO = gpuCaps != null && gpuCaps.supportsUniformBufferObjects();
        useUBO = supportsUBO;
        blockShader = supportsUBO
            ? Shader.loadFromResources("/shaders/block.vert", "/shaders/block.frag", "USE_UBO")
            : Shader.loadFromResources("/shaders/block.vert", "/shaders/block.frag");
        System.out.println("[ChunkRenderer] Shaders compiled successfully");

        ubo = new UniformBufferObject(0);
        if (supportsUBO) {
            try {
                ubo.init();
            } catch (Exception ex) {
                System.err.println("[ChunkRenderer] Failed to initialise UBO, falling back to legacy uniforms: " + ex.getMessage());
                useUBO = false;
            }
        }
        
        Map<String, ByteBuffer> generatedTextures = TextureGenerator.ensureDefaultBlockTextures();
        TextureGenerator.ensureAuxiliaryTextures();

        textureAtlas = new TextureAtlas();
        addMissingTexturePlaceholder(textureAtlas);

        int bakedCount = 0;
        if (generatedTextures != null) {
            for (Map.Entry<String, ByteBuffer> entry : generatedTextures.entrySet()) {
                if (entry.getValue() != null) {
                    textureAtlas.addTexture(entry.getKey(), entry.getValue(), TextureAtlas.TEXTURE_SIZE, TextureAtlas.TEXTURE_SIZE);
                    bakedCount++;
                }
            }
        }

        Map<String, ByteBuffer> modTextures = modLoader != null && modLoader.getModAPI() != null
            ? modLoader.getModAPI().getProceduralTextures()
            : Map.of();

        int modCount = 0;
        for (Map.Entry<String, ByteBuffer> entry : modTextures.entrySet()) {
            textureAtlas.addTexture(entry.getKey(), entry.getValue(), TextureAtlas.TEXTURE_SIZE, TextureAtlas.TEXTURE_SIZE);
            modCount++;
        }

        textureAtlas.build();
        System.out.println("[ChunkRenderer] Built texture atlas with " + bakedCount + " generated textures and " + modCount + " mod textures");
        
        // Create frustum
        frustum = new Frustum();
        
        System.out.println("[ChunkRenderer] Initialization complete");
    }

    private void addMissingTexturePlaceholder(TextureAtlas atlas) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(TextureAtlas.TEXTURE_SIZE * TextureAtlas.TEXTURE_SIZE * 4);

        for (int y = 0; y < TextureAtlas.TEXTURE_SIZE; y++) {
            for (int x = 0; x < TextureAtlas.TEXTURE_SIZE; x++) {
                boolean magenta = ((x / 4) + (y / 4)) % 2 == 0;

                if (magenta) {
                    buffer.put((byte) 255);
                    buffer.put((byte) 0);
                    buffer.put((byte) 255);
                    buffer.put((byte) 255);
                } else {
                    buffer.put((byte) 0);
                    buffer.put((byte) 0);
                    buffer.put((byte) 0);
                    buffer.put((byte) 255);
                }
            }
        }

        buffer.flip();
        atlas.addTexture("missing", buffer, TextureAtlas.TEXTURE_SIZE, TextureAtlas.TEXTURE_SIZE);
        // I don't know what is going on here but it's working, just like those Minecraft alpha days.
    }
    
    /**
     * Renders all visible chunks.
     * 
     * @param chunks Collection of chunks to render
     * @param view View matrix from camera
     * @param projection Projection matrix from camera
     */
    public void render(Collection<Chunk> chunks, Matrix4f view, Matrix4f projection) {
        List<Chunk> chunkSnapshot = new ArrayList<>(chunks);

        // Enable depth testing and face culling
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Enable blending for transparent blocks (leaves, ice, etc.)
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Bind shader and set global uniforms
        blockShader.bind();
        if (useUBO && ubo != null) {
            ubo.updateMatrices(projection, view);
        } else {
            blockShader.setUniform("uProjection", projection);
            blockShader.setUniform("uView", view);
        }
        
        // Bind texture atlas
        textureAtlas.bind(0);
        blockShader.setUniform("uTexture", 0);
        
        // Set lighting uniforms
        Vector3f lightDir = sunLight != null ? sunLight.getDirection() : DEFAULT_LIGHT_DIRECTION;
        Vector3f lightColor = new Vector3f(LIGHT_COLOR);
        if (sunLight != null) {
            lightColor.mul(sunLight.getColor());
            lightColor.mul(sunLight.getIntensity());
        }
        if (useUBO && ubo != null) {
            ubo.updateLighting(lightDir, lightColor, AMBIENT_COLOR, AMBIENT_STRENGTH);
        } else {
            blockShader.setUniform("uLightDirection", lightDir);
            blockShader.setUniform("uLightColor", lightColor);
            blockShader.setUniform("uAmbientColor", AMBIENT_COLOR);
            blockShader.setUniform("uAmbientStrength", AMBIENT_STRENGTH);
        }

        Vector3f fogColor = new Vector3f(DEFAULT_FOG_COLOR);
        float fogStart = DEFAULT_FOG_START;
        float fogEnd = DEFAULT_FOG_END;

        if (settings != null && settings.graphics != null) {
            float renderDistanceChunks = settings.graphics.renderDistance > 0
                ? settings.graphics.renderDistance
                : DEFAULT_FOG_END / Chunk.CHUNK_SIZE;
            float chunkSize = Chunk.CHUNK_SIZE;
            float maxDistance = Math.max(32f, renderDistanceChunks * chunkSize);

            if (settings.graphics.fogEnd > 0f) {
                fogEnd = settings.graphics.fogEnd;
                fogStart = settings.graphics.fogStart > 0f ? settings.graphics.fogStart : fogEnd * 0.6f;
            } else {
                fogEnd = maxDistance * 0.95f;
                fogStart = fogEnd * 0.55f;
            }

            if (settings.graphics.fogColor != null && settings.graphics.fogColor.length >= 3) {
                fogColor.set(
                    settings.graphics.fogColor[0],
                    settings.graphics.fogColor[1],
                    settings.graphics.fogColor[2]
                );
            }
        }

        if (useUBO && ubo != null) {
            ubo.updateFog(fogColor, fogStart, fogEnd);
            ubo.bind();
        } else {
            blockShader.setUniform("uFogColor", fogColor);
            blockShader.setUniform("uFogStart", fogStart);
            blockShader.setUniform("uFogEnd", fogEnd);
        }
        
        // Update frustum for culling
        projection.mul(view, viewProjectionMatrix);
        frustum.update(viewProjectionMatrix);
        
        // Render chunks
        for (Chunk chunk : chunkSnapshot) {
            // Frustum culling - skip chunks outside view
            if (!frustum.testChunk(chunk)) {
                continue;
            }

            // Get or create render data for this chunk
            ChunkRenderData renderData = getOrCreateRenderData(chunk);
            
            // Update mesh if dirty and upload only when version changed
            // This prevents redundant GPU uploads every frame! Huge performance win.
            ChunkMesh chunkMesh = chunk.getMesh(textureAtlas);
            if (chunkMesh != null && renderData.needsUpload(chunk.getMeshVersion())) {
                renderData.uploadMesh(chunkMesh, chunk.getMeshVersion());
            }
            
            // Set model matrix (chunk world position)
            modelMatrix.identity().translate(
                chunk.getPosition().x * Chunk.CHUNK_SIZE,
                0,
                chunk.getPosition().z * Chunk.CHUNK_SIZE
            );
            blockShader.setUniform("uModel", modelMatrix);
            
            // Render chunk
            renderData.render();
        }
        
        // Unbind shader
        blockShader.unbind();
        
        // Disable blending
        glDisable(GL_BLEND);
        
        // Optional: Log rendering stats (can be verbose, commented out by default)
        // System.out.println("[ChunkRenderer] Rendered: " + renderedChunks + ", Culled: " + culledChunks);
    }
    
    /**
     * Gets or creates render data for a chunk.
     * 
     * @param chunk Chunk to get render data for
     * @return ChunkRenderData instance
     */
    private ChunkRenderData getOrCreateRenderData(Chunk chunk) {
        ChunkPos pos = chunk.getPosition();
        return chunkRenderData.computeIfAbsent(pos, ignored -> new ChunkRenderData(gpuCaps, perfMon));
    }
    
    /**
     * Called when a chunk is unloaded.
     * Cleans up GPU resources for that chunk.
     * 
     * @param pos Position of unloaded chunk
     */
    public void onChunkUnloaded(ChunkPos pos) {
        ChunkRenderData renderData = chunkRenderData.remove(pos);
        if (renderData != null) {
            renderData.cleanup();
        }
    }

    /**
     * Cleans up all rendering resources.
     * Should be called when shutting down.
     */
    public void cleanup() {
        System.out.println("[ChunkRenderer] Cleaning up...");
        
        // Cleanup shader
        if (blockShader != null) {
            blockShader.cleanup();
        }

        if (ubo != null) {
            ubo.cleanup();
            ubo = null;
        }

        // Cleanup texture atlas
        if (textureAtlas != null) {
            textureAtlas.cleanup();
        }
        for (ChunkRenderData renderData : chunkRenderData.values()) {
            renderData.cleanup();
        }
        chunkRenderData.clear();
        
        System.out.println("[ChunkRenderer] Cleanup complete");
    }
    
    /**
     * Gets the number of chunks with render data.
     * Useful for debug displays.
     * 
     * @return Number of chunks with GPU buffers
     */
    public int getRenderedChunkCount() {
        return chunkRenderData.size();
    }
    
    /**
     * Gets the texture atlas.
     * Useful for future systems (e.g., UI rendering).
     * 
     * @return Texture atlas instance
     */
    public TextureAtlas getTextureAtlas() {
        return textureAtlas;
    }
}
