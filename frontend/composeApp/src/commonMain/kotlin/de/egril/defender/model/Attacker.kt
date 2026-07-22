package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * The kind of aura a [VillainAbility] projects onto units in range.
 */
enum class VillainAuraEffect {
    /** Grants extra movement (cells per turn) to friendly units of the villain's faction. */
    SPEED,
}

/**
 * A configurable villain aura ability.
 *
 * Villains (see [AttackerType.isVillain]) are unique enemy "heroes" that buff nearby friendly units
 * (or debuff the player's defenders) as long as they are on the battlefield. Every ability has a
 * [range] (measured in hex cells; use a large value such as [FULL_BATTLEFIELD] to cover the whole map)
 * and activates every [cooldown] rounds.
 *
 * @param effect     what the aura does.
 * @param range      radius in hex cells the aura reaches (use [FULL_BATTLEFIELD] for the entire map).
 * @param cooldown   number of rounds between activations (1 = every round / passive).
 * @param magnitude  strength of the effect (e.g. extra movement cells for [VillainAuraEffect.SPEED]).
 */
data class VillainAbility(
    val effect: VillainAuraEffect,
    val range: Int,
    val cooldown: Int,
    val magnitude: Int = 1,
) {
    companion object {
        /** Sentinel range covering the entire battlefield. */
        const val FULL_BATTLEFIELD: Int = Int.MAX_VALUE
    }
}

enum class AttackerType(
    val displayName: String,
    val health: Int,
    val speed: Int, // cells per turn
    val reward: Int, // coins when defeated
    val xp: Int, // XP when defeated (multiplied by level for non-dragons)
    val immuneToAcid: Boolean = false,
    val immuneToFireball: Boolean = false,
    val canSummon: Boolean = false,
    val canDisableTowers: Boolean = false,
    val canHeal: Boolean = false,
    val isBoss: Boolean = false,
    val isDragon: Boolean = false,
    val canBuildBridge: Boolean = false, // Can build bridges (Ork, Troll, Evil Wizard, Ewhad)
    // Villain system: villains are unique enemy "heroes" (see issue #538). Only one attacker of a
    // given villain subtype can exist on the battlefield at once and their health is never displayed.
    val isVillain: Boolean = false,
    val faction: EnemyFaction = EnemyFaction.NONE,
    val villainAbility: VillainAbility? = null,
    // Language-independent short name for villains, shown on the battlefield icon in place of the
    // health points. Villain proper names are identical in every language, so they live here in the
    // enum rather than in the translated string resources (see issue discussion). Null for regular
    // enemies, whose (translated) names come from the string resources instead.
    val villainName: String? = null,
) {
    GOBLIN("Goblin", health = 20, speed = 5, reward = 5, xp = 3, faction = EnemyFaction.HORDE),
    ORK("Ork", health = 40, speed = 2, reward = 10, xp = 6, canBuildBridge = true, faction = EnemyFaction.HORDE),
    OGRE("Ogre", health = 80, speed = 1, reward = 20, xp = 12, canBuildBridge = true, faction = EnemyFaction.HORDE),
    SKELETON("Skeleton", health = 15, speed = 5, reward = 7, xp = 4, faction = EnemyFaction.UNDEAD),
    EVIL_WIZARD("Evil Wizard", health = 30, speed = 2, reward = 15, xp = 9, canBuildBridge = true),
    BLUE_DEMON("Blue Demon", health = 15, speed = 6, reward = 10, xp = 6, immuneToAcid = true),
    RED_DEMON("Red Demon", health = 60, speed = 1, reward = 15, xp = 9, immuneToFireball = true),
    RED_WITCH("Red Witch", health = 30, speed = 5, reward = 18, xp = 11, canDisableTowers = true),
    GREEN_WITCH("Green Witch", health = 25, speed = 5, reward = 15, xp = 9, canHeal = true),

    // Weak early-game minion summoned by Gribnak the Squealer: 5 HP but very nimble (5 tiles/turn)
    SNOTLING("Snotling", health = 5, speed = 5, reward = 1, xp = 1),

    EWHAD(
        "Ewhad",
        health = 200,
        speed = 1,
        reward = 100,
        xp = 60,
        canSummon = true,
        isBoss = true,
        canBuildBridge = true,
        isVillain = true,
        villainName = "Ewhad",
    ),
    DRAGON("Dragon", health = 500, speed = 2, reward = 0, xp = 50, isDragon = true, isBoss = true), // Speed will be overridden: 2 on turn 1, 10 on turn 2+. XP is given per level lost, not multiplied

    // --- Villains (unique enemy heroes) ---

    // Gribnak the Squealer: weak early-game mini-boss that rallies snotlings around itself.
    // Snotling Rally: spawns snotlings on free path tiles within range 2 (3-turn cooldown).
    SNOTLING_BOSS(
        "Gribnak the Squealer",
        health = 30,
        speed = 3,
        reward = 25,
        xp = 15,
        canSummon = true,
        isBoss = true,
        isVillain = true,
        villainName = "Gribnak",
    ),

    // Garokk the Skullsplitter: tyrannical warchief who unites the Horde. War Cry (every 3 rounds)
    // grants +1 movement to nearby Horde units. Has no damage immunities, but is a tough boss.
    GAROKK(
        "Garokk the Skullsplitter",
        health = 250,
        speed = 1,
        reward = 300,
        xp = 90,
        isBoss = true,
        canBuildBridge = true,
        isVillain = true,
        faction = EnemyFaction.HORDE,
        villainAbility = VillainAbility(effect = VillainAuraEffect.SPEED, range = 3, cooldown = 3, magnitude = 1),
        villainName = "Garokk",
    ),

    // Morguk Bonewhisper: Goblin Shaman who supports goblin forces and disrupts defenders.
    // War Totem Aura: every turn, grants +1 movement to nearby Horde units (range 3).
    // Hex of Silence: disables an adjacent defender each turn.
    // Spirit Summon: every 3 turns, spawns goblins on all adjacent path tiles.
    MORGUK_BONEWHISPER(
        "Morguk Bonewhisper",
        health = 80,
        speed = 3,
        reward = 75,
        xp = 45,
        canSummon = true,
        canDisableTowers = true,
        isBoss = true,
        isVillain = true,
        faction = EnemyFaction.HORDE,
        villainAbility = VillainAbility(effect = VillainAuraEffect.SPEED, range = 3, cooldown = 1, magnitude = 1),
        villainName = "Morguk",
    ),
}

