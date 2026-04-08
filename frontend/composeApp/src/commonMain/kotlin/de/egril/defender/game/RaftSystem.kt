package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import de.egril.defender.config.LogConfig

/**
 * Event types for raft destruction
 */
enum class RaftLossReason {
    MAP_EDGE,
    MAELSTROM,
    OTHER
}

/**
 * Handles raft movement on river tiles.
 * 
 * Raft rules:
 * - Rafts are created when towers are placed on river tiles
 * - Each raft carries exactly one tower
 * - Rafts move according to the river's flow direction and speed
 * - Flow speed determines how many tiles to move (1 or 2)
 * - Rafts are destroyed if moved to a maelstrom
 * - Rafts are destroyed if moved out of map bounds
 * - Bridges prevent raft movement through them
 * - Other rafts prevent movement (cannot pass through)
 * - When multiple rafts in a row, move the one that can move first
 */
class RaftSystem(private val state: GameState) {
    
    // Callback for raft loss events (for achievements)
    var onRaftLost: ((RaftLossReason) -> Unit)? = null
    
    /**
     * Process all raft movements at the end of a turn.
     * Moves rafts according to river flow, handles collisions, and destroys rafts.
     */
    fun processRaftMovements() {
        val raftsToMove = state.rafts.filter { it.isActive }
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("RaftSystem: Processing ${raftsToMove.size} active rafts")
        }
        if (raftsToMove.isEmpty()) return
        
