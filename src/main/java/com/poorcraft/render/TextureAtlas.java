package com.poorcraft.render;

import com.poorcraft.world.block.BlockType;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.stb.STBImage.*;

/**
 * Manages a texture atlas that combines multiple 16x16 block textures into a single large texture.
 * 
 * This reduces texture binding overhead and improves rendering performance by allowing
 * all block types to be rendered with a single texture bind per frame.
 * 
 * The atlas is 256x256 pixels, holding up to 256 individual 16x16 textures in a 16x16 grid.
 * UV coordinates are automatically calculated for each texture based on its position in the atlas.
 * 
 * @author PoorCraft Team
 */
public class TextureAtlas {
    
    /** Size of each individual block texture in pixels */
    public static final int TEXTURE_SIZE = 16;
    
    /** Size of the entire atlas texture in pixels */
    public static final int ATLAS_SIZE = 256;
    
    /** Number of textures per row/column in the atlas */
    public static final int TEXTURES_PER_ROW = ATLAS_SIZE / TEXTURE_SIZE; // 16
    
    /** Size of one texture in UV space (0.0 to 1.0) */
    private static final float UV_UNIT = 1.0f / TEXTURES_PER_ROW;
    
    private Texture atlasTexture;
    private Map<String, Integer> textureIndices;
    private ByteBuffer atlasBuffer;
    private int nextIndex;
    
    /**
     * Creates an empty texture atlas.
     */
    public TextureAtlas() {
        this.textureIndices = new HashMap<>();
        this.nextIndex = 0;
        
        // Allocate buffer for the entire atlas (RGBA format)
        this.atlasBuffer = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
    }
    
    /**
     * Adds a texture to the atlas from a resource path.
     * 
     * @param name Texture name (e.g., "stone", "grass_top")
     * @param resourcePath Path to the texture resource
     * @throws RuntimeException if texture loading fails or atlas is full
     */
    public void addTexture(String name, String resourcePath) {
        if (nextIndex >= TEXTURES_PER_ROW * TEXTURES_PER_ROW) {
            throw new RuntimeException("Texture atlas is full! Cannot add texture: " + name);
        }
        
        try {
            // Load texture data using STB
            InputStream inputStream = com.poorcraft.resources.ResourceManager.getInstance()
                    .loadResourceStream(resourcePath);
            
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            
            IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer channelsBuffer = BufferUtils.createIntBuffer(1);
            
            ByteBuffer imageBuffer = stbi_load_from_memory(buffer, widthBuffer, heightBuffer, channelsBuffer, 4);
            
            if (imageBuffer == null) {
                throw new RuntimeException("Failed to load texture: " + resourcePath + " - " + stbi_failure_reason());
            }
            
            int width = widthBuffer.get(0);
            int height = heightBuffer.get(0);
            
            if (width != TEXTURE_SIZE || height != TEXTURE_SIZE) {
                stbi_image_free(imageBuffer);
                throw new RuntimeException("Texture " + name + " must be " + TEXTURE_SIZE + "x" + TEXTURE_SIZE + 
                        " pixels, but is " + width + "x" + height);
            }
            
            addTexture(name, imageBuffer, width, height);
            
            stbi_image_free(imageBuffer);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to add texture " + name + " from " + resourcePath, e);
        }
    }
    