/**
 * Faction an enemy belongs to. Villain auras only affect friendly units of the same faction.
 */
enum class EnemyFaction {
    NONE,
    HORDE, // Orks, Goblins, Ogres
    UNDEAD, // Skeletons and necromancy
}

/**
 * True if a given attacker type's health should be hidden on its battlefield icon.
 * Villains (including the Ewhad boss, which is a villain) never show their health points.
 */
val AttackerType.hidesHealthBar: Boolean
    get() = isVillain

data class Attacker(
    val id: Int,
    val type: AttackerType,
    val position: MutableState<Position>,
    val level: MutableState<Int> = mutableStateOf(1), // Made mutable for dragons to scale with health
    val currentHealth: MutableState<Int> = mutableStateOf(type.health * level.value),
    val isDefeated: MutableState<Boolean> = mutableStateOf(false),
    val wasMerged: MutableState<Boolean> = mutableStateOf(false), // True when a snotling was absorbed by another snotling (no XP/coins awarded)
    val isDisabled: MutableState<Boolean> = mutableStateOf(false), // For towers disabled by Red Witch
    val disabledTurnsRemaining: MutableState<Int> = mutableStateOf(0),
    val summonCooldown: MutableState<Int> = mutableStateOf(0), // Cooldown for summoning abilities
    val dragonTurnsSinceSpawned: MutableState<Int> = mutableStateOf(0), // Track dragon movement state
    val isFlying: MutableState<Boolean> = mutableStateOf(false), // Track if dragon is flying
    val spawnedFromLairId: Int? = null, // Track which lair this dragon came from (for dragons only)
    val dragonName: String? = null, // Dragon's name (for dragons only)
    val currentTarget: MutableState<Position>? = null, // Current target position (waypoint or final target). Null means use level target.
    val targetMineId: MutableState<Int?> = mutableStateOf(null), // ID of mine being targeted (for greedy dragons)
    val mineWarningShown: MutableState<Boolean> = mutableStateOf(false), // Track if mine warning has been shown for current target
    val isBuildingBridge: MutableState<Boolean> = mutableStateOf(false), // Track if this unit is currently building a bridge (sacrifice units)
    val movementPenalty: MutableState<Int> = mutableStateOf(0), // Movement points lost due to spike tower barbs (level 10+)
    val speedBonus: MutableState<Int> = mutableStateOf(0), // Extra movement granted by a villain aura (e.g. Garokk's War Cry)
    val villainCooldown: MutableState<Int> = mutableStateOf(0), // Rounds until this villain's ability next activates
) {
    // Callback for dragon level changes (for achievements)
    var onDragonLevelChanged: ((oldLevel: Int, newLevel: Int) -> Unit)? = null

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
            val oldLevel = level.value
            val newLevel = maxOf(1, currentHealth.value / baseHealth)

            if (oldLevel != newLevel) {
                level.value = newLevel
                // Emit event for achievement tracking
                onDragonLevelChanged?.invoke(oldLevel, newLevel)
            }
        }
    }

    /**
     * Calculate damage when this enemy reaches the target.
     * - Mighty enemies (wizards, witches, mages, demons, dragons): 1 HP × enemy level
     * - Ewhad (boss): All remaining health points (special handling required by caller)
     * - All other enemies: 1 HP
     */
    fun calculateTargetDamage(): Int = attackerTargetDamage(type, level.value)
}

