package com.poorcraft.world.entity;

import com.poorcraft.inventory.Inventory;
import com.poorcraft.world.block.BlockType;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages active item drops in the world.
 */
public class DropManager {

    private static final float PICKUP_RADIUS_SQUARED = 1.5f * 1.5f;

    private final List<ItemDrop> drops = new ArrayList<>();
    private final Random random = new Random();

    public void spawn(BlockType blockType, float x, float y, float z, int amount) {
        if (blockType == null || blockType == BlockType.AIR || amount <= 0) {
            return;
        }
        float phase = random.nextFloat() * (float) (Math.PI * 2.0);
        drops.add(new ItemDrop(blockType, x, y, z, amount, phase));
    }

    public void update(Vector3f playerPosition, Inventory inventory, float deltaTime) {
        if (drops.isEmpty()) {
            return;
        }

        for (int i = drops.size() - 1; i >= 0; i--) {
            ItemDrop drop = drops.get(i);
            drop.update(deltaTime);

            if (drop.isExpired()) {
                drops.remove(i);
                continue;
            }

            if (playerPosition != null && inventory != null) {
                float dx = (drop.getOriginX()) - playerPosition.x;
                float dy = (drop.getRenderY()) - (playerPosition.y + 1.0f); // approximate eye height
                float dz = (drop.getOriginZ()) - playerPosition.z;
                float distanceSquared = dx * dx + dy * dy + dz * dz;
                if (distanceSquared <= PICKUP_RADIUS_SQUARED) {
                    int remaining = inventory.add(drop.getBlockType(), drop.getAmount());
                    if (remaining <= 0) {
                        drops.remove(i);
                    } else {
                        drop.setAmount(remaining);
                    }
                }
            }
        }
    }

    public List<ItemDrop> getDrops() {
        return Collections.unmodifiableList(drops);
    }

    public void clear() {
        drops.clear();
    }
}
