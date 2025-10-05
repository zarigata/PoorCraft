package com.poorcraft.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * First-person camera with WASD movement and mouse look.
 * Uses Euler angles (yaw/pitch) for rotation.
 * 
 * This is the classic FPS camera. Nothing fancy, just like the good old days.
 * Remember when cameras were simple? Pepperidge Farm remembers.
 */
public class Camera {
    
    // Movement direction constants
    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int UP = 4;
    public static final int DOWN = 5;
    
    private final Vector3f position;
    private final Vector3f front;
    private final Vector3f up;
    private final Vector3f right;
    
    private float yaw;    // Rotation around Y axis (left/right)
    private float pitch;  // Rotation around X axis (up/down)
    
    private float moveSpeed;
    private float mouseSensitivity;
    
    private final Matrix4f viewMatrix;
    private final Matrix4f projectionMatrix;
    private boolean viewMatrixDirty;
    
    // Head bobbing state
    private float bobbingTimer = 0.0f;
    private float bobbingOffset = 0.0f;
    private boolean isMoving = false;
    
    /**
     * Creates a new first-person camera.
     * 
     * @param position Starting position in world space
     * @param moveSpeed Movement speed in units per second
     * @param mouseSensitivity Mouse sensitivity multiplier
     */
    public Camera(Vector3f position, float moveSpeed, float mouseSensitivity) {
        this.position = new Vector3f(position);
        this.moveSpeed = moveSpeed;
        this.mouseSensitivity = mouseSensitivity;
        
        // Initialize vectors
        this.front = new Vector3f();
        this.up = new Vector3f(0, 1, 0);
        this.right = new Vector3f();
        
        // Initialize rotation
        this.yaw = -90.0f;   // -90 degrees = looking along +Z axis
        this.pitch = 0.0f;
        
        // Initialize matrices
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        this.viewMatrixDirty = true;
        
        // Calculate initial vectors
        updateCameraVectors();
    }
    
    /**
     * Processes mouse movement and updates camera rotation.
     * 
     * @param xOffset Mouse movement in X direction
     * @param yOffset Mouse movement in Y direction
     */
    public void processMouseMovement(float xOffset, float yOffset) {
        xOffset *= mouseSensitivity;
        yOffset *= mouseSensitivity;
        
        yaw += xOffset;
        pitch += yOffset;
        
        // Clamp pitch to prevent camera flipping
        // -89 to 89 degrees is safe, -90/90 causes gimbal lock (scary stuff)
        pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
        
        viewMatrixDirty = true;
        updateCameraVectors();
    }
    
    /**
     * Updates front and right vectors based on yaw and pitch.
     * This is where the Euler angle magic happens.
     */
    private void updateCameraVectors() {
        // Convert angles to radians
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        
        // Calculate front vector using spherical coordinates
        // I'm not gonna lie, I copied this from LearnOpenGL and it just works
        front.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        front.y = (float) Math.sin(pitchRad);
        front.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        front.normalize();
        
        // Calculate right vector (cross product of front and world up)
        front.cross(up, right).normalize();
    }
    
    /**
     * Processes keyboard input for camera movement.
     * 
     * @param direction Movement direction (use constants: FORWARD, BACKWARD, etc.)
     * @param deltaTime Time since last frame in seconds
     */
    public void processKeyboard(int direction, float deltaTime) {
        float velocity = moveSpeed * deltaTime;
        
        switch (direction) {
            case FORWARD:
                position.add(new Vector3f(front).mul(velocity));
                break;
            case BACKWARD:
                position.sub(new Vector3f(front).mul(velocity));
                break;
            case LEFT:
                position.sub(new Vector3f(right).mul(velocity));
                break;
            case RIGHT:
                position.add(new Vector3f(right).mul(velocity));
                break;
            case UP:
                position.y += velocity;
                break;
            case DOWN:
                position.y -= velocity;
                break;
        }
        
        viewMatrixDirty = true;
    }
    
    /**
     * Returns the view matrix for rendering.
     * Recalculates only if camera has moved/rotated since last call.
     * Applies head bobbing offset to Y position.
     * 
     * @return View matrix
     */
    public Matrix4f getViewMatrix() {
        if (viewMatrixDirty) {
            // Apply bobbing offset to position
            Vector3f bobbedPosition = new Vector3f(position.x, position.y + bobbingOffset, position.z);
            Vector3f target = new Vector3f(bobbedPosition).add(front);
            viewMatrix.identity().lookAt(bobbedPosition, target, up);
            viewMatrixDirty = false;
        }
        return viewMatrix;
    }
    