        // Build a dependency graph to determine movement order
        // Rafts that block others should be moved first
        val movementOrder = determineMovementOrder(raftsToMove)
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("RaftSystem: Movement order determined for ${movementOrder.size} rafts")
        }
        
        for (raft in movementOrder) {
            if (!raft.isActive) continue
            
            val defender = state.defenders.find { it.id == raft.defenderId }
            if (defender == null) {
                // Tower was destroyed/sold, destroy the raft
                destroyRaft(raft)
                continue
            }
            
            moveRaft(raft, defender)
        }
        
        // Clean up destroyed rafts
        state.rafts.removeAll { !it.isActive }
    }
    
    /**
     * Determine the order in which rafts should be moved.
     * Rafts downstream (that can move) should be moved before rafts upstream.
     */
    private fun determineMovementOrder(rafts: List<Raft>): List<Raft> {
        val ordered = mutableListOf<Raft>()
        val remaining = rafts.toMutableList()
        
        // Keep iterating until all rafts are processed
        while (remaining.isNotEmpty()) {
            val beforeSize = remaining.size
            
            // Find rafts that can move (not blocked by other remaining rafts)
            val canMove = remaining.filter { raft ->
                val nextPos = calculateNextPosition(raft)
                nextPos == null || !remaining.any { other -> 
                    other != raft && other.currentPosition.value == nextPos 
                }
            }
            
            // Add movable rafts to ordered list and remove from remaining
            ordered.addAll(canMove)
            remaining.removeAll(canMove)
            
            // If no progress was made, break to avoid infinite loop
            // This can happen if rafts are in a circular dependency (shouldn't happen with river flow)
            if (remaining.size == beforeSize) {
                ordered.addAll(remaining)
                break
            }
        }
        
        return ordered
    }
    
    /**
     * Calculate the next position for a raft based on river flow.
     * Moves tile-by-tile, checking each tile's flow direction.
     * Returns null if the raft is blocked or cannot move.
     */
    private fun calculateNextPosition(raft: Raft): Position? {
        val startPos = raft.currentPosition.value
        val startRiverTile = state.level.getRiverTile(startPos) ?: return null
        
        // No flow means no movement
        if (startRiverTile.flowDirection == RiverFlow.NONE || startRiverTile.flowDirection == RiverFlow.MAELSTROM) {
            return null
        }
        
        // Move tile-by-tile based on flow speed
        var currentPos = startPos
        for (step in 1..startRiverTile.flowSpeed) {
            // Get the river tile at current position
            val riverTile = state.level.getRiverTile(currentPos) ?: return currentPos
            
            // Get the next position based on THIS tile's flow direction
            val nextStep = getNextPositionInDirection(currentPos, riverTile.flowDirection)
            
            // Check if there's a bridge blocking the way
            if (state.isBridgeAt(nextStep)) {
                return currentPos  // Return current position if blocked
            }
            
            // Check if the next position is a river tile
            val nextRiverTile = state.level.getRiverTile(nextStep)
            if (nextRiverTile == null) {
                return currentPos  // Can't move to non-river tile
            }
            
            currentPos = nextStep
        }
        
        return if (currentPos != startPos) currentPos else null
    }
    
    /**
     * Move a raft to its next position based on river flow.
     * Moves tile-by-tile, checking direction at each step.
     * Only moves on river tiles.
     */
    private fun moveRaft(raft: Raft, defender: Defender) {
        val startPos = raft.currentPosition.value
        val startRiverTile = state.level.getRiverTile(startPos)
        
        if (startRiverTile == null) {
            // Not on a river tile anymore, destroy the raft
            destroyRaft(raft)
            return
        }
        
        // Check for maelstrom at current position
        if (startRiverTile.flowDirection == RiverFlow.MAELSTROM) {
            println("Raft ${raft.id} destroyed by maelstrom at $startPos")
            destroyRaftAndTower(raft, defender, RaftLossReason.MAELSTROM)
            return
        }
        
        // No flow means no movement
        if (startRiverTile.flowDirection == RiverFlow.NONE) {
            return
        }
        
        // Move tile-by-tile based on flow speed
        var currentPos = startPos
        for (step in 1..startRiverTile.flowSpeed) {
            // Get the river tile at current position
            val riverTile = state.level.getRiverTile(currentPos)
            if (riverTile == null) {
                // Not on a river tile, can't continue movement
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Raft ${raft.id} reached non-river tile at $currentPos")
                }
                break
            }
            
            // Get the next position based on THIS tile's flow direction
            val nextStep = getNextPositionInDirection(currentPos, riverTile.flowDirection)
            
            // Check if next position is out of bounds
            if (!isPositionInBounds(nextStep)) {
                println("Raft ${raft.id} moved out of bounds at $nextStep")
                destroyRaftAndTower(raft, defender, RaftLossReason.MAP_EDGE)
                return
            }
            
            // Check if there's a bridge blocking the way
            if (state.isBridgeAt(nextStep)) {
                // Blocked by bridge, cannot move further
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Raft ${raft.id} blocked by bridge at $nextStep")
                }
                break
            }
            
            // Check if the next position is a river tile
            val nextRiverTile = state.level.getRiverTile(nextStep)
            if (nextRiverTile == null) {
                // Next position is not a river tile, can't move there
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Raft ${raft.id} cannot move to non-river tile at $nextStep")
                }
                break
            }
            
            // Check if another raft is at the destination
            if (state.isRaftAt(nextStep)) {
                // Blocked by another raft, cannot move further
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Raft ${raft.id} blocked by another raft at $nextStep")
                }
                break
            }
            
            // Move to next position
            currentPos = nextStep
            
            // Check if we reached a maelstrom
            if (nextRiverTile.flowDirection == RiverFlow.MAELSTROM) {
                println("Raft ${raft.id} destroyed by maelstrom at $currentPos")
                destroyRaftAndTower(raft, defender, RaftLossReason.MAELSTROM)
                return
            }
        }
        
        // Update raft and defender positions if they moved
        if (currentPos != startPos) {
            println("Moving raft ${raft.id} from $startPos to $currentPos")
            raft.currentPosition.value = currentPos
            defender.position.value = currentPos
        }
    }
    
    /**
     * Get the next position in a given hexagonal direction.
     * Uses odd-row offset coordinates (same as HexUtils).
     */
    private fun getNextPositionInDirection(pos: Position, direction: RiverFlow): Position {
        // Check if row is even or odd (not column!)
        val isOddRow = pos.y % 2 == 1
        
        return when (direction) {
            RiverFlow.EAST -> Position(pos.x + 1, pos.y)
            RiverFlow.NORTH_EAST -> {
                if (isOddRow) {
                    Position(pos.x + 1, pos.y - 1)
                } else {
                    Position(pos.x, pos.y - 1)
                }
            }
            RiverFlow.NORTH_WEST -> {
                if (isOddRow) {
                    Position(pos.x, pos.y - 1)
                } else {
                    Position(pos.x - 1, pos.y - 1)
                }
            }
            RiverFlow.WEST -> Position(pos.x - 1, pos.y)
            RiverFlow.SOUTH_WEST -> {
                if (isOddRow) {
                    Position(pos.x, pos.y + 1)
                } else {
                    Position(pos.x - 1, pos.y + 1)
                }
            }
            RiverFlow.SOUTH_EAST -> {
                if (isOddRow) {
                    Position(pos.x + 1, pos.y + 1)
                } else {
                    Position(pos.x, pos.y + 1)
                }
            }
            RiverFlow.NONE, RiverFlow.MAELSTROM -> pos
        }
    }
    
    /**
     * Check if a position is within map bounds.
     */
    private fun isPositionInBounds(pos: Position): Boolean {
        return pos.x >= 0 && pos.x < state.level.gridWidth &&
               pos.y >= 0 && pos.y < state.level.gridHeight
    }
    
    /**
     * Destroy a raft.
     */
    private fun destroyRaft(raft: Raft) {
        raft.isDestroyed.value = true
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("Raft ${raft.id} destroyed")
        }
    }
    
    /**
     * Destroy a raft and its tower.
     */
    private fun destroyRaftAndTower(raft: Raft, defender: Defender, reason: RaftLossReason = RaftLossReason.OTHER) {
        destroyRaft(raft)
        state.defenders.remove(defender)
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("Tower ${defender.type} destroyed with raft ${raft.id} due to $reason")
        }
        
        // Emit event for achievement tracking
        onRaftLost?.invoke(reason)
    }
}
