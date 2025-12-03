package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Represents a raft carrying a tower on a river tile.
 * 
 * Rafts are created when towers are placed on river tiles.
 * They move according to the river's flow direction and speed.
 * 
 * Destruction conditions:
 * - Moved to a maelstrom tile
 * - Moved outside map boundaries
 * 
 * Movement blocking:
 * - Bridges spanning the river prevent movement
 * - Other rafts in the way prevent movement
 */
data class Raft(
    val id: Int,
    val defenderId: Int,  // The tower on this raft
    val currentPosition: MutableState<Position> = mutableStateOf(Position(0, 0)),
    val isDestroyed: MutableState<Boolean> = mutableStateOf(false)
) {
    /**
     * Check if the raft is still active (not destroyed)
     */
    val isActive: Boolean get() = !isDestroyed.value
}
