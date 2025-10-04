package com.poorcraft.world.entity;

import com.poorcraft.world.block.BlockType;

/**
 * Represents a lightweight dropped item entity.
 */
public class ItemDrop {

    private static final float WIGGLE_FREQUENCY = 3.2f;
    private static final float WIGGLE_AMPLITUDE = 0.12f;
    private static final float LIFETIME_SECONDS = 1800.0f; // 30 minutes

    private final BlockType blockType;
    private final float originX;
    private final float originY;
    private final float originZ;
    private final float wigglePhase;

    private int amount;
    private float age;

    public ItemDrop(BlockType blockType, float x, float y, float z, int amount, float phase) {
        this.blockType = blockType;
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.amount = Math.max(1, amount);
        this.wigglePhase = phase;
        this.age = 0.0f;
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

    public void update(float deltaTime) {
        age += deltaTime;
    }

    public boolean isExpired() {
        return age >= LIFETIME_SECONDS || amount <= 0;
    }

    public float getRenderY() {
        return originY + (float) Math.sin((age + wigglePhase) * WIGGLE_FREQUENCY) * WIGGLE_AMPLITUDE;
    }

    public float getOriginX() {
        return originX;
    }

    public float getOriginY() {
        return originY;
    }

    public float getOriginZ() {
        return originZ;
    }

    public float getLifetimeSeconds() {
        return LIFETIME_SECONDS;
    }

    public float getWigglePhase() {
        return wigglePhase;
    }
}
