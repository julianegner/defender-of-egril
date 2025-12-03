package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

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
    
    /**
     * Process all raft movements at the end of a turn.
     * Moves rafts according to river flow, handles collisions, and destroys rafts.
     */
    fun processRaftMovements() {
        val raftsToMove = state.rafts.filter { it.isActive }
        if (raftsToMove.isEmpty()) return
        
        // Build a dependency graph to determine movement order
        // Rafts that block others should be moved first
        val movementOrder = determineMovementOrder(raftsToMove)
        
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
     * Returns null if the raft is blocked by a bridge.
     * Does not check for out-of-bounds (that's handled in moveRaft).
     */
    private fun calculateNextPosition(raft: Raft): Position? {
        val currentPos = raft.currentPosition.value
        val riverTile = state.level.getRiverTile(currentPos) ?: return null
        
        // No flow means no movement
        if (riverTile.flowDirection == RiverFlow.NONE || riverTile.flowDirection == RiverFlow.MAELSTROM) {
            return null
        }
        
        // Calculate movement based on flow direction and speed
        var nextPos = currentPos
        for (step in 1..riverTile.flowSpeed) {
            val nextStep = getNextPositionInDirection(nextPos, riverTile.flowDirection)
            
            // Check if there's a bridge blocking the way
            if (state.isBridgeAt(nextStep)) {
                return null  // Cannot move past bridge
            }
            
            nextPos = nextStep
        }
        
        return nextPos
    }
    
    /**
     * Move a raft to its next position based on river flow.
     */
    private fun moveRaft(raft: Raft, defender: Defender) {
        val currentPos = raft.currentPosition.value
        val riverTile = state.level.getRiverTile(currentPos)
        
        if (riverTile == null) {
            // Not on a river tile anymore, destroy the raft
            destroyRaft(raft)
            return
        }
        
        // Check for maelstrom
        if (riverTile.flowDirection == RiverFlow.MAELSTROM) {
            println("Raft ${raft.id} destroyed by maelstrom at $currentPos")
            destroyRaftAndTower(raft, defender)
            return
        }
        
        // No flow means no movement
        if (riverTile.flowDirection == RiverFlow.NONE) {
            return
        }
        
        // Calculate where the raft would move
        var nextPos = currentPos
        for (step in 1..riverTile.flowSpeed) {
            val nextStep = getNextPositionInDirection(nextPos, riverTile.flowDirection)
            
            // Check if next position is out of bounds
            if (!isPositionInBounds(nextStep)) {
                println("Raft ${raft.id} moved out of bounds at $nextStep")
                destroyRaftAndTower(raft, defender)
                return
            }
            
            // Check if there's a bridge blocking the way
            if (state.isBridgeAt(nextStep)) {
                // Blocked by bridge, cannot move
                return
            }
            
            nextPos = nextStep
        }
        
        // Check if another raft is at the destination
        if (state.isRaftAt(nextPos) && nextPos != currentPos) {
            // Blocked by another raft, cannot move
            return
        }
        
        // Move the raft and tower
        println("Moving raft ${raft.id} from $currentPos to $nextPos (flow: ${riverTile.flowDirection}, speed: ${riverTile.flowSpeed})")
        raft.currentPosition.value = nextPos
        defender.position.value = nextPos
    }
    
    /**
     * Get the next position in a given hexagonal direction.
     */
    private fun getNextPositionInDirection(pos: Position, direction: RiverFlow): Position {
        return when (direction) {
            RiverFlow.NORTH_EAST -> {
                if (pos.x % 2 == 0) {
                    Position(pos.x + 1, pos.y - 1)
                } else {
                    Position(pos.x + 1, pos.y)
                }
            }
            RiverFlow.EAST -> Position(pos.x + 1, pos.y)
            RiverFlow.SOUTH_EAST -> {
                if (pos.x % 2 == 0) {
                    Position(pos.x + 1, pos.y)
                } else {
                    Position(pos.x + 1, pos.y + 1)
                }
            }
            RiverFlow.SOUTH_WEST -> {
                if (pos.x % 2 == 0) {
                    Position(pos.x - 1, pos.y)
                } else {
                    Position(pos.x - 1, pos.y + 1)
                }
            }
            RiverFlow.WEST -> Position(pos.x - 1, pos.y)
            RiverFlow.NORTH_WEST -> {
                if (pos.x % 2 == 0) {
                    Position(pos.x - 1, pos.y - 1)
                } else {
                    Position(pos.x - 1, pos.y)
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
        println("Raft ${raft.id} destroyed")
    }
    
    /**
     * Destroy a raft and its tower.
     */
    private fun destroyRaftAndTower(raft: Raft, defender: Defender) {
        destroyRaft(raft)
        state.defenders.remove(defender)
        println("Tower ${defender.type} destroyed with raft ${raft.id}")
    }
}
