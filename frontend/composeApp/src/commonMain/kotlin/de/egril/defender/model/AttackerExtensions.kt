package de.egril.defender.model

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

/**
 * Returns true for attacker types that eat mushrooms: horde units (goblins, orks, ogres,
 * snotlings) and witches.
 */
val AttackerType.canEatMushroom: Boolean
    get() =
        this == AttackerType.GOBLIN ||
            this == AttackerType.GOBLIN_RUNNER ||
            this == AttackerType.ORK ||
            this == AttackerType.OGRE ||
            this == AttackerType.SNOTLING ||
            this == AttackerType.RED_WITCH ||
            this == AttackerType.GREEN_WITCH ||
            this == AttackerType.GRAND_COVEN_MOTHER_SYBILLA ||
            this == AttackerType.HAGA ||
            this == AttackerType.ZUSSA

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
        AttackerType.RUNEMASTER,
        AttackerType.RED_WITCH,
        AttackerType.GREEN_WITCH,
        AttackerType.BLUE_DEMON,
        AttackerType.RED_DEMON,
        AttackerType.DRAGON,
        AttackerType.UNDEAD_DRAGON,
        -> level
        AttackerType.EWHAD -> Int.MAX_VALUE // Special marker for "all HP" - caller must handle
        AttackerType.GARGOYLE -> level // Mighty unit: arriving at the target costs the player HP equal to its level
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
        AttackerType.MORVATH_THE_SHADOWMASTER -> level // Shadowmaster villain: 1 HP per level
        AttackerType.DRAGON_TERROR -> level // Summoned flying dragon-terror: level damage on reach
        AttackerType.XARITHON_THE_SHADOW_DRAGON -> level // Shadow dragon finale boss: level damage on reach
        AttackerType.ZYTHAR_THE_RIFTCALLER -> level // Riftcaller villain: 1 HP per level
        AttackerType.GRAND_RUNEMASTER_VAELEN -> level // Runemaster villain: 1 HP per level
        AttackerType.RAZZIK_THE_ROCKETEER -> level // Rocketeer villain: 1 HP per level
        else -> 1 // Goblin, Ork, Ogre, Skeleton
    }

/**
 * True for the Evil Wizard, Runemaster, and Ewhad wizard enemy types.
 */
fun AttackerType.isWizard(): Boolean =
    this == AttackerType.EVIL_WIZARD ||
        this == AttackerType.RUNEMASTER ||
        this == AttackerType.EWHAD ||
        this == AttackerType.GRAND_RUNEMASTER_VAELEN

/**
 * True for enemy types that can summon additional enemies during the enemy turn.
 * Such enemies can create an unbounded number of extra units, so the maximum threat they
 * pose to the player's health points cannot be reliably bounded ahead of time.
 */
fun AttackerType.isSummoner(): Boolean =
    isWizard() ||
        this == AttackerType.SNOTLING_BOSS ||
        this == AttackerType.MORGUK_BONEWHISPER ||
        this == AttackerType.ARAXXA ||
        this == AttackerType.BARON_RATTERZAHN ||
        this == AttackerType.SILAS_THE_MASKMASTER ||
        this == AttackerType.PRINCE_VALERIUS_THE_SOULREAPER ||
        this == AttackerType.IGNIS_VA_THE_DRAGONVOICE ||
        this == AttackerType.ZYTHAR_THE_RIFTCALLER

/**
 * Swarm units can stack by moving onto the same tile, merging their health into one unit.
 */
fun AttackerType.isSwarmUnit(): Boolean = this == AttackerType.SNOTLING || this == AttackerType.SPIDERLING || this == AttackerType.DEMONLING

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
        this == AttackerType.DEMONLING ||
        this == AttackerType.GOBLIN_RUNNER ||
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
        type.isGargoyle && defenderType in GargoyleRules.NO_DAMAGE_DEFENDERS -> true
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

/**
 * Returns the effective level of this attacker, including any mushroom level bonus.
 * During the mushroom buff, the effective level is doubled (level + mushroomLevelBonus).
 */
val Attacker.effectiveLevel: Int
    get() = level.value + mushroomLevelBonus.value

val Attacker.hasMushroomBuff: Boolean
    get() = mushroomTurnsRemaining.value > 0

val Attacker.displayLevel: Int
    get() = effectiveLevel
