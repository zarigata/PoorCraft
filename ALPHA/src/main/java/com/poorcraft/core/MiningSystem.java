package com.poorcraft.core;

import com.poorcraft.camera.Camera;
import com.poorcraft.input.InputHandler;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import com.poorcraft.world.entity.DropManager;
import org.joml.Vector3f;

import java.util.Optional;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * Handles player mining interactions including block targeting, break progress, and
 * interaction timing based on block hardness.
 */
public class MiningSystem {

    private static final float REACH_DISTANCE = 5.0f;
    private static final float RAY_STEP = 0.05f;
    private static final float HARDNESS_MULTIPLIER = 1.2f;

    private Target aimedTarget;
    private Target miningTarget;
    private float breakProgress;
    private DropManager dropManager;

    /**
     * Updates current mining state.
     */
    public void update(World world, Camera camera, InputHandler input, float deltaTime) {
        if (world == null || camera == null || input == null) {
            reset();
            return;
        }

        aimedTarget = pickTarget(world, camera);

        boolean leftButtonDown = input.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        if (!leftButtonDown || aimedTarget == null) {
            miningTarget = null;
            breakProgress = 0.0f;
            return;
        }

        if (miningTarget == null || !miningTarget.isSameBlock(aimedTarget)) {
            miningTarget = aimedTarget.copy();
            breakProgress = 0.0f;
        }

        float hardness = miningTarget.blockType.getHardness();
        if (Float.isInfinite(hardness)) {
            breakProgress = 0.0f;
            return;
        }

        if (hardness <= 0.0f) {
            BlockType brokenBlock = miningTarget.blockType;
            world.setBlock(miningTarget.x, miningTarget.y, miningTarget.z, BlockType.AIR);
            spawnDrop(brokenBlock, miningTarget.x, miningTarget.y, miningTarget.z);
            miningTarget = null;
            breakProgress = 0.0f;
            aimedTarget = pickTarget(world, camera);
            return;
        }

        float breakTime = Math.max(0.25f, hardness * HARDNESS_MULTIPLIER);
        breakProgress += deltaTime / breakTime;

        if (breakProgress >= 1.0f) {
            BlockType brokenBlock = miningTarget.blockType;
            world.setBlock(miningTarget.x, miningTarget.y, miningTarget.z, BlockType.AIR);
            spawnDrop(brokenBlock, miningTarget.x, miningTarget.y, miningTarget.z);
            miningTarget = null;
            breakProgress = 0.0f;
            aimedTarget = pickTarget(world, camera);
        }
    }

    /**
     * Clears the current mining state.
     */
    public void reset() {
        aimedTarget = null;
        miningTarget = null;
        breakProgress = 0.0f;
    }

    /**
     * @return block currently under the crosshair if any
     */
    public Optional<Target> getAimedTarget() {
        return Optional.ofNullable(aimedTarget);
    }

    /**
     * @return block that is currently being mined if any
     */
    public Optional<Target> getMiningTarget() {
        return Optional.ofNullable(miningTarget);
    }

    /**
     * @return current break progress in range [0, 1]
     */
    public float getBreakProgress() {
        return breakProgress;
    }

    public void setDropManager(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    private Target pickTarget(World world, Camera camera) {
        Vector3f origin = camera.getPosition();
        Vector3f direction = camera.getFront();
        direction.normalize();

        Vector3f probe = new Vector3f();
        for (float distance = 0.0f; distance <= REACH_DISTANCE; distance += RAY_STEP) {
            probe.set(direction).mul(distance).add(origin);
            int blockX = (int) Math.floor(probe.x);
            int blockY = (int) Math.floor(probe.y);
            int blockZ = (int) Math.floor(probe.z);

            BlockType blockType = world.getBlock(blockX, blockY, blockZ);
            if (blockType != BlockType.AIR) {
                return new Target(blockX, blockY, blockZ, blockType);
            }
        }
        return null;
    }

    private void spawnDrop(BlockType blockType, int x, int y, int z) {
        if (dropManager == null || blockType == null || blockType == BlockType.AIR) {
            return;
        }
        dropManager.spawn(blockType, x + 0.5f, y + 0.1f, z + 0.5f, 1);
    }

    /**
     * Immutable data about a targeted block.
     */
    public static final class Target {
        private final int x;
        private final int y;
        private final int z;
        private final BlockType blockType;

        private Target(int x, int y, int z, BlockType blockType) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockType = blockType;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public BlockType getBlockType() {
            return blockType;
        }

        private boolean isSameBlock(Target other) {
            return other != null && other.x == x && other.y == y && other.z == z;
        }

        private Target copy() {
            return new Target(x, y, z, blockType);
        }
    }
}
