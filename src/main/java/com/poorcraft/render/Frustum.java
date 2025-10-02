package com.poorcraft.render;

import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import org.joml.Matrix4f;

/**
 * Frustum culling utility for efficient chunk rendering.
 * 
 * A frustum is a truncated pyramid representing the visible volume in 3D space.
 * It has 6 planes: near, far, left, right, top, bottom.
 * 
 * This class extracts frustum planes from the view-projection matrix using the
 * Gribb-Hartmann method and tests AABBs (axis-aligned bounding boxes) against them.
 * 
 * Frustum culling can skip rendering 50-80% of chunks depending on FOV and view distance.
 * 
 * @author PoorCraft Team
 */
public class Frustum {
    
    // 6 planes, each with 4 components [a, b, c, d] where ax + by + cz + d = 0
    private final float[][] planes;
    
    // Plane indices
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    private static final int NEAR = 4;
    private static final int FAR = 5;
    
    /**
     * Creates a new frustum.
     */
    public Frustum() {
        this.planes = new float[6][4];
    }
    
    /**
     * Updates frustum planes from the view-projection matrix.
     * Uses the Gribb-Hartmann plane extraction method.
     * 
     * @param viewProjection Combined view-projection matrix
     */
    public void update(Matrix4f viewProjection) {
        // Extract matrix elements
        // I don't fully understand the math here, but it works
        // Something about homogeneous coordinates and clip space... yeah
        float[] m = new float[16];
        viewProjection.get(m);
        
        // Extract planes using Gribb-Hartmann method
        // Left plane: m[3] + m[0], m[7] + m[4], m[11] + m[8], m[15] + m[12]
        planes[LEFT][0] = m[3] + m[0];
        planes[LEFT][1] = m[7] + m[4];
        planes[LEFT][2] = m[11] + m[8];
        planes[LEFT][3] = m[15] + m[12];
        
        // Right plane: m[3] - m[0], m[7] - m[4], m[11] - m[8], m[15] - m[12]
        planes[RIGHT][0] = m[3] - m[0];
        planes[RIGHT][1] = m[7] - m[4];
        planes[RIGHT][2] = m[11] - m[8];
        planes[RIGHT][3] = m[15] - m[12];
        
        // Bottom plane: m[3] + m[1], m[7] + m[5], m[11] + m[9], m[15] + m[13]
        planes[BOTTOM][0] = m[3] + m[1];
        planes[BOTTOM][1] = m[7] + m[5];
        planes[BOTTOM][2] = m[11] + m[9];
        planes[BOTTOM][3] = m[15] + m[13];
        
        // Top plane: m[3] - m[1], m[7] - m[5], m[11] - m[9], m[15] - m[13]
        planes[TOP][0] = m[3] - m[1];
        planes[TOP][1] = m[7] - m[5];
        planes[TOP][2] = m[11] - m[9];
        planes[TOP][3] = m[15] - m[13];
        
        // Near plane: m[3] + m[2], m[7] + m[6], m[11] + m[10], m[15] + m[14]
        planes[NEAR][0] = m[3] + m[2];
        planes[NEAR][1] = m[7] + m[6];
        planes[NEAR][2] = m[11] + m[10];
        planes[NEAR][3] = m[15] + m[14];
        
        // Far plane: m[3] - m[2], m[7] - m[6], m[11] - m[10], m[15] - m[14]
        planes[FAR][0] = m[3] - m[2];
        planes[FAR][1] = m[7] - m[6];
        planes[FAR][2] = m[11] - m[10];
        planes[FAR][3] = m[15] - m[14];
        
        // Normalize all planes
        for (int i = 0; i < 6; i++) {
            normalizePlane(i);
        }
    }
    
    /**
     * Normalizes a plane equation.
     * Divides [a, b, c, d] by sqrt(a² + b² + c²).
     * 
     * @param planeIndex Plane index (0-5)
     */
    private void normalizePlane(int planeIndex) {
        float a = planes[planeIndex][0];
        float b = planes[planeIndex][1];
        float c = planes[planeIndex][2];
        float d = planes[planeIndex][3];
        
        float length = (float) Math.sqrt(a * a + b * b + c * c);
        
        if (length > 0.0f) {
            planes[planeIndex][0] /= length;
            planes[planeIndex][1] /= length;
            planes[planeIndex][2] /= length;
            planes[planeIndex][3] /= length;
        }
    }
    
    /**
     * Tests if an AABB (axis-aligned bounding box) intersects the frustum.
     * 
     * This is a conservative test - it may return true for boxes that are
     * actually outside the frustum (false positives), but will never return
     * false for boxes that are inside (no false negatives).
     * 
     * @param minX Minimum X coordinate
     * @param minY Minimum Y coordinate
     * @param minZ Minimum Z coordinate
     * @param maxX Maximum X coordinate
     * @param maxY Maximum Y coordinate
     * @param maxZ Maximum Z coordinate
     * @return true if AABB is inside or intersecting frustum
     */
    public boolean testAABB(float minX, float minY, float minZ, 
                            float maxX, float maxY, float maxZ) {
        // Test against all 6 planes
        for (int i = 0; i < 6; i++) {
            float a = planes[i][0];
            float b = planes[i][1];
            float c = planes[i][2];
            float d = planes[i][3];
            
            // Find the "positive vertex" - the corner of the AABB furthest
            // in the direction of the plane's normal
            float px = (a >= 0) ? maxX : minX;
            float py = (b >= 0) ? maxY : minY;
            float pz = (c >= 0) ? maxZ : minZ;
            
            // Calculate signed distance from plane
            float distance = a * px + b * py + c * pz + d;
            
            // If the positive vertex is outside this plane, the entire AABB is outside
            if (distance < 0) {
                return false;
            }
        }
        
        // AABB passed all plane tests - it's inside or intersecting the frustum
        return true;
    }
    
    /**
     * Tests if a chunk is visible in the frustum.
     * 
     * @param chunk Chunk to test
     * @return true if chunk is visible
     */
    public boolean testChunk(Chunk chunk) {
        ChunkPos pos = chunk.getPosition();
        
        // Calculate chunk AABB in world space
        float minX = pos.x * Chunk.CHUNK_SIZE;
        float minY = 0;
        float minZ = pos.z * Chunk.CHUNK_SIZE;
        
        float maxX = minX + Chunk.CHUNK_SIZE;
        float maxY = Chunk.CHUNK_HEIGHT;
        float maxZ = minZ + Chunk.CHUNK_SIZE;
        
        return testAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
