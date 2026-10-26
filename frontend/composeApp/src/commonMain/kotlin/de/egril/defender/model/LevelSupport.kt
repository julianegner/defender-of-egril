package de.egril.defender.model

const val INDEFINITE_SUPPORT_COUNT = Int.MAX_VALUE

fun isIndefiniteSupportCount(count: Int): Boolean = count == INDEFINITE_SUPPORT_COUNT

fun combineSupportCounts(
    existing: Int,
    added: Int,
): Int =
    if (isIndefiniteSupportCount(existing) || isIndefiniteSupportCount(added)) {
        INDEFINITE_SUPPORT_COUNT
    } else {
        existing + added
    }

fun consumeSupportCount(count: Int): Int = if (isIndefiniteSupportCount(count)) count else count - 1

fun supportCountDisplayText(count: Int): String = if (isIndefiniteSupportCount(count)) "∞" else "$count"

/**
 * Types of placable support objects a player can deploy from level-granted tokens.
 * These mirror the objects that can already be placed via towers, but supports let the
 * player place them directly regardless of whether the appropriate tower and tech level exist.
 */
enum class SupportObjectType {
    DWARVEN_TRAP,
    MAGICAL_TRAP,
    BARRICADE,
}

/**
 * A placable support object granted to the player for a level.
 *
 * @param type    Which object can be placed.
 * @param count   How many of this object the player may place.
 * @param damage  Damage dealt when the object is a [SupportObjectType.DWARVEN_TRAP].
 * @param healthPoints Health points of the object when it is a [SupportObjectType.BARRICADE].
 */
data class SupportObject(
    val type: SupportObjectType,
    val count: Int = 1,
    val damage: Int = 10,
    val healthPoints: Int = 50,
)

/**
 * A spell token granted to the player for a level. Casting a spell via a token does NOT
 * consume the player's mana, and works even if the spell has not been unlocked.
 *
 * @param spell The spell that can be cast.
 * @param count How many times the spell can be cast.
 */
data class SupportSpell(
    val spell: SpellType,
    val count: Int = 1,
)

/**
 * Cooldown-based support powers a player can trigger for the whole level.
 *
 * Unlike placable objects and spell tokens (which are consumed), a cooldown power exists once per
 * level and can be re-used after waiting [CooldownPowerType.defaultCooldown] (or a level-configured
 * number of) turns. Each type has a sensible default cooldown used when the level editor does not
 * override it.
 *
 * @param defaultCooldown Default number of turns before the power can be reused; can be overridden
 *   per level in the level editor.
 */
enum class CooldownPowerType(
    val defaultCooldown: Int,
) {
    /** Doubles all coins earned during the turn after the power is activated. */
    COIN_SURGE(5),

    /** All enemy units lose 10 health points. */
    SKY_IS_FALLING(5),

    /** All existing barricades gain 10 health points. */
    CONSTRUCTION_REPAIRS(5),

    /** Grants 10 mana. */
    MANA_WELL(3),

    /** Grants 50 mana. */
    DEEP_MANA_WELL(5),
    ;

    /** True for powers whose only effect is to add mana (pointless while mana is already full). */
    val addsMana: Boolean
        get() = this == MANA_WELL || this == DEEP_MANA_WELL
}

/**
 * A cooldown-based support power granted to the player for a level.
 *
 * @param type          Which power is available.
 * @param cooldownTurns Number of turns the power is unavailable after being used.
 * @param startActive   When true (default), the power is usable at the start of the level and the
 *                      cooldown only begins after the first use. When false, the power starts on
 *                      cooldown at the beginning of the level.
 */
data class CooldownPower(
    val type: CooldownPowerType,
    val cooldownTurns: Int = type.defaultCooldown,
    val startActive: Boolean = true,
)

/**
 * A fief support token granted to the player for a level. Allows the player to place a fief
 * on any valid path tile during gameplay.
 *
 * @param type  Which fief type can be placed.
 * @param count How many of this fief the player may place.
 */
data class SupportFief(
    val type: FiefType,
    val count: Int = 1,
)

/**
 * All player-usable supports (placable objects + spell tokens + cooldown powers + fief tokens) defined for a level.
 * Displayed as boxes above the gameplay buttons and mentioned in the level's story message.
 */
data class LevelSupports(
    val objects: List<SupportObject> = emptyList(),
    val spells: List<SupportSpell> = emptyList(),
    val cooldownPowers: List<CooldownPower> = emptyList(),
    val fiefs: List<SupportFief> = emptyList(),
) {
    fun isEmpty(): Boolean = objects.isEmpty() && spells.isEmpty() && cooldownPowers.isEmpty() && fiefs.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()
}
