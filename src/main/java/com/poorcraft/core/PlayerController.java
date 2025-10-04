package com.poorcraft.core;

import com.poorcraft.camera.Camera;
import com.poorcraft.config.Settings;
import com.poorcraft.input.InputHandler;
import com.poorcraft.world.World;
import com.poorcraft.world.block.BlockType;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Handles first-person player physics including gravity, jumping, and collision against blocks.
 *
 * The camera is treated as the player's head; this controller keeps the player anchored to the
 * terrain while allowing classic WASD movement. Nothing fancy, just the bare necessities to stop
 * the player from floating around like a ghost from Minecraft Classic.
 */
public class PlayerController {

    private static final float PLAYER_WIDTH = 0.6f;
    private static final float HALF_WIDTH = PLAYER_WIDTH / 2.0f;
    private static final float STANDING_HEIGHT = 1.8f;
    private static final float STANDING_EYE_HEIGHT = 1.62f;
    private static final float CROUCH_HEIGHT = 1.0f;
    private static final float CROUCH_EYE_HEIGHT = 0.9f;

    private static final float GRAVITY = 32.0f;
    private static final float TERMINAL_VELOCITY = 64.0f;
    private static final float JUMP_VELOCITY = 8.5f;
    private static final float STEP_SIZE = 0.05f;
    private static final float COLLISION_EPSILON = 0.001f;
    private static final float GROUND_CHECK_DEPTH = 0.05f;
    private static final float DOUBLE_TAP_MAX_TIME = 0.3f;

    private final Vector3f position;
    private final Vector3f velocity;
    private boolean onGround;
    private float playerHeight;
    private float eyeHeight;
    private boolean crouching;
    private boolean flying;
    private float jumpTapTimer;
    private boolean jumpWasPressed;
    private GameMode gameMode;

    public PlayerController(Vector3f startPosition) {
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f();
        this.onGround = false;
        this.playerHeight = STANDING_HEIGHT;
        this.eyeHeight = STANDING_EYE_HEIGHT;
        this.crouching = false;
        this.flying = false;
        this.jumpTapTimer = DOUBLE_TAP_MAX_TIME + 1.0f;
        this.jumpWasPressed = false;
        this.gameMode = GameMode.SURVIVAL;
    }