    /**
     * Returns the projection matrix for rendering.
     * 
     * @param fov Field of view in degrees
     * @param aspectRatio Aspect ratio (width/height)
     * @param near Near clipping plane
     * @param far Far clipping plane
     * @return Projection matrix
     */
    public Matrix4f getProjectionMatrix(float fov, float aspectRatio, float near, float far) {
        projectionMatrix.identity().perspective(
            (float) Math.toRadians(fov),
            aspectRatio,
            near,
            far
        );
        return projectionMatrix;
    }
    
    /**
     * Returns camera position (copy to prevent external modification).
     * @return Camera position
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    /**
     * Sets the camera position directly.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        this.viewMatrixDirty = true;
    }

    /**
     * Sets the camera position directly from a vector.
     *
     * @param position New position
     */
    public void setPosition(Vector3f position) {
        setPosition(position.x, position.y, position.z);
    }

    /**
     * Returns camera front vector (copy to prevent external modification).
     * 
     * @return Front direction vector
     */
    public Vector3f getFront() {
        return new Vector3f(front);
    }

    /**
     * Returns the camera right vector (copy to prevent external modification).
     *
     * @return Right direction vector
     */
    public Vector3f getRight() {
        return new Vector3f(right);
    }

    /**
     * Sets mouse sensitivity multiplier.
     *
     * @param mouseSensitivity New mouse sensitivity multiplier
     */
    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = mouseSensitivity;
    }

    /**
     * 
     * @return Yaw in degrees
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Gets camera pitch rotation.
     * 
     * @return Pitch in degrees
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Updates head bobbing animation based on player movement.
     * 
     * @param deltaTime Time since last frame
     * @param moving Whether the player is currently moving
     * @param movementSpeed Current movement speed magnitude
     * @param headBobbingEnabled Whether head bobbing is enabled in settings
     * @param intensity Head bobbing intensity multiplier from settings
     */
    public void updateHeadBobbing(float deltaTime, boolean moving, float movementSpeed, 
                                 boolean headBobbingEnabled, float intensity) {
        if (!headBobbingEnabled || intensity <= 0.0f) {
            // Smoothly return to zero if disabled
            if (Math.abs(bobbingOffset) > 0.001f) {
                bobbingOffset *= (1.0f - deltaTime * 5.0f);
                if (Math.abs(bobbingOffset) < 0.001f) {
                    bobbingOffset = 0.0f;
                }
                viewMatrixDirty = true;
            }
            return;
        }
        
        this.isMoving = moving;
        
        if (moving && movementSpeed > 0.1f) {
            // Calculate bobbing frequency based on movement speed
            float frequency = 4.0f + (movementSpeed * 0.5f);
            bobbingTimer += deltaTime * frequency;
            
            // Keep timer in reasonable range
            if (bobbingTimer > Math.PI * 2) {
                bobbingTimer -= (float)(Math.PI * 2);
            }
            
            // Calculate bobbing offset using sine wave
            float amplitude = 0.08f * intensity;
            bobbingOffset = (float)(Math.sin(bobbingTimer) * amplitude);
            viewMatrixDirty = true;
        } else {
            // Smoothly return to zero when not moving
            if (Math.abs(bobbingOffset) > 0.001f) {
                bobbingOffset *= (1.0f - deltaTime * 8.0f);
                if (Math.abs(bobbingOffset) < 0.001f) {
                    bobbingOffset = 0.0f;
                    bobbingTimer = 0.0f;
                }
                viewMatrixDirty = true;
            }
        }
    }
    
    /**
     * Gets the current head bobbing offset.
     * 
     * @return Vertical offset from head bobbing
     */
    public float getBobbingOffset() {
        return bobbingOffset;
    }
    
    /**
     * Sets the camera yaw rotation directly.
     * 
     * @param yaw Yaw angle in degrees
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
        viewMatrixDirty = true;
        updateCameraVectors();
    }
    
    /**
     * Sets the camera pitch rotation directly.
     * 
     * @param pitch Pitch angle in degrees (clamped to -89 to 89)
     */
    public void setPitch(float pitch) {
        this.pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
        viewMatrixDirty = true;
        updateCameraVectors();
    }
    
    /**
     * Makes the camera look at a target position.
     * 
     * @param target Target position to look at
     */
    public void lookAt(Vector3f target) {
        Vector3f direction = new Vector3f(target).sub(position).normalize();
        
        // Calculate yaw and pitch from direction vector
        float yaw = (float)Math.toDegrees(Math.atan2(direction.z, direction.x));
        float pitch = (float)Math.toDegrees(Math.asin(-direction.y));
        
        setYaw(yaw);
        setPitch(pitch);
    }
}
