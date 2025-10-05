package com.poorcraft.engine.rendering

import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * Camera for 3D rendering
 */
class Camera {
    val position = Vector3f(0f, 0f, 0f)
    val rotation = Vector3f(0f, 0f, 0f)
    
    val viewMatrix = Matrix4f()
    val projectionMatrix = Matrix4f()
    
    fun update() {
        viewMatrix.identity()
        viewMatrix.rotateX(Math.toRadians(rotation.x.toDouble()).toFloat())
        viewMatrix.rotateY(Math.toRadians(rotation.y.toDouble()).toFloat())
        viewMatrix.translate(-position.x, -position.y, -position.z)
    }
    
    fun updateProjection(aspectRatio: Float, fov: Float) {
        projectionMatrix.identity()
        projectionMatrix.perspective(
            Math.toRadians(fov.toDouble()).toFloat(),
            aspectRatio,
            0.1f,
            1000.0f
        )
    }
}