    /**
     * Teleports the player to the provided feet position and clears velocity.
     */
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        this.velocity.zero();
        this.onGround = false;
        this.playerHeight = STANDING_HEIGHT;
        this.eyeHeight = STANDING_EYE_HEIGHT;
        this.crouching = false;
    }

    public void setPosition(Vector3f newPosition) {
        setPosition(newPosition.x, newPosition.y, newPosition.z);
    }

    /**
     * Respawns the player at the world spawn column (0, 0) slightly above terrain height.
     */
    public void respawn(World world) {
        if (world == null) {
            setPosition(0.5f, 72.0f, 0.5f);
            return;
        }
        int spawnX = 0;
        int spawnZ = 0;
        int groundY = world.getHeightAt(spawnX, spawnZ);
        setPosition(spawnX + 0.5f, groundY + 2.0f, spawnZ + 0.5f);
        this.flying = false;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getEyePosition() {
        return new Vector3f(position).add(0.0f, eyeHeight, 0.0f);
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode != null ? gameMode : GameMode.SURVIVAL;
        if (this.gameMode != GameMode.CREATIVE && flying) {
            flying = false;
            velocity.y = 0.0f;
        }
    }

    /**
     * Updates player physics using current input, world collision, and camera orientation.
     */
    public void update(World world, InputHandler input, Settings settings, Camera camera, float deltaTime) {
        if (world == null || settings == null || camera == null || input == null) {
            return;
        }

        // Resolve camera orientation to plan movement.
        Vector3f forward = camera.getFront();
        forward.y = 0.0f;
        if (forward.lengthSquared() > 0.0001f) {
            forward.normalize();
        }

        Vector3f right = camera.getRight();
        right.y = 0.0f;
        if (right.lengthSquared() > 0.0001f) {
            right.normalize();
        }

        Vector3f desiredDirection = new Vector3f();
        int forwardKey = settings.controls.getKeybind("forward", GLFW_KEY_W);
        int backwardKey = settings.controls.getKeybind("backward", GLFW_KEY_S);
        int leftKey = settings.controls.getKeybind("left", GLFW_KEY_A);
        int rightKey = settings.controls.getKeybind("right", GLFW_KEY_D);
        int jumpKey = settings.controls.getKeybind("jump", GLFW_KEY_SPACE);
        int crouchKey = settings.controls.getKeybind("sneak", GLFW_KEY_LEFT_CONTROL);
        int sprintKey = settings.controls.getKeybind("sprint", GLFW_KEY_LEFT_SHIFT);

        boolean jumpPressed = input.isKeyPressed(jumpKey);
        boolean sprintPressed = input.isKeyPressed(sprintKey);
        boolean crouchHeld = input.isKeyPressed(crouchKey);

        if (gameMode == GameMode.CREATIVE) {
            jumpTapTimer += deltaTime;
            if (jumpPressed && !jumpWasPressed) {
                if (jumpTapTimer <= DOUBLE_TAP_MAX_TIME) {
                    toggleFlight(world);
                }
                jumpTapTimer = 0.0f;
            }
        } else {
            jumpTapTimer = DOUBLE_TAP_MAX_TIME + 1.0f;
        }

        if (!flying && onGround && jumpPressed) {
            velocity.y = JUMP_VELOCITY;
            onGround = false;
        }

        jumpWasPressed = jumpPressed;

        if (!flying) {
            setCrouching(world, crouchHeld);
        } else if (crouching) {
            setCrouching(world, false);
        }

        if (input.isKeyPressed(forwardKey)) {
            desiredDirection.add(forward);
        }
        if (input.isKeyPressed(backwardKey)) {
            desiredDirection.sub(forward);
        }
        if (input.isKeyPressed(rightKey)) {
            desiredDirection.add(right);
        }
        if (input.isKeyPressed(leftKey)) {
            desiredDirection.sub(right);
        }

        if (desiredDirection.lengthSquared() > 0.0001f) {
            desiredDirection.normalize();
        } else {
            desiredDirection.zero();
        }

        boolean crouchActive = crouching;
        float speedMultiplier = 1.0f;
        if (sprintPressed && !crouchActive) {
            speedMultiplier = settings.camera.sprintMultiplier;
        } else if (crouchActive) {
            speedMultiplier = settings.camera.sneakMultiplier;
        }

        float moveSpeed = settings.camera.moveSpeed * speedMultiplier;
        Vector3f horizontalVelocity = new Vector3f(desiredDirection).mul(moveSpeed);
        velocity.x = horizontalVelocity.x;
        velocity.z = horizontalVelocity.z;

        if (flying) {
            float flySpeed = settings.camera.moveSpeed * (sprintPressed ? settings.camera.sprintMultiplier : 1.0f);
            float verticalVelocity = 0.0f;
            if (jumpPressed) {
                verticalVelocity += flySpeed;
            }
            if (crouchHeld) {
                verticalVelocity -= flySpeed;
            }
            velocity.y = verticalVelocity;
            onGround = false;
        } else {
            velocity.y -= GRAVITY * deltaTime;
            if (velocity.y < -TERMINAL_VELOCITY) {
                velocity.y = -TERMINAL_VELOCITY;
            }
        }

        float moveX = velocity.x * deltaTime;
        float moveY = velocity.y * deltaTime;
        float moveZ = velocity.z * deltaTime;

        boolean preventEdgeFall = crouchActive && !flying;

        moveAxis(world, moveX, Axis.X, preventEdgeFall);
        moveAxis(world, moveY, Axis.Y, false);
        moveAxis(world, moveZ, Axis.Z, preventEdgeFall);

        // Check if standing on ground after movement.
        if (!onGround && !flying && velocity.y <= 0.0f) {
            onGround = collides(world, position.x, position.y - GROUND_CHECK_DEPTH, position.z);
        }
    }

    private void moveAxis(World world, float amount, Axis axis, boolean preventEdgeFall) {
        if (amount == 0.0f) {
            return;
        }

        float remaining = amount;
        while (Math.abs(remaining) > 0.0f) {
            float step = Math.signum(remaining) * Math.min(STEP_SIZE, Math.abs(remaining));
            float nextX = position.x + (axis == Axis.X ? step : 0.0f);
            float nextY = position.y + (axis == Axis.Y ? step : 0.0f);
            float nextZ = position.z + (axis == Axis.Z ? step : 0.0f);

            if (preventEdgeFall && axis != Axis.Y && onGround) {
                float checkY = (axis == Axis.Y) ? nextY : position.y;
                if (!hasSolidGround(world, nextX, checkY, nextZ)) {
                    if (axis == Axis.X) {
                        velocity.x = 0.0f;
                    } else {
                        velocity.z = 0.0f;
                    }
                    break;
                }
            }

            if (!collides(world, nextX, nextY, nextZ)) {
                position.set(nextX, nextY, nextZ);
                remaining -= step;
            } else {
                if (axis == Axis.X) {
                    velocity.x = 0.0f;
                } else if (axis == Axis.Y) {
                    if (step < 0.0f) {
                        onGround = true;
                    }
                    velocity.y = 0.0f;
                } else {
                    velocity.z = 0.0f;
                }
                break;
            }
        }
    }

    private boolean collides(World world, float px, float py, float pz) {
        return collidesWithHeight(world, px, py, pz, playerHeight);
    }

    private boolean collidesWithHeight(World world, float px, float py, float pz, float height) {
        if (world == null) {
            return false;
        }

        float minX = px - HALF_WIDTH;
        float maxX = px + HALF_WIDTH;
        float minY = py;
        float maxY = py + height;
        float minZ = pz - HALF_WIDTH;
        float maxZ = pz + HALF_WIDTH;

        int startX = (int) Math.floor(minX);
        int endX = (int) Math.floor(maxX - COLLISION_EPSILON);
        int startY = (int) Math.floor(minY);
        int endY = (int) Math.floor(maxY - COLLISION_EPSILON);
        int startZ = (int) Math.floor(minZ);
        int endZ = (int) Math.floor(maxZ - COLLISION_EPSILON);

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    BlockType block = world.getBlock(x, y, z);
                    if (block.isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasSolidGround(World world, float px, float py, float pz) {
        if (world == null) {
            return false;
        }

        float minX = px - HALF_WIDTH;
        float maxX = px + HALF_WIDTH;
        float minZ = pz - HALF_WIDTH;
        float maxZ = pz + HALF_WIDTH;

        int startX = (int) Math.floor(minX);
        int endX = (int) Math.floor(maxX - COLLISION_EPSILON);
        int startZ = (int) Math.floor(minZ);
        int endZ = (int) Math.floor(maxZ - COLLISION_EPSILON);
        int groundY = (int) Math.floor(py - COLLISION_EPSILON);

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                BlockType block = world.getBlock(x, groundY, z);
                if (block.isSolid()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void setCrouching(World world, boolean shouldCrouch) {
        if (shouldCrouch) {
            if (!crouching) {
                playerHeight = CROUCH_HEIGHT;
                eyeHeight = CROUCH_EYE_HEIGHT;
                crouching = true;
            }
            return;
        }

        if (!crouching) {
            return;
        }

        boolean obstruction = world != null && collidesWithHeight(world, position.x, position.y, position.z, STANDING_HEIGHT);
        if (!obstruction) {
            playerHeight = STANDING_HEIGHT;
            eyeHeight = STANDING_EYE_HEIGHT;
            crouching = false;
        }
    }

    private void toggleFlight(World world) {
        if (gameMode != GameMode.CREATIVE) {
            return;
        }

        flying = !flying;
        velocity.y = 0.0f;
        if (flying) {
            onGround = false;
            setCrouching(world, false);
        }
    }

    private enum Axis {
        X, Y, Z
    }
}
