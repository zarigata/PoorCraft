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
     * 
     * @return View matrix
     */
    public Matrix4f getViewMatrix() {
        if (viewMatrixDirty) {
            Vector3f target = new Vector3f(position).add(front);
            viewMatrix.identity().lookAt(position, target, up);
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
     * 
     * @return Camera position
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
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
     * Sets camera movement speed.
     * 
     * @param moveSpeed New movement speed in units per second
     */
    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }
    
    /**
     * Sets mouse sensitivity.
     * 
     * @param mouseSensitivity New mouse sensitivity multiplier
     */
    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = mouseSensitivity;
    }
    
    /**
     * Gets camera yaw rotation.
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
}
