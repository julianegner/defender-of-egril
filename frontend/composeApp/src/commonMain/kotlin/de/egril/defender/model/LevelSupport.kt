package de.egril.defender.model

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
 * All player-usable supports (placable objects + spell tokens) defined for a level.
 * Displayed as boxes above the gameplay buttons and mentioned in the level's story message.
 */
data class LevelSupports(
    val objects: List<SupportObject> = emptyList(),
    val spells: List<SupportSpell> = emptyList(),
) {
    fun isEmpty(): Boolean = objects.isEmpty() && spells.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()
}
