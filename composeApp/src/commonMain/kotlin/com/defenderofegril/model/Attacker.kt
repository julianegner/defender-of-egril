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
    val customMaxHealth: Int? = null  // For cheat-spawned enemies with scaled health
) {
    val maxHealth: Int get() = customMaxHealth ?: type.health
}
