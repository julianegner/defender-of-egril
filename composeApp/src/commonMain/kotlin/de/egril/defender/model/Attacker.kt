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
    val isDragon: Boolean = false,
    val canBuildBridge: Boolean = false  // Can build bridges (Ork, Troll, Evil Wizard, Ewhad)
) {
    GOBLIN("Goblin", health = 20, speed = 5, reward = 5),
    ORK("Ork", health = 40, speed = 2, reward = 10, canBuildBridge = true),
    OGRE("Ogre", health = 80, speed = 1, reward = 20, canBuildBridge = true),
    SKELETON("Skeleton", health = 15, speed = 5, reward = 7),
    EVIL_WIZARD("Evil Wizard", health = 30, speed = 2, reward = 15, canBuildBridge = true),
    WITCH("Witch", health = 25, speed = 4, reward = 12),
    BLUE_DEMON("Blue Demon", health = 15, speed = 6, reward = 10, immuneToAcid = true),
    RED_DEMON("Red Demon", health = 60, speed = 1, reward = 15, immuneToFireball = true),
    EVIL_MAGE("Evil Mage", health = 40, speed = 2, reward = 20, canSummon = true),
    RED_WITCH("Red Witch", health = 30, speed = 5, reward = 18, canDisableTowers = true),
    GREEN_WITCH("Green Witch", health = 25, speed = 5, reward = 15, canHeal = true),
    EWHAD("Ewhad", health = 200, speed = 1, reward = 100, canSummon = true, isBoss = true, canBuildBridge = true),
    DRAGON("Dragon", health = 500, speed = 2, reward = 0, isDragon = true, isBoss = true)  // Speed will be overridden: 2 on turn 1, 10 on turn 2+
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
    val dragonName: String? = null,  // Dragon's name (for dragons only)
    val currentTarget: MutableState<Position>? = null,  // Current target position (waypoint or final target). Null means use level target.
    val targetMineId: MutableState<Int?> = mutableStateOf(null),  // ID of mine being targeted (for greedy dragons)
    val mineWarningShown: MutableState<Boolean> = mutableStateOf(false),  // Track if mine warning has been shown for current target
    val isBuildingBridge: MutableState<Boolean> = mutableStateOf(false)  // Track if this unit is currently building a bridge (sacrifice units)
) {
    val maxHealth: Int get() = type.health * level.value
    
    /**
     * Calculate dragon's greed level based on its level.
     * Greed = level / 5
     * Level 0-4: greed = 0
     * Level 5-9: greed = 1
     * Level 10-14: greed = 2, etc.
     */
    val greed: Int get() = if (type.isDragon) level.value / 5 else 0
    
    /**
     * Check if dragon is very greedy (greed > 5, meaning level > 25)
     */
    val isVeryGreedy: Boolean get() = greed > 5
    
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
    
    /**
     * Calculate damage when this enemy reaches the target.
     * - Mighty enemies (wizards, witches, mages, demons, dragons): 1 HP × enemy level
     * - Ewhad (boss): All remaining health points (special handling required by caller)
     * - All other enemies: 1 HP
     */
    fun calculateTargetDamage(): Int {
        return when (type) {
            AttackerType.EVIL_WIZARD,
            AttackerType.WITCH,
            AttackerType.RED_WITCH,
            AttackerType.GREEN_WITCH,
            AttackerType.EVIL_MAGE,
            AttackerType.BLUE_DEMON,
            AttackerType.RED_DEMON,
            AttackerType.DRAGON -> level.value
            AttackerType.EWHAD -> Int.MAX_VALUE  // Special marker for "all HP" - caller must handle
            else -> 1  // Goblin, Ork, Ogre, Skeleton
        }
    }
}
