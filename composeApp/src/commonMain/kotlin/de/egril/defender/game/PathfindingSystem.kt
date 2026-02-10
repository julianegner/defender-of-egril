package de.egril.defender.game

import de.egril.defender.model.*

/**
 * Handles pathfinding for enemy movement using A* algorithm.
 */
class PathfindingSystem(private val state: GameState) {
    
    companion object {
        // LASTING damage is applied at half the initial damage per turn
        private const val LASTING_DAMAGE_DIVISOR = 2
    }
    
    fun findPath(start: Position, goal: Position, attacker: Attacker? = null): List<Position> {
        if (start == goal) return listOf(start)
        
        val openSet = mutableSetOf(start)
        val cameFrom = mutableMapOf<Position, Position>()
        val gScore = mutableMapOf(start to 0)
        val fScore = mutableMapOf(start to start.distanceTo(goal))
        
        var iterations = 0
        val maxIterations = 1000 // Prevent infinite loops
        
        while (openSet.isNotEmpty() && iterations < maxIterations) {
            iterations++
            
            // Select the position with the lowest fScore
            // If multiple positions have the same fScore, prefer the one closest to the goal (heuristic tiebreaker)
            val current = openSet.minWithOrNull(compareBy<Position> { fScore[it] ?: Int.MAX_VALUE }
                .thenBy { it.distanceTo(goal) }) ?: break
            
            if (current == goal) {
                return reconstructPath(cameFrom, current)
            }
            
            openSet.remove(current)
            
            for (neighbor in getNeighbors(current, goal, attacker)) {
                val moveCost = calculateMoveCost(neighbor, attacker)
                val tentativeGScore = (gScore[current] ?: Int.MAX_VALUE) + moveCost
                
                if (tentativeGScore < (gScore[neighbor] ?: Int.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + neighbor.distanceTo(goal)
                    
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor)
                    }
                }
            }
        }
        