    /**
     * Adds a texture to the atlas from raw RGBA data.
     * 
     * @param name Texture name
     * @param textureData Raw RGBA pixel data
     * @param width Texture width (must be 16)
     * @param height Texture height (must be 16)
     * @throws RuntimeException if dimensions are incorrect or atlas is full
     */
    public void addTexture(String name, ByteBuffer textureData, int width, int height) {
        if (width != TEXTURE_SIZE || height != TEXTURE_SIZE) {
            throw new RuntimeException("Texture must be " + TEXTURE_SIZE + "x" + TEXTURE_SIZE + " pixels");
        }
        
        if (nextIndex >= TEXTURES_PER_ROW * TEXTURES_PER_ROW) {
            throw new RuntimeException("Texture atlas is full!");
        }
        
        // Calculate position in atlas
        int row = nextIndex / TEXTURES_PER_ROW;
        int col = nextIndex % TEXTURES_PER_ROW;
        
        // Copy texture data into atlas buffer
        // This is like a game of Tetris but with pixels
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int atlasX = col * TEXTURE_SIZE + x;
                int atlasY = row * TEXTURE_SIZE + y;
                int atlasIndex = (atlasY * ATLAS_SIZE + atlasX) * 4;
                int textureIndex = (y * TEXTURE_SIZE + x) * 4;
                
                // Copy RGBA bytes
                atlasBuffer.put(atlasIndex, textureData.get(textureIndex));         // R
                atlasBuffer.put(atlasIndex + 1, textureData.get(textureIndex + 1)); // G
                atlasBuffer.put(atlasIndex + 2, textureData.get(textureIndex + 2)); // B
                atlasBuffer.put(atlasIndex + 3, textureData.get(textureIndex + 3)); // A
            }
        }
        
        // Store mapping
        textureIndices.put(name, nextIndex);
        nextIndex++;
    }
    
    /**
     * Finalizes the atlas and uploads it to the GPU.
     * Must be called after all textures have been added.
     */
    public void build() {
        atlasBuffer.position(0);
        atlasTexture = Texture.createFromData(atlasBuffer, ATLAS_SIZE, ATLAS_SIZE);
        
        System.out.println("[TextureAtlas] Built atlas with " + nextIndex + " textures");
    }
    
    /**
     * Gets UV coordinates for a texture by name.
     * 
     * @param textureName Texture name
     * @return Float array [u0, v0, u1, v1] representing min and max UV coordinates
     */
    public float[] getUVs(String textureName) {
        Integer index = textureIndices.get(textureName);
        
        if (index == null) {
            // Return UVs for index 0 as a fallback (missing texture)
            System.err.println("[TextureAtlas] Warning: Texture '" + textureName + "' not found, using fallback");
            index = 0;
        }
        
        // Calculate UV coordinates
        int row = index / TEXTURES_PER_ROW;
        int col = index % TEXTURES_PER_ROW;
        
        float u0 = col * UV_UNIT;
        float v0 = row * UV_UNIT;
        float u1 = u0 + UV_UNIT;
        float v1 = v0 + UV_UNIT;
        
        return new float[]{u0, v0, u1, v1};
    }
    
    /**
     * Gets UV coordinates for a specific block face.
     * 
     * Different block types may have different textures on different faces
     * (e.g., grass has a different texture on top vs sides).
     * 
     * @param blockType Block type
     * @param face Face index (0=top, 1=bottom, 2=north, 3=south, 4=east, 5=west)
     * @return Float array [u0, v0, u1, v1] representing UV coordinates
     */
    public float[] getUVsForFace(BlockType blockType, int face) {
        String textureName;
        
        // Map block type and face to texture name
        // Some blocks are simple (same texture everywhere), others are fancy
        switch (blockType) {
            case GRASS:
                if (face == 0) textureName = "grass_top";      // Top face
                else if (face == 1) textureName = "dirt";      // Bottom face
                else textureName = "grass_side";               // Side faces
                break;
                
            case JUNGLE_GRASS:
                if (face == 0) textureName = "jungle_grass_top";
                else if (face == 1) textureName = "jungle_dirt";
                else textureName = "jungle_grass_side";
                break;
                
            case WOOD:
                if (face == 0 || face == 1) textureName = "wood_top"; // Top and bottom
                else textureName = "wood_side";                       // Sides
                break;
                
            case CACTUS:
                if (face == 0 || face == 1) textureName = "cactus_top";
                else textureName = "cactus_side";
                break;
                
            case DIRT:
                textureName = "dirt";
                break;
                
            case STONE:
                textureName = "stone";
                break;
                
            case SAND:
                textureName = "sand";
                break;
                
            case SANDSTONE:
                textureName = "sandstone";
                break;
                
            case SNOW_BLOCK:
                textureName = "snow_block";
                break;
                
            case ICE:
                textureName = "ice";
                break;
                
            case SNOW_LAYER:
                textureName = "snow_layer";
                break;
                
            case JUNGLE_DIRT:
                textureName = "jungle_dirt";
                break;
                
            case LEAVES:
                textureName = "leaves";
                break;
                
            case BEDROCK:
                textureName = "bedrock";
                break;
                
            default:
                textureName = "missing"; // Magenta/black checkerboard for missing textures
                break;
        }
        
        return getUVs(textureName);
    }
    
    /**
     * Gets the atlas texture.
     * 
     * @return Atlas Texture instance
     */
    public Texture getTexture() {
        return atlasTexture;
    }
    
    /**
     * Binds the atlas texture.
     */
    public void bind() {
        if (atlasTexture != null) {
            atlasTexture.bind();
        }
    }
    
    /**
     * Binds the atlas texture to a specific texture unit.
     * 
     * @param unit Texture unit (0-31)
     */
    public void bind(int unit) {
        if (atlasTexture != null) {
            atlasTexture.bind(unit);
        }
    }
    
    /**
     * Cleans up the atlas texture.
     */
    public void cleanup() {
        if (atlasTexture != null) {
            atlasTexture.cleanup();
        }
    }
    
    /**
     * Creates a texture atlas with all default block textures.
     * 
     * @return Fully built TextureAtlas instance
     */
    public static TextureAtlas createDefault() {
        TextureAtlas atlas = new TextureAtlas();
        
        // Add a missing texture placeholder first (index 0)
        atlas.addMissingTexture();
        
        // Add all block textures
        // If textures don't exist yet, they'll fall back to the missing texture
        try {
            atlas.addTexture("dirt", "/textures/blocks/dirt.png");
            atlas.addTexture("stone", "/textures/blocks/stone.png");
            atlas.addTexture("bedrock", "/textures/blocks/bedrock.png");
            atlas.addTexture("grass_top", "/textures/blocks/grass_top.png");
            atlas.addTexture("grass_side", "/textures/blocks/grass_side.png");
            atlas.addTexture("sand", "/textures/blocks/sand.png");
            atlas.addTexture("sandstone", "/textures/blocks/sandstone.png");
            atlas.addTexture("cactus_top", "/textures/blocks/cactus_top.png");
            atlas.addTexture("cactus_side", "/textures/blocks/cactus_side.png");
            atlas.addTexture("snow_block", "/textures/blocks/snow_block.png");
            atlas.addTexture("ice", "/textures/blocks/ice.png");
            atlas.addTexture("snow_layer", "/textures/blocks/snow_layer.png");
            atlas.addTexture("jungle_grass_top", "/textures/blocks/jungle_grass_top.png");
            atlas.addTexture("jungle_grass_side", "/textures/blocks/jungle_grass_side.png");
            atlas.addTexture("jungle_dirt", "/textures/blocks/jungle_dirt.png");
            atlas.addTexture("wood_top", "/textures/blocks/wood_top.png");
            atlas.addTexture("wood_side", "/textures/blocks/wood_side.png");
            atlas.addTexture("leaves", "/textures/blocks/leaves.png");
        } catch (Exception e) {
            System.err.println("[TextureAtlas] Warning: Some textures failed to load, using placeholders");
            System.err.println("[TextureAtlas] Error: " + e.getMessage());
        }
        
        atlas.build();
        return atlas;
    }
    
    /**
     * Adds a magenta/black checkerboard pattern as a missing texture indicator.
     * This is what you see in games when textures fail to load - the classic "oops" texture.
     */
    private void addMissingTexture() {
        ByteBuffer missingTexture = BufferUtils.createByteBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4);
        
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                // Checkerboard pattern: alternate between magenta and black
                boolean isMagenta = ((x / 4) + (y / 4)) % 2 == 0;
                
                if (isMagenta) {
                    missingTexture.put((byte) 255); // R
                    missingTexture.put((byte) 0);   // G
                    missingTexture.put((byte) 255); // B
                    missingTexture.put((byte) 255); // A
                } else {
                    missingTexture.put((byte) 0);   // R
                    missingTexture.put((byte) 0);   // G
                    missingTexture.put((byte) 0);   // B
                    missingTexture.put((byte) 255); // A
                }
            }
        }
        
        missingTexture.flip();
        addTexture("missing", missingTexture, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
