package de.egril.defender.model

/**
 * Types of fiefs (Lehen) that provide regular income per turn.
 * Each fief type has a distinct income per turn and a localization key.
 */
enum class FiefType(
    val incomePerTurn: Int,
    val nameKey: String,
) {
    FISHER(3, "fief_type_fisher"),
    WOODCUTTER(5, "fief_type_woodcutter"),
    QUARRY(7, "fief_type_quarry"),
    MARKETPLACE(20, "fief_type_marketplace"),
}

/**
 * Represents a fief (Lehen) placed on a path tile before the level starts.
 * Fiefs generate regular coin income each turn.
 * They are destroyed when any enemy unit visits their tile
 * (even if the enemy does not end their turn there).
 * They are also destroyed by fireball (AREA) and acid (LASTING) attacks.
 */
data class Fief(
    val position: Position,
    val type: FiefType,
)
