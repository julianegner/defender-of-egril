package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * The kind of aura a [VillainAbility] projects onto units in range.
 */
enum class VillainAuraEffect {
    /** Grants extra movement (cells per turn) to friendly units of the villain's faction. */
    SPEED,

    /** Passive necromancy that resurrects fallen allies on the next round. */
    SOUL_CALL,

    /** Nearby Green Witch units heal allied enemies for 50 % more HP per round. */
    COVEN_HEAL_BOOST,

    /** Nearby Red Witch units extend their tower-disable duration by +1 extra round. */
    COVEN_DISABLE_BOOST,

    /** Combined coven synergy: both COVEN_HEAL_BOOST and COVEN_DISABLE_BOOST simultaneously. */
    COVEN_SYNERGY,
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
    val isRobotic: Boolean = false,
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
    // Mirror images reuse villain presentation (hidden HP, same icon/name) but must not count as
    // the unique "real" villain for spawn limits, defeat messages, or instant-loss target breaches.
    val isMirrorImage: Boolean = false,
    val mirrorImageCount: Int? = null,
    val mirrorImageRange: Int? = null,
    val mirrorImageCooldown: Int? = null,
    val mirrorBlindDurationTurns: Int? = null,
    val towerDisableRangeBase: Int? = null,
    val towerDisableCooldown: Int? = null,
    val towerDisableDurationTurns: Int? = null,
    val soulCallRange: Int? = null,
    val shieldWallFormationWidth: Int = 0,
    // Coven swap ability for Sybilla: every N rounds she can swap places with a nearby witch.
    // Null means no swap ability. The runtime cooldown is tracked via [Attacker.summonCooldown].
    val covenSwapCooldown: Int? = null,
    // Self-heal amount per enemy turn (0 = no self-healing). Used by Sylvanas the Molding.
    val selfHealPerTurn: Int = 0,
    // Floating movement: can pass over any tile type during movement but must end turn on a path
    // tile. Used by Archmage Malakor the Renegade.
    val canFlyOverTerrain: Boolean = false,
) {
    GOBLIN("Goblin", health = 20, speed = 5, reward = 5, xp = 3, faction = EnemyFaction.HORDE),
    ORK("Ork", health = 40, speed = 2, reward = 10, xp = 6, canBuildBridge = true, faction = EnemyFaction.HORDE),
    OGRE("Ogre", health = 80, speed = 1, reward = 20, xp = 12, canBuildBridge = true, faction = EnemyFaction.HORDE),
    SKELETON("Skeleton", health = 15, speed = 5, reward = 7, xp = 4, faction = EnemyFaction.UNDEAD),
    ZOMBIE("Zombie", health = 25, speed = 1, reward = 6, xp = 4, faction = EnemyFaction.UNDEAD),
    EVIL_WIZARD("Evil Wizard", health = 30, speed = 2, reward = 15, xp = 9, canBuildBridge = true),
    BLUE_DEMON("Blue Demon", health = 15, speed = 6, reward = 10, xp = 6, immuneToAcid = true),
    RED_DEMON("Red Demon", health = 60, speed = 1, reward = 15, xp = 9, immuneToFireball = true),
    RED_WITCH("Red Witch", health = 30, speed = 5, reward = 18, xp = 11, canDisableTowers = true),
    GREEN_WITCH("Green Witch", health = 25, speed = 5, reward = 15, xp = 9, canHeal = true),

    // Weak early-game minion summoned by Gribnak the Squealer: 5 HP but very nimble (5 tiles/turn)
    SNOTLING("Snotling", health = 5, speed = 5, reward = 1, xp = 1),

    // Weak spider swarm unit summoned by Araxxa. Behaves like a snotling stack.
    SPIDERLING("Spiderling", health = 10, speed = 5, reward = 1, xp = 1),

    // Mechanical goblin spawned by Baron Ratterzahn's Scrap-Bot wreckage.
    ROBOTIC_GOBLIN("Robotic Goblin", health = 40, speed = 5, reward = 6, xp = 4, isRobotic = true, faction = EnemyFaction.HORDE),

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
    UNDEAD_DRAGON("Undead Dragon", health = 500, speed = 2, reward = 0, xp = 50, isDragon = true, isBoss = true, faction = EnemyFaction.UNDEAD),

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

    // Araxxa the Giant Spider: villain that spawns spiderlings and spreads a web area that
    // empowers spiders standing inside it.
    ARAXXA(
        "Araxxa the Giant Spider",
        health = 90,
        speed = 2,
        reward = 80,
        xp = 48,
        canSummon = true,
        isBoss = true,
        isVillain = true,
        villainName = "Araxxa",
    ),
    BARON_RATTERZAHN(
        "Baron Ratterzahn",
        health = 140,
        speed = 2,
        reward = 120,
        xp = 60,
        canSummon = true,
        canDisableTowers = true,
        isBoss = true,
        isVillain = true,
        faction = EnemyFaction.HORDE,
        villainName = "Ratterzahn",
        towerDisableRangeBase = 5,
        towerDisableCooldown = 5,
        towerDisableDurationTurns = 3,
    ),
    SILAS_THE_MASKMASTER(
        "Silas the Maskmaster",
        health = 110,
        speed = 2,
        reward = 130,
        xp = 65,
        canSummon = true,
        canDisableTowers = true,
        isBoss = true,
        isVillain = true,
        villainName = "Silas",
        mirrorImageCount = 2,
        mirrorImageRange = 2,
        mirrorImageCooldown = 3,
        mirrorBlindDurationTurns = 2,
    ),
    SILAS_MIRROR_IMAGE(
        "Silas Mirror Image",
        health = 110,
        speed = 2,
        reward = 0,
        xp = 0,
        canSummon = true,
        canDisableTowers = true,
        isBoss = true,
        isVillain = true,
        villainName = "Silas",
        isMirrorImage = true,
    ),
    FALLEN_SHIELDMAIDEN_FREYA(
        "Fallen Shieldmaiden Freya",
        health = 280,
        speed = 1,
        reward = 150,
        xp = 80,
        isBoss = true,
        isVillain = true,
        faction = EnemyFaction.UNDEAD,
        villainName = "Freya",
        shieldWallFormationWidth = 3,
    ),
    PRINCE_VALERIUS_THE_SOULREAPER(
        "Prince Valerius the Soulreaper",
        health = 180,
        speed = 2,
        reward = 140,
        xp = 75,
        isBoss = true,
        isVillain = true,
        faction = EnemyFaction.UNDEAD,
        villainAbility = VillainAbility(effect = VillainAuraEffect.SOUL_CALL, range = 3, cooldown = 1),
        villainName = "Valerius",
        soulCallRange = 3,
    ),

    // Grand Coven-Mother Sybilla: powerful witch coven leader who coordinates red and green witches.
    // Coven Synergy (passive): Green witches within 3 tiles heal 50% more; Red witches within 3 tiles
    // extend tower disables by +1 extra round. Also has all green- and red-witch abilities herself.
    // Every 5 rounds she swaps places with a witch within 3 tiles.
    GRAND_COVEN_MOTHER_SYBILLA(
        "Grand Coven-Mother Sybilla",
        health = 180,
        speed = 2,
        reward = 200,
        xp = 90,
        canHeal = true,
        canDisableTowers = true,
        isBoss = true,
        isVillain = true,
        villainName = "Sybilla",
        villainAbility = VillainAbility(effect = VillainAuraEffect.COVEN_SYNERGY, range = 3, cooldown = 1),
        covenSwapCooldown = 5,
    ),

    // Haga: the healing twin. Green Witch with all normal green-witch abilities.
    // Coven synergy: Green witches within 3 tiles heal 50% more HP per round.
    HAGA(
        "Haga",
        health = 40,
        speed = 3,
        reward = 60,
        xp = 30,
        canHeal = true,
        isBoss = true,
        isVillain = true,
        villainName = "Haga",
        villainAbility = VillainAbility(effect = VillainAuraEffect.COVEN_HEAL_BOOST, range = 3, cooldown = 1),
    ),

    // Zussa: the disabling twin. Red Witch with all normal red-witch abilities.
    // Coven synergy: Red witches within 3 tiles extend tower disables by +1 extra round.
    ZUSSA(
        "Zussa",
        health = 40,
        speed = 3,
        reward = 60,
        xp = 30,
        canDisableTowers = true,
        isBoss = true,
        isVillain = true,
        villainName = "Zussa",
        villainAbility = VillainAbility(effect = VillainAuraEffect.COVEN_DISABLE_BOOST, range = 3, cooldown = 1),
    ),

    // Sylvanas the Molding: corrupted forest spirit who leaves a trail of rot.
    // Root Grip (every 3 rounds): thorny vines burst from beneath a tower within range 3,
    // blocking it for 2 player turns.
    // Self-Healing: restores 10 HP every enemy turn.
    SYLVANAS_THE_MOLDING(
        "Sylvanas the Molding",
        health = 120,
        speed = 3,
        reward = 120,
        xp = 80,
        canDisableTowers = true,
        isBoss = true,
        isVillain = true,
        villainName = "Sylvanas",
        towerDisableRangeBase = 4,
        towerDisableCooldown = 1,
        towerDisableDurationTurns = 2,
        selfHealPerTurn = 20,
    ),

    // Archmage Malakor the Renegade: powerful mage corrupted by forbidden astral magic.
    // Time Loop (every 2 rounds): slows the flow of time in a radius of 5 tiles, forcing all
    // affected towers to skip their current round.
    // Floating: moves over any terrain (path, water, towers…) but must end his turn on a path tile.
    ARCHMAGE_MALAKOR_THE_RENEGADE(
        "Archmage Malakor the Renegade",
        health = 160,
        speed = 2,
        reward = 200,
        xp = 90,
        canDisableTowers = true,
        isBoss = true,
        isVillain = true,
        villainName = "Malakor",
        towerDisableRangeBase = 5,
        towerDisableCooldown = 2,
        towerDisableDurationTurns = 1,
        canFlyOverTerrain = true,
    ),

    // Ignis-Va, the Dragonvoice: sinister dragon cultist, half human and half dragon.
    // Call of the Brood (every 3 rounds): summons two flying Dragon-Terrors that can fly over obstacles.
    // On defeat: leaves behind a burning tile that disables nearby towers for 2 rounds.
    // Immune to fireball attacks.
    IGNIS_VA_THE_DRAGONVOICE(
        "Ignis-Va, the Dragonvoice",
        health = 200,
        speed = 2,
        reward = 200,
        xp = 90,
        immuneToFireball = true,
        canSummon = true,
        isBoss = true,
        isVillain = true,
        villainName = "Ignis-Va",
    ),

    // Dragon-Terror: small flying dragon summoned by Ignis-Va. Always airborne — flies over water,
    // impassable terrain, and barricades. Does not target mines; always attacks the target objective.
    // Speed is double a regular dragon's base speed. Health is half a regular dragon's.
    // Every Dragon-Terror spawns at least level 2 and deals its level as target damage.
    DRAGON_TERROR(
        "Dragon-Terror",
        health = 250,
        speed = 4,
        reward = 20,
        xp = 25,
        isBoss = false,
        canFlyOverTerrain = true,
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

/**
 * True for battlefield-persistent villains that count for uniqueness, narrative enter/defeat
 * messages, and instant-loss target breaches. Mirror-image decoys intentionally do not count.
 */
val AttackerType.isRealVillain: Boolean
    get() = isVillain && !isMirrorImage

/**
 * Companion attacker types that are automatically spawned alongside this attacker when it first
 * enters the battlefield. Each companion is spawned once at a free position near the same spawn
 * point. Companions that are already on the battlefield (unique-villain check) are not re-spawned.
 */
val AttackerType.arrivalCompanions: List<AttackerType>
    get() =
        when (this) {
            AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> listOf(AttackerType.HAGA, AttackerType.ZUSSA)
            else -> emptyList()
        }

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
    val movementTurnsElapsed: MutableState<Int> = mutableStateOf(0), // Enemy turns elapsed on battlefield (for alternating movement patterns)
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
    val greed: Int get() = if (type == AttackerType.DRAGON) level.value / 5 else 0

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
        AttackerType.UNDEAD_DRAGON,
        -> level
        AttackerType.EWHAD -> Int.MAX_VALUE // Special marker for "all HP" - caller must handle
        AttackerType.GAROKK -> level // Boss villain: 1 HP per level, like other mighty enemies
        AttackerType.MORGUK_BONEWHISPER -> level // Goblin Shaman villain: 1 HP per level
        AttackerType.ARAXXA -> level // Giant spider villain: 1 HP per level
        AttackerType.BARON_RATTERZAHN -> level // Baron villain: 1 HP per level
        AttackerType.SILAS_THE_MASKMASTER -> level // Villain illusionist: 1 HP per level
        AttackerType.SILAS_MIRROR_IMAGE -> 0 // Illusions are decoys and never damage the target
        AttackerType.FALLEN_SHIELDMAIDEN_FREYA -> level // Death-knight villain: 1 HP per level
        AttackerType.PRINCE_VALERIUS_THE_SOULREAPER -> level // Lich villain: 1 HP per level
        AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> level // Coven-Mother villain: 1 HP per level
        AttackerType.HAGA -> level // Witch twin villain: 1 HP per level
        AttackerType.ZUSSA -> level // Witch twin villain: 1 HP per level
        AttackerType.SYLVANAS_THE_MOLDING -> level // Wild-nature villain: 1 HP per level
        AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE -> level // Archmage villain: 1 HP per level
        AttackerType.IGNIS_VA_THE_DRAGONVOICE -> level // Dragon-cultist villain: 1 HP per level
        AttackerType.DRAGON_TERROR -> level // Summoned flying dragon-terror: level damage on reach
        else -> 1 // Goblin, Ork, Ogre, Skeleton
    }

/**
 * True for enemy types that can summon additional enemies during the enemy turn.
 * Such enemies can create an unbounded number of extra units, so the maximum threat they
 * pose to the player's health points cannot be reliably bounded ahead of time.
 */
fun AttackerType.isSummoner(): Boolean =
    this == AttackerType.EVIL_WIZARD ||
        this == AttackerType.EWHAD ||
        this == AttackerType.SNOTLING_BOSS ||
        this == AttackerType.MORGUK_BONEWHISPER ||
        this == AttackerType.ARAXXA ||
        this == AttackerType.BARON_RATTERZAHN ||
        this == AttackerType.SILAS_THE_MASKMASTER ||
        this == AttackerType.PRINCE_VALERIUS_THE_SOULREAPER ||
        this == AttackerType.IGNIS_VA_THE_DRAGONVOICE

/**
 * Swarm units can stack by moving onto the same tile, merging their health into one unit.
 */
fun AttackerType.isSwarmUnit(): Boolean = this == AttackerType.SNOTLING || this == AttackerType.SPIDERLING

/**
 * Spider units receive a bonus while standing inside Araxxa's spider web.
 */
fun AttackerType.isSpider(): Boolean = this == AttackerType.SPIDERLING || this == AttackerType.ARAXXA

/**
 * Special enemies are grouped separately in the level editor.
 */
fun AttackerType.isSpecialEnemy(): Boolean =
    this == AttackerType.SNOTLING ||
        this == AttackerType.SPIDERLING ||
        this == AttackerType.ROBOTIC_GOBLIN ||
        this == AttackerType.ZOMBIE ||
        this == AttackerType.BLUE_DEMON ||
        this == AttackerType.RED_DEMON ||
        this == AttackerType.DRAGON ||
        this == AttackerType.UNDEAD_DRAGON ||
        this == AttackerType.SILAS_MIRROR_IMAGE ||
        this == AttackerType.DRAGON_TERROR

/**
 * Undead units rise in a stronger second form under Valerius's necromancy.
 */
fun AttackerType.isUndead(): Boolean = faction == EnemyFaction.UNDEAD

/**
 * Returns the unit Soul Call should summon for a fallen attacker, or null when the unit cannot be
 * resurrected.
 */
fun AttackerType.getSoulCallResurrectionType(): AttackerType? =
    when {
        isRobotic || this == AttackerType.SKELETON || this == AttackerType.UNDEAD_DRAGON || isMirrorImage -> null
        this == AttackerType.ZOMBIE -> AttackerType.SKELETON
        this == AttackerType.DRAGON -> AttackerType.UNDEAD_DRAGON
        isUndead() -> AttackerType.SKELETON
        else -> AttackerType.ZOMBIE
    }

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
    val mustBeUnique = type.isRealVillain
    return mustBeUnique && attackers.any { it.type == type && !it.isDefeated.value }
}
