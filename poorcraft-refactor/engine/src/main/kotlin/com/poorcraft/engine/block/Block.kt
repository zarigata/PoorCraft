package com.poorcraft.engine.block

/**
 * Block definition
 */
data class Block(
    val id: Int,
    val name: String,
    val textureName: String,
    val opaque: Boolean = true,
    val collidable: Boolean = true,
    val lightLevel: Int = 0
) {
    companion object {
        val AIR = Block(0, "air", "air", opaque = false, collidable = false)
    }
}
