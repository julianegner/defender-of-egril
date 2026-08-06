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
    // Ethereal resistance: immune to all tower attacks except direct magic (wizard fireball).
    // Player-cast spells still affect the unit normally. Used by Ghost.
    val immuneToNonMagicTowerDamage: Boolean = false,
    // Shadow resistance: immune to all non-magical attacks (MELEE and RANGED). Only magical attacks
    // (AREA/fireball and mana spells) can pierce this dark shroud. Used by Xarithon the Shadow Dragon.
    val immuneToNonMagical: Boolean = false,
    // Shadow Spew: every N rounds, shadowy flames erupt in a 2×2 area, disabling all towers there
    // for a fixed number of turns. Null means no shadow spew ability.
    val shadowSpewCooldown: Int? = null,
    val shadowSpewDurationTurns: Int = 0,
    // Seaworthy: the unit can traverse river tiles (move on them) and land there at end of turn.
    // Used by Cap'n Roderich.
    val canTraverseRiver: Boolean = false,
    // Broadside: every N rounds, fires a cannonball at the nearest barge (water tower), sinking it.
    // The tower cost is added to Roderich's treasure. Null means no Broadside ability.
    val broadsideCooldown: Int? = null,
    // Seaworthy damage reduction: fraction of damage absorbed (0.5 = 50% reduction) when attacked
    // by a tower mounted on a barge (raft). 0f means no reduction.
    val seaworthyDamageReduction: Float = 0f,
    // Coins per enemy turn: coins added to this unit's personal treasure chest each enemy turn.
    // Used by Cap'n Roderich's Gold Treasure passive.
    val coinsPerTurn: Int = 0,
    // Gold reward multiplier: multiplies the base coin reward on defeat.
    // Roderich drops 3× coins; accumulated treasure is also awarded on defeat.
    val goldRewardMultiplier: Int = 1,
    // Water-only movement: unit can ONLY traverse river tiles and may NOT move on land/path tiles.
    // Combined with canTraverseRiver = true so both water occupancy and pathfinding work.
    // Used by The Kraken.
    val canOnlyMoveOnWater: Boolean = false,
    // Barge Grip: every N rounds the unit grips an adjacent barge for one turn (player cannot sell
    // the tower while gripped) then drags it under and sinks it (no refund). Null = no grip ability.
    val bargeGripCooldown: Int? = null,
    // Barge grip range: maximum hex distance at which the unit can grip a barge.
    val bargeGripRange: Int = 1,
    // Dive ability: unit can submerge below the water surface for a turn, becoming invisible and
    // unattackable. True = dive is available; false = no dive ability.
    val canDive: Boolean = false,
    // How many enemy turns the dive lasts before the unit re-surfaces.
    val diveDurationTurns: Int = 1,
    // Dive cooldown: minimum number of enemy turns between consecutive dives.
    val diveCooldown: Int = 5,
    // Spawn point compatibility: determines which type of spawn point this unit may use.
    // canSpawnOnLand = true: unit can enter from a land spawn point (default for most enemies).
    // canSpawnOnWater = true: unit can enter from a water spawn point (river/sea entry).
    // Both flags may be true (e.g. Cap'n Roderich, who sails rivers but can also march on land).
    val canSpawnOnLand: Boolean = true,
    val canSpawnOnWater: Boolean = false,
    // Blade immunity: immune to melee and ranged (physical/blade) tower attacks.
    // Only area (fireball) and lasting (acid) attacks can deal damage. Used by Troll.
    val immuneToBladeAttacks: Boolean = false,
    // Alternating movement: unit moves every second enemy turn (moves on odd turns, pauses on even).
    // The turn parity is tracked via [Attacker.movementTurnsElapsed]. Used by Troll.
    val movesEveryOtherTurn: Boolean = false,
    // Trample: when moving onto a tile occupied by a smaller enemy unit (smaller than ORK),
    // that unit is immediately slain. The troll never tramples villains or dragons. Used by Troll.
    val canTrampleSmallerEnemies: Boolean = false,
    // Barricade damage multiplier: multiplies damage dealt to barricades. 1 = normal. Used by Troll (10×).
    val barricadeDamageMultiplier: Int = 1,
) {
    GOBLIN("Goblin", health = 20, speed = 5, reward = 5, xp = 3, faction = EnemyFaction.HORDE),
    ORK("Ork", health = 40, speed = 2, reward = 10, xp = 6, canBuildBridge = true, faction = EnemyFaction.HORDE),
    OGRE("Ogre", health = 80, speed = 1, reward = 20, xp = 12, canBuildBridge = true, faction = EnemyFaction.HORDE),
    // Troll: a creature made of stone. Immune to blade (melee/ranged) attacks.
    // Moves 1 tile per turn and then pauses one turn (alternating movement).
    // Can trample smaller enemy units (smaller than an Ork) that stand in its way.
    // Deals 10× damage to barricades.
    TROLL(
        "Troll",
        health = 200,
        speed = 1,
        reward = 30,
        xp = 18,
        canBuildBridge = true,
        faction = EnemyFaction.HORDE,
        immuneToBladeAttacks = true,
        movesEveryOtherTurn = true,
        canTrampleSmallerEnemies = true,
        barricadeDamageMultiplier = 10,
    ),
    SKELETON("Skeleton", health = 15, speed = 5, reward = 7, xp = 4, faction = EnemyFaction.UNDEAD),
    ZOMBIE("Zombie", health = 25, speed = 1, reward = 6, xp = 4, faction = EnemyFaction.UNDEAD),
    EVIL_WIZARD("Evil Wizard", health = 30, speed = 2, reward = 15, xp = 9, canBuildBridge = true),
    BLUE_DEMON("Blue Demon", health = 15, speed = 6, reward = 10, xp = 6, immuneToAcid = true),
    RED_DEMON("Red Demon", health = 60, speed = 1, reward = 15, xp = 9, immuneToFireball = true),
    GHOST(
        "Ghost",
        health = 20,
        speed = 3,
        reward = 10,
        xp = 6,
        canFlyOverTerrain = true,
        immuneToAcid = true,
        immuneToNonMagicTowerDamage = true,
        faction = EnemyFaction.UNDEAD,
    ),
    PIRATE(
        "Pirate",
        health = 35,
        speed = 2,
        reward = 12,
        xp = 7,
        canTraverseRiver = true,
        canSpawnOnLand = true,
        canSpawnOnWater = true,
    ),
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
        speed = 6,
        reward = 20,
        xp = 25,
        isBoss = false,
        canFlyOverTerrain = true,
    ),

    // Xarithon the Shadow Dragon: the personal champion of Ewhad, a shadow dragon filled with dark
    // energy. Uses the standard dragon alternating walk/fly movement mechanic.
    // Shadow Spew (every 3 rounds): dark flames erupt in a 2×2 area, shutting down all towers there
    // for 2 rounds — an enhanced version of the Red Witch disable effect.
    // Shadow Resistance (passive): immune to all non-magical attacks (melee, ranged, acid); only
    // fireball (wizard) and mana spells can pierce the dark shroud.
    XARITHON_THE_SHADOW_DRAGON(
        "Xarithon the Shadow Dragon",
        health = 800,
        speed = 2,
        reward = 500,
        xp = 150,
        isDragon = true,
        isBoss = true,
        isVillain = true,
        villainName = "Xarithon",
        immuneToAcid = true,
        immuneToNonMagical = true,
        shadowSpewCooldown = 3,
        shadowSpewDurationTurns = 2,
    ),

    // Cap'n Roderich, Scourge of the Seas: a notorious seafaring pirate villain who menaces the
    // waterways of the fantasy realm. His pockets overflow with stolen riches.
    //
    // Seaworthy (passive): navigates both land and water; takes 50 % less damage from barge (raft-
    // mounted) towers. When on a river tile the barge icon is drawn beneath him.
    //
    // Broadside (every 3 rounds): fires a cannonball at the nearest barge, sinking it immediately.
    // The player receives no refund; the tower's cost is added to Roderich's treasure instead.
    //
    // Gold Treasure (passive): each enemy turn he collects coins (coinsPerTurn). When defeated he
    // drops 3× the normal villain reward plus all accumulated treasure — immediately added to the
    // player's treasury so they can spend it on powerful upgrades.
    CAPTAIN_RODERICH(
        "Cap'n Roderich, Scourge of the Seas",
        health = 300,
        speed = 2,
        reward = 200,
        xp = 100,
        isBoss = true,
        isVillain = true,
        villainName = "Roderich",
        canTraverseRiver = true,
        broadsideCooldown = 3,
        seaworthyDamageReduction = 0.5f,
        coinsPerTurn = 10,
        goldRewardMultiplier = 3,
        canSpawnOnLand = true,
        canSpawnOnWater = true,
    ),

    // The Kraken: an ancient deep-sea horror that haunts waterways and drags barges to the abyss.
    //
    // Water Domain (passive): moves exclusively on river/water tiles; cannot set foot on land.
    //
    // Barge Grip (every 4 rounds): seizes an adjacent barge in its tentacles, holding it for one
    // turn — the tower on that barge cannot be sold while gripped. On the following turn the Kraken
    // drags the barge under, sinking it instantly (no coins refunded).
    //
    // Dive (periodic): the Kraken plunges beneath the surface for one turn, becoming invisible and
    // unattackable by any tower.
    //
    // Immune to acid (water creature).
    THE_KRAKEN(
        "The Kraken",
        health = 400,
        speed = 2,
        reward = 250,
        xp = 120,
        immuneToAcid = true,
        isBoss = true,
        isVillain = true,
        villainName = "Kraken",
        canTraverseRiver = true,
        canOnlyMoveOnWater = true,
        bargeGripCooldown = 4,
        bargeGripRange = 2,
        canDive = true,
        diveDurationTurns = 1,
        diveCooldown = 7,
        canSpawnOnLand = false,
        canSpawnOnWater = true,
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
    val wasMerged: MutableState<Boolean> = mutableStateOf(false), // True for non-reward removals (e.g. swarm merge or trample; no XP/coins awarded)
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
    // Cap'n Roderich's accumulated treasure: grows by coinsPerTurn each enemy turn and by the cost
    // of each barge he sinks via Broadside. The entire treasure is awarded to the player on defeat.
    val treasureCoins: MutableState<Int> = mutableStateOf(0),
    // Kraken: true while the unit is submerged (diving). Diving enemies are invisible and cannot
    // be targeted by towers.
    val isDiving: MutableState<Boolean> = mutableStateOf(false),
    // Number of enemy turns remaining in the current dive before the Kraken re-surfaces.
    val diveTurnsRemaining: MutableState<Int> = mutableStateOf(0),
    // Kraken Barge Grip: ID of the raft currently held in the Kraken's grip (null when not gripping).
    val grippedRaftId: MutableState<Int?> = mutableStateOf(null),
    // Kraken Barge Grip phase: 0 = no grip (cooldown), 1 = gripping (tower locked), 2 = dragging under (sinks next).
    val bargeGripPhase: MutableState<Int> = mutableStateOf(0),
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
        AttackerType.XARITHON_THE_SHADOW_DRAGON -> level // Shadow dragon finale boss: level damage on reach
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
        this == AttackerType.GHOST ||
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
 * Xarithon is immune to non-magical attacks (MELEE and RANGED).
 * Used for attack-damage previews shown when a defender is selected.
 */
fun Attacker.isImmuneToAttackFrom(defenderType: DefenderType): Boolean =
    when {
        type.immuneToNonMagicTowerDamage && defenderType.attackType != AttackType.AREA -> true
        type.immuneToBladeAttacks && (defenderType.attackType == AttackType.MELEE || defenderType.attackType == AttackType.RANGED) -> true
        else ->
            when (defenderType.attackType) {
                AttackType.AREA -> type.immuneToFireball
                AttackType.LASTING -> type.immuneToAcid
                AttackType.MELEE, AttackType.RANGED -> type.immuneToNonMagical
                else -> false
            }
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
