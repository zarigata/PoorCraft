package com.poorcraft.world.entity;

import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;

/**
 * Represents a lightweight dropped item entity.
 */
public class ItemDrop {

    private static final float WIGGLE_FREQUENCY = 3.2f;
    private static final float WIGGLE_AMPLITUDE = 0.12f;
    private static final float LIFETIME_SECONDS = 1800.0f; // 30 minutes
    private static final float GRAVITY = 18.0f;
    private static final float TERMINAL_VELOCITY = -22.0f;
    private static final float REST_OFFSET = 0.12f;
    private static final int GROUND_SEARCH_DEPTH = 24;

    private final BlockType blockType;
    private final float wigglePhase;

    private int amount;
    private float age;
    private float x;
    private float y;
    private float z;
    private float verticalVelocity;
    private boolean grounded;

    public ItemDrop(BlockType blockType, float x, float y, float z, int amount, float phase) {
        this.blockType = blockType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.amount = Math.max(1, amount);
        this.wigglePhase = phase;
        this.age = 0.0f;
        this.verticalVelocity = 0.0f;
        this.grounded = false;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    public float getAge() {
        return age;
    }

    public void update(World world, float deltaTime) {
        age += deltaTime;

        if (grounded) {
            float groundLevel = findGroundLevel(world);
            float targetY = groundLevel + REST_OFFSET;
            if (!Float.isInfinite(groundLevel) && y <= targetY + 0.05f) {
                y = targetY;
                verticalVelocity = 0.0f;
            } else {
                grounded = false;
            }
            return;
        }

        verticalVelocity = Math.max(TERMINAL_VELOCITY, verticalVelocity - GRAVITY * deltaTime);
        y += verticalVelocity * deltaTime;

        float groundLevel = findGroundLevel(world);
        if (!Float.isInfinite(groundLevel) && y <= groundLevel + REST_OFFSET) {
            y = groundLevel + REST_OFFSET;
            verticalVelocity = 0.0f;
            grounded = true;
        }
    }

    public boolean isExpired() {
        return age >= LIFETIME_SECONDS || amount <= 0;
    }

    public float getRenderY() {
        return y + (float) Math.sin((age + wigglePhase) * WIGGLE_FREQUENCY) * WIGGLE_AMPLITUDE;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getLifetimeSeconds() {
        return LIFETIME_SECONDS;
    }

    public float getWigglePhase() {
        return wigglePhase;
    }

    private float findGroundLevel(World world) {
        if (world == null) {
            return Float.POSITIVE_INFINITY;
        }

        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = Math.max(0, (int) Math.floor(y));

        int minY = Math.max(0, startY - GROUND_SEARCH_DEPTH);
        for (int checkY = startY; checkY >= minY; checkY--) {
            BlockType type = world.getBlock(blockX, checkY, blockZ);
            if (type != null && type != BlockType.AIR && type.isSolid()) {
                return checkY + 1.0f;
            }
        }

        return Float.POSITIVE_INFINITY;
    }
}
