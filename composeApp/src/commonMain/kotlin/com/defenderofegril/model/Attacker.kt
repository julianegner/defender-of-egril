package com.defenderofegril.model

enum class AttackerType(
    val displayName: String,
    val health: Int,
    val speed: Int, // cells per turn
    val reward: Int // coins when defeated
) {
    GOBLIN("Goblin", health = 20, speed = 2, reward = 5),
    ORK("Ork", health = 40, speed = 1, reward = 10),
    OGRE("Ogre", health = 80, speed = 1, reward = 20),
    SKELETON("Skeleton", health = 15, speed = 2, reward = 7),
    EVIL_WIZARD("Evil Wizard", health = 30, speed = 1, reward = 15),
    WITCH("Witch", health = 25, speed = 2, reward = 12)
}

data class Attacker(
    val id: Int,
    val type: AttackerType,
    var position: Position,
    var currentHealth: Int = type.health,
    var isDefeated: Boolean = false,
    var previousPosition: Position? = null,  // For movement animation
    var isSpawning: Boolean = false  // For spawn animation
) {
    val maxHealth: Int get() = type.health
    
    // Get the effective position for rendering (used during animations)
    fun getDisplayPosition(animationProgress: Float = 1f): Position {
        val prev = previousPosition
        if (prev != null && animationProgress < 1f) {
            // Interpolate between previous and current position
            val x = (prev.x + (position.x - prev.x) * animationProgress).toInt()
            val y = (prev.y + (position.y - prev.y) * animationProgress).toInt()
            return Position(x, y)
        }
        return position
    }
}
