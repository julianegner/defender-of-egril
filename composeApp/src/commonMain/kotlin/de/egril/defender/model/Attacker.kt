package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

enum class AttackerType(
    val displayName: String,
    val health: Int,
    val speed: Int, // cells per turn
    val reward: Int, // coins when defeated
    val immuneToAcid: Boolean = false,
    val immuneToFireball: Boolean = false,
    val canSummon: Boolean = false,
    val canDisableTowers: Boolean = false,
    val canHeal: Boolean = false,
    val isBoss: Boolean = false,
    val isDragon: Boolean = false
) {
    GOBLIN("Goblin", health = 20, speed = 2, reward = 5),
    ORK("Ork", health = 40, speed = 1, reward = 10),
    OGRE("Ogre", health = 80, speed = 1, reward = 20),
    SKELETON("Skeleton", health = 15, speed = 2, reward = 7),
    EVIL_WIZARD("Evil Wizard", health = 30, speed = 1, reward = 15),
    WITCH("Witch", health = 25, speed = 2, reward = 12),
    BLUE_DEMON("Blue Demon", health = 15, speed = 3, reward = 10, immuneToAcid = true),
    RED_DEMON("Red Demon", health = 60, speed = 1, reward = 15, immuneToFireball = true),
    EVIL_MAGE("Evil Mage", health = 40, speed = 1, reward = 20, canSummon = true),
    RED_WITCH("Red Witch", health = 30, speed = 2, reward = 18, canDisableTowers = true),
    GREEN_WITCH("Green Witch", health = 25, speed = 2, reward = 15, canHeal = true),
    EWHAD("Ewhad", health = 200, speed = 1, reward = 100, canSummon = true, isBoss = true),
    DRAGON("Dragon", health = 500, speed = 1, reward = 0, isDragon = true, isBoss = true)  // Speed will be overridden: 1 on turn 1, 5 on turn 2+
}

data class Attacker(
    val id: Int,
    val type: AttackerType,
    val position: MutableState<Position>,
    val level: MutableState<Int> = mutableStateOf(1),  // Made mutable for dragons to scale with health
    val currentHealth: MutableState<Int> = mutableStateOf(type.health * level.value),
    val isDefeated: MutableState<Boolean> = mutableStateOf(false),
    val isDisabled: MutableState<Boolean> = mutableStateOf(false), // For towers disabled by Red Witch
    val disabledTurnsRemaining: MutableState<Int> = mutableStateOf(0),
    val summonCooldown: MutableState<Int> = mutableStateOf(0), // Cooldown for summoning abilities
    val dragonTurnsSinceSpawned: MutableState<Int> = mutableStateOf(0), // Track dragon movement state
    val isFlying: MutableState<Boolean> = mutableStateOf(false),  // Track if dragon is flying
    val spawnedFromLairId: Int? = null,  // Track which lair this dragon came from (for dragons only)
    val dragonName: String? = null  // Dragon's name (for dragons only)
) {
    val maxHealth: Int get() = type.health * level.value
    
    // Helper to check if this enemy can be damaged by specific attack types
    fun canBeDamagedByAcid(): Boolean = !type.immuneToAcid
    fun canBeDamagedByFireball(): Boolean = !type.immuneToFireball
    
    /**
     * Update dragon level based on current health.
     * Dragon level = max(1, currentHealth / baseHealth)
     * Only applies to dragons.
     */
    fun updateDragonLevel() {
        if (type.isDragon && currentHealth.value > 0) {
            val baseHealth = type.health
            val newLevel = maxOf(1, currentHealth.value / baseHealth)
            level.value = newLevel
        }
    }
}
