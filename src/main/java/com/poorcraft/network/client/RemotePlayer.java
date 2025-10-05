package com.poorcraft.network.client;

/**
 * Remote player entity class for rendering other players on the client.
 * 
 * Remote players are visual representations of other clients.
 * Interpolation smooths out network jitter (packets arrive at irregular intervals).
 * 
 * Position updates come from server at ~20 Hz (server tick rate).
 * Rendering happens at 60+ Hz.
 * Interpolation fills the gaps for smooth movement.
 * 
 * Interpolation alpha of 0.2 means 20% toward target each frame.
 * At 60 FPS, this creates smooth movement that catches up in ~5 frames.
 * 
 * This is basically how every multiplayer game works. Nothing revolutionary here!
 * Just good old linear interpolation. LERP is love, LERP is life.
 */
public class RemotePlayer {
    
    private final int playerId;
    private final String username;
    private float x, y, z;
    private float yaw, pitch;
    private float targetX, targetY, targetZ;
    private float targetYaw, targetPitch;
    private boolean onGround;
    
    /**
     * Creates a new remote player.
     * 
     * @param playerId Unique player ID
     * @param username Player username
     * @param x Initial X position
     * @param y Initial Y position
     * @param z Initial Z position
     * @param yaw Initial yaw rotation
     * @param pitch Initial pitch rotation
     */
    public RemotePlayer(int playerId, String username, float x, float y, float z, float yaw, float pitch) {
        this.playerId = playerId;
        this.username = username;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.onGround = false;
    }
    
    /**
     * Sets target position for interpolation.
     * 
     * @param x Target X position
     * @param y Target Y position
     * @param z Target Z position
     * @param yaw Target yaw rotation
     * @param pitch Target pitch rotation
     * @param onGround On ground flag
     */
    public void setTargetPosition(float x, float y, float z, float yaw, float pitch, boolean onGround) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.onGround = onGround;
    }
    
    /**
     * Interpolates current position toward target position.
     * 
     * @param alpha Interpolation factor (0.0 to 1.0)
     */
    public void interpolate(float alpha) {
        x = lerp(x, targetX, alpha);
        y = lerp(y, targetY, alpha);
        z = lerp(z, targetZ, alpha);
        yaw = lerpAngle(yaw, targetYaw, alpha);
        pitch = lerpAngle(pitch, targetPitch, alpha);
    }
    
    /**
     * Linear interpolation between two values.
     * 
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated value
     */
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    /**
     * Angle interpolation with 360° wrap handling.
     * 
     * @param a Start angle
     * @param b End angle
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated angle
     */
    private float lerpAngle(float a, float b, float t) {
        // Normalize angles to -180 to 180 range
        float diff = b - a;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        
        return a + diff * t;
    }
    
    // Getters
    
    public int getPlayerId() {
        return playerId;
    }
    
    public String getUsername() {
        return username;
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
    
    public float getYaw() {
        return yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public boolean isOnGround() {
        return onGround;
    }
}