/**
 * Calculate the health-point damage a given enemy [type] at [level] deals when it reaches the target.
 * Mirrors [Attacker.calculateTargetDamage] but works from a plain type/level pair so it can be used
 * for enemies that have not spawned yet (see [PlannedEnemySpawn]).
 */
fun attackerTargetDamage(
    type: AttackerType,
    level: Int,
): Int =
    when (type) {
        AttackerType.EVIL_WIZARD,
        AttackerType.RED_WITCH,
        AttackerType.GREEN_WITCH,
        AttackerType.BLUE_DEMON,
        AttackerType.RED_DEMON,
        AttackerType.DRAGON,
        -> level
        AttackerType.EWHAD -> Int.MAX_VALUE // Special marker for "all HP" - caller must handle
        AttackerType.GAROKK -> level // Boss villain: 1 HP per level, like other mighty enemies
        AttackerType.MORGUK_BONEWHISPER -> level // Goblin Shaman villain: 1 HP per level
        else -> 1 // Goblin, Ork, Ogre, Skeleton
    }

/**
 * True for enemy types that can summon additional enemies during the enemy turn.
 * Such enemies can create an unbounded number of extra units, so the maximum threat they
 * pose to the player's health points cannot be reliably bounded ahead of time.
 */
fun AttackerType.isSummoner(): Boolean = this == AttackerType.EVIL_WIZARD || this == AttackerType.EWHAD || this == AttackerType.SNOTLING_BOSS || this == AttackerType.MORGUK_BONEWHISPER

/**
 * Returns true if this attacker is immune to a single attack from a defender of [defenderType].
 * Red Demons are immune to fireballs (AREA), Blue Demons are immune to acid (LASTING).
 * Used for attack-damage previews shown when a defender is selected.
 */
fun Attacker.isImmuneToAttackFrom(defenderType: DefenderType): Boolean =
    when (defenderType.attackType) {
        AttackType.AREA -> type.immuneToFireball
        AttackType.LASTING -> type.immuneToAcid
        else -> false
    }

/**
 * Returns true if a unique enemy of [type] cannot spawn because one is already alive on the
 * battlefield. Villains (see [AttackerType.isVillain]) are unique: only one of each subtype may
 * exist at a time. The Ewhad boss is a villain and therefore also unique.
 */
fun isUniqueEnemyAlreadyPresent(
    type: AttackerType,
    attackers: List<Attacker>,
): Boolean {
    val mustBeUnique = type.isVillain
    return mustBeUnique && attackers.any { it.type == type && !it.isDefeated.value }
}