        // No path found or max iterations reached, return simple path towards goal
        return listOf(start, moveTowards(start, goal, attacker))
    }
    
    /**
     * Calculate the cost of moving to a position, considering dangers like acid and tower coverage.
     * Returns higher costs for dangerous positions to encourage safer paths.
     */
    private fun calculateMoveCost(position: Position, attacker: Attacker?): Int {
        var cost = 1  // Base movement cost
        
        // If no attacker info, use basic cost (for compatibility)
        if (attacker == null) return cost
        
        val attackerHealth = attacker.currentHealth.value
        
        // Check for dead-end potential by counting available exit paths
        // This helps avoid getting stuck in branches that don't lead to the goal
        val exitCount = position.getHexNeighbors().count { neighbor ->
            neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
            neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
            (state.level.isOnPath(neighbor) || state.level.isTargetPosition(neighbor)) &&
            !state.level.isBuildIsland(neighbor)
        }
        
        // Penalize positions with few exits (potential dead ends)
        // 1 exit = dead end (100 penalty), 2 exits = corridor (20 penalty), 3+ exits = normal
        when (exitCount) {
            1 -> cost += 100  // Very likely a dead end
            2 -> cost += 20   // Could be a narrow corridor
            // 3+ exits get no penalty
        }
        
        // Check for acid field effects at this position
        val acidEffect = state.fieldEffects.find { 
            it.type == FieldEffectType.ACID && it.position == position 
        }
        if (acidEffect != null) {
            // Acid applies effect.damage each turn a unit stands in it
            // For pathfinding cost calculation, we assume 1 turn of exposure:
            // - Units move through cells one at a time during their movement phase
            // - Even if blocked, they won't choose to stay in acid (will seek alternate paths)
            // - This provides a reasonable heuristic for path cost without over-penalizing
            // Note: The high cost (1000) for lethal acid ensures it's only chosen as last resort
            val acidDamage = acidEffect.damage
            
            // If acid would defeat the unit, add very high cost (but not impossible)
            if (acidDamage >= attackerHealth) {
                cost += 1000  // Very high cost, avoid if possible
            } else {
                // Add cost proportional to the damage (encourage avoiding acid)
                cost += acidDamage * 10
            }
        }
        
        // Check for tower coverage at this position
        var maxTowerDamage = 0
        var totalTowerThreat = 0
        
        for (defender in state.defenders) {
            if (!defender.isReady) continue
            
            val distance = defender.position.value.distanceTo(position)
            
            // Check if position is in tower range
            if (distance >= defender.type.minRange && distance <= defender.range) {
                val potentialDamage = when (defender.type.attackType) {
                    AttackType.LASTING -> {
                        // DOT damage over multiple turns
                        val dotDamagePerTurn = defender.damage / LASTING_DAMAGE_DIVISOR
                        dotDamagePerTurn * defender.dotDuration
                    }
                    else -> defender.damage
                }
                
                maxTowerDamage = maxOf(maxTowerDamage, potentialDamage)
                totalTowerThreat += potentialDamage
            }
        }
        
        if (totalTowerThreat > 0) {
            // If tower damage would defeat the unit, add high cost
            if (maxTowerDamage >= attackerHealth) {
                cost += 500  // High cost for lethal positions
            } else {
                // Add moderate cost for tower coverage (prefer paths outside tower range)
                // Use total threat to account for multiple overlapping towers
                cost += (totalTowerThreat / 10).coerceAtMost(100)
            }
        }
        
        return cost
    }
    
    private fun reconstructPath(cameFrom: Map<Position, Position>, current: Position): List<Position> {
        val path = mutableListOf(current)
        var node = current
        while (cameFrom.containsKey(node)) {
            node = cameFrom[node]!!
            path.add(0, node)
        }
        return path
    }
    
    private fun getNeighbors(pos: Position, goal: Position, attacker: Attacker?): List<Position> {
        // Use hexagonal neighbors instead of square grid
        return pos.getHexNeighbors().filter { neighbor ->
            neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
            neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
            (state.level.isOnPath(neighbor) || 
             state.level.isTargetPosition(neighbor) || 
             isGoalMineForDragon(neighbor, goal, attacker) ||
             isDestroyedMinePosition(neighbor) ||
             state.isBridgeAt(neighbor)) &&  // Bridges are walkable for enemies
            !isBlocked(neighbor, attacker)
        }
    }
    
    /**
     * Check if the position is the goal and it's a mine being targeted by a dragon.
     * This allows dragons to path to mines even if surrounded by non-playable tiles.
     */
    private fun isGoalMineForDragon(pos: Position, goal: Position, attacker: Attacker?): Boolean {
        if (pos != goal) return false
        if (attacker == null || !attacker.type.isDragon) return false
        
        // Check if this is a mine position that the dragon is targeting
        val mine = state.defenders.find { 
            it.type == DefenderType.DWARVEN_MINE && 
            it.position == pos &&
            attacker.targetMineId.value == it.id
        }
        
        return mine != null
    }
    
    /**
     * Check if the position is a destroyed mine.
     * Destroyed mine positions are always valid for dragons to move through.
     */
    private fun isDestroyedMinePosition(pos: Position): Boolean {
        return state.destroyedMinePositions.contains(pos)
    }
    
    private fun isBlocked(pos: Position, attacker: Attacker? = null): Boolean {
        // Check if position has a build island (these block enemies)
        if (state.level.isBuildIsland(pos)) return true
        
        // Check if position has a barricade
        // Flying dragons can move over barricades (like they can fly over non-playable tiles)
        val isFlying = attacker?.isFlying?.value == true
        if (!isFlying) {
            val hasBarricade = state.barricades.any { it.position == pos && !it.isDestroyed() }
            if (hasBarricade) return true
        }
        
        return false
    }
    
    fun moveTowards(from: Position, to: Position, attacker: Attacker? = null): Position {
        // Use hexagonal neighbors to find the best next position
        val hexNeighbors = from.getHexNeighbors()
        
        // Filter to valid neighbors (on path or target, within bounds)
        val pathNeighbors = hexNeighbors.filter { neighbor ->
            neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
            neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
            (state.level.isOnPath(neighbor) || 
             state.level.isTargetPosition(neighbor) || 
             isGoalMineForDragon(neighbor, to, attacker) ||
             isDestroyedMinePosition(neighbor) ||
             state.isBridgeAt(neighbor))  // Bridges are walkable for enemies
        }
        
        // Filter to non-blocked neighbors
        val validNeighbors = pathNeighbors.filter { !isBlocked(it, attacker) }

        // println("Barricade selection VV ${attacker?.id} $validNeighbors")

        /*
         * If there are valid neighbors without barricades, choose the one closest to the goal.
         */
        if (validNeighbors.isNotEmpty()) {
            // Return the neighbor closest to the goal
            return validNeighbors.minByOrNull { it.distanceTo(to) } ?: from
        }

        println("Barricade selection XX")
        
        // No valid neighbors without barricades, check if there are neighbors with barricades
        // Flying dragons can move over barricades, so this only applies to non-flying units
        val isFlying = attacker?.isFlying?.value == true
        if (!isFlying) {
            val neighborsWithBarricades = pathNeighbors.filter { neighbor ->
                // Check if neighbor has a barricade
                state.barricades.any { it.position == neighbor && !it.isDestroyed() }
            }
            
            if (neighborsWithBarricades.isNotEmpty()) {
                // Calculate which barricade is fastest to break through and reach the goal
                // Formula: turns_to_destroy + distance_to_target
                // This considers both barricade strength and position optimally
                
                // Debug logging to understand barricade selection
                if (attacker != null) {
                    println("Barricade selection for ${attacker.type} (damage: ${if (attacker.type.isDragon) attacker.level.value * 5 else attacker.level.value}) at $from to $to:")
                    neighborsWithBarricades.forEach { pos ->
                        val barricade = state.barricades.find { it.position == pos && !it.isDestroyed() }
                        if (barricade != null) {
                            val attackerDamage = if (attacker.type.isDragon) {
                                attacker.level.value * 5
                            } else {
                                attacker.level.value
                            }
                            val turnsToDestroy = (barricade.healthPoints.value + attackerDamage - 1) / attackerDamage
                            val distanceAfter = pos.distanceTo(to)
                            val totalCost = turnsToDestroy + distanceAfter
                            println("  Barricade at $pos: HP=${barricade.healthPoints.value}, turns=$turnsToDestroy, dist=$distanceAfter, total=$totalCost")
                        }
                    }
                }
                
                return neighborsWithBarricades.minWithOrNull(
                    compareBy<Position> { pos ->
                        val barricade = state.barricades.find { it.position == pos && !it.isDestroyed() }
                        if (barricade == null) {
                            return@compareBy Int.MAX_VALUE
                        }
                        
                        // Calculate turns needed to destroy this barricade
                        val attackerDamage = if (attacker?.type?.isDragon == true) {
                            attacker.level.value * 5
                        } else {
                            attacker?.level?.value ?: 1
                        }
                        val turnsToDestroy = (barricade.healthPoints.value + attackerDamage - 1) / attackerDamage // Ceiling division
                        
                        // Distance from this barricade position to the goal
                        val distanceAfter = pos.distanceTo(to)
                        
                        // Total cost: turns to destroy + distance to goal
                        // Example: 25 HP barricade at distance 42 with 1 damage = 25 + 42 = 67
                        // Example: 98 HP barricade at distance 40 with 100 damage = 1 + 40 = 41 (better)
                        turnsToDestroy + distanceAfter
                    }
                ) ?: from
            }
        }
        
        // No valid moves at all, stay in place
        return from
    }
}
