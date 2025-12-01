package de.egril.defender.ui.gameplay

import androidx.compose.ui.graphics.Color
import de.egril.defender.model.AttackType
import de.egril.defender.model.Position

/**
 * Information about target circle rendering for a tile.
 * Each tile can either be the central target or a neighbor that needs to draw outer ring segments.
 */
sealed class TargetCircleInfo {
    /**
     * This tile is the central target - draw 3 inner circles
     */
    data class CentralTarget(
        val color: Color,
        val attackType: AttackType,
        val isExtendedArea: Boolean = false  // True if area radius is 2 (level 20+)
    ) : TargetCircleInfo()
    
    /**
     * This tile is a neighbor of the target - draw outer ring segments
     * Only applies to AREA and LASTING attack types
     */
    data class NeighborTarget(
        val color: Color,
        val attackType: AttackType,
        val centerPosition: Position,  // Position of the central target tile
        val thisPosition: Position,    // Position of this neighbor tile
        val distanceFromCenter: Int = 1,  // Distance in hex tiles (1 for inner ring, 2 for outer ring)
        val isExtendedArea: Boolean = false  // True if area radius is 2 (level 20+)
    ) : TargetCircleInfo()
}
