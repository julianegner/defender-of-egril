package de.egril.defender.model

import de.egril.defender.model.Position

/**
 * Represents a mushroom placed on the map during level preparation.
 * Mushrooms boost horde units (goblins, orks, ogres, snotlings) and witches:
 * - 2x walking range for 2 turns
 * - 2x level for 2 turns (witches: can use abilities twice for 2 turns)
 * The mushroom is removed (eaten) when a horde unit or witch steps on it.
 */
data class Mushroom(
    val position: Position,
)
