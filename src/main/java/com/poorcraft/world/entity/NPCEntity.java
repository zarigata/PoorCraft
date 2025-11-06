package com.poorcraft.world.entity;

import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import org.joml.Vector3f;

/**
 * Represents an NPC entity that can follow the player and interact with the world.
 */
public class NPCEntity {

    private static final float GRAVITY = 18.0f;
    private static final float TERMINAL_VELOCITY = -22.0f;
    private static final float JUMP_VELOCITY = 8.0f;
    private static final int GROUND_SEARCH_DEPTH = 24;

    private final int npcId;
    private final String name;
    private final String personality;
    private final String skinName;

    private float x;
    private float y;
    private float z;
    private float velocityX;
    private float velocityY;
    private float velocityZ;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private float age;

    private Vector3f targetPosition;
    private float followDistance;
    private float moveSpeed;
    private NPCTask currentTask;
    private float taskProgress;
    private long lastActionTime;

    public NPCEntity(int npcId, String name, float x, float y, float z, String personality, String skinName) {
        this.npcId = npcId;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.personality = personality;
        this.skinName = skinName != null ? skinName : "steve";
        
        this.velocityX = 0.0f;
        this.velocityY = 0.0f;
        this.velocityZ = 0.0f;
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.onGround = false;
        this.age = 0.0f;
        
        this.targetPosition = null;
        this.followDistance = 3.0f;
        this.moveSpeed = 4.0f;
        this.currentTask = null;
        this.taskProgress = 0.0f;
        this.lastActionTime = 0L;
    }

    public void update(World world, Vector3f playerPosition, float deltaTime) {
        age += deltaTime;

        // Apply gravity
        if (!onGround) {
            velocityY = Math.max(TERMINAL_VELOCITY, velocityY - GRAVITY * deltaTime);
        }

        boolean performingTask = false;
        if (currentTask != null) {
            boolean finished = currentTask.execute(world, this, deltaTime);
            if (finished || currentTask.isComplete()) {
                currentTask = null;
                taskProgress = 0.0f;
                targetPosition = null;
                velocityX = 0.0f;
                velocityZ = 0.0f;
                System.out.println("[NPCEntity] Task complete for NPC #" + npcId);
            } else {
                performingTask = true;
            }
        }

        // Task navigation logic
        if (performingTask && targetPosition != null) {
            float dx = targetPosition.x - x;
            float dz = targetPosition.z - z;
            float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);

            if (horizontalDistance > 0.3f) {
                float dirX = dx / Math.max(0.001f, horizontalDistance);
                float dirZ = dz / Math.max(0.001f, horizontalDistance);
                velocityX = dirX * moveSpeed;
                velocityZ = dirZ * moveSpeed;
                yaw = (float) Math.toDegrees(Math.atan2(dirZ, dirX)) - 90.0f;

                if (onGround && shouldJump(world)) {
                    velocityY = JUMP_VELOCITY;
                    onGround = false;
                }
            } else {
                velocityX = 0.0f;
                velocityZ = 0.0f;
            }
        }

