package de.egril.defender.model

/**
 * An active rift portal created by Zythar the Riftcaller's demonlings.
 *
 * When a demonling reaches a position that is either:
 * - within [PORTAL_NEAR_TARGET_DISTANCE] tiles of a target, or
 * - at least [PORTAL_ADVANCE_THRESHOLD] tiles closer to the target than the closest existing
 *   portal exit (or the villain, if no portals exist yet)
 *
 * …the demonling sacrifices itself and creates this portal pair:
 * - [entryPosition]: a tile adjacent to the villain (the "blue" entry rune).
 * - [exitPosition]: the position where the demonling was (the "orange" exit rune).
 *
 * Any enemy unit that steps onto [entryPosition] is instantly teleported to the best free tile
 * adjacent to [exitPosition].  Zythar himself uses the portal whenever the exit is within
 * [PORTAL_NEAR_TARGET_DISTANCE] tiles of a target.
 */
data class Portal(
    val id: Int,
    /** Tile adjacent to the villain — the blue "entry" rune where enemies step in. */
    val entryPosition: Position,
    /** Position where the demonling was when it triggered portal creation — the orange "exit" rune. */
    val exitPosition: Position,
    /** ID of the Zythar villain who owns this portal. */
    val villainId: Int,
    /**
     * Index into the Futhark rune pool used to draw this portal pair.
     * Both the entry and exit tile display the same rune so the player can match them.
     * Distinct portals on the same map cycle through the pool so they look different.
     */
    val runeIndex: Int,
) {
    companion object {
        /** Maximum distance to target for a demonling to create a portal (and for the villain to use one). */
        const val PORTAL_NEAR_TARGET_DISTANCE = 5

        /**
         * Minimum lead (in tiles) by which a demonling must be closer to the target than the
         * current closest portal exit / villain in order to justify creating a new portal.
         */
        const val PORTAL_ADVANCE_THRESHOLD = 20

        /** Number of distinct Futhark rune shapes in the portal drawing pool. */
        const val RUNE_POOL_SIZE = 24
    }
}
