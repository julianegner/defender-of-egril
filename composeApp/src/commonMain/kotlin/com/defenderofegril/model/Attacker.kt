package com.defenderofegril.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

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
    val currentHealth: MutableState<Int> = mutableStateOf(type.health),
    val isDefeated: MutableState<Boolean> = mutableStateOf(false)
) {
    val maxHealth: Int get() = type.health
}
