package com.poorcraft.test;

import com.poorcraft.test.util.TestUtils;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkNeighborUpdateTest {

    @Test
    @DisplayName("Placing block on chunk edge marks neighbor mesh dirty")
    void testEdgeBlockPlacementMarksNeighborDirty() {
        World world = TestUtils.createTestWorld(42L);

        Chunk center = world.getOrCreateChunk(new ChunkPos(0, 0));
        Chunk east = world.getOrCreateChunk(new ChunkPos(1, 0));

        setMeshDirty(center, false);
        setMeshDirty(east, false);

        int worldY = 64;
        world.setBlock(15, worldY, 8, BlockType.STONE);

        assertTrue(isMeshDirty(center), "Center chunk should be dirty after edge placement");
        assertTrue(isMeshDirty(east), "East neighbor should be dirty after edge placement");
    }

    @Test
    @DisplayName("Breaking block on chunk edge marks neighbor mesh dirty")
    void testEdgeBlockRemovalMarksNeighborDirty() {
        World world = TestUtils.createTestWorld(84L);

        Chunk center = world.getOrCreateChunk(new ChunkPos(0, 0));
        Chunk east = world.getOrCreateChunk(new ChunkPos(1, 0));

        int worldY = 70;
        world.setBlock(15, worldY, 8, BlockType.STONE);

        setMeshDirty(center, false);
        setMeshDirty(east, false);

        world.setBlock(15, worldY, 8, BlockType.AIR);

        assertTrue(isMeshDirty(center), "Center chunk should be dirty after breaking edge block");
        assertTrue(isMeshDirty(east), "East neighbor should be dirty after breaking edge block");
    }

    @Test
    @DisplayName("Corner edge updates mark both orthogonal neighbors dirty")
    void testCornerBlockMarksOrthogonalNeighborsDirty() {
        World world = TestUtils.createTestWorld(126L);

        Chunk center = world.getOrCreateChunk(new ChunkPos(0, 0));
        Chunk east = world.getOrCreateChunk(new ChunkPos(1, 0));
        Chunk south = world.getOrCreateChunk(new ChunkPos(0, 1));

        setMeshDirty(center, false);
        setMeshDirty(east, false);
        setMeshDirty(south, false);

        int worldY = 68;
        world.setBlock(15, worldY, 15, BlockType.STONE);

        assertTrue(isMeshDirty(center), "Center chunk should be dirty for corner placement");
        assertTrue(isMeshDirty(east), "East neighbor should be dirty for corner placement");
        assertTrue(isMeshDirty(south), "South neighbor should be dirty for corner placement");
    }

    @Test
    @DisplayName("Interior block updates do not dirty neighbors")
    void testInteriorBlockDoesNotMarkNeighborsDirty() {
        World world = TestUtils.createTestWorld(168L);

        Chunk center = world.getOrCreateChunk(new ChunkPos(0, 0));
        Chunk east = world.getOrCreateChunk(new ChunkPos(1, 0));
        Chunk north = world.getOrCreateChunk(new ChunkPos(0, -1));

        setMeshDirty(center, false);
        setMeshDirty(east, false);
        setMeshDirty(north, false);

        int worldY = 66;
        world.setBlock(8, worldY, 8, BlockType.STONE);

        assertTrue(isMeshDirty(center), "Center chunk should be dirty after interior placement");
        assertFalse(isMeshDirty(east), "East neighbor should remain clean for interior placement");
        assertFalse(isMeshDirty(north), "North neighbor should remain clean for interior placement");
    }

    private static boolean isMeshDirty(Chunk chunk) {
        try {
            Field field = Chunk.class.getDeclaredField("meshDirty");
            field.setAccessible(true);
            return field.getBoolean(chunk);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to inspect chunk meshDirty flag", e);
        }
    }

    private static void setMeshDirty(Chunk chunk, boolean value) {
        try {
            Field field = Chunk.class.getDeclaredField("meshDirty");
            field.setAccessible(true);
            field.setBoolean(chunk, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to update chunk meshDirty flag", e);
        }
    }
}