        // Follow player logic
        if (!performingTask && playerPosition != null) {
            float distanceToPlayer = getDistanceToPlayer(playerPosition);
            
            if (distanceToPlayer > followDistance) {
                // Calculate direction to player
                float dx = playerPosition.x - x;
                float dz = playerPosition.z - z;
                float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);
                
                if (horizontalDistance > 0.1f) {
                    // Normalize and apply move speed
                    float dirX = dx / horizontalDistance;
                    float dirZ = dz / horizontalDistance;
                    
                    velocityX = dirX * moveSpeed;
                    velocityZ = dirZ * moveSpeed;
                    
                    // Update yaw to face movement direction
                    yaw = (float) Math.toDegrees(Math.atan2(dirZ, dirX)) - 90.0f;
                    
                    // Check if should jump
                    if (onGround && shouldJump(world)) {
                        velocityY = JUMP_VELOCITY;
                        onGround = false;
                    }
                } else {
                    velocityX = 0.0f;
                    velocityZ = 0.0f;
                }
            } else {
                // Close enough, stop moving
                velocityX = 0.0f;
                velocityZ = 0.0f;
            }
        }

        // Apply velocities
        x += velocityX * deltaTime;
        z += velocityZ * deltaTime;
        y += velocityY * deltaTime;

        // Ground collision detection
        float groundLevel = findGroundLevel(world);
        if (!Float.isInfinite(groundLevel) && y <= groundLevel) {
            y = groundLevel;
            velocityY = 0.0f;
            onGround = true;
        } else if (y > groundLevel + 0.1f) {
            onGround = false;
        }
    }

    public boolean shouldJump(World world) {
        if (world == null) {
            return false;
        }

        // Check if there's a block in front at feet level
        int blockX = (int) Math.floor(x + Math.cos(Math.toRadians(yaw + 90.0f)) * 0.5f);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z + Math.sin(Math.toRadians(yaw + 90.0f)) * 0.5f);

        BlockType type = world.getBlock(blockX, blockY, blockZ);
        if (type != null && type != BlockType.AIR && type.isSolid()) {
            // Check if there's space above to jump
            BlockType above = world.getBlock(blockX, blockY + 1, blockZ);
            BlockType above2 = world.getBlock(blockX, blockY + 2, blockZ);
            return (above == null || above == BlockType.AIR || !above.isSolid()) &&
                   (above2 == null || above2 == BlockType.AIR || !above2.isSolid());
        }

        return false;
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

    public float getDistanceToPlayer(Vector3f playerPos) {
        if (playerPos == null) {
            return Float.MAX_VALUE;
        }
        float dx = playerPos.x - x;
        float dy = playerPos.y - y;
        float dz = playerPos.z - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public Vector3f getPosition() {
        return new Vector3f(x, y, z);
    }

    public Vector3f getRenderPosition() {
        // Add slight bobbing effect
        float bobAmount = 0.05f;
        float bobSpeed = 4.0f;
        float bob = (float) Math.sin(age * bobSpeed) * bobAmount;
        return new Vector3f(x, y + bob, z);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getSkinName() {
        return skinName;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return npcId;
    }

    public String getPersonality() {
        return personality;
    }

    public void setTargetPosition(Vector3f target) {
        this.targetPosition = target;
    }

    public void teleport(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocityX = 0.0f;
        this.velocityY = 0.0f;
        this.velocityZ = 0.0f;
        this.targetPosition = null;
    }

    public void setFollowDistance(float distance) {
        this.followDistance = Math.max(0.5f, distance);
    }

    public void setMoveSpeed(float speed) {
        this.moveSpeed = Math.max(0.1f, speed);
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

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setCurrentTask(NPCTask task) {
        this.currentTask = task;
        this.taskProgress = 0.0f;
        this.targetPosition = null;
        if (task != null) {
            this.lastActionTime = System.currentTimeMillis();
            System.out.println("[NPCEntity] Starting task " + task.getType() + " for NPC #" + npcId);
        }
    }

    public NPCTask getCurrentTask() {
        return currentTask;
    }

    public void setTaskProgress(float progress) {
        this.taskProgress = progress;
    }

    public float getTaskProgress() {
        return taskProgress;
    }

    public long getLastActionTime() {
        return lastActionTime;
    }

    public void markActionPerformed() {
        this.lastActionTime = System.currentTimeMillis();
    }

    public boolean breakBlockAt(World world, int blockX, int blockY, int blockZ) {
        if (world == null) {
            return false;
        }
        BlockType type = world.getBlock(blockX, blockY, blockZ);
        if (type == null || type == BlockType.AIR) {
            return false;
        }

        world.setBlock(blockX, blockY, blockZ, BlockType.AIR);
        System.out.println("[NPCEntity] NPC #" + npcId + " broke block " + type + " at "
            + blockX + "," + blockY + "," + blockZ);
        return true;
    }

    public boolean canReachBlock(int blockX, int blockY, int blockZ) {
        float dx = (blockX + 0.5f) - x;
        float dy = (blockY + 0.5f) - y;
        float dz = (blockZ + 0.5f) - z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance <= 5.0f;
    }

    public Vector3f findNearestBlock(World world, BlockType type, int radius) {
        if (world == null || type == null) {
            return null;
        }

        int originX = (int) Math.floor(x);
        int originY = (int) Math.floor(y);
        int originZ = (int) Math.floor(z);

        double bestDistance = Double.MAX_VALUE;
        Vector3f bestPosition = null;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int checkX = originX + dx;
                    int checkY = originY + dy;
                    int checkZ = originZ + dz;

                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance > radius) {
                        continue;
                    }

                    if (world.getBlock(checkX, checkY, checkZ) == type) {
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestPosition = new Vector3f(checkX, checkY, checkZ);
                        }
                    }
                }
            }
        }

        return bestPosition;
    }

    public enum TaskType {
        GATHER,
        MOVE_TO,
        BREAK_BLOCK,
        IDLE
    }

    public abstract static class NPCTask {

        private final TaskType type;

        protected NPCTask(TaskType type) {
            this.type = type;
        }

        public TaskType getType() {
            return type;
        }

        public abstract boolean execute(World world, NPCEntity npc, float deltaTime);

        public abstract boolean isComplete();
    }

    public static class GatherTask extends NPCTask {

        private final String resourceType;
        private final int targetQuantity;
        private final int searchRadius;
        private final BlockType targetBlockType;

        private int gathered;
        private Vector3f currentTarget;
        private int targetBlockX;
        private int targetBlockY;
        private int targetBlockZ;

        public GatherTask(String resourceType, int quantity, int searchRadius) {
            super(TaskType.GATHER);
            this.resourceType = resourceType != null ? resourceType : "wood";
            this.targetQuantity = Math.max(1, quantity);
            this.searchRadius = Math.max(4, searchRadius);
            this.targetBlockType = mapResourceToBlock(this.resourceType);
            this.gathered = 0;
            this.currentTarget = null;
        }

        @Override
        public boolean execute(World world, NPCEntity npc, float deltaTime) {
            if (world == null || targetBlockType == null) {
                return true;
            }

            if (gathered >= targetQuantity) {
                npc.setTaskProgress(gathered);
                return true;
            }

            if (currentTarget == null || world.getBlock(targetBlockX, targetBlockY, targetBlockZ) != targetBlockType) {
                Vector3f blockPos = npc.findNearestBlock(world, targetBlockType, searchRadius);
                if (blockPos == null) {
                    return true; // Nothing to gather
                }
                targetBlockX = (int) blockPos.x;
                targetBlockY = (int) blockPos.y;
                targetBlockZ = (int) blockPos.z;
                currentTarget = new Vector3f(targetBlockX + 0.5f, targetBlockY + 0.5f, targetBlockZ + 0.5f);
                npc.setTargetPosition(new Vector3f(currentTarget));
            }

            Vector3f npcPos = npc.getPosition();
            float distance = npcPos.distance(currentTarget);
            if (distance <= 1.6f && npc.canReachBlock(targetBlockX, targetBlockY, targetBlockZ)) {
                long now = System.currentTimeMillis();
                if (now - npc.getLastActionTime() >= 600L) {
                    boolean broke = npc.breakBlockAt(world, targetBlockX, targetBlockY, targetBlockZ);
                    if (broke) {
                        gathered++;
                        npc.setTaskProgress(gathered);
                        npc.markActionPerformed();
                        currentTarget = null;
                        npc.setTargetPosition(null);
                    }
                }
            }

            return gathered >= targetQuantity;
        }

        @Override
        public boolean isComplete() {
            return gathered >= targetQuantity;
        }

        private static BlockType mapResourceToBlock(String resource) {
            if (resource == null) {
                return BlockType.WOOD;
            }
            String lowered = resource.toLowerCase();
            return switch (lowered) {
                case "wood", "logs" -> BlockType.WOOD;
                case "stone", "cobblestone" -> BlockType.STONE;
                case "dirt" -> BlockType.DIRT;
                case "leaves" -> BlockType.LEAVES;
                case "sand" -> BlockType.SAND;
                default -> BlockType.WOOD;
            };
        }

        public String getResourceType() {
            return resourceType;
        }

        public int getTargetQuantity() {
            return targetQuantity;
        }

        public int getGathered() {
            return gathered;
        }
    }
}
